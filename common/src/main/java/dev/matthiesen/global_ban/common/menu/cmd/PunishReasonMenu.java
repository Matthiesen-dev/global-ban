package dev.matthiesen.global_ban.common.menu.cmd;

import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.*;
import dev.matthiesen.global_ban.common.menu.PaginatedScreen;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.api.server_menu.button.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PunishReasonMenu extends PaginatedScreen {
    private final ServerPlayer executor;
    private final PunishmentType type;
    private final ServerPlayer target;
    private final long duration;
    private final boolean isTemp;

    public PunishReasonMenu(ServerPlayer player, PunishmentType type, ServerPlayer target, long duration) {
        super(player);
        this.executor = player;
        this.type = type;
        this.target = target;
        this.duration = duration;
        this.isTemp = duration != PunishmentRecord.NON_EXPIRING_PUNISHMENT;
    }

    public static Button create(ServerPlayer executor, PunishmentType type, ServerPlayer target, long duration) {
        return ServerButton.builder()
                .display(MenuUtilities.getPlayerHeadItem(target))
                .onClick(action ->
                        new PunishReasonMenu(executor, type, target, duration).open()
                )
                .build();
    }

    private String getLabel() {
        return switch (type) {
            case BAN ->  isTemp ? "Temporary Ban Reason" : "Ban Reason";
            case IP_BAN -> isTemp ? "Temporary IP Ban Reason" : "IP Ban Reason";
            case KICK -> "Kick Reason";
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> getReasons() {
        var reasons = switch (type) {
            case BAN, IP_BAN -> isTemp ? GlobalBanConfig.SERVER_CONFIG.gui_tempBanReasons.get() : GlobalBanConfig.SERVER_CONFIG.gui_banReasons.get();
            case KICK -> GlobalBanConfig.SERVER_CONFIG.gui_kickReasons.get();
        };
        return (List<String>) reasons;
    }

    @Override
    protected Permission requiredPermission() {
        return switch (type) {
            case BAN -> isTemp ? PermissionRegistry.COMMAND_TEMP_BAN_PERMISSION : PermissionRegistry.COMMAND_BAN_PERMISSION;
            case IP_BAN -> isTemp ? PermissionRegistry.COMMAND_TEMP_BAN_IP_PERMISSION : PermissionRegistry.COMMAND_BAN_IP_PERMISSION;
            case KICK -> PermissionRegistry.COMMAND_KICK_PERMISSION;
        };
    }

    @Override
    public Component getDisplayTitle() {
        return Component.literal(getLabel());
    }

    public List<ServerButton> getReasonButtons() {
        return getReasons().stream()
                .map(reason -> ServerButton.builder()
                        .display(MenuUtilities.getReasonItem(reason))
                        .onClick(action -> new PunishConfirmationMenu(executor, type, target, duration, reason).open())
                        .build())
                .toList();
    }

    @Override
    public List<Button> getContentButtons() {
        return List.copyOf(getReasonButtons());
    }
}
