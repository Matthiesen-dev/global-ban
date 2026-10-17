package dev.matthiesen.global_ban.common.registry;

import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.*;
import dev.matthiesen.global_ban.common.def.*;
import dev.matthiesen.global_ban.common.def.events.*;
import dev.matthiesen.global_ban.common.utils.*;
import dev.matthiesen.matthiesen_core.common.api.events.config.ConfigEvent;
import dev.matthiesen.matthiesen_core.common.api.events.server.*;
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

@SuppressWarnings("unused")
public final class EventHandlers {
    public static void onServerStarting(ServerEvent.Starting starting) {
        IpCache.handleServerStarting();
    }

    public static void onServerStarted(ServerEvent.Started started) {
        Schedulers.register();
        GlobalBanCommon.INSTANCE.createInfoLog("Global Ban is running...");
    }

    public static void onServerStopping(ServerEvent.Stopping stopping) {
        IpCache.writeCacheToFile();
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
        Set<UUID> knownAccounts = IpCache.getUuidByIp(ip);

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

            // If punishment is an IP ban, we need to ensure that the IP is not a private IP address scope before proceeding with the punishment.
            if (Helpers.isPrivateIpAddress(record.playerIp) ) {
                GlobalBanCommon.INSTANCE.createWarnLog("Admin " + record.punisherDisplayName + " (" + record.punisherUuid + ") attempted to punish IP " + record.playerIp + ", which is a private IP address. This action has been blocked.");
                ServerPlayer punisher = getServer().getPlayerList().getPlayer(record.punisherUuid);
                if (punisher != null) {
                    punisher.sendSystemMessage(Component.literal("You attempted to punish IP " + record.playerIp + ", which is a private IP address. This action has been blocked.").withStyle(ChatFormatting.RED));
                }
                return;
            }

            for (ServerPlayer player : getServer().getPlayerList().getPlayers()) {
                if (player.getIpAddress().equals(record.playerIp)) {
                    player.connection.disconnect(record.getDisconnectScreenComponent());

                    if (serverConfig.autoBanPlayersByIP.getAsBoolean() && record.type() == PunishmentType.IP_BAN) {
                        PunishmentRecord newPunishment = PunishmentRecord.punishAlt(player.getUUID(), player.getName().getString(), record);

                        if (player.getUUID() == record.playerUuid) {
                            alreadyBanned = true;
                        }

                        GlobalBanCommon.Events.PUNISH_PLAYER.emit(new PunishPlayerEvent(newPunishment));
                    }
                }
            }

            if (serverConfig.autoBanPlayersByIP.getAsBoolean() && record.type() == PunishmentType.IP_BAN && !alreadyBanned) {
                PunishmentRecord newPunishment = PunishmentRecord.punishAlt(record.playerUuid, record.playerDisplayName, record);
                GlobalBanCommon.Events.PUNISH_PLAYER.emit(new PunishPlayerEvent(newPunishment));
            }
        } else if (record.type().shouldKick) {
            ServerPlayer player = getServer().getPlayerList().getPlayer(record.playerUuid);

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
        return GlobalBanConfig.removePunishment(record, event.fromRemote());
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
            IpCache.addToCache(event.gameProfile().getId(), stringAddress);
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
                    (entry.type() == PunishmentType.IP_BAN && entry.playerIp.equals(ip)) ||
                    (entry.type() == PunishmentType.BAN && entry.playerUuid.equals(gameProfile.getId()))
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
