package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
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
                .executes(this::listAction)
                .argument("page", IntegerArgumentType.integer(1), arg -> arg.executes(this::listAction));

        CommandBuilder rootCmd = CommandBuilder.create("global-bans")
                .requires(src -> PermissionRegistry.checkPermission(src, PermissionRegistry.COMMAND_ROOT_PERMISSION))
                .then(reloadCmd)
                .then(importCmd)
                .then(banListCmd);

        commandDispatcher.register(rootCmd.build());
        commandDispatcher.register(banListCmd.build()); // We also register the banlist command as a root command for convenience, so that players can use /banlist instead of /global-bans banlist
    }

    public int reloadAction(CommandContext<CommandSourceStack> context) {
        GlobalBanConfig.invalidatePunished();
        context.getSource().sendSystemMessage(
                Component.literal("Global Ban configuration reloaded successfully.")
        );
        return 1;
    }

    public int importAction(CommandContext<CommandSourceStack> context) {
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
        return 1;
    }

    public int listAction(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        CompletableFuture.runAsync(() -> {
            List<PunishmentRecord> punishments = GlobalBanConfig.getPunished();
            punishments.sort(Comparator.comparingLong(p -> -p.timestamp()));
            if (source.isPlayer() && GlobalBanCommon.INSTANCE.isGooeyLibsLoaded()) {
                createBanListMenu(punishments, source);
            } else {
                createBanListTextOutput(punishments, source, context);
            }
        });
        return 1;
    }

    private void createBanListMenu(List<PunishmentRecord> punishments, CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        new BanListMenu(player, punishments).open();
    }

    private void createBanListTextOutput(List<PunishmentRecord> punishments, CommandSourceStack source, CommandContext<CommandSourceStack> context) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            for (PunishmentRecord punishment : punishments) {
                Component punishmentInfo = Component.literal(String.format("Player: %s, Type: %s, Reason: %s, Timestamp: %d",
                        punishment.playerDisplayName(), punishment.type(), punishment.reason(), punishment.timestamp()));
                source.sendSystemMessage(punishmentInfo);
            }
            return;
        }
        int pageSize = 8;
        int totalPages = (int) Math.ceil((double) punishments.size() / pageSize);
        int startPage;
        if (context.getArgument("page", Integer.class) != null) {
            startPage = IntegerArgumentType.getInteger(context, "page") - 1;
        } else {
            startPage = 0;
        }
        int start = startPage * pageSize;
        int end = Math.min(start + pageSize, punishments.size());
        List<PunishmentRecord> pagePunishments = punishments.subList(start, end);
        List<Component> messages = new ArrayList<>();
        messages.add(Component.literal(String.format("Global Ban List - Page %d/%d", startPage + 1, totalPages)));
        for (PunishmentRecord punishment : pagePunishments) {
            Component punishmentInfo = Component.literal(String.format("Player: %s, Type: %s, Reason: %s, Timestamp: %d",
                    punishment.playerDisplayName(), punishment.type(), punishment.reason(), punishment.timestamp()));
            messages.add(punishmentInfo);
        }
        if (startPage + 1 < totalPages) {
            messages.add(Component.literal("[Next Page]").withStyle(style -> style
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to view the next page")))
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/global-bans banlist " + (startPage + 2)))
            ));
        }
        for (Component message : messages) {
            source.sendSystemMessage(message);
        }
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
