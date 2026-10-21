package com.example.dungeonhud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * HUD в стиле Minecraft Dungeons: текстурные рамки (генерируются скриптом, лежат в assets),
 * шары здоровья и голода, хотбар, слот лечения, счётчики и ещё куча мелочей.
 */
public final class DungeonHudOverlay implements IGuiOverlay {
    public static final DungeonHudOverlay INSTANCE = new DungeonHudOverlay();

    // ---------- геометрия ----------
    private static final int SLOT = 22;
    private static final int PITCH = 24;
    private static final int BAR_W = 9 * PITCH - 2; // 214
    private static final int R_IN = 21;             // радиус жидкости в шаре

    // ---------- палитра ----------
    private static final int[] HEALTH_BANDS = {0xFFFF7A68, 0xFFF04438, 0xFFD9261C, 0xFFB81A14, 0xFF911010, 0xFF6E0A0C};
    private static final int[] POISON_BANDS = {0xFFB7E86A, 0xFF8CCB3C, 0xFF6BAA2A, 0xFF528A1E, 0xFF3C6A16, 0xFF2A4C10};
    private static final int[] WITHER_BANDS = {0xFF6A6A6A, 0xFF545454, 0xFF404040, 0xFF2F2F2F, 0xFF202020, 0xFF141414};
    private static final int[] FOOD_BANDS   = {0xFFFFD36B, 0xFFF5B83A, 0xFFE09A22, 0xFFC27A14, 0xFF9C5C0E, 0xFF7A450A};
    private static final int[] HUNGER_BANDS = {0xFFC9D98A, 0xFFA9BE62, 0xFF8AA043, 0xFF6D842F, 0xFF54671F, 0xFF3F5016};
    private static final int[] EMPTY_ORB    = {0xFF140C0C};

    private static final long START = System.currentTimeMillis();

    // ---------- состояние ----------
    private final HudExtras extras = new HudExtras();
    private final int[] ghostBand = new int[1];
    private final List<Popup> popups = new ArrayList<>();

    private float shownHealth = -1f, ghostHealth = -1f, lastHealth = -1f;
    private float shownFood = -1f;
    private float shownXp = 0f;
    private int shownLevel = -1;
    private long lastHitMs, lastFrameMs;

    private int lastLevel = -1, levelUpValue;
    private long levelUpMs;

    private ItemStack emeraldIcon, arrowIcon;

    private static final class Popup {
        final String text;
        final int rgb;
        final long t0;
        final float dx;

        Popup(String text, int rgb, long t0, float dx) {
            this.text = text;
            this.rgb = rgb;
            this.t0 = t0;
            this.dx = dx;
        }
    }

    private DungeonHudOverlay() {}

    // =====================================================================
    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        long now = System.currentTimeMillis();
        float dt = lastFrameMs == 0L ? 1f : Math.min((now - lastFrameMs) / 50f, 5f);
        lastFrameMs = now;
        float time = (now - START) / 50f;
        float pulse = 0.5f + 0.5f * Mth.sin(time * 0.25f);
        Font font = mc.font;
        boolean survival = mc.gameMode.canHurtPlayer();

        RenderSystem.enableBlend();

        if (survival && HudConfig.VIGNETTE.get()) drawVignette(g, p, w, h, pulse);

        // ---------- верхние элементы ----------
        if (HudConfig.COMPASS.get()) extras.drawCompass(g, font, p, w);
        if (HudConfig.INFO.get() && !mc.options.renderDebug) extras.drawInfo(g, font, p, now);
        if (HudConfig.TARGET.get()) extras.drawTarget(g, font, mc, w, now, dt);

        // ---------- геометрия низа ----------
        int x0 = w / 2 - BAR_W / 2;
        int sy = h - 26;
        int orbY = h - 34;
        int healthCx = x0 - 38;
        int hungerCx = x0 + BAR_W + 38;

        // ---------- хотбар ----------
        Inventory inv = p.getInventory();
        for (int i = 0; i < 9; i++) {
            if (i != inv.selected) drawSlot(g, font, x0 + i * PITCH, sy, inv.items.get(i), false, pulse, i + 1, null);
        }
        drawSlot(g, font, x0 + inv.selected * PITCH, sy, inv.items.get(inv.selected), true, pulse, inv.selected + 1, null);

        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty()) drawSlot(g, font, healthCx - 58, sy, off, false, pulse, 0, null);

        // ---------- опыт / шкала прыжка ----------
        drawXpOrJump(g, font, p, mc, x0, sy, w, h, now, dt, pulse);

        // ---------- броня и воздух ----------
        drawPips(g, p, x0, sy - 9 - 9);

        // ---------- шары ----------
        if (survival) {
            drawHealthOrb(g, font, p, healthCx, orbY, now, dt, time, pulse);
            drawFoodOrb(g, font, p, hungerCx, orbY, dt, time, pulse);
        }

        // ---------- справа: лечение, изумруды, стрелы ----------
        if (HudConfig.COUNTERS.get()) drawCounters(g, font, p, hungerCx + 38, sy, pulse);

        // ---------- всплывающие цифры ----------
        if (survival && HudConfig.POPUPS.get()) drawPopups(g, font, healthCx, orbY, now);

        // ---------- лента добычи, заряд, уровень ----------
        if (HudConfig.LOOT.get()) {
            extras.scanLoot(p, now);
            extras.drawLoot(g, font, w, h, now);
        }
        extras.drawCharge(g, p, w, h, pulse);
        trackLevelUp(p, now);
        if (HudConfig.LEVELUP.get()) drawLevelUp(g, font, w, h, now);

        // двигаем название предмета выше наших элементов
        gui.leftHeight = 66;
        gui.rightHeight = 66;
    }

    // =====================================================================
    //  Слоты
    // =====================================================================

    private void drawSlot(GuiGraphics g, Font font, int x, int y, ItemStack stack, boolean selected,
                          float pulse, int number, String countText) {
        if (selected) {
            g.setColor(1f, 1f, 1f, 0.55f + 0.45f * pulse);
            HudDraw.tex(g, HudTextures.SLOT_GLOW, x - 4, y - 4, 0, 0, 30, 30, 30, 30);
            g.setColor(1f, 1f, 1f, 1f);
        }
        g.fillGradient(x + 3, y + 3, x + 19, y + 19, 0xFF2B2016, 0xFF14100B);

        if (!stack.isEmpty()) {
            int tint = switch (stack.getRarity()) {
                case UNCOMMON -> 0xFFE35A;
                case RARE -> 0x55D7FF;
                case EPIC -> 0xC06BFF;
                default -> stack.isEnchanted() ? 0xB070FF : -1;
            };
            if (tint != -1) {
                g.fillGradient(x + 3, y + 3, x + 19, y + 19, (0x58 << 24) | tint, (0x10 << 24) | tint);
            }
            // предупреждение о низкой прочности
            if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                float rem = 1f - stack.getDamageValue() / (float) stack.getMaxDamage();
                if (rem < 0.15f) {
                    int a = (int) (40 + 80 * pulse);
                    g.fill(x + 3, y + 3, x + 19, y + 19, (a << 24) | 0xFF2020);
                }
            }
        }

        HudDraw.tex(g, HudTextures.SLOT, x, y, selected ? 22 : 0, 0, SLOT, SLOT, 44, 22);

        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 3, y + 3);
            if (countText == null) g.renderItemDecorations(font, stack, x + 3, y + 3);
            else g.renderItemDecorations(font, stack, x + 3, y + 3, countText);
        }

        if (number > 0 && HudConfig.SLOT_NUMBERS.get()) {
            g.pose().pushPose();
            g.pose().translate(x + 4, y + 4, 300f);
            g.pose().scale(0.6f, 0.6f, 1f);
            g.drawString(font, Integer.toString(number), 0, 0, 0xB0FFFFFF, true);
            g.pose().popPose();
        }
    }

    // =====================================================================
    //  Опыт / прыжок
    // =====================================================================

    private void drawXpOrJump(GuiGraphics g, Font font, LocalPlayer p, Minecraft mc, int x0, int sy,
                              int w, int h, long now, float dt, float pulse) {
        boolean jumpMount = p.getVehicle() instanceof PlayerRideableJumping;
        if (!jumpMount && !mc.gameMode.hasExperience()) return;

        int by = sy - 9;
        HudDraw.barBack(g, x0, by, BAR_W);

        if (jumpMount) {
            float charge = Mth.clamp(p.getJumpRidingScale(), 0f, 1f);
            int fw = (int) (BAR_W * charge);
            if (fw > 0) {
                g.fillGradient(x0, by, x0 + fw, by + 5, 0xFFFFE27A, 0xFFE0721C);
                g.fill(x0, by, x0 + fw, by + 1, 0x66FFFFFF);
                if (charge >= 0.95f) {
                    g.fill(x0, by, x0 + fw, by + 5, ((int) (40 + 70 * pulse) << 24) | 0xFFFFFF);
                }
            }
        } else {
            if (p.experienceLevel != shownLevel) {
                shownLevel = p.experienceLevel;
                shownXp = p.experienceProgress;
            } else {
                shownXp += (p.experienceProgress - shownXp) * (1f - (float) Math.pow(0.85, dt));
            }
            int fw = (int) (BAR_W * Mth.clamp(shownXp, 0f, 1f));
            if (fw > 0) {
                g.fillGradient(x0, by, x0 + fw, by + 5, 0xFFB6FF7A, 0xFF3FA52B);
                g.fill(x0, by, x0 + fw, by + 1, 0x66FFFFFF);
            }
            if (p.experienceLevel > 0) {
                String lvl = Integer.toString(p.experienceLevel);
                int bw2 = font.width(lvl) + 18;
                HudDraw.plate(g, w / 2 - bw2 / 2, by - 17, bw2, 15);
                g.drawString(font, lvl, w / 2 - font.width(lvl) / 2, by - 13, 0xFFB6FF7A, true);
            }
        }
        HudDraw.barCaps(g, x0, by, BAR_W);
    }

    // =====================================================================
    //  Броня и воздух
    // =====================================================================

    private void drawPips(GuiGraphics g, LocalPlayer p, int x0, int py) {
        int armor = Math.min(p.getArmorValue(), 20);
        if (armor > 0) {
            for (int i = 0; i < 10; i++) {
                int px = x0 + i * 7;
                g.fill(px, py, px + 6, py + 4, HudDraw.OUTLINE);
                g.fill(px + 1, py + 1, px + 5, py + 3, 0xFF2A2F36);
                int v = armor - i * 2;
                if (v >= 2) g.fillGradient(px + 1, py + 1, px + 5, py + 3, 0xFFD6E4F2, 0xFF7C93AE);
                else if (v == 1) g.fillGradient(px + 1, py + 1, px + 3, py + 3, 0xFFD6E4F2, 0xFF7C93AE);
            }
        }
        int air = p.getAirSupply();
        int maxAir = Math.max(1, p.getMaxAirSupply());
        if (p.isUnderWater() || air < maxAir) {
            int filled = Mth.clamp(Mth.ceil(air * 10f / maxAir), 0, 10);
            for (int i = 0; i < 10; i++) {
                int px = x0 + BAR_W - 70 + i * 7;
                g.fill(px, py, px + 6, py + 4, HudDraw.OUTLINE);
                g.fill(px + 1, py + 1, px + 5, py + 3, 0xFF14262E);
                if (i < filled) g.fillGradient(px + 1, py + 1, px + 5, py + 3, 0xFFB8F2FF, 0xFF3FA8D8);
            }
        }
    }

    // =====================================================================
    //  Шары
    // =====================================================================

    private void drawHealthOrb(GuiGraphics g, Font font, LocalPlayer p, int cx, int cy,
                               long now, float dt, float time, float pulse) {
        float max = Math.max(1f, p.getMaxHealth());
        float hp = Math.max(0f, p.getHealth());

        if (shownHealth < 0f) { shownHealth = ghostHealth = lastHealth = hp; }
        float delta = hp - lastHealth;
        if (hp < lastHealth - 0.01f) lastHitMs = now;
        if (Math.abs(delta) >= 0.5f && lastHealth > 0f && hp > 0f) {
            int n = Math.round(Math.abs(delta));
            if (n >= 1) {
                popups.add(new Popup((delta < 0 ? "-" : "+") + n, delta < 0 ? 0xFF4A3C : 0x7CFF6A, now,
                        (popups.size() % 3 - 1) * 9f));
                while (popups.size() > 8) popups.remove(0);
            }
        }
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

        float abs = p.getAbsorptionAmount();
        drawOrb(g, cx, cy, shownHealth / max, ghostHealth / max, bands, 0xFFFFC8B8, 0xFF2020, glow, time,
                abs / 20f, 0xFFFFD84A);

        g.drawCenteredString(font, Integer.toString(Mth.ceil(hp)), cx, cy - 4, 0xFFFFFFFF);
        if (abs > 0f) g.drawCenteredString(font, "+" + Mth.ceil(abs), cx, cy - 46, 0xFFFFD84A);
    }

    private void drawFoodOrb(GuiGraphics g, Font font, LocalPlayer p, int cx, int cy,
                             float dt, float time, float pulse) {
        int food = p.getFoodData().getFoodLevel();
        if (shownFood < 0f) shownFood = food;
        shownFood += (food - shownFood) * (1f - (float) Math.pow(0.82, dt));
        if (Math.abs(food - shownFood) < 0.05f) shownFood = food;

        int[] bands = p.hasEffect(MobEffects.HUNGER) ? HUNGER_BANDS : FOOD_BANDS;
        float glow = food <= 6 ? 0.15f + 0.2f * pulse : 0f;
        float sat = p.getFoodData().getSaturationLevel() / 20f;

        drawOrb(g, cx, cy, shownFood / 20f, shownFood / 20f, bands, 0xFFFFE9B0,
                food == 0 ? 0xFF2020 : 0xFFA020, glow, time, sat, 0xFFFFF0A0);
        g.drawCenteredString(font, Integer.toString(food), cx, cy - 4, 0xFFFFFFFF);
    }

    private void drawOrb(GuiGraphics g, int cx, int cy, float fill, float ghost, int[] bands, int ghostCol,
                         int glowRgb, float glowStrength, float time, float arcFrac, int arcColor) {
        if (glowStrength > 0f) {
            int a = (int) Mth.clamp(glowStrength * 255f, 0f, 255f);
            HudDraw.ring(g, cx, cy, 30, 36, (a << 24) | glowRgb, (a << 24) | glowRgb);
        }

        HudDraw.liquid(g, cx, cy, R_IN, 1f, 0f, false, EMPTY_ORB);
        ghostBand[0] = ghostCol;
        if (ghost > fill) HudDraw.liquid(g, cx, cy, R_IN, ghost, 0f, false, ghostBand);
        HudDraw.liquid(g, cx, cy, R_IN, fill, time, true, bands);

        // блик на «стекле»
        for (int deg = 200; deg <= 250; deg += 5) {
            double rad = Math.toRadians(deg);
            int gx = cx + (int) Math.round(Math.cos(rad) * 16);
            int gy = cy + (int) Math.round(Math.sin(rad) * 16);
            g.fill(gx, gy, gx + 2, gy + 2, 0x50FFFFFF);
        }

        // тёмный жёлоб под прозрачной частью рамки + дуга (поглощение / насыщение)
        HudDraw.ring(g, cx, cy, 25, 28, 0xFF0E0A08, 0xFF0E0A08);
        if (arcFrac > 0f) HudDraw.arcDots(g, cx, cy, 26.3f, Math.min(1f, arcFrac), arcColor);

        HudDraw.tex(g, HudTextures.ORB, cx - 32, cy - 32, 0, 0, 64, 64, 64, 64);
    }

    // =====================================================================
    //  Счётчики справа: лечение, изумруды, стрелы
    // =====================================================================

    private void drawCounters(GuiGraphics g, Font font, LocalPlayer p, int gx, int sy, float pulse) {
        Inventory inv = p.getInventory();
        if (emeraldIcon == null) {
            emeraldIcon = new ItemStack(Items.EMERALD);
            arrowIcon = new ItemStack(Items.ARROW);
        }

        // слот лечения
        HealFinder.Result heal = HealFinder.find(inv);
        ItemStack healStack = heal == null ? ItemStack.EMPTY : heal.stack();
        String cnt = heal != null && heal.count() > 1 ? Integer.toString(heal.count()) : "";
        drawSlot(g, font, gx, sy, healStack, false, pulse, 0, cnt);
        g.pose().pushPose();
        g.pose().translate(gx + 4, sy + 4, 300f);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawString(font, HudKeys.HEAL.getTranslatedKeyMessage().getString(), 0, 0, 0xFFFFE08A, true);
        g.pose().popPose();

        // изумруды и стрелы
        int emeralds = 0, arrows = 0;
        for (ItemStack s : inv.items) {
            if (s.isEmpty()) continue;
            if (s.is(Items.EMERALD)) emeralds += s.getCount();
            else if (s.is(ItemTags.ARROWS)) arrows += s.getCount();
        }
        ItemStack off = p.getOffhandItem();
        if (off.is(Items.EMERALD)) emeralds += off.getCount();
        else if (off.is(ItemTags.ARROWS)) arrows += off.getCount();

        int iy = sy - 20;
        if (emeralds > 0) {
            drawCounter(g, font, emeraldIcon, emeralds, gx + 3, iy);
            iy -= 18;
        }
        boolean ranged = p.getMainHandItem().getItem() instanceof ProjectileWeaponItem;
        if (arrows > 0 || ranged) {
            drawCounter(g, font, arrowIcon, arrows, gx + 3, iy);
        }
    }


    private void drawCounter(GuiGraphics g, Font font, ItemStack icon, int count, int x, int y) {
        g.renderItem(icon, x, y);
        g.drawString(font, Integer.toString(count), x + 19, y + 4, count == 0 ? 0xFFFF6A5A : 0xFFFFFFFF, true);
    }

    // =====================================================================
    //  Эффекты: всплывающие цифры, виньетка, левел-ап
    // =====================================================================

    private void drawPopups(GuiGraphics g, Font font, int cx, int cy, long now) {
        Iterator<Popup> it = popups.iterator();
        while (it.hasNext()) {
            Popup pp = it.next();
            long age = now - pp.t0;
            if (age > 1100L) { it.remove(); continue; }
            float k = age / 1100f;
            int a = (int) (255 * (1f - k * k));
            if (a < 8) continue;
            int y = cy - 40 - (int) (26 * k);
            float sc = 1f + (k < 0.15f ? (0.15f - k) * 3f : 0f);
            g.pose().pushPose();
            g.pose().translate(cx + pp.dx, y, 400f);
            g.pose().scale(sc, sc, 1f);
            g.drawCenteredString(font, pp.text, 0, 0, (a << 24) | pp.rgb);
            g.pose().popPose();
        }
    }

    private void drawVignette(GuiGraphics g, LocalPlayer p, int w, int h, float pulse) {
        float max = Math.max(1f, p.getMaxHealth());
        float frac = p.getHealth() / max;
        float s = 0f;
        if (frac <= 0.3f && p.getHealth() > 0f) s = ((0.3f - frac) / 0.3f) * (0.35f + 0.25f * pulse);
        if (p.hurtTime > 0) s = Math.max(s, 0.5f * Math.min(1f, p.hurtTime / 10f));
        if (s < 0.02f) return;

        int t = Math.max(24, Math.min(w, h) / 6);
        for (int i = 0; i < t; i += 2) {
            float k = 1f - i / (float) t;
            int a = (int) (180 * s * k * k);
            if (a <= 0) continue;
            int c = (a << 24) | 0xB00000;
            g.fill(0, i, w, i + 2, c);
            g.fill(0, h - i - 2, w, h - i, c);
            g.fill(i, 0, i + 2, h, c);
            g.fill(w - i - 2, 0, w - i, h, c);
        }
    }

    private void trackLevelUp(LocalPlayer p, long now) {
        int lvl = p.experienceLevel;
        if (lastLevel >= 0 && lvl > lastLevel) {
            levelUpMs = now;
            levelUpValue = lvl;
        }
        lastLevel = lvl;
    }

    private void drawLevelUp(GuiGraphics g, Font font, int w, int h, long now) {
        if (levelUpMs == 0L) return;
        long age = now - levelUpMs;
        if (age > 2400L) return;
        float a = age < 300L ? age / 300f : (age > 1800L ? (2400L - age) / 600f : 1f);
        int alpha = (int) (255 * Mth.clamp(a, 0f, 1f));
        if (alpha < 8) return;

        int cx = w / 2, cy = h / 3;
        String title = Component.translatable("hud.dungeonhud.levelup").getString();

        int lw = (int) (70 + 90 * Mth.clamp(age / 400f, 0f, 1f));
        g.fill(cx - lw, cy + 24, cx + lw, cy + 25, ((alpha / 2) << 24) | 0xF2D488);
        g.fill(cx - lw, cy - 3, cx + lw, cy - 2, ((alpha / 2) << 24) | 0xF2D488);

        g.pose().pushPose();
        g.pose().translate(cx, cy, 400f);
        g.pose().scale(2f, 2f, 1f);
        g.drawCenteredString(font, title, 0, 0, (alpha << 24) | 0xF2D488);
        g.pose().popPose();

        g.pose().pushPose();
        g.pose().translate(cx, cy + 18, 400f);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawCenteredString(font, Integer.toString(levelUpValue), 0, 0, (alpha << 24) | 0xB6FF7A);
        g.pose().popPose();
    }
}
