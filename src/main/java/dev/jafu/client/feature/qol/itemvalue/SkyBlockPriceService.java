package dev.jafu.client.feature.qol.itemvalue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class SkyBlockPriceService {
    public static final SkyBlockPriceService INSTANCE = new SkyBlockPriceService();

    private static final String BAZAAR_URL = "https://api.hypixel.net/v2/skyblock/bazaar";
    private static final String AUCTIONS_URL = "https://api.hypixel.net/v2/skyblock/auctions?page=";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(12L);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new PriceThreadFactory());
    private final AtomicBoolean refreshRunning = new AtomicBoolean();

    private volatile Map<String, ItemPriceEstimate.BazaarPrice> bazaarPrices = Map.of();
    private volatile Map<String, ItemPriceEstimate.AuctionPrice> auctionPrices = Map.of();
    private volatile long updatedAtMillis;

    private SkyBlockPriceService() {
    }

    public ItemPriceEstimate estimate(SkyBlockItemKey key) {
        refreshIfNeeded(false);
        ItemPriceEstimate.BazaarPrice bazaarPrice = bazaarPrices.get(key.productId());
        ItemPriceEstimate.AuctionPrice auctionPrice = auctionPrices.get(SkyBlockItemKey.auctionLookupKey(key.auctionName()));
        return new ItemPriceEstimate(key.productId(), bazaarPrice, auctionPrice, refreshRunning.get(), updatedAtMillis);
    }

    public PriceCacheStatus status() {
        return new PriceCacheStatus(bazaarPrices.size(), auctionPrices.size(), refreshRunning.get(), updatedAtMillis);
    }

    public void refreshIfNeeded(boolean force) {
        long nowMillis = System.currentTimeMillis();
        long refreshIntervalMillis = Math.max(1, ItemValueOverlaySettings.INSTANCE.intValue(ItemValueNumericSetting.REFRESH_MINUTES)) * 60_000L;
        if (!force && updatedAtMillis > 0L && nowMillis - updatedAtMillis < refreshIntervalMillis) {
            return;
        }
        if (!refreshRunning.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, ItemPriceEstimate.BazaarPrice> nextBazaarPrices = ItemValueOverlaySettings.INSTANCE.isVisible(ItemValueOverlayOption.SHOW_BAZAAR)
                        ? fetchBazaarPrices()
                        : Map.of();
                Map<String, ItemPriceEstimate.AuctionPrice> nextAuctionPrices = ItemValueOverlaySettings.INSTANCE.isVisible(ItemValueOverlayOption.SHOW_AUCTION)
                        ? fetchAuctionPrices()
                        : Map.of();
                bazaarPrices = nextBazaarPrices;
                auctionPrices = nextAuctionPrices;
                updatedAtMillis = System.currentTimeMillis();
            } catch (Exception ignored) {
                if (updatedAtMillis == 0L) {
                    updatedAtMillis = System.currentTimeMillis();
                }
            } finally {
                refreshRunning.set(false);
            }
        }, executor);
    }

    private Map<String, ItemPriceEstimate.BazaarPrice> fetchBazaarPrices() throws IOException, InterruptedException {
        HttpResponse<String> response = send(BAZAAR_URL);
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject products = root.getAsJsonObject("products");
        if (products == null) {
            return Map.of();
        }

        Map<String, ItemPriceEstimate.BazaarPrice> prices = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject product = entry.getValue().getAsJsonObject();
            JsonObject quickStatus = product.getAsJsonObject("quick_status");
            if (quickStatus == null) {
                continue;
            }

            double sellPrice = doubleValue(quickStatus, "sellPrice");
            double buyPrice = doubleValue(quickStatus, "buyPrice");
            if (sellPrice > 0.0D || buyPrice > 0.0D) {
                prices.put(entry.getKey().toUpperCase(Locale.ROOT), new ItemPriceEstimate.BazaarPrice(sellPrice, buyPrice));
            }
        }
        return Map.copyOf(prices);
    }

    private Map<String, ItemPriceEstimate.AuctionPrice> fetchAuctionPrices() throws IOException, InterruptedException {
        int maxAuctionPages = ItemValueOverlaySettings.INSTANCE.intValue(ItemValueNumericSetting.AUCTION_PAGES);
        if (maxAuctionPages <= 0) {
            return Map.of();
        }

        Map<String, MutableAuctionPrice> mutablePrices = new HashMap<>();
        JsonObject firstPage = fetchAuctionPage(0);
        int totalPages = Math.min(intValue(firstPage, "totalPages", 1), maxAuctionPages);
        readAuctionPage(firstPage, mutablePrices);
        for (int page = 1; page < totalPages; page++) {
            readAuctionPage(fetchAuctionPage(page), mutablePrices);
        }

        Map<String, ItemPriceEstimate.AuctionPrice> prices = new HashMap<>();
        for (Map.Entry<String, MutableAuctionPrice> entry : mutablePrices.entrySet()) {
            MutableAuctionPrice price = entry.getValue();
            if (price.lowestBin > 0.0D) {
                prices.put(entry.getKey(), new ItemPriceEstimate.AuctionPrice(price.lowestBin, price.samples));
            }
        }
        return Map.copyOf(prices);
    }

    private JsonObject fetchAuctionPage(int page) throws IOException, InterruptedException {
        HttpResponse<String> response = send(AUCTIONS_URL + page);
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private void readAuctionPage(JsonObject page, Map<String, MutableAuctionPrice> mutablePrices) {
        JsonArray auctions = page.getAsJsonArray("auctions");
        if (auctions == null) {
            return;
        }

        for (JsonElement element : auctions) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject auction = element.getAsJsonObject();
            if (!booleanValue(auction, "bin")) {
                continue;
            }

            String key = SkyBlockItemKey.auctionLookupKey(stringValue(auction, "item_name"));
            double price = doubleValue(auction, "starting_bid");
            if (key.isBlank() || price <= 0.0D) {
                continue;
            }

            mutablePrices.computeIfAbsent(key, ignored -> new MutableAuctionPrice()).add(price);
        }
    }

    private HttpResponse<String> send(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", "JAFU-Item-Value-Overlay")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Hypixel price lookup returned HTTP " + response.statusCode());
        }
        return response;
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

    private static double doubleValue(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? 0.0D : element.getAsDouble();
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
    }

    private static boolean booleanValue(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }

    private static final class MutableAuctionPrice {
        private double lowestBin = Double.MAX_VALUE;
        private int samples;

        private void add(double price) {
            lowestBin = Math.min(lowestBin, price);
            samples++;
        }
    }

    private static final class PriceThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "JAFU Item Value Prices");
            thread.setDaemon(true);
            return thread;
        }
    }
}
