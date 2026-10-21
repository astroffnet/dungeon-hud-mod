package com.example.dungeonhud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Общие функции отрисовки. */
public final class HudDraw {
    private HudDraw() {}

    public static final int OUTLINE = 0xFF0B0907;
    public static final int GOLD_HI = 0xFFF2D488;
    public static final int GOLD    = 0xFFC9A24A;
    public static final int GOLD_DK = 0xFF6B4E1A;

    /** Кусок текстуры 1:1. */
    public static void tex(GuiGraphics g, ResourceLocation t, int x, int y, int u, int v, int w, int h, int tw, int th) {
        RenderSystem.enableBlend();
        g.blit(t, x, y, (float) u, (float) v, w, h, tw, th);
    }

    /** Панель из текстуры plate.png (9-slice), минимум 10x10. */
    public static void plate(GuiGraphics g, int x, int y, int w, int h) {
        final int c = 5, s = 16;
        int mw = w - 2 * c, mh = h - 2 * c;
        ResourceLocation t = HudTextures.PLATE;
        RenderSystem.enableBlend();
        g.blit(t, x, y, c, c, 0f, 0f, c, c, s, s);
        g.blit(t, x + w - c, y, c, c, (float) (s - c), 0f, c, c, s, s);
        g.blit(t, x, y + h - c, c, c, 0f, (float) (s - c), c, c, s, s);
        g.blit(t, x + w - c, y + h - c, c, c, (float) (s - c), (float) (s - c), c, c, s, s);
        g.blit(t, x + c, y, mw, c, (float) c, 0f, s - 2 * c, c, s, s);
        g.blit(t, x + c, y + h - c, mw, c, (float) c, (float) (s - c), s - 2 * c, c, s, s);
        g.blit(t, x, y + c, c, mh, 0f, (float) c, c, s - 2 * c, s, s);
        g.blit(t, x + w - c, y + c, c, mh, (float) (s - c), (float) c, c, s - 2 * c, s, s);
        g.blit(t, x + c, y + c, mw, mh, (float) c, (float) c, s - 2 * c, s - 2 * c, s, s);
    }

    /** Фон полосы высотой 5 px с золотыми линиями и литыми концами (итого 9 px по высоте). */
    public static void barBack(GuiGraphics g, int x, int y, int w) {
        g.fill(x - 1, y - 2, x + w + 1, y + 7, OUTLINE);
        g.fill(x, y - 1, x + w, y, GOLD);
        g.fill(x, y + 5, x + w, y + 6, GOLD_DK);
        g.fill(x, y, x + w, y + 5, 0xFF1C1710);
    }

    public static void barCaps(GuiGraphics g, int x, int y, int w) {
        tex(g, HudTextures.BAR_CAPS, x - 8, y - 2, 0, 0, 10, 9, 20, 9);
        tex(g, HudTextures.BAR_CAPS, x + w - 2, y - 2, 10, 0, 10, 9, 20, 9);
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    public static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int aa = (int) Mth.lerp(t, a >>> 24, b >>> 24);
        int rr = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int gg = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bb = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    /** Кольцо между rIn и rOut с вертикальным градиентом. */
    public static void ring(GuiGraphics g, int cx, int cy, int rIn, int rOut, int topCol, int botCol) {
        for (int dy = -rOut; dy < rOut; dy++) {
            double yc = dy + 0.5;
            int xo = (int) Math.round(Math.sqrt(rOut * rOut - yc * yc));
            int xi = Math.abs(yc) < rIn ? (int) Math.round(Math.sqrt(rIn * rIn - yc * yc)) : 0;
            int col = lerpColor(topCol, botCol, (dy + rOut) / (2f * rOut));
            if (xi <= 0) {
                g.fill(cx - xo, cy + dy, cx + xo, cy + dy + 1, col);
            } else {
                g.fill(cx - xo, cy + dy, cx - xi, cy + dy + 1, col);
                g.fill(cx + xi, cy + dy, cx + xo, cy + dy + 1, col);
            }
        }
    }

    /** Жидкость в круглой колбе: по колонкам, с волной на поверхности и цветовыми полосами. */
    public static void liquid(GuiGraphics g, int cx, int cy, int r, float level, float time, boolean wave, int[] bands) {
        if (level <= 0f) return;
        level = Math.min(level, 1f);
        int n = bands.length;
        for (int dx = -r; dx < r; dx++) {
            double xc = dx + 0.5;
            int half = (int) Math.round(Math.sqrt(r * r - xc * xc));
            int top = -half, bot = half;
            double s = r - level * 2.0 * r;
            if (wave && level < 1f) s += Math.sin(time * 0.3 + dx * 0.45) * 1.2;
            int from = Math.max(top, (int) Math.round(s));
            if (from >= bot) continue;
            for (int k = 0; k < n; k++) {
                int bandTop = -r + (2 * r * k) / n;
                int bandBot = -r + (2 * r * (k + 1)) / n;
                int a = Math.max(from, bandTop);
                int b = Math.min(bot, bandBot);
                if (a < b) g.fill(cx + dx, cy + a, cx + dx + 1, cy + b, bands[k]);
            }
        }
    }

    /** Дуга из точек по жёлобу кольца, начиная сверху по часовой стрелке. */
    public static void arcDots(GuiGraphics g, int cx, int cy, float radius, float fraction, int color) {
        int n = Mth.clamp((int) (fraction * 45f), 0, 45);
        for (int i = 0; i < n; i++) {
            double rad = Math.toRadians(-90 + i * 8);
            int px = cx + (int) Math.round(Math.cos(rad) * radius);
            int py = cy + (int) Math.round(Math.sin(rad) * radius);
            g.fill(px - 1, py - 1, px + 1, py + 1, color);
        }
    }
}
