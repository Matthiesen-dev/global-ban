package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.config.ServerConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.Helpers;
import dev.matthiesen.matthiesen_core.common.api.command.CoreCommand;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.utility.commands.CommandBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
        var tempArg = GeneralCommands.playerArgument("player").then(
                Commands.argument("duration", StringArgumentType.word())
                        .executes(ctx -> punishAction(ctx, type, true))
                        .then(GeneralCommands.reasonArgument("reason").executes(ctx -> punishAction(ctx, type, true)))
        );
        var permArg = GeneralCommands.playerArgument("player")
                .executes(ctx -> punishAction(ctx, type, false))
                .then(GeneralCommands.reasonArgument("reason").executes(ctx -> punishAction(ctx, type, false))
        );
        return CommandBuilder.create(command)
                .requires(src -> PermissionRegistry.checkPermission(src, permission))
                .then(temp ? tempArg : permArg)
                .build();
    }

    private int punishAction(CommandContext<CommandSourceStack> ctx, PunishmentType type, boolean isTemp) {
        CompletableFuture.runAsync(() -> {
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
                Component message = Component.literal("Player entry not found: " + playerArg).withStyle(net.minecraft.ChatFormatting.RED);
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
                        GlobalBanCommon.INSTANCE.punishPlayer(punishment);
                    }
                }
            }

        });
        return 1;
    }
}
