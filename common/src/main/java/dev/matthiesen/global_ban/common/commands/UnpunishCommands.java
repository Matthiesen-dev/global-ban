package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.*;
import dev.matthiesen.global_ban.common.def.*;
import dev.matthiesen.global_ban.common.def.events.*;
import dev.matthiesen.global_ban.common.menu.BanListMenu;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.Helpers;
import dev.matthiesen.matthiesen_core.common.api.command.CoreCommand;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.utility.commands.CommandBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class UnpunishCommands implements CoreCommand {
    public static final UnpunishCommands INSTANCE = new UnpunishCommands();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        commandDispatcher.register(create("unban", PermissionRegistry.COMMAND_UNBAN_PERMISSION, PunishmentType.BAN));
        commandDispatcher.register(create("unban-ip", PermissionRegistry.COMMAND_UNBAN_IP_PERMISSION, PunishmentType.IP_BAN));
        commandDispatcher.register(create("pardon", PermissionRegistry.COMMAND_PARDON_PERMISSION, PunishmentType.BAN));
        commandDispatcher.register(create("pardon-ip", PermissionRegistry.COMMAND_PARDON_IP_PERMISSION, PunishmentType.IP_BAN));
    }

    private LiteralArgumentBuilder<CommandSourceStack> create(String command, Permission permission, PunishmentType type) {
        return CommandBuilder.create(command)
                .requires(src -> PermissionRegistry.checkPermission(src, permission))
                .executes(ctx -> createBanListMenu(ctx, type))
                .then(
                        GeneralCommands.playerArgument("player")
                                .executes(ctx -> unpunishAction(ctx, type))
                )
                .build();
    }

    private int createBanListMenu(CommandContext<CommandSourceStack> ctx, PunishmentType type) {
        CompletableFuture.runAsync(() -> {
            try {
                ServerPlayer player = ctx.getSource().getPlayer();
                List<PunishmentRecord> punishments = GlobalBanConfig.getPunished();
                punishments.sort(Comparator.comparingLong(p -> -p.timestamp));
                switch (type) {
                    case IP_BAN -> punishments = punishments.stream()
                            .filter(p -> p.type() == PunishmentType.IP_BAN)
                            .toList();
                    case BAN -> punishments = punishments.stream()
                            .filter(p -> p.type() == PunishmentType.BAN)
                            .toList();
                }
                new BanListMenu(player, punishments).open();
            } catch (Exception e) {
                ctx.getSource().sendFailure(Component.literal("You must be a player to use this command."));
            }
        });
        return 1;
    }

    private int unpunishAction(CommandContext<CommandSourceStack> ctx, PunishmentType type) {
        CompletableFuture.runAsync(() -> {
            try {
                ServerConfig serverConfig = GlobalBanConfig.SERVER_CONFIG;
                var playerArg = StringArgumentType.getString(ctx, "player");
                var player = Helpers.lookupServerUser(playerArg);

                if (player == null) {
                    Component message = Component.literal("Player entry not found: " + playerArg).withStyle(ChatFormatting.RED);
                    ctx.getSource().sendSystemMessage(message);
                    return;
                }

                ServerPlayer executor;
                try {
                    executor = ctx.getSource().getPlayerOrException();
                } catch (Exception e) {
                    executor = null;
                }

                var punishments = Helpers.getPunishmentsForUser(player);

                for (var punishment : punishments) {
                    Component message = null;
                    int count = 0;

                    if (type != null) {
                        switch (type) {
                            case BAN -> {
                                Integer result = GlobalBanCommon.Events.UNPUNISH_PLAYER.emit(new UnPunishPlayerEvent(punishment, false, executor));
                                count += result != null ? result : 0;
                                var temp = String.join("\n", serverConfig.messages_unbanChatMessage.get());
                                var processed = Helpers.processPlaceholders(temp, punishment.getPlaceholders());
                                message = processed.toComponent();
                            }
                            case IP_BAN -> {
                                Integer result = GlobalBanCommon.Events.UNPUNISH_PLAYER.emit(new UnPunishPlayerEvent(punishment, false, executor));
                                count += result != null ? result : 0;
                                var temp = String.join("\n", serverConfig.messages_ipUnbanChatMessage.get());
                                var processed = Helpers.processPlaceholders(temp, punishment.getPlaceholders());
                                message = processed.toComponent();
                            }
                        }
                    } else {
                        if (player.getUUID() != null) {
                            Integer result = GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromUUID(player.getUUID()));
                            count += result != null ? result : 0;
                        }
                        if (player.getOnlinePlayer() != null) {
                            Integer result = GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromIpAddress(player.getOnlinePlayer().getIpAddress()));
                            count += result != null ? result : 0;
                        }
                        if (punishment.playerUuid != null) {
                            Integer result = GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromUUID(punishment.playerUuid));
                            count += result != null ? result : 0;
                        }
                        if (punishment.playerIp != null) {
                            Integer result = GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromIpAddress(punishment.playerIp));
                            count += result != null ? result : 0;
                        }

                        var temp = String.join("\n", serverConfig.messages_pardonChatMessage.get());
                        var processed = Helpers.processPlaceholders(temp, punishment.getPlaceholders());
                        message = processed.toComponent();
                    }

                    if (count > 0) {
                        if (executor != null) {
                            executor.sendSystemMessage(message);
                        } else {
                            ctx.getSource().sendSystemMessage(message);
                        }
                    } else {
                        MutableComponent noPunishmentMessage = Component.literal("No punishment found for: " + player.getUsername()).withStyle(ChatFormatting.RED);
                        if (executor != null) {
                            executor.sendSystemMessage(noPunishmentMessage);
                        } else {
                            ctx.getSource().sendSystemMessage(noPunishmentMessage);
                        }
                    }
                }
            } catch (Exception e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to execute unpunish command", e);
                ctx.getSource().sendFailure(Component.literal("Failed to execute unpunish command. Check the server logs for more information.").withStyle(ChatFormatting.RED));
            }
        });
        return 1;
    }
}
