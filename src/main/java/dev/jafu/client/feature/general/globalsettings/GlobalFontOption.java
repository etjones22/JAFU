package dev.jafu.client.feature.general.globalsettings;

import dev.jafu.client.JafuClient;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

public enum GlobalFontOption {
    CLEAN("clean", "Clean", Identifier.of(JafuClient.MOD_ID, "clean")),
    DEFAULT("default", "Default", Identifier.of("minecraft", "default")),
    UNIFORM("uniform", "Uniform", Identifier.of("minecraft", "uniform")),
    ALT("alt", "Alt", Identifier.of("minecraft", "alt"));

    private final String id;
    private final String label;
    private final StyleSpriteSource.Font font;

    GlobalFontOption(String id, String label, Identifier identifier) {
        this.id = id;
        this.label = label;
        this.font = new StyleSpriteSource.Font(identifier);
    }

    public static GlobalFontOption fromId(String id) {
        for (GlobalFontOption option : values()) {
            if (option.id.equals(id)) {
                return option;
            }
        }
        return CLEAN;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public StyleSpriteSource.Font font() {
        return font;
    }
}
