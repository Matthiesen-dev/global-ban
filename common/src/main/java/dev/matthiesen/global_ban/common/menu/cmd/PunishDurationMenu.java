package dev.matthiesen.global_ban.common.menu.cmd;

import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.menu.PaginatedScreen;
import dev.matthiesen.global_ban.common.utils.*;
import dev.matthiesen.matthiesen_core.common.api.server_menu.button.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PunishDurationMenu extends PaginatedScreen {
    private final ServerPlayer executor;
    private final PunishmentType type;
    private final ServerPlayer target;

    public PunishDurationMenu(ServerPlayer player, PunishmentType type, ServerPlayer target) {
        super(player);
        this.executor = player;
        this.type = type;
        this.target = target;
    }

    public static Button create(ServerPlayer executor, PunishmentType type, ServerPlayer target) {
        return ServerButton.builder()
                .display(MenuUtilities.getPlayerHeadItem(target))
                .onClick(action ->
                        new PunishDurationMenu(executor, type, target).open()
                )
                .build();
    }

    private String getLabel() {
        return switch (type) {
            case BAN -> "Temporary Ban Duration";
            case IP_BAN -> "Temporary IP Ban Duration";
            case KICK -> "How did you get here? Kicks don't have durations!";
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> getDurations() {
        var durations = GlobalBanConfig.SERVER_CONFIG.gui_tempDurations.get();
        return (List<String>) durations;
    }

    private List<ServerButton> getDurationButtons() {
        return getDurations().stream()
                .map(duration -> ServerButton.builder()
                        .display(MenuUtilities.getDurationItem(duration))
                        .onClick(action -> new PunishReasonMenu(executor, type, target, Helpers.parseDuration(duration)).open())
                        .build())
                .toList();
    }

    @Override
    public Component getDisplayTitle() {
        return Component.literal(getLabel());
    }

    @Override
    public List<Button> getContentButtons() {
        return List.copyOf(getDurationButtons());
    }
}
