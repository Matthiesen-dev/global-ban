package dev.matthiesen.global_ban.common.def.events;

import dev.matthiesen.global_ban.common.def.PunishmentRecord;

public record UnPunishPlayerEvent(PunishmentRecord punishment, boolean expired) {
}
