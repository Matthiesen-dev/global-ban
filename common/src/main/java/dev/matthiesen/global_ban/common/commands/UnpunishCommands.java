package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.config.ServerConfig;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.def.events.ClearPunishmentEvent;
import dev.matthiesen.global_ban.common.def.events.UnPunishPlayerEvent;
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
                .then(
                        GeneralCommands.playerArgument("player")
                                .executes(ctx -> unpunishAction(ctx, type))
                )
                .build();
    }

    private int unpunishAction(CommandContext<CommandSourceStack> ctx, PunishmentType type) {
        CompletableFuture.runAsync(() -> {
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
                            Integer result = GlobalBanCommon.Events.UNPUNISH_PLAYER.emit(new UnPunishPlayerEvent(punishment, false));
                            count += result != null ? result : 0;
                            var temp = String.join("\n", serverConfig.messages_unbanChatMessage.get());
                            var processed = Helpers.processPlaceholders(temp, punishment.getPlaceholders());
                            message = processed.toComponent();
                        }
                        case IP_BAN -> {
                            Integer result = GlobalBanCommon.Events.UNPUNISH_PLAYER.emit(new UnPunishPlayerEvent(punishment, false));
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
                    if (punishment.playerUuid() != null) {
                        Integer result = GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromUUID(punishment.playerUuid()));
                        count += result != null ? result : 0;
                    }
                    if (punishment.playerIp() != null) {
                        Integer result = GlobalBanCommon.Events.CLEAR_PUNISHMENTS.emit(ClearPunishmentEvent.fromIpAddress(punishment.playerIp()));
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
        });
        return 1;
    }
}
