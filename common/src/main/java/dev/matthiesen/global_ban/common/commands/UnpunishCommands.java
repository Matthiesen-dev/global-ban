package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentType;
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
                .argument("player", StringArgumentType.word(), playerArg -> playerArg
                        .suggests(GeneralCommands.playerSuggestionProvider())
                        .executes(ctx -> unpunishAction(ctx, type))
                )
                .build();
    }

    private int unpunishAction(CommandContext<CommandSourceStack> ctx, PunishmentType type) {
        CompletableFuture.runAsync(() -> {
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
                            count += GlobalBanCommon.INSTANCE.unpunishPlayer(punishment);
                            var temp = String.join("\n", GlobalBanConfig.SERVER_CONFIG.messages_unbanChatMessage.get());
                            var processed = Helpers.processPlaceholders(temp, punishment.getPlaceholders());
                            message = processed.toComponent();
                        }
                        case IP_BAN -> {
                            count += GlobalBanCommon.INSTANCE.unpunishPlayer(punishment);
                            var temp = String.join("\n", GlobalBanConfig.SERVER_CONFIG.messages_ipUnbanChatMessage.get());
                            var processed = Helpers.processPlaceholders(temp, punishment.getPlaceholders());
                            message = processed.toComponent();
                        }
                    }
                } else {
                    if (player.getUUID() != null) {
                        count += GlobalBanCommon.INSTANCE.clearPunishments(player.getUUID());
                    }
                    if (player.getOnlinePlayer() != null) {
                        count += GlobalBanCommon.INSTANCE.clearIpPunishments(player.getOnlinePlayer().getIpAddress());
                    }
                    if (punishment.playerUuid() != null) {
                        count += GlobalBanCommon.INSTANCE.clearPunishments(punishment.playerUuid());
                    }
                    if (punishment.playerIp() != null) {
                        count += GlobalBanCommon.INSTANCE.clearIpPunishments(punishment.playerIp());
                    }

                    var temp = String.join("\n", GlobalBanConfig.SERVER_CONFIG.messages_pardonChatMessage.get());
                    var processed = Helpers.processPlaceholders(temp, punishment.getPlaceholders());
                    message = processed.toComponent();
                }

                if (count > 0) {
                    if (executor != null) {
                        executor.sendSystemMessage(message);
                    } else {
                        ctx.getSource().sendSystemMessage(message);
                    }
                    for (ServerPlayer plistEntry : GlobalBanCommon.INSTANCE.getCommonUtils().getServer().getPlayerList().getPlayers()) {
                        if (PermissionRegistry.checkPermission(plistEntry, PermissionRegistry.SEE_BANS_PERMISSION)) {
                            plistEntry.sendSystemMessage(message);
                        }
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
