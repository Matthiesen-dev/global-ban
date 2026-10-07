package dev.matthiesen.global_ban.common.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "canPlayerLogin", at = @At("HEAD"))
    private void globalBan$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Component> cir) {
        // TODO
    }

    @Inject(method = "canPlayerLogin", at = @At("TAIL"), cancellable = true)
    private void globalBan$canPlayerLoginTail(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Component> cir) {
        // TODO
    }
}
