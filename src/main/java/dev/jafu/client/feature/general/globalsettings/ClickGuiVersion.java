package dev.jafu.client.feature.general.globalsettings;

public enum ClickGuiVersion {
    V1("v1", "V1"),
    V2("v2", "V2");

    private final String id;
    private final String label;

    ClickGuiVersion(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static ClickGuiVersion fromId(String id) {
        for (ClickGuiVersion version : values()) {
            if (version.id.equals(id)) {
                return version;
            }
        }
        return V1;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
