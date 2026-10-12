package dev.matthiesen.global_ban.common.def.events;

import java.util.UUID;

public record ClearPunishmentEvent(UUID uuid, String ipAddress) {
    public static ClearPunishmentEvent fromUUID(UUID uuid) {
        return new ClearPunishmentEvent(uuid, null);
    }

    public static ClearPunishmentEvent fromIpAddress(String ipAddress) {
        return new ClearPunishmentEvent(null, ipAddress);
    }
}
