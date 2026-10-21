package com.example.dungeonhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * HUD в стиле Minecraft Dungeons.
 * Всё рисуется кодом (без текстур): золотые рамки, шары здоровья и голода,
 * хотбар с подсветкой редкости предметов, полоска опыта, броня и воздух.
 */
public final class DungeonHudOverlay implements IGuiOverlay {
    public static final DungeonHudOverlay INSTANCE = new DungeonHudOverlay();

    // ---------- палитра ----------
    private static final int OUTLINE = 0xFF0B0907;
    private static final int GOLD_HI = 0xFFF2D488;
    private static final int GOLD    = 0xFFC9A24A;
    private static final int GOLD_DK = 0xFF6B4E1A;
    private static final int INNER   = 0xFF3A2A12;
    private static final int BG_TOP  = 0xF2261C13;
    private static final int BG_BOT  = 0xF2110D09;

    // полосы шара сверху вниз (6 тонов — пиксельное затенение)
    private static final int[] HEALTH_BANDS = {0xFFFF7A68, 0xFFF04438, 0xFFD9261C, 0xFFB81A14, 0xFF911010, 0xFF6E0A0C};
    private static final int[] POISON_BANDS = {0xFFB7E86A, 0xFF8CCB3C, 0xFF6BAA2A, 0xFF528A1E, 0xFF3C6A16, 0xFF2A4C10};
    private static final int[] WITHER_BANDS = {0xFF6A6A6A, 0xFF545454, 0xFF404040, 0xFF2F2F2F, 0xFF202020, 0xFF141414};
    private static final int[] FOOD_BANDS   = {0xFFFFD36B, 0xFFF5B83A, 0xFFE09A22, 0xFFC27A14, 0xFF9C5C0E, 0xFF7A450A};
    private static final int[] HUNGER_BANDS = {0xFFC9D98A, 0xFFA9BE62, 0xFF8AA043, 0xFF6D842F, 0xFF54671F, 0xFF3F5016};
    private static final int[] EMPTY_ORB    = {0xFF140C0C};

    private static final int R_IN = 19; // внутренний радиус шара

    private static final long START = System.currentTimeMillis();

    // ---------- состояние анимации ----------
    private final int[] ghostBand = new int[1];
    private float shownHealth = -1f, ghostHealth = -1f, lastHealth = -1f;
    private float shownFood = -1f;
    private long lastHitMs = 0L, lastFrameMs = 0L;

    private DungeonHudOverlay() {}

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0L ? 1f : Math.min((now - lastFrameMs) / 50f, 5f); // в тиках
        lastFrameMs = now;
        float time = (now - START) / 50f;
        float pulse = 0.5f + 0.5f * Mth.sin(time * 0.25f);
        Font font = mc.font;

        int x0 = w / 2 - 98; // левый край первого слота
        int sy = h - 30;     // верх слотов

        // ---------- панель хотбара ----------
        panel(g, x0 - 5, sy - 4, 206, 31);

        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            drawSlot(g, font, x0 + i * 22, sy, inv.items.get(i), i == inv.selected, pulse);
        }

        // ---------- вторая рука ----------
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty()) {
            drawSlot(g, font, x0 - 76, sy, off, false, pulse);
        }

        // ---------- полоса опыта / шкала прыжка + уровень ----------
        boolean jumpMount = player.getVehicle() instanceof PlayerRideableJumping;
        if (jumpMount || mc.gameMode.hasExperience()) {
            int bx = x0, by = sy + 21, bw = 196;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + 4, OUTLINE);
            g.fill(bx, by, bx + bw, by + 3, 0xFF1C1710);
            if (jumpMount) {
                // шкала прыжка заменяет полосу опыта, как в ванили
                float charge = Mth.clamp(player.getJumpRidingScale(), 0f, 1f);
                int fw = (int) (bw * charge);
                if (fw > 0) {
                    g.fillGradient(bx, by, bx + fw, by + 3, 0xFFFFE27A, 0xFFE0721C);
                    g.fill(bx, by, bx + fw, by + 1, 0x66FFFFFF);
                    if (charge >= 0.95f) {
                        g.fill(bx, by, bx + fw, by + 3, (int) (40 + 70 * pulse) << 24 | 0xFFFFFF);
                    }
                }
            } else {
                int fw = (int) (bw * Mth.clamp(player.experienceProgress, 0f, 1f));
                if (fw > 0) {
                    g.fillGradient(bx, by, bx + fw, by + 3, 0xFFB6FF7A, 0xFF3FA52B);
                    g.fill(bx, by, bx + fw, by + 1, 0x66FFFFFF);
                }
                if (player.experienceLevel > 0) {
                    String lvl = Integer.toString(player.experienceLevel);
                    int bw2 = font.width(lvl) + 14;
                    int lx = w / 2 - bw2 / 2;
                    panel(g, lx, h - 44, bw2, 13);
                    g.drawString(font, lvl, w / 2 - font.width(lvl) / 2, h - 41, 0xFFB6FF7A, true);
                }
            }
        }

        // ---------- броня (10 плашек над левой частью хотбара) ----------
        int armor = Math.min(player.getArmorValue(), 20);
        if (armor > 0) {
            for (int i = 0; i < 10; i++) {
                int px = x0 + i * 7, py = h - 40;
                g.fill(px, py, px + 6, py + 4, OUTLINE);
                g.fill(px + 1, py + 1, px + 5, py + 3, 0xFF2A2F36);
                int v = armor - i * 2;
                if (v >= 2) g.fillGradient(px + 1, py + 1, px + 5, py + 3, 0xFFD6E4F2, 0xFF7C93AE);
                else if (v == 1) g.fillGradient(px + 1, py + 1, px + 3, py + 3, 0xFFD6E4F2, 0xFF7C93AE);
            }
        }

        // ---------- воздух под водой (над правой частью хотбара) ----------
        int air = player.getAirSupply();
        int maxAir = Math.max(1, player.getMaxAirSupply());
        if (player.isUnderWater() || air < maxAir) {
            int filled = Mth.clamp(Mth.ceil(air * 10f / maxAir), 0, 10);
            for (int i = 0; i < 10; i++) {
                int px = x0 + 196 - 70 + i * 7, py = h - 40;
                g.fill(px, py, px + 6, py + 4, OUTLINE);
                g.fill(px + 1, py + 1, px + 5, py + 3, 0xFF14262E);
                if (i < filled) g.fillGradient(px + 1, py + 1, px + 5, py + 3, 0xFFB8F2FF, 0xFF3FA8D8);
            }
        }

        // ---------- шары здоровья и голода ----------
        if (mc.gameMode.canHurtPlayer()) {
            drawHealthOrb(g, font, player, x0 - 30, h - 25, now, dt, time, pulse);
            drawFoodOrb(g, font, player, x0 + 196 + 30, h - 25, dt, time, pulse);
        }

        // Сдвигаем название предмета выше шаров
        gui.leftHeight = 62;
        gui.rightHeight = 62;
    }

    // =====================================================================
    //  Шары
    // =====================================================================

    private void drawHealthOrb(GuiGraphics g, Font font, LocalPlayer p, int cx, int cy,
                               long now, float dt, float time, float pulse) {
        float max = Math.max(1f, p.getMaxHealth());
        float hp = Math.max(0f, p.getHealth());

        if (shownHealth < 0f) { shownHealth = ghostHealth = lastHealth = hp; }
        if (hp < lastHealth - 0.01f) lastHitMs = now;
        lastHealth = hp;

        if (hp >= shownHealth) {
            shownHealth += (hp - shownHealth) * (1f - (float) Math.pow(0.82, dt));
            if (hp - shownHealth < 0.05f) shownHealth = hp;
        } else {
            shownHealth = hp;
        }
        if (ghostHealth < shownHealth) {
            ghostHealth = shownHealth;
        } else if (now - lastHitMs > 450L) {
            ghostHealth = Math.max(shownHealth, ghostHealth - dt * max * 0.012f);
        }

        int[] bands = HEALTH_BANDS;
        if (p.hasEffect(MobEffects.WITHER)) bands = WITHER_BANDS;
        else if (p.hasEffect(MobEffects.POISON)) bands = POISON_BANDS;

        float glow = 0f;
        if (hp / max <= 0.3f) glow = 0.18f + 0.22f * pulse;
        if (p.hurtTime > 0) glow = Math.max(glow, 0.5f);

        drawOrb(g, cx, cy, shownHealth / max, ghostHealth / max, bands, 0xFFFFC8B8, 0xFF2020, glow, time);

        String s = Integer.toString(Mth.ceil(hp));
        g.drawCenteredString(font, s, cx, cy - 4, 0xFFFFFFFF);

        float abs = p.getAbsorptionAmount();
        if (abs > 0f) {
            g.drawCenteredString(font, "+" + Mth.ceil(abs), cx, cy - R_IN - 15, 0xFFFFD84A);
        }
    }

    private void drawFoodOrb(GuiGraphics g, Font font, LocalPlayer p, int cx, int cy,
                             float dt, float time, float pulse) {
        int food = p.getFoodData().getFoodLevel();
        if (shownFood < 0f) shownFood = food;
        shownFood += (food - shownFood) * (1f - (float) Math.pow(0.82, dt));
        if (Math.abs(food - shownFood) < 0.05f) shownFood = food;

        int[] bands = p.hasEffect(MobEffects.HUNGER) ? HUNGER_BANDS : FOOD_BANDS;
        float glow = 0f;
        if (food <= 6) glow = 0.15f + 0.2f * pulse;

        drawOrb(g, cx, cy, shownFood / 20f, shownFood / 20f, bands, 0xFFFFE9B0, food == 0 ? 0xFF2020 : 0xFFA020, glow, time);
        g.drawCenteredString(font, Integer.toString(food), cx, cy - 4, 0xFFFFFFFF);
    }

    private void drawOrb(GuiGraphics g, int cx, int cy, float fill, float ghost, int[] bands, int ghostCol,
                         int glowRgb, float glowStrength, float time) {
        if (glowStrength > 0f) {
            int a = (int) Mth.clamp(glowStrength * 255f, 0f, 255f);
            ring(g, cx, cy, R_IN + 3, R_IN + 7, (a << 24) | glowRgb, (a << 24) | glowRgb);
        }

        liquid(g, cx, cy, R_IN, 1f, 0f, false, EMPTY_ORB);
        ghostBand[0] = ghostCol;
        if (ghost > fill) liquid(g, cx, cy, R_IN, ghost, 0f, false, ghostBand);
        liquid(g, cx, cy, R_IN, fill, time, true, bands);

        // блик на «стекле»
        for (int deg = 200; deg <= 250; deg += 5) {
            double rad = Math.toRadians(deg);
            int gx = cx + (int) Math.round(Math.cos(rad) * 14);
            int gy = cy + (int) Math.round(Math.sin(rad) * 14);
            g.fill(gx, gy, gx + 2, gy + 2, 0x50FFFFFF);
        }

        ring(g, cx, cy, R_IN - 1, R_IN + 4, OUTLINE, OUTLINE);
        ring(g, cx, cy, R_IN, R_IN + 3, GOLD_HI, GOLD_DK);
    }

    /** Жидкость в круглой колбе: по колонкам, с волной на поверхности и цветовыми полосами. */
    private static void liquid(GuiGraphics g, int cx, int cy, int r, float level, float time, boolean wave, int[] bands) {
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

    /** Кольцо между rIn и rOut с вертикальным градиентом. */
    private static void ring(GuiGraphics g, int cx, int cy, int rIn, int rOut, int topCol, int botCol) {
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

    // =====================================================================
    //  Рамки и слоты
    // =====================================================================

    /** Золотая панель с двойной рамкой и тёмным градиентным фоном. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, GOLD_HI);          // верх — светлый
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, GOLD_DK);  // низ — тёмный
        g.fill(x + 1, y + 2, x + 2, y + h - 2, GOLD);             // лево
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, GOLD_DK);  // право
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, INNER);        // внутренняя линия
        g.fillGradient(x + 3, y + 3, x + w - 3, y + h - 3, BG_TOP, BG_BOT);
        // «заклёпки» в углах
        g.fill(x + 1, y + 1, x + 3, y + 3, GOLD_HI);
        g.fill(x + w - 3, y + 1, x + w - 1, y + 3, GOLD);
        g.fill(x + 1, y + h - 3, x + 3, y + h - 1, GOLD);
        g.fill(x + w - 3, y + h - 3, x + w - 1, y + h - 1, GOLD_DK);
    }

    private static void drawSlot(GuiGraphics g, Font font, int x, int y, ItemStack stack, boolean selected, float pulse) {
        if (selected) {
            int a1 = (int) (70 + 60 * pulse);
            int a2 = (int) (30 + 40 * pulse);
            outline(g, x - 1, y - 1, 22, 22, (a1 << 24) | 0xFFD66B);
            outline(g, x - 2, y - 2, 24, 24, (a2 << 24) | 0xFFD66B);
        }
        g.fill(x, y, x + 20, y + 20, OUTLINE);
        outline(g, x + 1, y + 1, 18, 18, selected ? lerpColor(GOLD_HI, 0xFFFFFFFF, pulse * 0.6f) : GOLD_DK);
        g.fillGradient(x + 2, y + 2, x + 18, y + 18, 0xFF2B2016, 0xFF14100B);

        if (stack.isEmpty()) return;

        // подсветка редкости / зачарования
        int tint = switch (stack.getRarity()) {
            case UNCOMMON -> 0xFFE35A;
            case RARE -> 0x55D7FF;
            case EPIC -> 0xC06BFF;
            default -> stack.isEnchanted() ? 0xB070FF : -1;
        };
        if (tint != -1) {
            g.fillGradient(x + 2, y + 2, x + 18, y + 18, (0x58 << 24) | tint, (0x10 << 24) | tint);
        }

        g.renderItem(stack, x + 2, y + 2);
        g.renderItemDecorations(font, stack, x + 2, y + 2);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int aa = (int) Mth.lerp(t, a >>> 24, b >>> 24);
        int rr = (int) Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF);
        int gg = (int) Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF);
        int bb = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }
}
