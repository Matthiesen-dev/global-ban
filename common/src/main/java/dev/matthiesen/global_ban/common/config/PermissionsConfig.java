package dev.matthiesen.global_ban.common.config;

import dev.matthiesen.matthiesen_core.common.api.permissions.PermissionLevel;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class PermissionsConfig {

    // Permissions
    public ModConfigSpec.EnumValue<PermissionLevel> seeKnownAccounts;
    public ModConfigSpec.EnumValue<PermissionLevel> seeBans;
    public ModConfigSpec.EnumValue<PermissionLevel> canBanAdmins;
    public ModConfigSpec.EnumValue<PermissionLevel> blockPunishments;

    // Command Permissions

    public PermissionsConfig(ModConfigSpec.Builder builder) {
        builder.comment("Permissions for the Global Ban mod. These permissions control access to various features and commands within the mod.")
                .push("permissions");

        seeKnownAccounts = builder.comment("Permission level required to see known accounts for a player when they join the server")
                .defineEnum("seeKnownAccounts", PermissionLevel.ALL_COMMANDS);
        seeBans = builder.comment("Permission level required to see bans in chat when a player is banned")
                .defineEnum("seeBans", PermissionLevel.ALL_COMMANDS);
        canBanAdmins = builder.comment("Permission level required to ban admins")
                .defineEnum("canBanAdmins", PermissionLevel.ALL_COMMANDS);
        blockPunishments = builder.comment("Permission level required to block punishments")
                .defineEnum("blockPunishments", PermissionLevel.ALL_COMMANDS);

        builder.pop(); // permissions
    }
}
