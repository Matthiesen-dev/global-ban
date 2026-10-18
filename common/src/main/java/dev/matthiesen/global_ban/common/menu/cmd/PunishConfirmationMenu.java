package dev.matthiesen.global_ban.common.menu.cmd;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.*;
import dev.matthiesen.global_ban.common.def.events.PunishPlayerEvent;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.api.server_menu.ServerUIManager;
import dev.matthiesen.matthiesen_core.common.api.server_menu.button.*;
import dev.matthiesen.matthiesen_core.common.api.server_menu.helpers.PaginationHelper;
import dev.matthiesen.matthiesen_core.common.api.server_menu.page.*;
import dev.matthiesen.matthiesen_core.common.api.server_menu.template.types.ChestTemplate;
import dev.matthiesen.matthiesen_core.common.utility.SoundsPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

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
        Button frame = ServerButton.builder()
                .display(MenuUtilities.getBackgroundItem())
                .build();

        PunishmentRecord record = getPunishmentRecord();

        Button recordItem = ServerButton.builder()
                .display(MenuUtilities.getRecordItem(record))
                .build();

        Button confirmButton = ServerButton.builder()
                .display(MenuUtilities.getPunishItem(target.getName().getString()))
                .onClick(action -> {
                    GlobalBanCommon.Events.PUNISH_PLAYER.emit(new PunishPlayerEvent(record));
                    ServerUIManager.closeUI(executor);
                    new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(executor);
                })
                .build();

        Button cancelButton = ServerButton.builder()
                .display(MenuUtilities.getCancelItem())
                .onClick(action -> {
                    ServerUIManager.closeUI(executor);
                    new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(executor);
                })
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

    public Permission requiredPermission() {
        var temp = duration != PunishmentRecord.NON_EXPIRING_PUNISHMENT;
        return switch (type) {
            case BAN -> temp ? PermissionRegistry.COMMAND_TEMP_BAN_PERMISSION : PermissionRegistry.COMMAND_BAN_PERMISSION;
            case IP_BAN -> temp ? PermissionRegistry.COMMAND_TEMP_BAN_IP_PERMISSION : PermissionRegistry.COMMAND_BAN_IP_PERMISSION;
            case KICK -> PermissionRegistry.COMMAND_KICK_PERMISSION;
        };
    }

    public void open() {
        if (!PermissionRegistry.checkPermission(executor, requiredPermission())) {
            executor.sendSystemMessage(Component.literal("You do not have permission to view this menu.").withStyle(ChatFormatting.RED));
            return;
        }
        ServerUIManager.openUIForcefully(executor, getPage());
        new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(executor);
    }
}
