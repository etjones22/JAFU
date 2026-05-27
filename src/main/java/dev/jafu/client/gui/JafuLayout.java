package dev.jafu.client.gui;

import dev.jafu.client.gui.util.Rect;

public record JafuLayout(Rect panel, Rect sidebar, Rect header, Rect closeButton) {
    private static final int MAX_WIDTH = 620;
    private static final int MAX_HEIGHT = 364;
    private static final int OUTER_PADDING_X = 32;
    private static final int OUTER_PADDING_Y = 28;

    private static final int SIDEBAR_WIDTH = 148;
    private static final int HEADER_HEIGHT = 42;

    public static JafuLayout fromScreen(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(MAX_WIDTH, screenWidth - OUTER_PADDING_X);
        int panelHeight = Math.min(MAX_HEIGHT, screenHeight - OUTER_PADDING_Y);
        int left = (screenWidth - panelWidth) / 2;
        int top = (screenHeight - panelHeight) / 2;

        Rect panel = new Rect(left, top, panelWidth, panelHeight);
        Rect sidebar = new Rect(left, top, SIDEBAR_WIDTH, panelHeight);
        Rect header = new Rect(left, top, panelWidth, HEADER_HEIGHT);
        Rect closeButton = new Rect(panel.right() - 24, top + 8, 14, 14);
        return new JafuLayout(panel, sidebar, header, closeButton);
    }

    public Rect categoryButton(int index) {
        return new Rect(panel.x() + 13, panel.y() + 62 + index * 34, 122, 24);
    }

    public Rect moduleRow(int index) {
        return new Rect(panel.x() + 154, panel.y() + 72 + index * 46, 210, 36);
    }

    public Rect moduleToggle(int index) {
        Rect row = moduleRow(index);
        return new Rect(row.right() - 38, row.y() + 8, 30, 20);
    }

    public Rect detailPanel() {
        return new Rect(panel.x() + 384, panel.y() + 72, panel.right() - panel.x() - 402, panel.height() - 116);
    }

    public Rect commandBadge() {
        return new Rect(panel.x() + 18, panel.y() + 262, 110, 20);
    }

    public Rect hudLayoutButton() {
        return new Rect(panel.x() + 18, panel.y() + 292, 110, 22);
    }

    public int contentX() {
        return panel.x() + SIDEBAR_WIDTH;
    }

    public int contentBottom() {
        return panel.bottom();
    }
}
