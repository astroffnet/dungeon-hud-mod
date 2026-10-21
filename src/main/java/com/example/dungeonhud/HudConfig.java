package com.example.dungeonhud;

import net.minecraftforge.common.ForgeConfigSpec;

/** Клиентский конфиг: config/dungeonhud-client.toml. Каждую фишку можно выключить. */
public final class HudConfig {
    private HudConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue COMPASS;
    public static final ForgeConfigSpec.BooleanValue INFO;
    public static final ForgeConfigSpec.BooleanValue TARGET;
    public static final ForgeConfigSpec.BooleanValue LOOT;
    public static final ForgeConfigSpec.BooleanValue POPUPS;
    public static final ForgeConfigSpec.BooleanValue VIGNETTE;
    public static final ForgeConfigSpec.BooleanValue SLOT_NUMBERS;
    public static final ForgeConfigSpec.BooleanValue COUNTERS;
    public static final ForgeConfigSpec.BooleanValue LEVELUP;
    public static final ForgeConfigSpec.BooleanValue CHARGE;
    public static final ForgeConfigSpec.BooleanValue COOLDOWN;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("widgets");
        COMPASS = b.comment("Compass strip at the top of the screen").define("compass", true);
        INFO = b.comment("Info plate (coordinates, biome, time) in the top-left corner").define("info", true);
        TARGET = b.comment("Enemy frame with name and health bar").define("target", true);
        LOOT = b.comment("Loot feed on the right side of the screen").define("loot", true);
        POPUPS = b.comment("Floating damage/heal numbers over the health orb").define("popups", true);
        VIGNETTE = b.comment("Red screen edges at low health and when hit").define("vignette", true);
        SLOT_NUMBERS = b.comment("Small slot numbers 1-9 on the hotbar").define("slotNumbers", true);
        COUNTERS = b.comment("Heal slot, emerald and arrow counters").define("counters", true);
        LEVELUP = b.comment("Level up banner").define("levelUp", true);
        CHARGE = b.comment("Bow/crossbow/trident charge bar under the crosshair").define("chargeBar", true);
        COOLDOWN = b.comment("Weapon attack cooldown bar under the crosshair (turn the vanilla attack indicator off if you enable this)")
                .define("cooldownBar", false);
        b.pop();
        SPEC = b.build();
    }
}
