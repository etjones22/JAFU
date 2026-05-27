package dev.jafu.client.feature.general.updater;

public enum UpdateChannel {
    STABLE("stable", "Stable"),
    SNAPSHOT("snapshot", "Snapshot");

    private final String id;
    private final String label;

    UpdateChannel(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public UpdateChannel next() {
        return this == STABLE ? SNAPSHOT : STABLE;
    }

    public static UpdateChannel fromId(String id) {
        if ("dev".equalsIgnoreCase(id)) {
            return SNAPSHOT;
        }

        for (UpdateChannel channel : values()) {
            if (channel.id.equalsIgnoreCase(id)) {
                return channel;
            }
        }
        return STABLE;
    }
}
