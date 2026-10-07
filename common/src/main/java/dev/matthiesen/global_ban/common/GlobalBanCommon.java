package dev.matthiesen.global_ban.common;

import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.matthiesen_core.common.AbstractCommonMod;
import dev.matthiesen.matthiesen_core.common.api.platform.loader.ModConfigType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GlobalBanCommon extends AbstractCommonMod {
    public static final String MOD_ID = "global_ban";
    public static final String MOD_NAME = "Global Ban";
    public static @Token final String METRICS_TOKEN = "382f15e125d207a935a0999a3d268084";
    public static final GlobalBanCommon INSTANCE = new GlobalBanCommon();

    public static ConcurrentHashMap<UUID, String> UUID_TO_IP_CACHE = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Set<UUID>> IP_TO_UUID_CACHE = new ConcurrentHashMap<>();

    public GlobalBanCommon() {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public @Token @NotNull String getMetricsToken() {
        return METRICS_TOKEN;
    }

    public static String modConfig(String path) {
        return MOD_ID + "/" + path + ".toml";
    }

    public void initialize() {
        super.initialize();

        registerModConfig(MOD_ID, ModConfigType.COMMON, GlobalBanConfig.SERVER_SPEC, modConfig("server-config"));
        registerModConfig(MOD_ID, ModConfigType.COMMON, GlobalBanConfig.PUNISHMENTS_SPEC, modConfig("punished-accounts"));

        createInfoLog("Initialized");
    }

    public void punishPlayer(PunishmentRecord record) {
        // TODO: Implement punishment logic here
    }

    public void unpunishPlayer(PunishmentRecord record) {
        // TODO: Implement unpunishment logic here
    }

    public void handleExpiredPunishments() {
        Set<PunishmentRecord> expiredPunishments = ConcurrentHashMap.newKeySet();

        for (PunishmentRecord record : GlobalBanConfig.getPunished()) {
            if (record.isExpired()) {
                expiredPunishments.add(record);
            }
        }

        for (PunishmentRecord record : expiredPunishments) {
            unpunishPlayer(record);
        }
    }
}
