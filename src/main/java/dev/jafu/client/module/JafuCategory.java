package dev.jafu.client.module;

public enum JafuCategory {
    GENERAL("General", "General Updates"),
    SKYBLOCK("SkyBlock", "SkyBlock Updates"),
    MINING("Mining", "Mining Updates"),
    DUNGEONS("Dungeons", "Dungeon Updates"),
    GARDEN("Garden", "Garden Updates"),
    CREDITS("Credits", "Credits");

    private final String label;
    private final String title;

    JafuCategory(String label, String title) {
        this.label = label;
        this.title = title;
    }

    public String label() {
        return label;
    }

    public String title() {
        return title;
    }
}
