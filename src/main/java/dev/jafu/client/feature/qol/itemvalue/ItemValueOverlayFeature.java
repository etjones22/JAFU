package dev.jafu.client.feature.qol.itemvalue;

import java.text.DecimalFormat;
import java.util.List;

import dev.jafu.client.module.JafuModules;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ItemValueOverlayFeature {
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.#");

    private ItemValueOverlayFeature() {
    }

    public static void appendTooltip(ItemStack stack, List<Text> tooltip) {
        if (!enabled() || stack == null || stack.isEmpty()) {
            return;
        }

        SkyBlockItemKey key = SkyBlockItemKey.from(stack);
        ItemPriceEstimate estimate = SkyBlockPriceService.INSTANCE.estimate(key);
        if (!estimate.hasAnyPrice() && !estimate.loading()) {
            return;
        }

        boolean showBazaar = ItemValueOverlaySettings.INSTANCE.isVisible(ItemValueOverlayOption.SHOW_BAZAAR);
        boolean showAuction = ItemValueOverlaySettings.INSTANCE.isVisible(ItemValueOverlayOption.SHOW_AUCTION);
        boolean showLoading = ItemValueOverlaySettings.INSTANCE.isVisible(ItemValueOverlayOption.SHOW_LOADING_STATUS);

        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("JAFU Item Value").styled(style -> style.withColor(0xFFAA00)));

        if (showLoading && estimate.loading() && !estimate.hasAnyPrice()) {
            tooltip.add(Text.literal("Loading Hypixel prices...").formatted(Formatting.GRAY));
            return;
        }

        if (showBazaar && estimate.bazaarPrice() != null) {
            addBazaarLines(stack, tooltip, estimate.bazaarPrice());
        }
        if (showAuction && estimate.auctionPrice() != null) {
            tooltip.add(Text.literal("AH lowest BIN: " + coins(estimate.auctionPrice().lowestBin())
                    + " (" + estimate.auctionPrice().samples() + " samples)").formatted(Formatting.GRAY));
        } else if (showAuction && estimate.bazaarPrice() == null) {
            tooltip.add(Text.literal("AH estimate: not in current cache").formatted(Formatting.DARK_GRAY));
        }
        if (showLoading && estimate.updatedAtMillis() > 0L) {
            tooltip.add(Text.literal("Prices updated " + age(estimate.updatedAtMillis())).formatted(Formatting.DARK_GRAY));
        }
    }

    public static PriceCacheStatus status() {
        return SkyBlockPriceService.INSTANCE.status();
    }

    private static void addBazaarLines(ItemStack stack, List<Text> tooltip, ItemPriceEstimate.BazaarPrice price) {
        int count = Math.max(1, stack.getCount());
        tooltip.add(Text.literal("Bazaar sell: " + coins(price.instantSell())).formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Bazaar buy: " + coins(price.instantBuy())).formatted(Formatting.GRAY));
        if (count > 1 && ItemValueOverlaySettings.INSTANCE.isVisible(ItemValueOverlayOption.SHOW_STACK_VALUE)) {
            tooltip.add(Text.literal("Stack sell: " + coins(price.instantSell() * count)).formatted(Formatting.DARK_GRAY));
        }
    }

    private static String coins(double value) {
        return PRICE_FORMAT.format(Math.max(0.0D, value)) + " coins";
    }

    private static boolean enabled() {
        return ItemValueOverlaySettings.INSTANCE.enabled() && JafuModules.isEnabled(JafuModules.ITEM_VALUE_OVERLAY);
    }

    private static String age(long updatedAtMillis) {
        long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - updatedAtMillis) / 1000L);
        if (elapsedSeconds < 60L) {
            return elapsedSeconds + "s ago";
        }
        long minutes = elapsedSeconds / 60L;
        if (minutes < 60L) {
            return minutes + "m ago";
        }
        return (minutes / 60L) + "h ago";
    }
}
