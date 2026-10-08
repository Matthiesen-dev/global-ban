package dev.matthiesen.global_ban.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.matthiesen.matthiesen_core.common.api.command.CoreCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class UnpunishCommands implements CoreCommand {
    public static final UnpunishCommands INSTANCE = new UnpunishCommands();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        // TODO:
        // unban
        // unban-ip
        // pardon
        // pardon-ip
    }
}
