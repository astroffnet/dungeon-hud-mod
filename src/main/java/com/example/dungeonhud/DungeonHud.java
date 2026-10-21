package com.example.dungeonhud;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;

/** Клиентский мод: серверу он не нужен, поэтому говорим Forge игнорировать версию на сервере. */
@Mod(DungeonHud.MODID)
public class DungeonHud {
    public static final String MODID = "dungeonhud";

    public DungeonHud() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, isServer) -> true));
    }
}
