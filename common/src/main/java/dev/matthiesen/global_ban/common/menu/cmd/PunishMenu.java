package dev.matthiesen.global_ban.common.menu.cmd;

import ca.landonjw.gooeylibs2.api.button.Button;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.menu.PaginatedScreen;
import dev.matthiesen.global_ban.common.utils.Helpers;
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
                buttons.add(PunishTempDurationMenu.create(executor, type, player));
            } else {
                buttons.add(PunishReasonMenu.create(executor, type, player, PunishmentRecord.NON_EXPIRING_PUNISHMENT, false));
            }
        }

        return buttons;
    }
}
