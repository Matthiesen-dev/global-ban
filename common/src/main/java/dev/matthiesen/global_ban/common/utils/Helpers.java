package dev.matthiesen.global_ban.common.utils;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import net.minecraft.network.chat.Component;

import java.net.SocketAddress;
import java.util.Map;

public final class Helpers {

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
