package dev.matthiesen.global_ban.common.mixins;

import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.events.CanPlayerLogin;
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
        GlobalBanCommon.Events.CAN_PLAYER_LOGIN_PRE.emit(new CanPlayerLogin.Pre(socketAddress, gameProfile));
    }

    @Inject(method = "canPlayerLogin", at = @At("TAIL"), cancellable = true)
    private void globalBan$canPlayerLoginTail(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Component> cir) {
        PunishmentRecord result = GlobalBanCommon.Events.CAN_PLAYER_LOGIN_POST.emit(new CanPlayerLogin.Post(socketAddress, gameProfile));
        if (result != null) {
            cir.setReturnValue(result.getDisconnectScreenComponent());
        }
    }
}
