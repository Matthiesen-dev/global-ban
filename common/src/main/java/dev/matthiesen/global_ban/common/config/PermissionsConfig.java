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
    public ModConfigSpec.EnumValue<PermissionLevel> commands_root;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_reload;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_kick;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_ban;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_ban_ip;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_temp_ban;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_temp_ban_ip;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_ban_list;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_pardon;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_unban;
    public ModConfigSpec.EnumValue<PermissionLevel> commands_unban_ip;

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

        builder.comment("Permissions for the Global Ban commands. These permissions control access to the /global-bans command and its subcommands.")
                .push("commands");

        commands_root = builder.comment("Permission level required to use the /global-bans command")
                .defineEnum("root", PermissionLevel.ALL_COMMANDS);
        commands_reload = builder.comment("Permission level required to use the /global-bans reload command")
                .defineEnum("reload", PermissionLevel.ALL_COMMANDS);
        commands_kick = builder.comment("Permission level required to use the /kick command")
                .defineEnum("kick", PermissionLevel.ALL_COMMANDS);
        commands_ban = builder.comment("Permission level required to use the /ban command")
                .defineEnum("ban", PermissionLevel.ALL_COMMANDS);
        commands_ban_ip = builder.comment("Permission level required to use the /ban-ip command")
                .defineEnum("ban-ip", PermissionLevel.ALL_COMMANDS);
        commands_temp_ban = builder.comment("Permission level required to use the /temp-ban command")
                .defineEnum("temp-ban", PermissionLevel.ALL_COMMANDS);
        commands_temp_ban_ip = builder.comment("Permission level required to use the /temp-ban-ip command")
                .defineEnum("temp-ban-ip", PermissionLevel.ALL_COMMANDS);
        commands_ban_list = builder.comment("Permission level required to use the /ban-list command")
                .defineEnum("ban-list", PermissionLevel.ALL_COMMANDS);
        commands_pardon = builder.comment("Permission level required to use the /pardon command")
                .defineEnum("pardon", PermissionLevel.ALL_COMMANDS);
        commands_unban = builder.comment("Permission level required to use the /unban command")
                .defineEnum("unban", PermissionLevel.ALL_COMMANDS);
        commands_unban_ip = builder.comment("Permission level required to use the /unban-ip command")
                .defineEnum("unban-ip", PermissionLevel.ALL_COMMANDS);

        builder.pop(); // commands

        builder.pop(); // permissions
    }
}
