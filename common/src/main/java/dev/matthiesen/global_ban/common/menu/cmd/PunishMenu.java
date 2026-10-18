package dev.matthiesen.global_ban.common.menu.cmd;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.*;
import dev.matthiesen.global_ban.common.menu.PaginatedScreen;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.Helpers;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.api.server_menu.button.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class PunishMenu extends PaginatedScreen {
    private final ServerPlayer executor;
    private final PunishmentType type;
    private final boolean temp;

    public PunishMenu(ServerPlayer player, PunishmentType type, boolean temp) {
        super(player);
        this.executor = player;
        this.type = type;
        this.temp = temp;
    }

    private String getLabel() {
        return switch (type) {
            case BAN -> temp ? "Temporary Ban" : "Ban";
            case IP_BAN -> temp ? "Temporary IP Ban" : "IP Ban";
            case KICK -> "Kick";
        };
    }

    @Override
    protected Permission requiredPermission() {
        return switch (type) {
            case BAN -> temp ? PermissionRegistry.COMMAND_TEMP_BAN_PERMISSION : PermissionRegistry.COMMAND_BAN_PERMISSION;
            case IP_BAN -> temp ? PermissionRegistry.COMMAND_TEMP_BAN_IP_PERMISSION : PermissionRegistry.COMMAND_BAN_IP_PERMISSION;
            case KICK -> PermissionRegistry.COMMAND_KICK_PERMISSION;
        };
    }

    @Override
    public Component getDisplayTitle() {
        return Component.literal(getLabel());
    }

    @Override
    public List<Button> getContentButtons() {
        List<ServerPlayer> onlinePlayers = GlobalBanCommon.INSTANCE.getCommonUtils().getServer().getPlayerList().getPlayers();
        List<Button> buttons = new ArrayList<>();

        for (ServerPlayer player : onlinePlayers) {
            if (player.getUUID().equals(executor.getUUID())) continue;
            if (!Helpers.isPunishableBy(player, executor)) continue;
            if (temp) {
                buttons.add(PunishDurationMenu.create(executor, type, player));
            } else {
                buttons.add(PunishReasonMenu.create(executor, type, player, PunishmentRecord.NON_EXPIRING_PUNISHMENT));
            }
        }

        return buttons;
    }
}
