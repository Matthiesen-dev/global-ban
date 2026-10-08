package dev.matthiesen.global_ban.common.utils;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.matthiesen_core.common.utility.player_data.ServerUser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.GameProfileCache;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Helpers {
    public static boolean isPunishableBy(GameProfile profile, CommandSourceStack source) {
        if (profile == null) {
            return true;
        }

        var server = GlobalBanCommon.INSTANCE.getCommonUtils().getServer();
        var entry = server.getPlayerList().getOps().get(profile);

        boolean canBanAdmins = PermissionRegistry.checkPermission(source, PermissionRegistry.CAN_BAN_ADMINS_PERMISSION);
        boolean blocksPunishments = PermissionRegistry.checkPermission(source, PermissionRegistry.BLOCK_PUNISHMENTS_PERMISSION);

        boolean permission = canBanAdmins && !blocksPunishments;

        return (server.name().equals("Server") && source.getEntity() == null) || ((entry == null || source.hasPermission(entry.getLevel())))
                && !blocksPunishments
                && permission;
    }

    public static List<PunishmentRecord> getPunishmentsForUser(ServerUser user) {
        return GlobalBanConfig.getPunished().stream()
                .filter(p -> !p.isExpired() && (p.playerUuid().equals(user.getUUID())))
                .toList();
    }

    public static ServerUser lookupServerUser(String usernameOrIp) {
        try {
            boolean isUuid;
            boolean isIpLike = InetAddresses.isInetAddress(usernameOrIp);

            UUID uuid = null;

            try {
                uuid = UUID.fromString(usernameOrIp);
                isUuid = true;
            } catch (IllegalArgumentException e) {
                isUuid = false;
            }

            ServerUser serverUser = null;
            if (isUuid) {
                serverUser = new ServerUser(uuid);
            } else if (!isIpLike) {
                serverUser = new ServerUser(usernameOrIp);
            }

            if (serverUser != null) {
                return serverUser;
            }

            GameProfileCache profileCache = GlobalBanCommon.INSTANCE.getCommonUtils().getServer().getProfileCache();
            if (profileCache == null) {
                return new ServerUser(usernameOrIp);
            }

            Set<UUID> uuidCache = GlobalBanCommon.IP_TO_UUID_CACHE.get(usernameOrIp);
            if (uuidCache == null || uuidCache.isEmpty()) {
                return new ServerUser(usernameOrIp);
            } else {
                for (var uuidEntry : uuidCache) {
                    var optional = profileCache.get(uuidEntry);
                    if (optional.isPresent()) {
                        return new ServerUser(optional.get().getName());
                    }
                }
            }

            GameProfile profile = null;
            var possibleProfile = profileCache.get(usernameOrIp);
            if (possibleProfile.isPresent()) {
                profile = possibleProfile.orElse(null);
            }
            if (profile == null) {
                return new ServerUser("UnknownPlayer");
            }
            return new ServerUser(profile.getId());
        } catch (Exception e) {
            GlobalBanCommon.INSTANCE.createErrorLog("Failed to lookup server user for: " + usernameOrIp, e);
            return null;
        }
    }

    public static String stringifyAddress(SocketAddress socketAddress) {
        String string = socketAddress.toString();
        if (string.contains("/")) {
            string = string.substring(string.indexOf(47) + 1);
        }

        if (string.contains(":")) {
            string = string.substring(0, string.indexOf(58));
        }

        return string;
    }

    public static ProcessedMessage processPlaceholders(String message, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return new ProcessedMessage(message);
    }

    public record ProcessedMessage(String message) {
        public Component toComponent() {
            return GlobalBanCommon.INSTANCE.getTextParserManager().getTextParser(GlobalBanConfig.SERVER_CONFIG.textParser.get()).parse(message);
        }
    }
}
