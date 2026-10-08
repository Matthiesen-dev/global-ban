package dev.matthiesen.global_ban.common;

import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.config.ServerConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.registry.CommandRegistry;
import dev.matthiesen.global_ban.common.registry.EventHandlers;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.Helpers;
import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.matthiesen_core.common.AbstractCommonMod;
import dev.matthiesen.matthiesen_core.common.api.events.PlatformEvents;
import dev.matthiesen.matthiesen_core.common.api.platform.loader.ModConfigType;
import net.minecraft.server.level.ServerPlayer;
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
        registerModConfig(MOD_ID, ModConfigType.STARTUP, GlobalBanConfig.PERMISSIONS_SPEC, modConfig("permissions"));

        PermissionRegistry.init();
        CommandRegistry.init();

        PlatformEvents.SERVER_STARTING.subscribe(EventHandlers::onServerStarting);
        PlatformEvents.SERVER_STARTED.subscribe(EventHandlers::onServerStarted);
        PlatformEvents.SERVER_STOPPING.subscribe(EventHandlers::onServerStopping);
        PlatformEvents.PLAYER_JOIN.subscribe(EventHandlers::onPlayerJoin);
        PlatformEvents.CONFIG_RELOADING(MOD_ID).subscribe(EventHandlers::onConfigReloading);

        createInfoLog("Initialized");
    }

    public boolean isGooeyLibsLoaded() {
        return getCommonUtils().isModLoaded("gooeylibs");
    }

    public void punishPlayer(PunishmentRecord record) {
        ServerConfig serverConfig = GlobalBanConfig.SERVER_CONFIG;
        if (record.type().shouldKick && record.type().ipBased) {
            boolean alreadyBanned = false;

            for (ServerPlayer player : getCommonUtils().getServer().getPlayerList().getPlayers()) {
                if (player.getIpAddress().equals(record.playerIp())) {
                    player.connection.disconnect(record.getDisconnectScreenComponent());

                    if (serverConfig.autoBanPlayersByIP.getAsBoolean() && record.type() == PunishmentType.IP_BAN) {
                        PunishmentRecord newPunishment = PunishmentRecord.punishAlt(player.getUUID(), player.getName().getString(), record);

                        if (player.getUUID() == record.playerUuid()) {
                            alreadyBanned = true;
                        }

                        punishPlayer(newPunishment);
                    }
                }
            }

            if (serverConfig.autoBanPlayersByIP.getAsBoolean() && record.type() == PunishmentType.IP_BAN && !alreadyBanned) {
                PunishmentRecord newPunishment = PunishmentRecord.punishAlt(record.playerUuid(), record.playerDisplayName(), record);
                punishPlayer(newPunishment);
            }
        } else if (record.type().shouldKick) {
            ServerPlayer player = getCommonUtils().getServer().getPlayerList().getPlayer(record.playerUuid());

            if (player != null) {
                player.connection.disconnect(record.getDisconnectScreenComponent());
            }
        }

        getCommonUtils().getServer().sendSystemMessage(record.getChatMessage());
        if (serverConfig.showPunishActionsInChat.getAsBoolean()) {
            for (ServerPlayer player : getCommonUtils().getServer().getPlayerList().getPlayers()) {
                if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_BANS_PERMISSION)) {
                    player.sendSystemMessage(record.getChatMessage());
                }
            }
        }

        if (record.type().shouldRecord) {
            GlobalBanConfig.addPunishment(record);
        }
    }

    public int clearPunishments(UUID player) {
        return GlobalBanConfig.clearPunishments(player);
    }

    public int clearIpPunishments(String ip) {
        return GlobalBanConfig.clearIpPunishments(ip);
    }

    public int unpunishPlayer(PunishmentRecord record) {
        return unpunishPlayer(record, false);
    }

    public int unpunishPlayer(PunishmentRecord record, boolean expired) {
        ServerConfig serverConfig = GlobalBanConfig.SERVER_CONFIG;
        if (!expired) {
            var message = switch (record.type()) {
                case BAN -> serverConfig.messages_unbanChatMessage.get();
                case IP_BAN -> serverConfig.messages_ipUnbanChatMessage.get();
                case KICK -> null;
            };
            if (message != null) {
                String singleStringMessage = String.join("\n", message);
                var parsed = Helpers.processPlaceholders(singleStringMessage, record.getPlaceholders());
                getCommonUtils().getServer().sendSystemMessage(parsed.toComponent());
                if (serverConfig.showPunishActionsInChat.getAsBoolean()) {
                    for (ServerPlayer player : getCommonUtils().getServer().getPlayerList().getPlayers()) {
                        if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_BANS_PERMISSION)) {
                            player.sendSystemMessage(parsed.toComponent());
                        }
                    }
                }
            }
        } else {
            createInfoLog("Punishment expired: " + record.toString());
        }
        return GlobalBanConfig.removePunishment(record);
    }
}
