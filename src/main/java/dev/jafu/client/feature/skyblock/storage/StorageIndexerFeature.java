package dev.jafu.client.feature.skyblock.storage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.jafu.client.module.JafuModules;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class StorageIndexerFeature {
    private static final long PENDING_STORAGE_TTL_MILLIS = 5_000L;
    private static final int MAX_TOOLTIP_ROWS = 12;

    private static PendingStorage pendingStorage;
    private static String lastCaptureFingerprint = "";

    private StorageIndexerFeature() {
    }

    public static void rememberClickedStorage(ItemStack stack) {
        if (!enabled() || stack == null || stack.isEmpty() || !isStorageStack(stack)) {
            return;
        }

        long now = System.currentTimeMillis();
        pendingStorage = new PendingStorage(keysForStack(stack), clean(stack.getName().getString()), now);
    }

    public static void captureScreen(String rawTitle, ScreenHandler handler, PlayerInventory playerInventory) {
        if (!enabled() || handler == null || playerInventory == null) {
            return;
        }

        long now = System.currentTimeMillis();
        String title = clean(rawTitle);
        boolean storageTitle = isStorageTitle(title);
        PendingStorage pending = activePending(now);
        if (!storageTitle && pending == null) {
            return;
        }

        List<StorageIndexedItem> items = captureContainerItems(handler, playerInventory);
        String fingerprint = handler.syncId + ":" + title + ":" + items;
        if (fingerprint.equals(lastCaptureFingerprint)) {
            return;
        }
        lastCaptureFingerprint = fingerprint;

        Set<String> keys = new LinkedHashSet<>();
        keys.add(titleKey(title));
        globalKeyForTitle(title).ifPresent(keys::add);
        if (pending != null) {
            keys.addAll(pending.keys());
        }
        if (keys.isEmpty()) {
            return;
        }

        String sourceName = pending == null ? title : pending.sourceName();
        StorageSnapshot snapshot = new StorageSnapshot(title, sourceName, now, items);
        StorageIndexStore.INSTANCE.put(keys, snapshot);
    }

    public static void appendTooltip(ItemStack stack, List<Text> tooltip) {
        if (!enabled() || stack == null || stack.isEmpty()) {
            return;
        }

        Optional<StorageSnapshot> snapshot = StorageIndexStore.INSTANCE.find(keysForStack(stack));
        if (snapshot.isEmpty() && !isStorageStack(stack)) {
            return;
        }

        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("JAFU Storage Index").styled(style -> style.withColor(0xFFAA00)));
        if (snapshot.isEmpty()) {
            tooltip.add(Text.literal("Open this storage once to cache contents.").formatted(Formatting.GRAY));
            return;
        }

        StorageSnapshot cached = snapshot.get();
        tooltip.add(Text.literal(cached.sourceName() + " - " + age(cached.capturedAtMillis())).formatted(Formatting.GRAY));
        tooltip.add(Text.literal(cached.occupiedSlots() + " slots, " + cached.totalItems() + " items cached").formatted(Formatting.DARK_GRAY));

        if (cached.items().isEmpty()) {
            tooltip.add(Text.literal("No items were cached last open.").formatted(Formatting.DARK_GRAY));
            return;
        }

        int shown = Math.min(MAX_TOOLTIP_ROWS, cached.items().size());
        for (int i = 0; i < shown; i++) {
            StorageIndexedItem item = cached.items().get(i);
            tooltip.add(itemLine(item));
        }
        int remaining = cached.items().size() - shown;
        if (remaining > 0) {
            tooltip.add(Text.literal("+ " + remaining + " more cached items").formatted(Formatting.DARK_GRAY));
        }
    }

    private static List<StorageIndexedItem> captureContainerItems(ScreenHandler handler, PlayerInventory playerInventory) {
        List<StorageIndexedItem> items = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (slot.inventory == playerInventory) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                continue;
            }

            String name = clean(stack.getName().getString());
            if (isMenuButton(name)) {
                continue;
            }
            items.add(new StorageIndexedItem(slot.id, name, stack.getCount()));
        }
        return List.copyOf(items);
    }

    private static MutableText itemLine(StorageIndexedItem item) {
        MutableText line = Text.literal("  " + item.displayName()).formatted(Formatting.GRAY);
        return line;
    }

    private static List<String> keysForStack(ItemStack stack) {
        List<String> keys = new ArrayList<>();
        keys.add(stackKey(stack));
        String name = clean(stack.getName().getString());
        keys.add(nameKey(name));
        globalKeyForName(name).ifPresent(keys::add);
        return List.copyOf(keys);
    }

    private static String stackKey(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        int hash = ItemStack.hashCode(stack.copyWithCount(1));
        return "stack:" + id + ":" + Integer.toHexString(hash);
    }

    private static String nameKey(String name) {
        return "name:" + normalize(name);
    }

    private static String titleKey(String title) {
        return "title:" + normalize(title);
    }

    private static Optional<String> globalKeyForTitle(String title) {
        return globalKey(normalize(title));
    }

    private static Optional<String> globalKeyForName(String name) {
        return globalKey(normalize(name));
    }

    private static Optional<String> globalKey(String normalized) {
        if (normalized.contains("ender chest")) {
            return Optional.of("global:ender_chest");
        }
        if (normalized.contains("sack")) {
            return Optional.of("global:sacks");
        }
        if (normalized.contains("storage")) {
            return Optional.of("global:storage");
        }
        return Optional.empty();
    }

    private static boolean isStorageStack(ItemStack stack) {
        return isStorageName(clean(stack.getName().getString()));
    }

    private static boolean isStorageTitle(String title) {
        return isStorageName(title);
    }

    private static boolean isStorageName(String name) {
        String normalized = normalize(name);
        return normalized.contains("backpack")
                || normalized.contains("ender chest")
                || normalized.contains("storage")
                || normalized.contains("sack")
                || normalized.contains("personal vault");
    }

    private static boolean isMenuButton(String name) {
        String normalized = normalize(name);
        return normalized.equals("close")
                || normalized.equals("go back")
                || normalized.equals("back")
                || normalized.equals("previous page")
                || normalized.equals("next page")
                || normalized.equals("search")
                || normalized.equals("sort")
                || normalized.equals("filter");
    }

    private static PendingStorage activePending(long nowMillis) {
        if (pendingStorage == null || nowMillis - pendingStorage.capturedAtMillis() > PENDING_STORAGE_TTL_MILLIS) {
            pendingStorage = null;
            return null;
        }
        return pendingStorage;
    }

    private static boolean enabled() {
        return StorageIndexerSettings.INSTANCE.enabled() && JafuModules.isEnabled(JafuModules.STORAGE_INDEXER);
    }

    private static String age(long capturedAtMillis) {
        long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - capturedAtMillis) / 1000L);
        if (elapsedSeconds < 10L) {
            return "just now";
        }
        if (elapsedSeconds < 60L) {
            return elapsedSeconds + "s ago";
        }
        long minutes = elapsedSeconds / 60L;
        if (minutes < 60L) {
            return minutes + "m ago";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + "h ago";
        }
        return (hours / 24L) + "d ago";
    }

    private static String clean(String value) {
        return value == null ? "" : value
                .replaceAll("Â§.", "")
                .replaceAll("Ã‚Â§.", "")
                .replaceAll("[\\p{Cc}\\p{Cf}]", "")
                .trim();
    }

    private static String normalize(String value) {
        return clean(value).toLowerCase().replaceAll("\\s+", " ");
    }

    private record PendingStorage(List<String> keys, String sourceName, long capturedAtMillis) {
    }
}
