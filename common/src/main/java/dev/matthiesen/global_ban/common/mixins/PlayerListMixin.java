package dev.matthiesen.global_ban.common.mixins;

import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.utils.Helpers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.HashSet;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "canPlayerLogin", at = @At("HEAD"))
    private void globalBan$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Component> cir) {
        if (socketAddress != null) {
            String stringAddress = Helpers.stringifyAddress(socketAddress);
            GlobalBanCommon.UUID_TO_IP_CACHE.put(gameProfile.getId(), stringAddress);
            GlobalBanCommon.IP_TO_UUID_CACHE.computeIfAbsent(stringAddress, ip -> new HashSet<>()).add(gameProfile.getId());
        }
    }

    @Inject(method = "canPlayerLogin", at = @At("TAIL"), cancellable = true)
    private void globalBan$canPlayerLoginTail(SocketAddress socketAddress, GameProfile gameProfile, CallbackInfoReturnable<Component> cir) {
        PunishmentRecord punishment = null;

        if (socketAddress == null || gameProfile == null) {
            return;
        }

        String ip = Helpers.stringifyAddress(socketAddress);

        for (var entry : GlobalBanConfig.getPunished()) {
            if (!entry.isExpired() &&
                    (entry.type() == PunishmentType.IP_BAN && entry.playerIp().equals(ip)) ||
                    (entry.type() == PunishmentType.BAN && entry.playerUuid().equals(gameProfile.getId()))
            ) {
                punishment = entry;
                break;
            }
        }

        if (punishment == null) {
            final var playerBans = GlobalBanConfig.isPlayerUUIDPunished(gameProfile.getId());
            final var ipBans = GlobalBanConfig.isIpPunished(ip);

            if (!playerBans.isEmpty()) {
                punishment = playerBans.stream().filter(p -> !p.isExpired()).findFirst().orElse(null);
            } else if (!ipBans.isEmpty()) {
                punishment = ipBans.stream().filter(p -> !p.isExpired()).findFirst().orElse(null);
            }
        }

        if (punishment != null) {
            if (punishment.type() == PunishmentType.IP_BAN && GlobalBanConfig.SERVER_CONFIG.autoBanPlayersByIP.getAsBoolean()) {
                PunishmentRecord newPunishment = PunishmentRecord.punishAlt(gameProfile.getId(), gameProfile.getName(), punishment);
                GlobalBanCommon.INSTANCE.punishPlayer(newPunishment);
            }
            cir.setReturnValue(punishment.getDisconnectChatMessage());
        }
    }
}
