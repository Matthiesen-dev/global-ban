package dev.matthiesen.global_ban.common.mixins.overrides;

import net.minecraft.server.commands.PardonCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PardonCommand.class)
public class PardonCommandMixin {
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
        return "minecraft:pardon";
    }
}
