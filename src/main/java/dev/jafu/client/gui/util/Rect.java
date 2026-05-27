package dev.jafu.client.gui.util;

public record Rect(int x, int y, int width, int height) {
    public int left() {
        return x;
    }

    public int top() {
        return y;
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double pointX, double pointY) {
        return pointX >= left() && pointX <= right() && pointY >= top() && pointY <= bottom();
    }
}
