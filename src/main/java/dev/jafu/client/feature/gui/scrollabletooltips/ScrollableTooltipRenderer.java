package dev.jafu.client.feature.gui.scrollabletooltips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.jafu.client.gui.util.Rect;
import dev.jafu.client.module.JafuModules;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2ic;

public final class ScrollableTooltipRenderer {
    private static final int CONTENT_GAP_AFTER_TITLE = 2;
    private static final int TRACK_PADDING = 3;
    private static final int SCROLLBAR_GAP = 5;
    private static final int FADE_HEIGHT = 13;
    private static final long BAR_VISIBLE_MILLIS = 850L;
    private static final long STATE_TTL_MILLIS = 300_000L;

    private static final Map<String, TooltipScrollState> STATES = new HashMap<>();
    private static String pendingItemKey;
    private static String preparedTooltipKey;
    private static String activeKey;
    private static Rect activeBounds;
    private static Rect activeScrollBarBounds;
    private static int activeMaxScroll;
    private static boolean draggingScrollbar;

    private ScrollableTooltipRenderer() {
    }

    public static void rememberItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            pendingItemKey = null;
            return;
        }

        Identifier id = Registries.ITEM.getId(stack.getItem());
        pendingItemKey = "item:" + id + ":" + stack.getCount() + ":" + ItemStack.hashCode(stack);
    }

    public static void prepareTooltip(List<TooltipComponent> components, TextRenderer textRenderer) {
        String itemKey = pendingItemKey;
        pendingItemKey = null;
        preparedTooltipKey = itemKey == null ? "tooltip:" + signature(components, textRenderer) : itemKey;
    }

    public static boolean render(
            DrawContext context,
            TextRenderer textRenderer,
            List<TooltipComponent> components,
            int mouseX,
            int mouseY,
            TooltipPositioner positioner,
            Identifier texture
    ) {
        if (!enabled() || components.isEmpty()) {
            clearActive();
            return false;
        }

        TooltipLayout layout = layout(components, textRenderer);
        int maxHeight = maxTooltipHeight(context.getScaledWindowHeight());
        if (layout.contentHeight() <= maxHeight) {
            clearActive();
            return false;
        }

        long nowMillis = System.currentTimeMillis();
        pruneStates(nowMillis);

        String key = preparedTooltipKey == null ? "tooltip:" + layout.signature() : preparedTooltipKey;
        preparedTooltipKey = null;

        TooltipScrollState state = STATES.computeIfAbsent(key, ignored -> new TooltipScrollState());
        state.layout = layout;
        if (!key.equals(activeKey) && state.newState) {
            TooltipScrollState oldState = activeKey == null ? null : STATES.get(activeKey);
            state.animatedScroll = oldState == null ? 0.0D : oldState.animatedScroll;
            state.targetScroll = 0.0D;
            state.newState = false;
        }

        int visibleHeight = Math.min(layout.contentHeight(), maxHeight);
        int barWidth = ScrollableTooltipsSettings.INSTANCE.intValue(ScrollableTooltipNumericSetting.BAR_WIDTH);
        int tooltipWidth = layout.contentWidth() + SCROLLBAR_GAP + barWidth + TRACK_PADDING;
        int maxScroll = Math.max(0, layout.contentHeight() - visibleHeight);
        state.targetScroll = MathHelper.clamp(state.targetScroll, 0.0D, maxScroll);
        state.animatedScroll = animateScroll(state.animatedScroll, state.targetScroll, state.lastRenderMillis, nowMillis);
        state.animatedScroll = MathHelper.clamp(state.animatedScroll, 0.0D, maxScroll);
        state.lastRenderMillis = nowMillis;
        state.lastSeenMillis = nowMillis;

        Vector2ic position = positioner.getPosition(
                context.getScaledWindowWidth(),
                context.getScaledWindowHeight(),
                mouseX,
                mouseY,
                tooltipWidth,
                visibleHeight
        );
        int x = position.x();
        int y = position.y();

        Rect bounds = new Rect(x - 4, y - 4, tooltipWidth + 8, visibleHeight + 8);
        Rect track = new Rect(x + layout.contentWidth() + SCROLLBAR_GAP, y + TRACK_PADDING, barWidth, Math.max(1, visibleHeight - TRACK_PADDING * 2));
        boolean barHovered = isNear(mouseX, mouseY, track, 7);
        updateBarAlpha(state, nowMillis, barHovered || draggingScrollbar || Math.abs(state.targetScroll - state.animatedScroll) > 0.4D);

        activeKey = key;
        activeBounds = bounds;
        activeScrollBarBounds = track;
        activeMaxScroll = maxScroll;

        TooltipBackgroundRenderer.render(context, x, y, tooltipWidth, visibleHeight, texture);
        renderBorderHighlight(context, x, y, tooltipWidth, visibleHeight, state.barAlpha);
        renderComponents(context, textRenderer, components, layout, x, y, visibleHeight, state.animatedScroll);
        if (ScrollableTooltipsSettings.INSTANCE.fadeGradients()) {
            renderFades(context, x, y, tooltipWidth, visibleHeight, state.animatedScroll, maxScroll);
        }
        renderScrollBar(context, track, visibleHeight, layout.contentHeight(), state.animatedScroll, maxScroll, state.barAlpha, barHovered);
        return true;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (!canInteract(mouseX, mouseY) || activeKey == null || activeMaxScroll <= 0) {
            return false;
        }

        TooltipScrollState state = STATES.get(activeKey);
        if (state == null) {
            return false;
        }

        double speed = ScrollableTooltipsSettings.INSTANCE.value(ScrollableTooltipNumericSetting.SCROLL_SPEED);
        state.targetScroll = MathHelper.clamp(state.targetScroll - verticalAmount * speed, 0.0D, activeMaxScroll);
        state.lastInteractionMillis = System.currentTimeMillis();
        return true;
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !canInteract(mouseX, mouseY) || activeKey == null || activeScrollBarBounds == null || activeMaxScroll <= 0) {
            return false;
        }

        if (!isNear(mouseX, mouseY, activeScrollBarBounds, 5)) {
            return false;
        }

        draggingScrollbar = true;
        updateScrollFromBar(mouseY);
        return true;
    }

    public static boolean mouseDragged(double mouseY) {
        if (!draggingScrollbar) {
            return false;
        }

        updateScrollFromBar(mouseY);
        return true;
    }

    public static boolean mouseReleased() {
        if (!draggingScrollbar) {
            return false;
        }

        draggingScrollbar = false;
        return true;
    }

    private static void updateScrollFromBar(double mouseY) {
        TooltipScrollState state = activeKey == null ? null : STATES.get(activeKey);
        if (state == null || activeScrollBarBounds == null || activeMaxScroll <= 0) {
            return;
        }

        double percent = (mouseY - activeScrollBarBounds.y()) / Math.max(1.0D, activeScrollBarBounds.height());
        state.targetScroll = MathHelper.clamp(percent, 0.0D, 1.0D) * activeMaxScroll;
        state.lastInteractionMillis = System.currentTimeMillis();
    }

    private static void renderComponents(
            DrawContext context,
            TextRenderer textRenderer,
            List<TooltipComponent> components,
            TooltipLayout layout,
            int x,
            int y,
            int visibleHeight,
            double scroll
    ) {
        int contentRight = x + layout.contentWidth() + 1;
        context.enableScissor(x - 1, y, contentRight, y + visibleHeight);
        try {
            for (int i = 0; i < components.size(); i++) {
                drawTextComponent(context, textRenderer, components.get(i), x, y, layout.rows().get(i), scroll, visibleHeight);
            }
            for (int i = 0; i < components.size(); i++) {
                drawItemComponent(context, textRenderer, components.get(i), x, y, layout.rows().get(i), scroll, visibleHeight, layout);
            }
        } finally {
            context.disableScissor();
        }
    }

    private static void drawTextComponent(
            DrawContext context,
            TextRenderer textRenderer,
            TooltipComponent component,
            int x,
            int y,
            TooltipRow row,
            double scroll,
            int visibleHeight
    ) {
        if (!row.visible(scroll, visibleHeight)) {
            return;
        }
        component.drawText(context, textRenderer, x, y + (int) Math.round(row.y() - scroll));
    }

    private static void drawItemComponent(
            DrawContext context,
            TextRenderer textRenderer,
            TooltipComponent component,
            int x,
            int y,
            TooltipRow row,
            double scroll,
            int visibleHeight,
            TooltipLayout layout
    ) {
        if (!row.visible(scroll, visibleHeight)) {
            return;
        }
        component.drawItems(textRenderer, x, y + (int) Math.round(row.y() - scroll), layout.contentWidth(), layout.contentHeight(), context);
    }

    private static void renderScrollBar(
            DrawContext context,
            Rect track,
            int visibleHeight,
            int contentHeight,
            double scroll,
            int maxScroll,
            double alpha,
            boolean hovered
    ) {
        if (maxScroll <= 0 || alpha <= 0.01D) {
            return;
        }

        double opacity = ScrollableTooltipsSettings.INSTANCE.value(ScrollableTooltipNumericSetting.BAR_OPACITY) * alpha;
        int trackColor = withAlpha(0xFF05070B, 0.42D * opacity);
        int thumbColor = brighten(ScrollableTooltipsSettings.INSTANCE.scrollBarColor(), hovered || draggingScrollbar ? 1.22D : 1.0D);
        thumbColor = withAlpha(thumbColor, opacity);
        int thumbHeight = Math.max(14, (int) Math.round(track.height() * (visibleHeight / (double) contentHeight)));
        int travel = Math.max(0, track.height() - thumbHeight);
        int thumbY = track.y() + (maxScroll == 0 ? 0 : (int) Math.round(travel * (scroll / maxScroll)));

        context.fill(track.left(), track.top(), track.right(), track.bottom(), trackColor);
        context.fill(track.x(), thumbY, track.right(), thumbY + thumbHeight, thumbColor);
        if (hovered || draggingScrollbar) {
            context.fill(track.x() - 1, thumbY, track.x(), thumbY + thumbHeight, withAlpha(thumbColor, 0.58D));
            context.fill(track.right(), thumbY, track.right() + 1, thumbY + thumbHeight, withAlpha(thumbColor, 0.58D));
        }
    }

    private static void renderFades(DrawContext context, int x, int y, int width, int height, double scroll, int maxScroll) {
        int fade = Math.min(FADE_HEIGHT, height / 3);
        int dark = withAlpha(0xFF05070B, 0.72D);
        int clear = withAlpha(0xFF05070B, 0.0D);
        if (scroll > 0.75D) {
            context.fillGradient(x, y, x + width, y + fade, dark, clear);
        }
        if (scroll < maxScroll - 0.75D) {
            context.fillGradient(x, y + height - fade, x + width, y + height, clear, dark);
        }
    }

    private static void renderBorderHighlight(DrawContext context, int x, int y, int width, int height, double alpha) {
        int color = withAlpha(ScrollableTooltipsSettings.INSTANCE.scrollBarColor(), 0.26D * alpha);
        context.fill(x - 1, y - 1, x + width + 1, y, color);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        context.fill(x - 1, y, x, y + height, color);
        context.fill(x + width, y, x + width + 1, y + height, color);
    }

    private static TooltipLayout layout(List<TooltipComponent> components, TextRenderer textRenderer) {
        String signature = signature(components, textRenderer);
        TooltipScrollState state = preparedTooltipKey == null ? null : STATES.get(preparedTooltipKey);
        if (state != null && state.layout != null && state.layout.signature().equals(signature)) {
            return state.layout;
        }

        List<TooltipRow> rows = new ArrayList<>(components.size());
        int width = 0;
        int y = 0;
        for (int i = 0; i < components.size(); i++) {
            TooltipComponent component = components.get(i);
            int componentWidth = component.getWidth(textRenderer);
            int height = component.getHeight(textRenderer);
            width = Math.max(width, componentWidth);
            rows.add(new TooltipRow(y, height));
            y += height;
            if (i == 0 && components.size() > 1) {
                y += CONTENT_GAP_AFTER_TITLE;
            }
        }

        TooltipLayout layout = new TooltipLayout(signature, width, Math.max(0, y), rows);
        if (state != null) {
            state.layout = layout;
        }
        return layout;
    }

    private static String signature(List<TooltipComponent> components, TextRenderer textRenderer) {
        StringBuilder builder = new StringBuilder();
        for (TooltipComponent component : components) {
            builder.append(component.getClass().getName())
                    .append(':')
                    .append(component.getWidth(textRenderer))
                    .append('x')
                    .append(component.getHeight(textRenderer))
                    .append(';');
        }
        return builder.toString();
    }

    private static double animateScroll(double current, double target, long lastRenderMillis, long nowMillis) {
        int duration = ScrollableTooltipsSettings.INSTANCE.intValue(ScrollableTooltipNumericSetting.ANIMATION_DURATION);
        if (duration <= 0 || lastRenderMillis == 0L) {
            return target;
        }

        double progress = MathHelper.clamp((nowMillis - lastRenderMillis) / (double) duration, 0.0D, 1.0D);
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        double next = current + (target - current) * eased;
        return Math.abs(target - next) < 0.05D ? target : next;
    }

    private static void updateBarAlpha(TooltipScrollState state, long nowMillis, boolean shouldShow) {
        double target = shouldShow || nowMillis - state.lastInteractionMillis < BAR_VISIBLE_MILLIS ? 1.0D : 0.35D;
        state.barAlpha += (target - state.barAlpha) * 0.22D;
        state.barAlpha = MathHelper.clamp(state.barAlpha, 0.0D, 1.0D);
    }

    private static int maxTooltipHeight(int screenHeight) {
        double percent = ScrollableTooltipsSettings.INSTANCE.value(ScrollableTooltipNumericSetting.MAX_HEIGHT_PERCENT) / 100.0D;
        return Math.max(48, (int) Math.floor(screenHeight * percent));
    }

    private static boolean canInteract(double mouseX, double mouseY) {
        return enabled() && activeBounds != null && activeBounds.contains(mouseX, mouseY);
    }

    private static boolean enabled() {
        return ScrollableTooltipsSettings.INSTANCE.enabled() && JafuModules.isEnabled(JafuModules.SCROLLABLE_TOOLTIPS);
    }

    private static void clearActive() {
        activeKey = null;
        activeBounds = null;
        activeScrollBarBounds = null;
        activeMaxScroll = 0;
        draggingScrollbar = false;
    }

    private static boolean isNear(double x, double y, Rect rect, int padding) {
        return x >= rect.left() - padding
                && x <= rect.right() + padding
                && y >= rect.top() - padding
                && y <= rect.bottom() + padding;
    }

    private static int withAlpha(int color, double alpha) {
        int alphaByte = (int) Math.round(MathHelper.clamp(alpha, 0.0D, 1.0D) * 255.0D);
        return alphaByte << 24 | color & 0x00FFFFFF;
    }

    private static int brighten(int color, double multiplier) {
        int red = Math.min(255, (int) Math.round(((color >> 16) & 0xFF) * multiplier));
        int green = Math.min(255, (int) Math.round(((color >> 8) & 0xFF) * multiplier));
        int blue = Math.min(255, (int) Math.round((color & 0xFF) * multiplier));
        return color & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static void pruneStates(long nowMillis) {
        STATES.entrySet().removeIf(entry -> nowMillis - entry.getValue().lastSeenMillis > STATE_TTL_MILLIS);
    }

    private static final class TooltipScrollState {
        private double targetScroll;
        private double animatedScroll;
        private double barAlpha = 0.35D;
        private long lastRenderMillis;
        private long lastInteractionMillis;
        private long lastSeenMillis = System.currentTimeMillis();
        private boolean newState = true;
        private TooltipLayout layout;
    }

    private record TooltipLayout(String signature, int contentWidth, int contentHeight, List<TooltipRow> rows) {
    }

    private record TooltipRow(int y, int height) {
        private boolean visible(double scroll, int visibleHeight) {
            return y + height >= scroll && y <= scroll + visibleHeight;
        }
    }
}
