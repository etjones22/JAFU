package dev.jafu.client.feature.general.globalsettings;

public enum HudStylePreset {
    REFERENCE("reference", "Reference"),
    CLEAN("clean", "Clean"),
    MINIMAL("minimal", "Minimal");

    private final String id;
    private final String label;

    HudStylePreset(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static HudStylePreset fromId(String id) {
        for (HudStylePreset preset : values()) {
            if (preset.id.equals(id)) {
                return preset;
            }
        }
        return REFERENCE;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
