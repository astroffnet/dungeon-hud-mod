package com.example.dungeonhud;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Клавиша быстрого выбора лечения (по умолчанию H). */
@Mod.EventBusSubscriber(modid = DungeonHud.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HudKeys {
    private HudKeys() {}

    public static final KeyMapping HEAL = new KeyMapping("key.dungeonhud.heal",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.dungeonhud");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(HEAL);
    }
}
