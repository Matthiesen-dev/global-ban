package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.BanImporter;
import dev.matthiesen.matthiesen_core.common.api.command.CoreCommand;
import dev.matthiesen.matthiesen_core.common.utility.commands.CommandBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
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

            if (source.isPlayer()) {
                createBanListMenu(punishments, source);
            } else {
                createBanListTextOutput(punishments, source);
            }
        });
        return 1;
    }

    private void createBanListMenu(List<PunishmentRecord> punishments, CommandSourceStack source) {
    }

    private void createBanListTextOutput(List<PunishmentRecord> punishments, CommandSourceStack source) {
    }
}
