package dev.matthiesen.global_ban.common.registry;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.commands.GeneralCommands;
import dev.matthiesen.global_ban.common.commands.PunishCommands;
import dev.matthiesen.global_ban.common.commands.UnpunishCommands;

public final class CommandRegistry {
    public static void init() {
        var registry = GlobalBanCommon.INSTANCE.getCommandsRegistryManager();
        registry.registerCommand(GeneralCommands.INSTANCE);
        registry.registerCommand(PunishCommands.INSTANCE);
        registry.registerCommand(UnpunishCommands.INSTANCE);
    }
}
