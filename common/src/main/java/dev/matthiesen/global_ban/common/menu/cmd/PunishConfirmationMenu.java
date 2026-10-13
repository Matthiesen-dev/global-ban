package dev.matthiesen.global_ban.common.menu.cmd;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.def.events.PunishPlayerEvent;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record PunishConfirmationMenu(
        ServerPlayer executor,
        PunishmentType type,
        ServerPlayer target,
        long duration,
        String reason
) {

    public List<Button> getButtons() {
        return List.of();
    }

    public Component getPageTitle() {
        return Component.literal("Confirm Punishment");
    }

    public PunishmentRecord getPunishmentRecord() {
        return PunishmentRecord.create(
                target,
                executor.createCommandSourceStack(),
                type,
                duration,
                reason
        );
    }

    public Page getPage() {
        Button frame = GooeyButton.builder()
                .display(MenuUtilities.getBackgroundItem())
                .build();

        PunishmentRecord record = getPunishmentRecord();

        Button recordItem = GooeyButton.builder()
                .display(MenuUtilities.getRecordItem(record))
                .build();

        Button confirmButton = GooeyButton.builder()
                .display(MenuUtilities.getPunishItem(target.getName().getString()))
                .onClick(action -> {
                    GlobalBanCommon.Events.PUNISH_PLAYER.emit(new PunishPlayerEvent(record));
                    UIManager.closeUI(executor);
                })
                .build();

        Button cancelButton = GooeyButton.builder()
                .display(MenuUtilities.getCancelItem())
                .onClick(action -> UIManager.closeUI(executor))
                .build();

        ChestTemplate template = ChestTemplate.builder(3)
                .fill(frame)
                .set(0, 4, recordItem)
                .set(1, 3, confirmButton)
                .set(1, 5, cancelButton)
                .build();

        LinkedPage page = PaginationHelper.createPagesFromPlaceholders(template, getButtons(), null);
        page.setTitle(getPageTitle());

        return page;
    }

    public void open() {
        UIManager.openUIForcefully(executor, getPage());
    }
}
