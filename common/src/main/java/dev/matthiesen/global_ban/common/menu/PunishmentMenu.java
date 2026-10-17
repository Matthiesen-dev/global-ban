package dev.matthiesen.global_ban.common.menu;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.events.*;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import dev.matthiesen.matthiesen_core.common.api.server_menu.ServerUIManager;
import dev.matthiesen.matthiesen_core.common.api.server_menu.button.*;
import dev.matthiesen.matthiesen_core.common.api.server_menu.helpers.PaginationHelper;
import dev.matthiesen.matthiesen_core.common.api.server_menu.page.*;
import dev.matthiesen.matthiesen_core.common.api.server_menu.template.types.ChestTemplate;
import dev.matthiesen.matthiesen_core.common.utility.SoundsPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

public record PunishmentMenu(ServerPlayer player, PunishmentRecord punishment) {
    public String getLabel() {
        return switch (punishment.type()) {
            case BAN -> punishment.playerDisplayName;
            case IP_BAN -> punishment.playerIp;
            default -> "Unknown";
        };
    }

    public Component getPageTitle() {
        return Component.literal("Punishment Record: " + getLabel());
    }

    public List<Button> getButtons() {
        Button cancelButton = ServerButton.builder()
                .display(MenuUtilities.getCancelItem())
                .onClick(event -> new BanListMenu(player).open())
                .build();

        ServerButton unpunishButton = ServerButton.builder()
                .display(MenuUtilities.getUnpunishItem(getLabel()))
                .onClick(event -> {
                    GlobalBanCommon.Events.UNPUNISH_PLAYER.emit(new UnPunishPlayerEvent(punishment, false));
                    new BanListMenu(player).open();
                })
                .build();

        ServerButton pardonButton = ServerButton.builder()
                .display(MenuUtilities.getPardonItem(getLabel()))
                .onClick(event -> {
                    GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromUUID(punishment.playerUuid));
                    new BanListMenu(player).open();
                })
                .build();

        ServerButton pardonIpButton = ServerButton.builder()
                .display(MenuUtilities.getPardonItem(punishment.playerIp))
                .onClick(event -> {
                    GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromIpAddress(punishment.playerIp));
                    new BanListMenu(player).open();
                })
                .build();

        return List.of(cancelButton, unpunishButton, pardonButton, pardonIpButton);
    }

    public Page getPage() {
        Button frame = ServerButton.builder()
                .display(MenuUtilities.getBackgroundItem())
                .build();

        Button recordItem = ServerButton.builder()
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
        ServerUIManager.openUIForcefully(player, getPage());
        new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(player);
    }
}
