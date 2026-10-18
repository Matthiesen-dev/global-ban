package dev.matthiesen.global_ban.common.menu;

import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.api.server_menu.ServerUIManager;
import dev.matthiesen.matthiesen_core.common.api.server_menu.container.VirtualBookContainer;
import dev.matthiesen.matthiesen_core.common.utility.SoundsPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

public record PunishmentMenu(ServerPlayer player, PunishmentRecord punishment) {
    private MutableComponent createLine(String label, String value, ChatFormatting valueColor) {
        return Component.literal(label)
                .setStyle(Style.EMPTY.withBold(true))
                .append(Component.literal(value).setStyle(Style.EMPTY.withBold(false).withColor(valueColor)))
                .append(Component.literal("\n"));
    }

    private VirtualBookContainer getBook() {
        String labelPrefix = switch (punishment.type()) {
            case BAN -> "Player: ";
            case IP_BAN -> "IP: ";
            default -> "Unknown: ";
        };

        String label = switch (punishment.type()) {
            case BAN -> punishment.playerDisplayName;
            case IP_BAN -> punishment.playerIp;
            default -> "N/A";
        };

        VirtualBookContainer container = new VirtualBookContainer(label, "Server Console");

        container.addSection(
                createLine(labelPrefix, label, ChatFormatting.DARK_BLUE)
                        .append(createLine("Type: ", punishment.type().name(), ChatFormatting.DARK_RED))
                        .append(createLine("Date: ", punishment.getFormattedDate(), ChatFormatting.DARK_BLUE))
                        .append(createLine("Expires: ", punishment.getFormattedExpirationDate(), ChatFormatting.DARK_RED))
                        .append(createLine("By: ", punishment.punisherDisplayName, ChatFormatting.DARK_PURPLE))
                        .append(createLine("Reason: ", punishment.reason, ChatFormatting.RESET))
                        .append(Component.literal("\n"))
                        .append(
                                container.createActionButton(
                                        "[Back to List]",
                                        eventPlayer -> new BanListMenu(eventPlayer).open(),
                                        ChatFormatting.DARK_RED
                                )
                        )
        );

        return container;
    }

    public Permission requiredPermission() {
        return PermissionRegistry.COMMAND_BAN_LIST_PERMISSION;
    }

    public void open() {
        if (!PermissionRegistry.checkPermission(player, requiredPermission())) {
            player.sendSystemMessage(Component.literal("You do not have permission to view this menu.").withStyle(ChatFormatting.RED));
            return;
        }
        ServerUIManager.openUIForcefully(player, getBook());
        new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(player);
    }
}
