package com.example.dungeonhud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/** Прячем ванильные элементы, которые заменяет наш HUD (шина Forge). */
@Mod.EventBusSubscriber(modid = DungeonHud.MODID, value = Dist.CLIENT)
public final class HudEvents {
    private HudEvents() {}

    private static final Set<ResourceLocation> HIDDEN = Set.of(
            VanillaGuiOverlay.HOTBAR.id(),
            VanillaGuiOverlay.PLAYER_HEALTH.id(),
            VanillaGuiOverlay.ARMOR_LEVEL.id(),
            VanillaGuiOverlay.FOOD_LEVEL.id(),
            VanillaGuiOverlay.EXPERIENCE_BAR.id(),
            VanillaGuiOverlay.JUMP_BAR.id(),
            VanillaGuiOverlay.AIR_LEVEL.id()
    );

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;
        // В режиме наблюдателя остаётся ванильное меню спектатора
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        if (HIDDEN.contains(event.getOverlay().id())) {
            event.setCanceled(true);
        }
    }
}
