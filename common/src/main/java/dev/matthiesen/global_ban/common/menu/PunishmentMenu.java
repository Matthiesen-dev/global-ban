package dev.matthiesen.global_ban.common.menu;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record PunishmentMenu(ServerPlayer player, PunishmentRecord punishment) {
    public String getLabel() {
        return switch (punishment.type()) {
            case BAN -> punishment.playerDisplayName();
            case IP_BAN -> punishment.playerIp();
            default -> "Unknown";
        };
    }

    public Component getPageTitle() {
        return Component.literal("Punishment Record: " + getLabel());
    }

    public List<Button> getButtons() {
        Button cancelButton = GooeyButton.builder()
                .display(MenuUtilities.getCancelItem())
                .onClick(event -> new BanListMenu(player).open())
                .build();

        GooeyButton unpunishButton = GooeyButton.builder()
                .display(MenuUtilities.getUnpunishItem(getLabel()))
                .onClick(event -> {
                    GlobalBanCommon.INSTANCE.unpunishPlayer(punishment);
                    new BanListMenu(player).open();
                })
                .build();

        return List.of(cancelButton, unpunishButton);
    }

    public Page getPage() {
        Button frame = GooeyButton.builder()
                .display(MenuUtilities.getBackgroundItem())
                .build();

        Button recordItem = GooeyButton.builder()
                .display(MenuUtilities.getRecordItem(punishment))
                .build();

        ChestTemplate template = ChestTemplate.builder(3)
                .fill(frame)
                .set(0, 4, recordItem)
                .rectangle(1, 1, 1, 7, new PlaceholderButton())
                .build();

        LinkedPage page = PaginationHelper.createPagesFromPlaceholders(template, getButtons(), null);
        page.setTitle(getPageTitle());

        return page;
    }

    public void open() {
        UIManager.openUIForcefully(player, getPage());
    }
}
