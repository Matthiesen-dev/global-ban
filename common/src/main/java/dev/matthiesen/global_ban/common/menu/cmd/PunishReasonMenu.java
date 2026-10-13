package dev.matthiesen.global_ban.common.menu.cmd;

import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.menu.PaginatedScreen;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PunishReasonMenu extends PaginatedScreen {
    private final ServerPlayer executor;
    private final PunishmentType type;
    private final ServerPlayer target;
    private final long duration;
    private final boolean isTemp;

    public PunishReasonMenu(ServerPlayer player, PunishmentType type, ServerPlayer target, long duration, boolean isTemp) {
        super(player);
        this.executor = player;
        this.type = type;
        this.target = target;
        this.duration = duration;
        this.isTemp = isTemp;
    }

    public static Button create(ServerPlayer executor, PunishmentType type, ServerPlayer target, long duration, boolean isTemp) {
        return GooeyButton.builder()
                .display(MenuUtilities.getPlayerHeadItem(target))
                .onClick(action ->
                        new PunishReasonMenu(executor, type, target, duration, isTemp).open()
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
    public Component getDisplayTitle() {
        return Component.literal(getLabel());
    }

    public List<GooeyButton> getReasonButtons() {
        return getReasons().stream()
                .map(reason -> GooeyButton.builder()
                        .display(MenuUtilities.getPageItem(getReasons().indexOf(reason) + 1, getReasons().size()))
                        .onClick(action -> new PunishConfirmationMenu(executor, type, target, duration, reason).open())
                        .build())
                .toList();
    }

    @Override
    public List<Button> getContentButtons() {
        return List.copyOf(getReasonButtons());
    }
}
