package dev.jafu.client.feature.general.chat;

import java.util.List;

public enum ChatEnhancementOption {
    SMOOTH_CHAT("smooth_chat", "Smooth chat"),
    HIDE_REPEATED_MESSAGES("hide_repeated_messages", "Hide repeated messages"),
    HIDE_SPAM("hide_spam", "Hide spam");

    private static final List<ChatEnhancementOption> VALUES = List.of(values());

    private final String id;
    private final String label;

    ChatEnhancementOption(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static List<ChatEnhancementOption> all() {
        return VALUES;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
