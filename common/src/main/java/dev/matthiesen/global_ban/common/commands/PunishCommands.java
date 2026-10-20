package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.*;
import dev.matthiesen.global_ban.common.def.*;
import dev.matthiesen.global_ban.common.def.events.PunishPlayerEvent;
import dev.matthiesen.global_ban.common.menu.cmd.PunishMenu;
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
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public final class PunishCommands implements CoreCommand {
    public static final PunishCommands INSTANCE = new PunishCommands();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        commandDispatcher.register(create("kick", PermissionRegistry.COMMAND_KICK_PERMISSION, PunishmentType.KICK, false));
        commandDispatcher.register(create("ban", PermissionRegistry.COMMAND_BAN_PERMISSION, PunishmentType.BAN, false));
        commandDispatcher.register(create("tempban", PermissionRegistry.COMMAND_TEMP_BAN_PERMISSION, PunishmentType.BAN, true));
        commandDispatcher.register(create("ban-ip", PermissionRegistry.COMMAND_BAN_IP_PERMISSION, PunishmentType.IP_BAN, false));
        commandDispatcher.register(create("tempban-ip", PermissionRegistry.COMMAND_TEMP_BAN_IP_PERMISSION, PunishmentType.IP_BAN, true));
    }

    private LiteralArgumentBuilder<CommandSourceStack> create(String command, Permission permission, PunishmentType type, boolean temp) {
        var reasonArg = GeneralCommands.reasonArgument("reason").executes(ctx -> punishAction(ctx, type, temp));
        var tempArg = GeneralCommands.playerArgument("player")
                .then(Commands.argument("duration", StringArgumentType.word())
                        .executes(ctx -> punishAction(ctx, type, true))
                        .then(reasonArg)
                );
        var permArg = GeneralCommands.playerArgument("player")
                .executes(ctx -> punishAction(ctx, type, false))
                .then(reasonArg);
        return CommandBuilder.create(command)
                .requires(src -> PermissionRegistry.checkPermission(src, permission))
                .executes(ctx -> punishGUIAction(ctx, type, temp))
                .then(temp ? tempArg : permArg)
                .build();
    }

    private int punishGUIAction(CommandContext<CommandSourceStack> ctx, PunishmentType type, boolean isTemp) {
        CompletableFuture.runAsync(() -> {
            try {
                ServerPlayer executor = ctx.getSource().getPlayerOrException();
                new PunishMenu(executor, type, isTemp).open();
            } catch (CommandSyntaxException e) {
                ctx.getSource().sendFailure(Component.literal("This command can only be executed by a player."));
            }
        });
        return 1;
    }

    private int punishAction(CommandContext<CommandSourceStack> ctx, PunishmentType type, boolean isTemp) {
        CompletableFuture.runAsync(() -> {
            try {
                ServerConfig serverConfig = GlobalBanConfig.SERVER_CONFIG;
                var playerArg = StringArgumentType.getString(ctx, "player");
                long duration = PunishmentRecord.NON_EXPIRING_PUNISHMENT;

                if (isTemp) {
                    try {
                        var durationArg = StringArgumentType.getString(ctx, "duration");
                        long parsedDuration = Helpers.parseDuration(durationArg);
                        if (parsedDuration <= 0) {
                            ctx.getSource().sendFailure(Component.literal("Duration must be greater than 0."));
                            return;
                        }
                        duration = parsedDuration;
                    } catch (Exception e) {
                        ctx.getSource().sendFailure(Component.literal("Invalid duration format. Supported formats: \"1d\", \"2h\", \"30m\", \"15s\", \"1y\", \"6mo\", \"2w\", and combinations like \"1d2h30m\""));
                    }
                }

                String reason;

                try {
                    String reasonArg = StringArgumentType.getString(ctx, "reason");
                    if (reasonArg == null || reasonArg.isEmpty()) {
                        reason = serverConfig.defaultBanReason.get();
                    } else {
                        reason = reasonArg;
                    }
                } catch (Exception e) {
                    reason = serverConfig.defaultBanReason.get();
                }

                var players = Helpers.lookupServerUsers(playerArg);

                if (players.isEmpty()) {
                    Component message = Component.literal("Player entry not found: " + playerArg).withStyle(ChatFormatting.RED);
                    ctx.getSource().sendSystemMessage(message);
                } else {
                    for (var player : players) {
                        if (Helpers.isPunishableBy(player, ctx.getSource())) {
                            var punishment = PunishmentRecord.create(
                                    player.getUUID(),
                                    player.getOnlinePlayer() != null ? player.getOnlinePlayer().getIpAddress() : null,
                                    player.getUsername(),
                                    ctx.getSource(),
                                    type, duration, reason
                            );
                            GlobalBanCommon.Events.PUNISH_PLAYER.emit(new PunishPlayerEvent(punishment));
                        } else {
                            Component message = Component.literal("You do not have permission to punish this player: " + player.getUsername()).withStyle(ChatFormatting.RED);
                            ctx.getSource().sendSystemMessage(message);
                            GlobalBanCommon.securityLog("Player " + ctx.getSource().getTextName() + " attempted to punish player " + player.getUsername() + " but lacked permission.");
                        }
                    }
                }
            } catch (Exception e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to execute punishment command", e);
                ctx.getSource().sendFailure(Component.literal("Failed to execute punishment command. Check the server logs for more information."));
            }
        });
        return 1;
    }
}
