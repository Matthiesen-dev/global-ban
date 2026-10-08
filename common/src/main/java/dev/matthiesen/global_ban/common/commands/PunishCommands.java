package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.matthiesen_core.common.api.command.CoreCommand;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.utility.commands.CommandBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class PunishCommands implements CoreCommand {
    public static final PunishCommands INSTANCE = new PunishCommands();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        // TODO:
        // kick
        // ban
        // tempban
        // ban-ip
        // tempban-ip
    }

    private LiteralArgumentBuilder<CommandSourceStack> create(String command, Permission permission, PunishmentType type, boolean temp) {
        return CommandBuilder.create(command)
                .requires(src -> PermissionRegistry.checkPermission(src, permission))
                .build();
    }
}
