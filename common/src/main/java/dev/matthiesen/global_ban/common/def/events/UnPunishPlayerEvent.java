package dev.matthiesen.global_ban.common.def.events;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record UnPunishPlayerEvent(PunishmentRecord punishment, boolean expired, boolean fromRemote, @Nullable ServerPlayer executor) {
    public UnPunishPlayerEvent(PunishmentRecord punishment, boolean expired, ServerPlayer executor) {
        this(punishment, expired, false, executor);
    }

    public UnPunishPlayerEvent(PunishmentRecord punishment, boolean expired) {
        this(punishment, expired, false, null);
    }

    public UnPunishPlayerEvent(PunishmentRecord punishment, boolean expired, boolean fromRemote) {
        this(punishment, expired, fromRemote, null);
    }

    private String toSecurityLogString() {
        boolean expired = this.expired;
        boolean fromRemote = this.fromRemote;
        PunishmentType type = punishment.type;
        StringBuilder sb = new StringBuilder();
        String label = switch (type) {
            case BAN -> "Player " + punishment.playerDisplayName;
            case IP_BAN -> "IP Address " + punishment.playerIp;
            default -> "Unknown type";
        };
        if (expired) {
            sb.append("Expired punishment for ").append(label);
        } else if (fromRemote) {
            sb.append("Remote unpunishment for ").append(label);
        } else {
            String executorName = executor != null ? executor.getGameProfile().getName() : "Unknown executor";
            sb.append("Admin ").append(executorName).append(" (").append(punishment.punisherUuid).append(") unpunished ").append(label);
        }
        return sb.toString();
    }

    public void logSecurity() {
        GlobalBanCommon.securityLog(toSecurityLogString());
    }
}
