package com.example.dungeonhud;

import net.minecraft.resources.ResourceLocation;

public final class HudTextures {
    private HudTextures() {}

    public static final ResourceLocation ORB = tex("orb_frame");       // 64x64
    public static final ResourceLocation SLOT = tex("slot_frame");     // 44x22 (обычный | выбранный)
    public static final ResourceLocation SLOT_GLOW = tex("slot_glow"); // 30x30
    public static final ResourceLocation PLATE = tex("plate");         // 16x16, 9-slice (угол 5)
    public static final ResourceLocation BAR_CAPS = tex("bar_caps");   // 20x9 (левый | правый)

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(DungeonHud.MODID, "textures/gui/" + name + ".png");
    }
}
