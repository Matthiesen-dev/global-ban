package dev.matthiesen.global_ban.neoforge;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import net.neoforged.fml.common.Mod;

@Mod(GlobalBanCommon.MOD_ID)
public final class GlobalBanNeoForge {
    public static final GlobalBanCommon INSTANCE = GlobalBanCommon.INSTANCE;

    public GlobalBanNeoForge() {
        INSTANCE.createInfoLog("Loading for NeoForge Mod Loader");
        INSTANCE.initialize();
    }
}
