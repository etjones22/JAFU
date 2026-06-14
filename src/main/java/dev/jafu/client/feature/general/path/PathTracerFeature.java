package dev.jafu.client.feature.general.path;

import dev.jafu.client.JafuClient;
import dev.jafu.client.feature.general.updater.AutoUpdater;
import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.gui.util.GuiDraw;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class PathTracerFeature {
    private static final double REACHED_DISTANCE = 2.0D;
    private static final int ACCENT = 0xFFFFAA00;
    private static final int MUTED_ACCENT = 0x88FFAA00;
    private static final int LINE_RGB = 0xFFAA00;
    private static PathTarget target;

    private PathTracerFeature() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(PathTracerFeature::tick);
        HudElementRegistry.addLast(Identifier.of(JafuClient.MOD_ID, "path_tracer"), PathTracerFeature::render);
    }

    public static void setTarget(double x, double y, double z) {
        target = new PathTarget(x, y, z);
        AutoUpdater.notifyPlayer("Path tracer set to " + formatCoordinate(x) + " " + formatCoordinate(y) + " " + formatCoordinate(z) + ".");
    }

    public static void removeTarget(boolean notify) {
        boolean hadTarget = target != null;
        target = null;
        if (notify) {
            AutoUpdater.notifyPlayer(hadTarget ? "Path tracer removed." : "No active path tracer.");
        }
    }

    private static void tick(MinecraftClient client) {
        PathTarget activeTarget = target;
        if (activeTarget == null || client.player == null) {
            return;
        }

        if (distanceTo(client.player, activeTarget) <= REACHED_DISTANCE) {
            target = null;
            AutoUpdater.notifyPlayer("Path target reached.");
        }
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        PathTarget activeTarget = target;
        if (activeTarget == null || client.player == null || client.options.hudHidden) {
            return;
        }

        ClientPlayerEntity player = client.player;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.max(38, Math.min(width, height) / 5);

        double relativeYaw = relativeYaw(player, activeTarget);
        double radians = Math.toRadians(relativeYaw);
        int targetX = centerX + (int) Math.round(Math.sin(radians) * radius);
        int targetY = centerY - (int) Math.round(Math.cos(radians) * radius);

        drawTracerLine(context, centerX, centerY, targetX, targetY);
        drawArrowHead(context, centerX, centerY, targetX, targetY);
        drawTargetLabel(context, client.textRenderer, activeTarget, player, targetX, targetY);
    }

    private static double relativeYaw(ClientPlayerEntity player, PathTarget activeTarget) {
        double dx = activeTarget.x() - player.getX();
        double dz = activeTarget.z() - player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0D;
        return MathHelper.wrapDegrees(targetYaw - player.getYaw());
    }

    private static double distanceTo(ClientPlayerEntity player, PathTarget activeTarget) {
        double dx = activeTarget.x() - player.getX();
        double dy = activeTarget.y() - player.getY();
        double dz = activeTarget.z() - player.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void drawTracerLine(DrawContext context, int startX, int startY, int endX, int endY) {
        int dx = endX - startX;
        int dy = endY - startY;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) {
            return;
        }

        for (int step = 0; step <= steps; step += 2) {
            double progress = step / (double) steps;
            int alpha = 65 + (int) Math.round(progress * 130.0D);
            int x = startX + (int) Math.round(dx * progress);
            int y = startY + (int) Math.round(dy * progress);
            context.fill(x - 1, y - 1, x + 2, y + 2, withAlpha(LINE_RGB, alpha));
        }
    }

    private static void drawArrowHead(DrawContext context, int startX, int startY, int endX, int endY) {
        double angle = Math.atan2(endY - startY, endX - startX);
        drawTracerLine(context, endX, endY, endX - (int) Math.round(Math.cos(angle - 0.62D) * 10.0D), endY - (int) Math.round(Math.sin(angle - 0.62D) * 10.0D));
        drawTracerLine(context, endX, endY, endX - (int) Math.round(Math.cos(angle + 0.62D) * 10.0D), endY - (int) Math.round(Math.sin(angle + 0.62D) * 10.0D));
        context.fill(endX - 3, endY - 3, endX + 4, endY + 4, ACCENT);
        context.fill(endX - 1, endY - 1, endX + 2, endY + 2, 0xFFFFFFFF);
    }

    private static void drawTargetLabel(DrawContext context, TextRenderer textRenderer, PathTarget activeTarget, ClientPlayerEntity player, int markerX, int markerY) {
        double distance = distanceTo(player, activeTarget);
        String distanceText = "Path " + Math.round(distance) + "m";
        String coordinateText = formatCoordinate(activeTarget.x()) + " " + formatCoordinate(activeTarget.y()) + " " + formatCoordinate(activeTarget.z());
        int labelWidth = Math.max(textRenderer.getWidth(distanceText), textRenderer.getWidth(coordinateText));
        int labelX = MathHelper.clamp(markerX - labelWidth / 2, 6, MinecraftClient.getInstance().getWindow().getScaledWidth() - labelWidth - 6);
        int labelY = markerY + 12;
        int maxY = MinecraftClient.getInstance().getWindow().getScaledHeight() - 26;
        if (labelY > maxY) {
            labelY = markerY - 28;
        }

        context.fill(labelX - 5, labelY - 4, labelX + labelWidth + 5, labelY + 22, 0xAA05070D);
        context.fill(labelX - 5, labelY - 4, labelX + labelWidth + 5, labelY - 3, MUTED_ACCENT);
        GuiDraw.text(context, textRenderer, distanceText, labelX, labelY, ACCENT);
        GuiDraw.text(context, textRenderer, coordinateText, labelX, labelY + 11, JafuTheme.TEXT_MUTED);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | rgb;
    }

    private static String formatCoordinate(double coordinate) {
        if (Math.abs(coordinate - Math.rint(coordinate)) < 0.001D) {
            return Long.toString(Math.round(coordinate));
        }
        return String.format("%.1f", coordinate);
    }

    private record PathTarget(double x, double y, double z) {
    }
}
