package dev.matthiesen.global_ban.common.menu;

import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BanListMenu extends PaginatedScreen {
    private final List<PunishmentRecord> punishments;

    public BanListMenu(ServerPlayer player, List<PunishmentRecord> punishments) {
        super(player);
        this.punishments = punishments;
    }

    public BanListMenu(ServerPlayer player) {
        super(player);
        List<PunishmentRecord> punishments = GlobalBanConfig.getPunished();
        punishments.sort(Comparator.comparingLong(p -> -p.timestamp()));
        this.punishments = new ArrayList<>(punishments);
    }

    @Override
    public Component getDisplayTitle() {
        return Component.literal("Global Ban List");
    }

    @Override
    public List<Button> getContentButtons() {
        List<Button> buttons = new ArrayList<>();

        for (PunishmentRecord record : punishments) {
            Button button = GooeyButton.builder()
                    .display(MenuUtilities.getRecordItem(record))
                    .onClick(action -> new PunishmentMenu(player, record).open())
                    .build();
            buttons.add(button);
        }

        return buttons;
    }
}
