package dev.matthiesen.global_ban.common;

import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.matthiesen_core.common.AbstractCommonMod;
import org.jetbrains.annotations.NotNull;

public final class GlobalBanCommon extends AbstractCommonMod {
    public static final String MOD_ID = "global_ban";
    public static final String MOD_NAME = "Global Ban";
    public static @Token final String METRICS_TOKEN = "382f15e125d207a935a0999a3d268084";
    public static final GlobalBanCommon INSTANCE = new GlobalBanCommon();

    public GlobalBanCommon() {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public @Token @NotNull String getMetricsToken() {
        return METRICS_TOKEN;
    }

    public void initialize() {
        super.initialize();

        createInfoLog("Initialized");
    }
}
