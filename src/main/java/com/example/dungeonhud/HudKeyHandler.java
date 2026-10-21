package com.example.dungeonhud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DungeonHud.MODID, value = Dist.CLIENT)
public final class HudKeyHandler {
    private HudKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (HudKeys.HEAL.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            HealFinder.Result r = HealFinder.find(mc.player.getInventory());
            if (r == null) {
                mc.player.displayClientMessage(Component.translatable("hud.dungeonhud.heal.none"), true);
            } else if (r.hotbarSlot() >= 0) {
                mc.player.getInventory().selected = r.hotbarSlot();
            } else {
                mc.player.displayClientMessage(Component.translatable("hud.dungeonhud.heal.hotbar"), true);
            }
        }
    }
}
