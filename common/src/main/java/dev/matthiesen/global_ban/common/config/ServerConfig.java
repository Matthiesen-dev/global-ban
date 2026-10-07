package dev.matthiesen.global_ban.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.UUID;

public final class ServerConfig {

    public ModConfigSpec.ConfigValue<String> serverUUID;
    public ModConfigSpec.BooleanValue autoBanPlayersByIP;

    public ServerConfig(ModConfigSpec.Builder builder) {
        builder.comment("Server Config").push("server");

        serverUUID = builder.comment("Server UUID for the global ban system")
                .define("serverUUID", UUID.randomUUID().toString());
        autoBanPlayersByIP = builder.comment("Automatically ban alt accounts by IP address when a player is banned")
                .define("autoBanPlayersByIP", false);

        builder.pop();
    }
}
