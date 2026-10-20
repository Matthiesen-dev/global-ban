package dev.matthiesen.global_ban.neoforge.mixins.corecommands;

import cc.corecommands.CoreCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CoreCommands.UnbanCmd.class)
public class CCUnbanCmdMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/Commands;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;"
            ),
            index = 0,
            require = 0
    )
    private static String globalBan$modifyModCommand(String def) {
        return "corecommands:unban";
    }
}
