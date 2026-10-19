package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.menu.BanListMenu;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.BanImporter;
import dev.matthiesen.matthiesen_core.common.api.command.CoreCommand;
import dev.matthiesen.matthiesen_core.common.utility.commands.CommandBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class GeneralCommands implements CoreCommand {
    public static final GeneralCommands INSTANCE = new GeneralCommands();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        CommandBuilder reloadCmd = CommandBuilder.create("reload")
                .requires(src -> PermissionRegistry.checkPermission(src, PermissionRegistry.COMMAND_RELOAD_PERMISSION))
                .executes(this::reloadAction);

        CommandBuilder importCmd = CommandBuilder.create("import")
                .requires(src -> PermissionRegistry.checkPermission(src, PermissionRegistry.COMMAND_IMPORT_PERMISSION))
                .argument("cleanup", BoolArgumentType.bool(), arg -> arg.executes(this::importAction));

        CommandBuilder banListCmd = CommandBuilder.create("banlist")
                .requires(src -> PermissionRegistry.checkPermission(src, PermissionRegistry.COMMAND_BAN_LIST_PERMISSION))
                .executes(this::listAction);

        CommandBuilder forceSyncCmd = CommandBuilder.create("force-sync")
                .requires(src -> PermissionRegistry.checkPermission(src, PermissionRegistry.COMMAND_FORCE_SYNC_PERMISSION))
                .executes(this::forceSyncAction);

        CommandBuilder rootCmd = CommandBuilder.create("global-ban")
                .requires(src -> PermissionRegistry.checkPermission(src, PermissionRegistry.COMMAND_ROOT_PERMISSION))
                .then(reloadCmd)
                .then(importCmd)
                .then(banListCmd)
                .then(forceSyncCmd);

        commandDispatcher.register(rootCmd.build());
        commandDispatcher.register(banListCmd.build()); // We also register the banlist command as a root command for convenience, so that players can use /banlist instead of /global-bans banlist
    }

    public int reloadAction(CommandContext<CommandSourceStack> context) {
        try {
            GlobalBanConfig.invalidatePunished();
            context.getSource().sendSystemMessage(
                    Component.literal("Global Ban configuration reloaded successfully.")
            );
            GlobalBanCommon.securityLog("Player " + context.getSource().getTextName() + " reloaded the Global Ban configuration.");

        } catch (Exception e) {
            GlobalBanCommon.INSTANCE.createErrorLog("Failed to reload configuration", e);
            context.getSource().sendFailure(Component.literal("Failed to reload configuration. Check the server logs for more information."));
        }
        return 1;
    }

    public int forceSyncAction(CommandContext<CommandSourceStack> context) {
        CompletableFuture.runAsync(() -> {
            try {
                var config = GlobalBanConfig.SERVER_CONFIG;
                if (!config.scheduler_apiSyncEnabled.getAsBoolean()) {
                    context.getSource().sendSystemMessage(
                            Component.literal("API sync is disabled. This action is disabled.")
                    );
                }
                if (config.scheduler_apiSyncUrl.get().isEmpty()) {
                    context.getSource().sendSystemMessage(
                            Component.literal("API sync URL is empty. This action is disabled.")
                    );
                }
                GlobalBanConfig.createForceSyncRequest();
                context.getSource().sendSystemMessage(Component.literal("Successfully forced a sync of punishments."));
                GlobalBanCommon.securityLog("Player " + context.getSource().getTextName() + " forced a sync of punishments.");
            } catch (Exception e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to force sync punishments", e);
                context.getSource().sendFailure(Component.literal("Failed to force sync punishments. Check the server logs for more information."));
            }
        });
        return 1;
    }

    public int importAction(CommandContext<CommandSourceStack> context) {
        try {
            boolean cleanup = BoolArgumentType.getBool(context, "cleanup");
            boolean status = BanImporter.importVanillaBans(cleanup);
            if (status) {
                context.getSource().sendSystemMessage(
                        Component.literal("Successfully imported vanilla bans into Global Ban.")
                );
            } else {
                context.getSource().sendSystemMessage(
                        Component.literal("Failed to import vanilla bans into Global Ban. Check the server logs for more information.")
                );
            }
            GlobalBanCommon.securityLog("Player " + context.getSource().getTextName() + " imported vanilla bans into Global Ban. Cleanup: " + cleanup);
        } catch (Exception e) {
            GlobalBanCommon.INSTANCE.createErrorLog("Failed to import vanilla bans", e);
            context.getSource().sendFailure(Component.literal("Failed to import vanilla bans into Global Ban. Check the server logs for more information."));
        }
        return 1;
    }

    public int listAction(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        CompletableFuture.runAsync(() -> {
            try {
                List<PunishmentRecord> punishments = GlobalBanConfig.getPunished();
                punishments.sort(Comparator.comparingLong(p -> -p.timestamp));
                createBanListMenu(punishments, source);
                GlobalBanCommon.securityLog("Player " + source.getTextName() + " listed punishments.");
            } catch (Exception e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to list punishments", e);
                source.sendFailure(Component.literal("Failed to list punishments. Check the server logs for more information."));
            }
        });
        return 1;
    }

    private void createBanListMenu(List<PunishmentRecord> punishments, CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        new BanListMenu(player, punishments).open();
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> reasonArgument(String argName) {
        return Commands.argument(argName, StringArgumentType.greedyString());
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> playerArgument(String argName) {
        return Commands.argument(argName, StringArgumentType.word())
                .suggests(playerSuggestionProvider());
    }

    public static SuggestionProvider<CommandSourceStack> playerSuggestionProvider() {
        return (ctx, builder) -> {
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

            for (String player : ctx.getSource().getServer().getPlayerNames()) {
                if (player.toLowerCase(Locale.ROOT).contains(remaining)) {
                    builder.suggest(player);
                }
            }

            return builder.buildFuture();
        };
    }
}
