package dev.matthiesen.global_ban.common.def.events;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;

public record PunishPlayerEvent(PunishmentRecord punishment) {
    private String toSecurityLogString() {
        return "Admin " + punishment.punisherDisplayName + " ("
                + punishment.punisherUuid + ") punished player "
                + punishment.playerDisplayName + " (" + punishment.playerUuid
                + ") with type " + punishment.type() + " for reason: "
                + punishment.reason;
    }

    public void logSecurity() {
        GlobalBanCommon.securityLog(toSecurityLogString());
    }
}
