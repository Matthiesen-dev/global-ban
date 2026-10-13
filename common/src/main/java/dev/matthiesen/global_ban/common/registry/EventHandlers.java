package dev.matthiesen.global_ban.common.registry;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.config.ServerConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.def.events.CanPlayerLogin;
import dev.matthiesen.global_ban.common.def.events.ClearPunishmentEvent;
import dev.matthiesen.global_ban.common.def.events.PunishPlayerEvent;
import dev.matthiesen.global_ban.common.def.events.UnPunishPlayerEvent;
import dev.matthiesen.global_ban.common.utils.Helpers;
import dev.matthiesen.matthiesen_core.common.api.events.config.ConfigEvent;
import dev.matthiesen.matthiesen_core.common.api.events.server.PlayerEvent;
import dev.matthiesen.matthiesen_core.common.api.events.server.ServerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class EventHandlers {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
            .create();

    public static void onServerStarting(ServerEvent.Starting starting) {
        try {
            ConcurrentHashMap<String, String> cache = GlobalBanCommon.IP_CACHE_FILE.exists()
                    ? GSON.fromJson(new FileReader(GlobalBanCommon.IP_CACHE_FILE), new TypeToken<ConcurrentHashMap<String, String>>() {}.getType())
                    : null;

            if (cache != null) {
                for (var entry : cache.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getValue());
                        String ip = entry.getKey();

                        GlobalBanCommon.UUID_TO_IP_CACHE.put(uuid, ip);
                        GlobalBanCommon.IP_TO_UUID_CACHE.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                    } catch (Exception e) {
                        // Ignore invalid entries
                    }
                }
            }

        } catch (FileNotFoundException e) {
            GlobalBanCommon.INSTANCE.createWarnLog("IP cache file not found, creating a new one...");
        }
    }

    public static void onServerStarted(ServerEvent.Started started) {
        Schedulers.register();
        GlobalBanCommon.INSTANCE.createInfoLog("Global Ban is running...");
    }

    public static void onServerStopping(ServerEvent.Stopping stopping) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(GlobalBanCommon.IP_CACHE_FILE));

            var ipCache = new HashMap<>();

            for (var entry : GlobalBanCommon.UUID_TO_IP_CACHE.entrySet()) {
                ipCache.put(entry.getKey().toString(), entry.getValue());
            }

            writer.write(GSON.toJson(ipCache));
            writer.close();
        } catch (IOException exception) {
            GlobalBanCommon.INSTANCE.createErrorLog("Failed to save IP cache file: " + exception.getMessage(), exception);
        }
    }

    public static void onConfigReloading(ConfigEvent.Reloading reloading) {
        GlobalBanConfig.invalidatePunished();
        GlobalBanCommon.INSTANCE.createInfoLog("Reloaded Global Ban configuration");
    }

    public static void onPlayerJoin(PlayerEvent.Join join) {
        MinecraftServer server = GlobalBanCommon.INSTANCE.getCommonUtils().getServer();
        ServerConfig serverConfig = GlobalBanConfig.SERVER_CONFIG;

        if (!serverConfig.showKnownAccounts.getAsBoolean()) {
            return;
        }

        String ip = join.player().getIpAddress();
        Set<UUID> knownAccounts = GlobalBanCommon.IP_TO_UUID_CACHE.get(ip);

        if (knownAccounts == null || knownAccounts.isEmpty()) return;

        List<Component> playerMessages = new LinkedList<>();

        for (UUID player : knownAccounts) {
            var profileCache = server.getProfileCache();
            String name;
            if (profileCache != null) {
                name = profileCache.get(player).map(GameProfile::getName).orElse(player.toString());
            } else {
                name = player.toString();
            }
            MutableComponent messageText = Component.literal("[" + name + "]");
            List<PunishmentRecord> punishments = GlobalBanConfig.isPlayerUUIDPunished(player);

            if (server.getPlayerList().getPlayer(player) != null || player.equals(join.player().getUUID())) {
                messageText.withStyle(ChatFormatting.GREEN);
            } else if (punishments.isEmpty()) {
                messageText.withStyle(ChatFormatting.GRAY);
            } else {
                messageText.withStyle(ChatFormatting.RED);
                PunishmentRecord punishment = punishments.getFirst();

                messageText.withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, punishment.getChatMessage()))
                );
            }

            playerMessages.add(messageText);
        }

        Component message = ComponentUtils.formatList(playerMessages, Component.literal(" "));
        server.sendSystemMessage(message);

        if (serverConfig.showPunishActionsInChat.getAsBoolean()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_KNOWN_ACCOUNTS_PERMISSION)) {
                    player.sendSystemMessage(message);
                }
            }
        }
    }

    private static MinecraftServer getServer() {
        return GlobalBanCommon.INSTANCE.getCommonUtils().getServer();
    }

    public static void onPunishPlayer(PunishPlayerEvent event) {
        PunishmentRecord record = event.punishment();
        ServerConfig serverConfig = GlobalBanConfig.SERVER_CONFIG;
        if (record.type().shouldKick && record.type().ipBased) {
            boolean alreadyBanned = false;

            for (ServerPlayer player : getServer().getPlayerList().getPlayers()) {
                if (player.getIpAddress().equals(record.playerIp())) {
                    player.connection.disconnect(record.getDisconnectScreenComponent());

                    if (serverConfig.autoBanPlayersByIP.getAsBoolean() && record.type() == PunishmentType.IP_BAN) {
                        PunishmentRecord newPunishment = PunishmentRecord.punishAlt(player.getUUID(), player.getName().getString(), record);

                        if (player.getUUID() == record.playerUuid()) {
                            alreadyBanned = true;
                        }

                        GlobalBanCommon.Events.PUNISH_PLAYER.emit(new PunishPlayerEvent(newPunishment));
                    }
                }
            }

            if (serverConfig.autoBanPlayersByIP.getAsBoolean() && record.type() == PunishmentType.IP_BAN && !alreadyBanned) {
                PunishmentRecord newPunishment = PunishmentRecord.punishAlt(record.playerUuid(), record.playerDisplayName(), record);
                GlobalBanCommon.Events.PUNISH_PLAYER.emit(new PunishPlayerEvent(newPunishment));
            }
        } else if (record.type().shouldKick) {
            ServerPlayer player = getServer().getPlayerList().getPlayer(record.playerUuid());

            if (player != null) {
                player.connection.disconnect(record.getDisconnectScreenComponent());
            }
        }

        getServer().sendSystemMessage(record.getChatMessage());
        if (serverConfig.showPunishActionsInChat.getAsBoolean()) {
            for (ServerPlayer player : getServer().getPlayerList().getPlayers()) {
                if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_BANS_PERMISSION)) {
                    player.sendSystemMessage(record.getChatMessage());
                }
            }
        }

        if (record.type().shouldRecord) {
            GlobalBanConfig.addPunishment(record);
        }
    }

    public static Integer onUnpunishPlayer(UnPunishPlayerEvent event) {
        PunishmentRecord record = event.punishment();
        boolean expired = event.expired();

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
                getServer().sendSystemMessage(parsed.toComponent());
                if (serverConfig.showPunishActionsInChat.getAsBoolean()) {
                    for (ServerPlayer player : getServer().getPlayerList().getPlayers()) {
                        if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_BANS_PERMISSION)) {
                            player.sendSystemMessage(parsed.toComponent());
                        }
                    }
                }
            }
        } else {
            GlobalBanCommon.INSTANCE.createInfoLog("Punishment expired: " + record.toString());
        }
        return GlobalBanConfig.removePunishment(record);
    }

    public static Integer onClearPunishments(ClearPunishmentEvent event) {
        if (event.ipAddress() != null) {
            return GlobalBanConfig.clearIpPunishments(event.ipAddress());
        } else if (event.uuid() != null) {
            return GlobalBanConfig.clearPunishments(event.uuid());
        }
        return 0;
    }

    public static void onCanPlayerLoginPre(CanPlayerLogin.Pre event) {
        if (event.socketAddress() != null) {
            String stringAddress = Helpers.stringifyAddress(event.socketAddress());
            GlobalBanCommon.UUID_TO_IP_CACHE.put(event.gameProfile().getId(), stringAddress);
            GlobalBanCommon.IP_TO_UUID_CACHE.computeIfAbsent(stringAddress, ip -> new HashSet<>()).add(event.gameProfile().getId());
        }
    }

    public static PunishmentRecord onCanPlayerLoginPost(CanPlayerLogin.Post event) {
        SocketAddress socketAddress = event.socketAddress();
        GameProfile gameProfile = event.gameProfile();
        PunishmentRecord punishment = null;

        if (socketAddress == null || gameProfile == null) {
            return null;
        }

        String ip = Helpers.stringifyAddress(socketAddress);

        for (var entry : GlobalBanConfig.getPunished()) {
            if (!entry.isExpired() &&
                    (entry.type() == PunishmentType.IP_BAN && entry.playerIp().equals(ip)) ||
                    (entry.type() == PunishmentType.BAN && entry.playerUuid().equals(gameProfile.getId()))
            ) {
                punishment = entry;
                break;
            }
        }

        if (punishment == null) {
            final var playerBans = GlobalBanConfig.isPlayerUUIDPunished(gameProfile.getId());
            final var ipBans = GlobalBanConfig.isIpPunished(ip);

            if (!playerBans.isEmpty()) {
                punishment = playerBans.stream().filter(p -> !p.isExpired()).findFirst().orElse(null);
            } else if (!ipBans.isEmpty()) {
                punishment = ipBans.stream().filter(p -> !p.isExpired()).findFirst().orElse(null);
            }
        }

        if (punishment != null) {
            if (punishment.type() == PunishmentType.IP_BAN && GlobalBanConfig.SERVER_CONFIG.autoBanPlayersByIP.getAsBoolean()) {
                PunishmentRecord newPunishment = PunishmentRecord.punishAlt(gameProfile.getId(), gameProfile.getName(), punishment);
                GlobalBanCommon.Events.PUNISH_PLAYER.emit(new PunishPlayerEvent(newPunishment));
            }
        }

        return punishment;
    }
}
