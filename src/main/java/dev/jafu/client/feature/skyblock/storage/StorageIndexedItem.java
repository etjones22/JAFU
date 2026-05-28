package dev.jafu.client.feature.skyblock.storage;

public record StorageIndexedItem(int slot, String name, int count) {
    public String displayName() {
        return count > 1 ? count + "x " + name : name;
    }
}
