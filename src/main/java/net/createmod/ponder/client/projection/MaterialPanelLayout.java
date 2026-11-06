package net.createmod.ponder.client.projection;

import java.util.*;

/** Screen geometry only: keep targets inside the viewport and outside occupied controls. */
public final class MaterialPanelLayout {
    private MaterialPanelLayout() {}
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(int px, int py) { return px >= x && py >= y && px < right() && py < bottom(); }
        public boolean intersects(Rect other) { return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y; }
    }

    public static Rect place(int screenWidth, int screenHeight, int width, int height,
                             int preferredX, int preferredY, List<Rect> obstacles) {
        List<Rect> free = new ArrayList<>();
        free.add(new Rect(4, 4, Math.max(0, screenWidth - 8), Math.max(0, screenHeight - 8)));
        for (Rect obstacle : obstacles) {
            List<Rect> next = new ArrayList<>();
            for (Rect area : free) {
                if (!area.intersects(obstacle)) { next.add(area); continue; }
                add(next, area.x, area.y, obstacle.x - area.x, area.height);
                add(next, obstacle.right(), area.y, area.right() - obstacle.right(), area.height);
                add(next, area.x, area.y, area.width, obstacle.y - area.y);
                add(next, area.x, obstacle.bottom(), area.width, area.bottom() - obstacle.bottom());
            }
            free = next.stream().distinct().filter(a -> next.stream().noneMatch(b -> a != b &&
                b.x <= a.x && b.y <= a.y && b.right() >= a.right() && b.bottom() >= a.bottom() && !a.equals(b))).toList();
        }
        Rect best = new Rect(0, 0, 0, 0); long score = Long.MAX_VALUE;
        for (Rect area : free) {
            int w = Math.min(width, area.width), h = Math.min(height, area.height);
            if (w < Math.min(104, width) || h < 24) continue;
            int x = Math.max(area.x, Math.min(area.right() - w, preferredX));
            int y = Math.max(area.y, Math.min(area.bottom() - h, preferredY));
            long dx = x - preferredX, dy = y - preferredY;
            long candidate = (height - h) * 1_000_000L + (width - w) * 100_000L + dx * dx + dy * dy;
            if (candidate < score) { best = new Rect(x, y, w, h); score = candidate; }
        }
        return best;
    }
    private static void add(List<Rect> areas, int x, int y, int width, int height) {
        if (width > 0 && height > 0) areas.add(new Rect(x, y, width, height));
    }
    public static int normalize(int position, int screen, int panel) {
        return Math.max(0, Math.min(10000, (int)Math.round(position * 10000.0 / Math.max(1, screen - panel))));
    }
    public static int restore(int normalized, int screen, int panel) {
        return (int)Math.round(Math.max(0, Math.min(10000, normalized)) * Math.max(0, screen - panel) / 10000.0);
    }
}
