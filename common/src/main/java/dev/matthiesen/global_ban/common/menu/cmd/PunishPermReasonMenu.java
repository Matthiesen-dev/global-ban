package dev.matthiesen.global_ban.common.menu.cmd;

import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.menu.PaginatedScreen;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PunishPermReasonMenu extends PaginatedScreen {
    private final ServerPlayer executor;
    private final PunishmentType type;
    private final ServerPlayer target;
    private final long duration = -1;

    public PunishPermReasonMenu(ServerPlayer player, PunishmentType type, ServerPlayer target) {
        super(player);
        this.executor = player;
        this.type = type;
        this.target = target;
    }

    public static Button create(ServerPlayer executor, PunishmentType type, ServerPlayer target) {
        return GooeyButton.builder()
                .display(MenuUtilities.getPlayerHeadItem(target))
                .onClick(action ->
                        new PunishPermReasonMenu(executor, type, target).open()
                )
                .build();
    }

    private String getLabel() {
        return switch (type) {
            case BAN ->  "Ban Reason";
            case IP_BAN -> "IP Ban Reason";
            case KICK -> "Kick Reason";
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
