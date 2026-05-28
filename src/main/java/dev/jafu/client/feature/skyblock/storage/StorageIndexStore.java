package dev.jafu.client.feature.skyblock.storage;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;

import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import net.fabricmc.loader.api.FabricLoader;

public final class StorageIndexStore implements JafuConfigurable {
    public static final StorageIndexStore INSTANCE = new StorageIndexStore();

    private static final String CONFIG_ID = "storage-index";
    private static final int MAX_KEYS = 256;

    private final Path path = FabricLoader.getInstance().getConfigDir().resolve("jafu-storage-index.properties");
    private final Map<String, StorageSnapshot> snapshots = new HashMap<>();

    private StorageIndexStore() {
        JafuConfigManager.register(this);
        load();
    }

    public synchronized Optional<StorageSnapshot> find(Collection<String> keys) {
        for (String key : keys) {
            StorageSnapshot snapshot = snapshots.get(key);
            if (snapshot != null) {
                return Optional.of(snapshot);
            }
        }
        return Optional.empty();
    }

    public synchronized int snapshotKeyCount() {
        return snapshots.size();
    }

    public synchronized Optional<StorageSnapshot> newestSnapshot() {
        return snapshots.values().stream()
                .max(Comparator.comparingLong(StorageSnapshot::capturedAtMillis));
    }

    public synchronized void put(Collection<String> keys, StorageSnapshot snapshot) {
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                snapshots.put(key, snapshot);
            }
        }
        prune();
        save();
    }

    @Override
    public String configId() {
        return CONFIG_ID;
    }

    @Override
    public synchronized boolean load() {
        snapshots.clear();
        if (!Files.isRegularFile(path)) {
            return true;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        } catch (IOException ignored) {
            return false;
        }

        for (String encodedKey : split(properties.getProperty("keys", ""))) {
            String key = decode(encodedKey);
            if (key == null) {
                continue;
            }

            String prefix = "entry." + encodedKey + ".";
            String title = properties.getProperty(prefix + "title", "Storage");
            String source = properties.getProperty(prefix + "source", title);
            long capturedAt = parseLong(properties.getProperty(prefix + "captured_at"), 0L);
            List<StorageIndexedItem> items = decodeItems(properties.getProperty(prefix + "items", ""));
            snapshots.put(key, new StorageSnapshot(title, source, capturedAt, items));
        }
        prune();
        return true;
    }

    @Override
    public synchronized boolean save() {
        Properties properties = new Properties();
        StringJoiner keys = new StringJoiner(",");
        for (Map.Entry<String, StorageSnapshot> entry : snapshots.entrySet()) {
            String encodedKey = encode(entry.getKey());
            keys.add(encodedKey);
            String prefix = "entry." + encodedKey + ".";
            StorageSnapshot snapshot = entry.getValue();
            properties.setProperty(prefix + "title", snapshot.title());
            properties.setProperty(prefix + "source", snapshot.sourceName());
            properties.setProperty(prefix + "captured_at", Long.toString(snapshot.capturedAtMillis()));
            properties.setProperty(prefix + "items", encodeItems(snapshot.items()));
        }
        properties.setProperty("keys", keys.toString());

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer, "JAFU storage index");
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void prune() {
        if (snapshots.size() <= MAX_KEYS) {
            return;
        }

        List<String> orderedKeys = snapshots.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().capturedAtMillis()))
                .map(Map.Entry::getKey)
                .toList();
        int removeCount = snapshots.size() - MAX_KEYS;
        for (int i = 0; i < removeCount; i++) {
            snapshots.remove(orderedKeys.get(i));
        }
    }

    private static String encodeItems(List<StorageIndexedItem> items) {
        StringJoiner joiner = new StringJoiner("|");
        for (StorageIndexedItem item : items) {
            joiner.add(item.slot() + ":" + item.count() + ":" + encode(item.name()));
        }
        return joiner.toString();
    }

    private static List<StorageIndexedItem> decodeItems(String encodedItems) {
        List<StorageIndexedItem> items = new ArrayList<>();
        if (encodedItems == null || encodedItems.isBlank()) {
            return List.of();
        }
        for (String encodedItem : encodedItems.split("\\|")) {
            if (encodedItem.isBlank()) {
                continue;
            }
            String[] parts = encodedItem.split(":", 3);
            if (parts.length != 3) {
                continue;
            }
            String name = decode(parts[2]);
            if (name == null) {
                continue;
            }
            items.add(new StorageIndexedItem(parseInt(parts[0], 0), name, parseInt(parts[1], 1)));
        }
        return List.copyOf(items);
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                parts.add(part.trim());
            }
        }
        return parts;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
