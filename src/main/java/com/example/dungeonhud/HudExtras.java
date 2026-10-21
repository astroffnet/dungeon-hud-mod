package com.example.dungeonhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Дополнительные элементы: компас, инфо-панель, рамка врага, лента добычи, полоска заряда. */
public final class HudExtras {

    // ------------------------------------------------------------ лента добычи
    private static final class Loot {
        final ItemStack icon;
        final String name;
        final Item item;
        int amount;
        long t;

        Loot(Item item, int amount, long t) {
            this.item = item;
            this.icon = new ItemStack(item);
            this.name = icon.getHoverName().getString();
            this.amount = amount;
            this.t = t;
        }
    }

    private final List<Loot> loot = new ArrayList<>();
    private final Map<Item, Integer> lastCounts = new HashMap<>();
    private boolean lootInit;
    private Player lastPlayer;
    private Level lastLevelObj;
    private long warmupUntil;
    private long lastScan;

    // ------------------------------------------------------------ рамка врага
    private LivingEntity target;
    private long targetSeen;
    private float tShown, tGhost, tLastHp;
    private long tHit;

    // ------------------------------------------------------------ кэш биома
    private String biomeName = "";
    private long biomeAt;

    // =====================================================================
    //  Добыча
    // =====================================================================

    public void scanLoot(Player p, long now) {
        if (p != lastPlayer || p.level() != lastLevelObj) {
            lastPlayer = p;
            lastLevelObj = p.level();
            warmupUntil = now + 2000L;
            lootInit = false;
            loot.clear();
        }
        if (now - lastScan < 150L) return;
        lastScan = now;

        Map<Item, Integer> cur = new HashMap<>();
        for (ItemStack s : p.getInventory().items) {
            if (!s.isEmpty()) cur.merge(s.getItem(), s.getCount(), Integer::sum);
        }
        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty()) cur.merge(off.getItem(), off.getCount(), Integer::sum);

        if (lootInit && now >= warmupUntil) {
            for (Map.Entry<Item, Integer> e : cur.entrySet()) {
                int d = e.getValue() - lastCounts.getOrDefault(e.getKey(), 0);
                if (d > 0) addLoot(e.getKey(), d, now);
            }
        }
        lootInit = true;
        lastCounts.clear();
        lastCounts.putAll(cur);
    }

    private void addLoot(Item item, int amount, long now) {
        for (Loot l : loot) {
            if (l.item == item && now - l.t < 2000L) {
                l.amount += amount;
                l.t = now;
                return;
            }
        }
        loot.add(new Loot(item, amount, now));
        while (loot.size() > 5) loot.remove(0);
    }

    public void drawLoot(GuiGraphics g, Font font, int w, int h, long now) {
        final long life = 3500L;
        Iterator<Loot> it = loot.iterator();
        int i = 0;
        while (it.hasNext()) {
            Loot l = it.next();
            long age = now - l.t;
            if (age > life) { it.remove(); continue; }
            float a = age > life - 600L ? (life - age) / 600f : 1f;
            int alpha = (int) (255 * Mth.clamp(a, 0f, 1f));
            String text = "+" + l.amount + " " + l.name;
            int tw = font.width(text);
            int x = w - 10 - tw;
            int y = h / 2 + 6 + i * 18;
            if (a > 0.5f) g.renderItem(l.icon, x - 20, y - 4);
            if (alpha >= 8) g.drawString(font, text, x, y, (alpha << 24) | 0xF2D488, true);
            i++;
        }
    }

    // =====================================================================
    //  Компас
    // =====================================================================

    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public void drawCompass(GuiGraphics g, Font font, Player p, int w) {
        int cx = w / 2, y = 3, half = 90, hgt = 12;
        g.fillGradient(cx - half, y, cx + half, y + hgt, 0x99120D09, 0x66120D09);
        g.fill(cx - half, y, cx + half, y + 1, 0xAAC9A24A);
        g.fill(cx - half, y + hgt - 1, cx + half, y + hgt, 0xAA6B4E1A);

        float yaw = Mth.wrapDegrees(p.getYRot());
        float ppd = 1.4f;
        float range = 64f;

        int start = (int) (Math.floor((yaw - range) / 15f) * 15);
        for (int deg = start; deg <= yaw + range; deg += 15) {
            float rel = deg - yaw;
            if (Math.abs(rel) > range) continue;
            int x = cx + Math.round(rel * ppd);
            int a = (int) (200 * (1f - Math.abs(rel) / range));
            if (a > 8) g.fill(x, y + hgt - 4, x + 1, y + hgt - 1, (a << 24) | 0xC9A24A);
        }
        for (int k = 0; k < 8; k++) {
            float rel = Mth.wrapDegrees(k * 45f - yaw);
            if (Math.abs(rel) > range) continue;
            int x = cx + Math.round(rel * ppd);
            int a = (int) (255 * (1f - Math.abs(rel) / range));
            if (a < 8) continue;
            int rgb = k == 4 ? 0xFF5A4A : (k % 2 == 0 ? 0xF2D488 : 0xB89C5A);
            g.drawCenteredString(font, DIRS[k], x, y + 2, (a << 24) | rgb);
        }
        g.fill(cx - 2, y + hgt, cx + 3, y + hgt + 1, 0xFFF2D488);
        g.fill(cx - 1, y + hgt + 1, cx + 2, y + hgt + 2, 0xFFF2D488);
        g.fill(cx, y + hgt + 2, cx + 1, y + hgt + 3, 0xFFF2D488);
    }

    // =====================================================================
    //  Инфо-панель (координаты, биом, время)
    // =====================================================================

    private static String pad(int v) {
        return v < 10 ? "0" + v : Integer.toString(v);
    }

    public void drawInfo(GuiGraphics g, Font font, Player p, long now) {
        BlockPos bp = p.blockPosition();
        if (now - biomeAt > 500L) {
            biomeAt = now;
            biomeName = p.level().getBiome(bp).unwrapKey()
                    .map(k -> Component.translatable("biome." + k.location().getNamespace() + "." + k.location().getPath()).getString())
                    .orElse("?");
        }
        long dayTime = p.level().getDayTime();
        int hour = (int) ((dayTime / 1000L + 6L) % 24L);
        int min = (int) ((dayTime % 1000L) * 60L / 1000L);
        String coords = "X " + bp.getX() + "  Y " + bp.getY() + "  Z " + bp.getZ();
        String time = Component.translatable("hud.dungeonhud.day", dayTime / 24000L + 1L).getString()
                + "  " + pad(hour) + ":" + pad(min);

        int pw = Math.max(font.width(coords), Math.max(font.width(biomeName), font.width(time))) + 18;
        int ph = 3 * 11 + 11;
        HudDraw.plate(g, 4, 4, pw, ph);
        g.drawString(font, coords, 13, 11, 0xFFFFFFFF, true);
        g.drawString(font, biomeName, 13, 22, 0xFFA8E07A, true);
        g.drawString(font, time, 13, 33, 0xFFF2D488, true);
    }

    // =====================================================================
    //  Рамка врага
    // =====================================================================

    public void drawTarget(GuiGraphics g, Font font, Minecraft mc, int w, long now, float dt) {
        Entity e = mc.crosshairPickEntity;
        if (e instanceof LivingEntity le && le.isAlive() && le != mc.player && !(le instanceof ArmorStand)) {
            if (le != target) {
                target = le;
                tShown = tGhost = tLastHp = le.getHealth();
            }
            targetSeen = now;
        }
        if (target == null) return;
        if (!target.isAlive() || target.isRemoved() || now - targetSeen > 3000L) {
            target = null;
            return;
        }

        float max = Math.max(1f, target.getMaxHealth());
        float hp = Math.max(0f, target.getHealth());
        if (hp < tLastHp - 0.01f) tHit = now;
        tLastHp = hp;
        tShown = hp;
        if (tGhost < tShown) tGhost = tShown;
        else if (now - tHit > 350L) tGhost = Math.max(tShown, tGhost - dt * max * 0.02f);

        boolean boss = target instanceof EnderDragon || target instanceof WitherBoss || target instanceof Warden
                || target instanceof ElderGuardian || (target instanceof Enemy && max >= 100f);
        boolean elite = !boss && target instanceof Enemy && max >= 40f;

        int pw = boss ? 240 : 170;
        int ph = 30;
        int x = w / 2 - pw / 2, y = 36;
        HudDraw.plate(g, x, y, pw, ph);

        int nameColor = boss ? 0xFFFF5A3C : (elite ? 0xFFFFB347 : 0xFFFFFFFF);
        g.drawCenteredString(font, target.getName().getString(), w / 2, y + 6, nameColor);

        int bx = x + 14, bw = pw - 28, by = y + 17;
        HudDraw.barBack(g, bx, by, bw);
        int gw = (int) (bw * Mth.clamp(tGhost / max, 0f, 1f));
        if (gw > 0) g.fill(bx, by, bx + gw, by + 5, 0xFFFFC8B8);
        int fw = (int) (bw * Mth.clamp(tShown / max, 0f, 1f));
        if (fw > 0) {
            int top = boss ? 0xFFFF5A3C : (elite ? 0xFFFFB347 : 0xFFE5483B);
            int bot = boss ? 0xFF8A0E10 : (elite ? 0xFFC4601A : 0xFF8F1913);
            g.fillGradient(bx, by, bx + fw, by + 5, top, bot);
            g.fill(bx, by, bx + fw, by + 1, 0x55FFFFFF);
        }
        if (boss || elite) HudDraw.barCaps(g, bx, by, bw);

        g.pose().pushPose();
        g.pose().translate(w / 2f, by + 0.5f, 10f);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawCenteredString(font, Mth.ceil(hp) + " / " + Mth.ceil(max), 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
    }

    // =====================================================================
    //  Полоска заряда / перезарядки под прицелом
    // =====================================================================

    public void drawCharge(GuiGraphics g, Player p, int w, int h, float pulse) {
        float prog = -1f;
        if (HudConfig.CHARGE.get() && p.isUsingItem()) {
            ItemStack u = p.getUseItem();
            Item it = u.getItem();
            int t = p.getTicksUsingItem();
            if (it instanceof BowItem) {
                prog = BowItem.getPowerForTime(t);
            } else if (it instanceof CrossbowItem) {
                prog = Mth.clamp(t / (float) Math.max(1, CrossbowItem.getChargeDuration(u)), 0f, 1f);
            } else if (it instanceof TridentItem) {
                prog = Mth.clamp(t / 10f, 0f, 1f);
            }
        }
        if (prog < 0f && HudConfig.COOLDOWN.get()) {
            float s = p.getAttackStrengthScale(0f);
            if (s < 1f) prog = s;
        }
        if (prog < 0f) return;

        int bw = 40, bx = w / 2 - bw / 2, by = h / 2 + 14;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 5, HudDraw.OUTLINE);
        g.fill(bx, by, bx + bw, by + 4, 0xFF1C1710);
        int fw = (int) (bw * Mth.clamp(prog, 0f, 1f));
        if (fw > 0) {
            if (prog >= 1f) {
                g.fillGradient(bx, by, bx + fw, by + 4, 0xFFFFFFFF, 0xFFF2D488);
            } else {
                g.fillGradient(bx, by, bx + fw, by + 4, 0xFFFFE27A, 0xFFE0721C);
            }
        }
        if (prog >= 1f) HudDraw.outline(g, bx - 1, by - 1, bw + 2, 6, ((int) (110 + 120 * pulse) << 24) | 0xFFD66B);
    }
}
