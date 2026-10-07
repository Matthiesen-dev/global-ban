package dev.matthiesen.global_ban.common.mixins.overrides;

import net.minecraft.server.commands.PardonIpCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PardonIpCommand.class)
public class PardonIpCommandMixin {
    @ModifyArg(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/Commands;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;"
            ),
            index = 0,
            require = 0
    )
    private static String globalBan$modifyVanillaCommand(String def) {
        return "minecraft:pardon-ip";
    }
}
