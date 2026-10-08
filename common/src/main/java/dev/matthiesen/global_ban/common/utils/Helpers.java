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
import java.util.*;

public final class Helpers {
    public static boolean isPunishableBy(ServerUser profile, CommandSourceStack source) {
        if (profile == null) {
            return true;
        }
        GameProfile gameProfile = new GameProfile(profile.getUUID(), profile.getUsername());
        var server = GlobalBanCommon.INSTANCE.getCommonUtils().getServer();
        var entry = server.getPlayerList().getOps().get(gameProfile);
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

    public static List<ServerUser> lookupServerUsers(String usernameOrIp) {
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
                return List.of(serverUser);
            }

            GameProfileCache profileCache = GlobalBanCommon.INSTANCE.getCommonUtils().getServer().getProfileCache();
            if (profileCache == null) {
                return List.of(new ServerUser(usernameOrIp));
            }

            Set<UUID> uuidCache = GlobalBanCommon.IP_TO_UUID_CACHE.get(usernameOrIp);

            List<ServerUser> users = new ArrayList<>();

            if (uuidCache == null || uuidCache.isEmpty()) {
                return List.of(new ServerUser(usernameOrIp));
            } else {
                for (var uuidEntry : uuidCache) {
                    var optional = profileCache.get(uuidEntry);
                    optional.ifPresent(profile -> users.add(new ServerUser(profile.getName())));
                }
            }

            if (!users.isEmpty()) {
                return users;
            }

            GameProfile profile = null;
            var possibleProfile = profileCache.get(usernameOrIp);
            if (possibleProfile.isPresent()) {
                profile = possibleProfile.orElse(null);
            }
            if (profile == null) {
                return List.of(new ServerUser("UnknownPlayer"));
            }
            return List.of(new ServerUser(profile.getId()));
        } catch (Exception e) {
            GlobalBanCommon.INSTANCE.createErrorLog("Failed to lookup server user for: " + usernameOrIp, e);
            return List.of();
        }
    }

    public static ServerUser lookupServerUser(String usernameOrIp) {
        return lookupServerUsers(usernameOrIp).stream().findFirst().orElse(null);
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

    public static long parseDuration(String text) throws NumberFormatException {
        text = text.toLowerCase(Locale.ROOT);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            String[] times = text.replaceAll("([a-z]+)", "$1|").split("\\|");
            long time = 0;
            for (String x : times) {
                String numberOnly = x.replaceAll("[a-z]", "");
                String suffixOnly = x.replaceAll("[^a-z]", "");

                time = (long) (time + switch (suffixOnly) {
                    case "c" -> Double.parseDouble(numberOnly) * 60 * 60 * 24L * 365L * 100L;
                    case "y", "year", "years" -> Double.parseDouble(numberOnly) * 60 * 60 * 24L * 365L;
                    case "mo", "month", "months" -> Double.parseDouble(numberOnly) * 60 * 60 * 24L * 30L;
                    case "w", "week", "weeks" -> Double.parseDouble(numberOnly) * 60 * 60 * 24L * 7L;
                    case "d", "day", "days" -> Double.parseDouble(numberOnly) * 60 * 60 * 24;
                    case "h", "hour", "hours" -> Double.parseDouble(numberOnly) * 60 * 60;
                    case "m", "minute", "minutes" -> Double.parseDouble(numberOnly) * 60;
                    default -> Double.parseDouble(numberOnly);
                });
            }
            return time;
        }
    }
}
