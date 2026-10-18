package dev.matthiesen.global_ban.common.menu;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.events.ClearPunishmentEvent;
import dev.matthiesen.global_ban.common.def.events.UnPunishPlayerEvent;
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
        String label = switch (punishment.type()) {
            case BAN -> "Player: " + punishment.playerDisplayName;
            case IP_BAN -> "IP: " + punishment.playerIp;
            default -> "Unknown";
        };

        VirtualBookContainer container = new VirtualBookContainer(label, "Server Console");

        container.addSection(
                createLine("Type: ", punishment.type().name(), ChatFormatting.DARK_RED)
                        .append(createLine("Date: ", punishment.getFormattedDate(), ChatFormatting.DARK_BLUE))
                        .append(createLine("Expires: ", punishment.getFormattedExpirationDate(), ChatFormatting.AQUA))
                        .append(createLine("By: ", punishment.punisherDisplayName, ChatFormatting.GREEN))
                        .append(createLine("Reason: ", punishment.reason, ChatFormatting.RESET))
                        .append(Component.literal("\n"))
                        .append(
                                container.createActionButton(
                                        "[Cancel]",
                                        eventPlayer -> new BanListMenu(eventPlayer).open(),
                                        ChatFormatting.DARK_RED
                                )
                        )
                        .append(Component.literal(" "))
                        .append(
                                container.createActionButton(
                                        "[Unpunish]",
                                        eventPlayer -> {
                                            GlobalBanCommon.Events.UNPUNISH_PLAYER.emit(new UnPunishPlayerEvent(punishment, false));
                                            new BanListMenu(eventPlayer).open();
                                        },
                                        ChatFormatting.GREEN
                                )
                        )
                        .append(Component.literal("\n"))
                        .append(
                                container.createActionButton(
                                        "[Pardon]",
                                        eventPlayer -> {
                                            GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromUUID(punishment.playerUuid));
                                            new BanListMenu(eventPlayer).open();
                                        },
                                        ChatFormatting.GREEN
                                )
                        )
                        .append(Component.literal(" "))
                        .append(
                                container.createActionButton(
                                        "[Pardon IP]",
                                        eventPlayer -> {
                                            GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromIpAddress(punishment.playerIp));
                                            new BanListMenu(eventPlayer).open();
                                        },
                                        ChatFormatting.GREEN
                                )
                        )
        );

        return container;
    }

    public void open() {
        ServerUIManager.openUIForcefully(player, getBook());
        new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(player);
    }
}
