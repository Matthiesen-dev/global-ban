package dev.matthiesen.global_ban.common.config;

import dev.matthiesen.matthiesen_core.common.api.text_parsers.BuiltInTextParsers;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.UUID;

public final class ServerConfig {

    public ModConfigSpec.ConfigValue<String> serverUUID;
    public ModConfigSpec.BooleanValue autoBanPlayersByIP;
    public ModConfigSpec.BooleanValue showKnownAccounts;
    public ModConfigSpec.ConfigValue<String> defaultBanReason;
    public ModConfigSpec.EnumValue<BuiltInTextParsers> textParser;

    public ModConfigSpec.ConfigValue<List<? extends String>> messages_banChatMessage;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_tempBanChatMessage;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_ipBanChatMessage;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_tempIpBanChatMessage;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_kickMessage;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_unbanChatMessage;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_ipUnbanChatMessage;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_pardonChatMessage;

    public ModConfigSpec.ConfigValue<List<? extends String>> messages_banScreen;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_tempBanScreen;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_ipBanScreen;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_tempIpBanScreen;
    public ModConfigSpec.ConfigValue<List<? extends String>> messages_kickScreen;

    public ServerConfig(ModConfigSpec.Builder builder) {
        builder.comment("Server Config").push("server");

        serverUUID = builder.comment(
                "Server UUID for the global ban system",
                "This is only used when syncing bans between servers, and should be unique for each server. If you want to reset your server's UUID, delete this config and restart the server."
                )
                .define("serverUUID", UUID.randomUUID().toString());
        autoBanPlayersByIP = builder.comment("Automatically ban alt accounts by IP address when a player is banned")
                .define("autoBanPlayersByIP", false);
        showKnownAccounts = builder.comment("Show known accounts for a player when they join the server")
                .define("showKnownAccounts", false);

        defaultBanReason = builder.comment("Default ban reason to use when banning a player without a reason")
                .define("defaultBanReason", "Unknown reason");

        textParser = builder.comment("The text parser to use for parsing text")
                .defineEnum("textParser", BuiltInTextParsers.VANILLA);

        builder.comment("Messages to display to players when they are banned, temp banned, or kicked")
                .push("messages");

        messages_banChatMessage = builder.comment("The message to display to players when they are banned (chat)")
                .defineList(
                        "banChatMessage",
                        List.of(
                                "Player &c%player% &rhas been banned by &6%operator%&r!",
                                "Reason: &e%reason%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );
        messages_tempBanChatMessage = builder.comment("The message to display to players when they are temp banned (chat)")
                .defineList(
                        "tempBanChatMessage",
                        List.of(
                                "Player &c%player% &rhas been temp banned by &6%operator%&r!",
                                "Reason: &e%reason%&r",
                                "Expires in: &e%expiration_time%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );
        messages_ipBanChatMessage = builder.comment("The message to display to players when they are IP banned (chat)")
                .defineList(
                        "ipBanChatMessage",
                        List.of(
                                "Player &c%player% &rhas been IP banned by &6%operator%&r!",
                                "Reason: &e%reason%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );
        messages_tempIpBanChatMessage = builder.comment("The message to display to players when they are temp IP banned (chat)")
                .defineList(
                        "tempIpBanChatMessage",
                        List.of(
                                "Player &c%player% &rhas been temp IP banned by &6%operator%&r!",
                                "Reason: &e%reason%&r",
                                "Expires in: &e%expiration_time%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );
        messages_kickMessage = builder.comment("The message to display to players when they are kicked")
                .defineList(
                        "kickMessage",
                        List.of(
                                "Player &c%player% &rhas been kicked by &6%operator%&r!",
                                "Reason: &e%reason%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );

        messages_unbanChatMessage = builder.comment("The message to display to players when they are unbanned (chat)")
                .defineList(
                        "unbanChatMessage",
                        List.of(
                                "Player &c%player% &rhas been unbanned by &6%operator%&r!"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );

        messages_ipUnbanChatMessage = builder.comment("The message to display to players when they are IP unbanned (chat)")
                .defineList(
                        "ipUnbanChatMessage",
                        List.of(
                                "Player &c%player% &rhas been IP unbanned by &6%operator%&r!"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );

        messages_pardonChatMessage = builder.comment("The message to display to players when they are pardoned (chat)")
                .defineList(
                        "pardonChatMessage",
                        List.of(
                                "Player &c%player% &rhas been pardoned by &6%operator%&r!"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );

        messages_banScreen = builder.comment("The message to display to players when they are banned (disconnect screen)")
                .defineList(
                        "banScreen",
                        List.of(
                                "&c&lYou have been banned from this server!&r",
                                "&7Reason: &e%reason%&r",
                                "&7By: &6%operator%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );
        messages_tempBanScreen = builder.comment("The message to display to players when they are temp banned (disconnect screen)")
                .defineList(
                        "tempBanScreen",
                        List.of(
                                "&c&lYou have been temporarily banned from this server!&r",
                                "&7Reason: &e%reason%&r",
                                "&7By: &6%operator%&r",
                                "&7Expires in: &e%expiration_time%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );
        messages_ipBanScreen = builder.comment("The message to display to players when they are IP banned (disconnect screen)")
                .defineList(
                        "ipBanScreen",
                        List.of(
                                "&c&lYou have been banned from this server!&r",
                                "&7Reason: &e%reason%&r",
                                "&7By: &6%operator%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );
        messages_tempIpBanScreen = builder.comment("The message to display to players when they are temp IP banned (disconnect screen)")
                .defineList(
                        "tempIpBanScreen",
                        List.of(
                                "&c&lYou have been temporarily banned from this server!&r",
                                "&7Reason: &e%reason%&r",
                                "&7By: &6%operator%&r",
                                "&7Expires in: &e%expiration_time%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );
        messages_kickScreen = builder.comment("The message to display to players when they are kicked (disconnect screen)")
                .defineList(
                        "kickScreen",
                        List.of(
                                "&c&lYou have been kicked from this server!&r",
                                "&7Reason: &e%reason%&r",
                                "&7By: &6%operator%&r"
                        ),
                        () -> "",
                        obj -> obj instanceof String
                );

        builder.pop(); // messages

        builder.pop(); // server
    }
}
