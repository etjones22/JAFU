package dev.jafu.client.module;

public final class JafuModule {
    private final String id;
    private final JafuCategory category;
    private final String name;
    private final String description;
    private final int color;
    private boolean enabled;

    public JafuModule(String id, JafuCategory category, String name, String description, boolean enabled, int color) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public JafuCategory category() {
        return category;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public int color() {
        return color;
    }

    public boolean enabled() {
        return enabled;
    }

    public void toggle() {
        enabled = !enabled;
    }
}
