package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.matthiesen.matthiesen_core.common.api.command.CoreCommand;
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
}
