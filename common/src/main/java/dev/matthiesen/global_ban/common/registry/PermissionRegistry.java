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
