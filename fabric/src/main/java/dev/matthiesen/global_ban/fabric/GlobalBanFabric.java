package dev.matthiesen.global_ban.fabric;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import net.fabricmc.api.ModInitializer;

public final class GlobalBanFabric implements ModInitializer {
    public static final GlobalBanCommon INSTANCE = GlobalBanCommon.INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE.createInfoLog("Loading for Fabric Mod Loader");
        INSTANCE.initialize();
    }
}
