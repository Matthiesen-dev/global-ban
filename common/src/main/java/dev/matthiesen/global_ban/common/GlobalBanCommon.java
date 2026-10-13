package dev.matthiesen.global_ban.common;

import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.events.*;
import dev.matthiesen.global_ban.common.registry.CommandRegistry;
import dev.matthiesen.global_ban.common.registry.EventHandlers;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.matthiesen_core.common.AbstractCommonMod;
import dev.matthiesen.matthiesen_core.common.api.events.EventObservable;
import dev.matthiesen.matthiesen_core.common.api.events.PlatformEvents;
import dev.matthiesen.matthiesen_core.common.api.platform.loader.ModConfigType;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GlobalBanCommon extends AbstractCommonMod {
    public static final String MOD_ID = "global_ban";
    public static final String MOD_NAME = "Global Ban";
    public static @Token final String METRICS_TOKEN = "382f15e125d207a935a0999a3d268084";
    public static final GlobalBanCommon INSTANCE = new GlobalBanCommon();

    public static ConcurrentHashMap<UUID, String> UUID_TO_IP_CACHE = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Set<UUID>> IP_TO_UUID_CACHE = new ConcurrentHashMap<>();
    public static final File IP_CACHE_FILE = Paths.get("ipcache.json").toFile();

    public static class Events {
        public static final EventObservable<CanPlayerLogin.Pre> CAN_PLAYER_LOGIN_PRE = new EventObservable<>();
        public static final ResultEventObservable<CanPlayerLogin.Post, PunishmentRecord> CAN_PLAYER_LOGIN_POST = new ResultEventObservable<>();
        public static final EventObservable<PunishPlayerEvent> PUNISH_PLAYER = new EventObservable<>();
        public static final ResultEventObservable<UnPunishPlayerEvent, Integer> UNPUNISH_PLAYER = new ResultEventObservable<>();
        public static final ResultEventObservable<ClearPunishmentEvent, Integer> CLEAR_PUNISHMENTS = new ResultEventObservable<>();
    }

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

        // Configs
        registerModConfig(MOD_ID, ModConfigType.COMMON, GlobalBanConfig.SERVER_SPEC, modConfig("server-config"));
        registerModConfig(MOD_ID, ModConfigType.COMMON, GlobalBanConfig.PUNISHMENTS_SPEC, modConfig("punished-accounts"));
        registerModConfig(MOD_ID, ModConfigType.STARTUP, GlobalBanConfig.PERMISSIONS_SPEC, modConfig("permissions"));

        // Registries
        PermissionRegistry.init();
        CommandRegistry.init();

        // Platform Events
        PlatformEvents.SERVER_STARTING.subscribe(EventHandlers::onServerStarting);
        PlatformEvents.SERVER_STARTED.subscribe(EventHandlers::onServerStarted);
        PlatformEvents.SERVER_STOPPING.subscribe(EventHandlers::onServerStopping);
        PlatformEvents.PLAYER_JOIN.subscribe(EventHandlers::onPlayerJoin);
        PlatformEvents.CONFIG_RELOADING(MOD_ID).subscribe(EventHandlers::onConfigReloading);

        // Global Ban Events
        Events.PUNISH_PLAYER.subscribe(EventHandlers::onPunishPlayer);
        Events.UNPUNISH_PLAYER.subscribe(EventHandlers::onUnpunishPlayer);
        Events.CLEAR_PUNISHMENTS.subscribe(EventHandlers::onClearPunishments);
        Events.CAN_PLAYER_LOGIN_PRE.subscribe(EventHandlers::onCanPlayerLoginPre);
        Events.CAN_PLAYER_LOGIN_POST.subscribe(EventHandlers::onCanPlayerLoginPost);

        createInfoLog("Initialized");
    }

    public boolean isGooeyLibsLoaded() {
        return getCommonUtils().isModLoaded("gooeylibs");
    }
}
