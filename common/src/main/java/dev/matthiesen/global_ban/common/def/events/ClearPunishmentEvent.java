package dev.matthiesen.global_ban.common.def.events;

import dev.matthiesen.global_ban.common.GlobalBanCommon;

import java.util.UUID;

public record ClearPunishmentEvent(UUID uuid, String ipAddress) {
    public static ClearPunishmentEvent fromUUID(UUID uuid) {
        return new ClearPunishmentEvent(uuid, null);
    }

    public static ClearPunishmentEvent fromIpAddress(String ipAddress) {
        return new ClearPunishmentEvent(null, ipAddress);
    }

    private String toSecurityLogString() {
        if (uuid != null) {
            return "Clearing punishments for UUID: " + uuid;
        } else if (ipAddress != null) {
            return "Clearing punishments for IP Address: " + ipAddress;
        } else {
            return "Clearing punishments for unknown identifier";
        }
    }

    public void logSecurity() {
        GlobalBanCommon.securityLog(toSecurityLogString());
    }
}
