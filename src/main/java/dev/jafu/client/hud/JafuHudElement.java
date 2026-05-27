package dev.jafu.client.hud;

public record JafuHudElement(
        String id,
        String moduleId,
        String title,
        int defaultX,
        int defaultY,
        int width,
        int height
) {
}
