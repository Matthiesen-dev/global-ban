package dev.matthiesen.global_ban.common.menu.cmd;

import ca.landonjw.gooeylibs2.api.button.Button;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.menu.PaginatedScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PunishTempReasonMenu extends PaginatedScreen {
    private final ServerPlayer executor;
    private final PunishmentType type;
    private final ServerPlayer target;
    private final long duration;

    public PunishTempReasonMenu(ServerPlayer player, PunishmentType type, ServerPlayer target, long duration) {
        super(player);
        this.executor = player;
        this.type = type;
        this.target = target;
        this.duration = duration;
    }

    private String getLabel() {
        return switch (type) {
            case BAN -> "Temporary Ban Reason";
            case IP_BAN -> "Temporary IP Ban Reason";
            case KICK -> "Kick";
        };
    }

    @Override
    public Component getDisplayTitle() {
        return Component.literal(getLabel());
    }

    @Override
    public List<Button> getContentButtons() {
        // TODO: Implement buttons selection for Reason then forward to Confirmation Menu
        return List.of();
    }
}
