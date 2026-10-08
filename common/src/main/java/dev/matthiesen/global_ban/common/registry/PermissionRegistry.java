package dev.matthiesen.global_ban.common.registry;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.api.permissions.PermissionLevel;
import dev.matthiesen.matthiesen_core.common.utility.AbstractPermission;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class PermissionRegistry {
    public static final Permission SEE_KNOWN_ACCOUNTS_PERMISSION = register("see_known_accounts",
            GlobalBanConfig.PERMISSIONS_CONFIG.seeKnownAccounts.get());
    public static final Permission SEE_BANS_PERMISSION = register("see_bans",
            GlobalBanConfig.PERMISSIONS_CONFIG.seeBans.get());
    public static final Permission CAN_BAN_ADMINS_PERMISSION = register("can_ban_admins",
            GlobalBanConfig.PERMISSIONS_CONFIG.canBanAdmins.get());
    public static final Permission BLOCK_PUNISHMENTS_PERMISSION = register("block_punishments",
            GlobalBanConfig.PERMISSIONS_CONFIG.blockPunishments.get());
    public static final Permission COMMAND_ROOT_PERMISSION = register("command.global_ban",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_root.get());
    public static final Permission COMMAND_RELOAD_PERMISSION = register("command.global_ban.reload",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_root_reload.get());
    public static final Permission COMMAND_IMPORT_PERMISSION = register("command.global_ban.import",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_root_import.get());
    public static final Permission COMMAND_KICK_PERMISSION = register("command.kick",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_kick.get());
    public static final Permission COMMAND_BAN_PERMISSION = register("command.ban",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_ban.get());
    public static final Permission COMMAND_BAN_IP_PERMISSION = register("command.ban_ip",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_ban_ip.get());
    public static final Permission COMMAND_TEMP_BAN_PERMISSION = register("command.temp_ban",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_temp_ban.get());
    public static final Permission COMMAND_TEMP_BAN_IP_PERMISSION = register("command.temp_ban_ip",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_temp_ban_ip.get());
    public static final Permission COMMAND_BAN_LIST_PERMISSION = register("command.ban_list",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_ban_list.get());
    public static final Permission COMMAND_PARDON_PERMISSION = register("command.pardon",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_pardon.get());
    public static final Permission COMMAND_PARDON_IP_PERMISSION = register("command.pardon_ip",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_pardon_ip.get());
    public static final Permission COMMAND_UNBAN_PERMISSION = register("command.unban",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_unban.get());
    public static final Permission COMMAND_UNBAN_IP_PERMISSION = register("command.unban_ip",
            GlobalBanConfig.PERMISSIONS_CONFIG.commands_unban_ip.get());

    public static void init() {}

    public static boolean checkPermission(CommandSourceStack source, Permission permission) {
        return GlobalBanCommon.INSTANCE.getPermissionsManager().getPermissionValidator().hasPermission(source, permission);
    }

    public static boolean checkPermission(ServerPlayer source, Permission permission) {
        return GlobalBanCommon.INSTANCE.getPermissionsManager().getPermissionValidator().hasPermission(source, permission);
    }

    private static Permission register(String node, PermissionLevel level) {
        Permission permission = new AbstractPermission(node, level) {
            @Override
            protected String getModId() {
                return GlobalBanCommon.MOD_ID;
            }

            @Override
            protected String getPermissionNamespace() {
                return "GlobalBan";
            }
        };
        GlobalBanCommon.INSTANCE.getPermissionsManager().registerPermission(permission);
        return permission;
    }
}
