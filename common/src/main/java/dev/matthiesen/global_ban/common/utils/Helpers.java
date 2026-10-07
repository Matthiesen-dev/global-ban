package dev.matthiesen.global_ban.common.utils;

import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.net.SocketAddress;
import java.util.Map;

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
