package dev.jafu.client.feature.garden.rng;

public enum GardenDropFamily {
    FEAST("feast", "Feast"),
    ARMOR("armor", "Armor drops"),
    OTHER("other", "Other");

    private final String id;
    private final String label;

    GardenDropFamily(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
