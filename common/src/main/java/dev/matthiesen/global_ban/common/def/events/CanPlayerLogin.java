package dev.matthiesen.global_ban.common.def.events;

import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;

public final class CanPlayerLogin {
    public record Pre(SocketAddress socketAddress, GameProfile gameProfile) {
    }

    public record Post(SocketAddress socketAddress, GameProfile gameProfile) {
        private String toSecurityLogString(@Nullable PunishmentRecord punishment) {
            StringBuilder sb = new StringBuilder();
            sb.append("Player ").append(gameProfile.getName()).append(" (").append(gameProfile.getId()).append(") attempted to log in from ").append(socketAddress);
            if (punishment != null) {
                sb.append(", but was denied due to punishment: ").append(punishment.type()).append(" for reason: ").append(punishment.reason);
            } else {
                sb.append(", and was allowed to log in.");
            }
            return sb.toString();
        }

        public void logSecurity(@Nullable PunishmentRecord punishment) {
            GlobalBanCommon.securityLog(toSecurityLogString(punishment));
        }
    }
}
