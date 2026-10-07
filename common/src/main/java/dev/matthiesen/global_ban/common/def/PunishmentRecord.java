package dev.matthiesen.global_ban.common.def;

import com.electronwill.nightconfig.core.Config;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;
import java.util.UUID;

public record PunishmentRecord(
        PunishmentType type,
        long timestamp,
        long duration,
        UUID playerUuid,
        String playerIp,
        String playerDisplayName,
        UUID punisherUuid,
        String punisherDisplayName,
        String reason,
        UUID serverUuid
) {
    public static PunishmentRecord punishAlt(UUID playerUuid, String playerDisplayName, PunishmentRecord previousRecord) {
        return new PunishmentRecord(
                PunishmentType.BAN,
                previousRecord.timestamp,
                previousRecord.duration,
                playerUuid,
                previousRecord.playerIp,
                playerDisplayName,
                previousRecord.punisherUuid,
                previousRecord.punisherDisplayName,
                previousRecord.reason,
                previousRecord.serverUuid
        );
    }

    public static PunishmentRecord deserialize(Config config) {
        return new PunishmentRecord(
                PunishmentType.fromName(config.get("type")),
                config.getLong("timestamp"),
                config.getLong("duration"),
                UUID.fromString(config.get("playerUuid")),
                config.get("playerIp"),
                config.get("playerDisplayName"),
                UUID.fromString(config.get("punisherUuid")),
                config.get("punisherDisplayName"),
                config.get("reason"),
                UUID.fromString(config.get("serverUuid"))
        );
    }

    public static boolean isValid(Config config) {
        if (!(config.get("type") instanceof String typeRaw) || PunishmentType.fromName(typeRaw) == null) {
            return false;
        }
        if (!(config.get("timestamp") instanceof Long timestamp)) {
            return false;
        }
        if (!(config.get("duration") instanceof Long duration)) {
            return false;
        }
        if (!(config.get("playerUuid") instanceof String playerUuidRaw)) {
            return false;
        }
        if (!(config.get("playerIp") instanceof String playerIp)) {
            return false;
        }
        if (!(config.get("playerDisplayName") instanceof String playerDisplayName)) {
            return false;
        }
        if (!(config.get("punisherUuid") instanceof String punisherUuidRaw)) {
            return false;
        }
        if (!(config.get("punisherDisplayName") instanceof String punisherDisplayName)) {
            return false;
        }
        if (!(config.get("reason") instanceof String reason)) {
            return false;
        }
        if (!(config.get("serverUuid") instanceof String serverUuidRaw)) {
            return false;
        }

        try {
            UUID.fromString(playerUuidRaw);
            UUID.fromString(punisherUuidRaw);
            UUID.fromString(serverUuidRaw);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    public Config serialize() {
        Config config = Config.inMemory();
        config.set("type", this.type.name);
        config.set("timestamp", this.timestamp);
        config.set("duration", this.duration);
        config.set("playerUuid", this.playerUuid.toString());
        config.set("playerIp", this.playerIp);
        config.set("playerDisplayName", this.playerDisplayName);
        config.set("punisherUuid", this.punisherUuid.toString());
        config.set("punisherDisplayName", this.punisherDisplayName);
        config.set("reason", this.reason);
        config.set("serverUuid", this.serverUuid.toString());
        return config;
    }

    public static PunishmentRecord create(ServerPlayer punished, CommandSourceStack punisher, PunishmentType type, long duration, String reason) {
        return create(
                punished.getUUID(),
                punished.getIpAddress(),
                punished.getDisplayName().getString(),
                punisher,
                type,
                duration,
                reason
        );
    }

    public static PunishmentRecord create(UUID uuid, String ipAddress, String displayName, CommandSourceStack punisher, PunishmentType type, long duration, String reason) {
        return create(
                uuid,
                ipAddress,
                displayName,
                punisher,
                type,
                duration,
                reason,
                GlobalBanConfig.getServerUUID()
        );
    }

    public static PunishmentRecord create(UUID uuid, String ipAddress, String displayName, CommandSourceStack punisher, PunishmentType type, long duration, String reason, UUID serverUuid) {
        return new PunishmentRecord(
                type,
                System.currentTimeMillis(),
                duration,
                uuid,
                ipAddress,
                displayName,
                punisher.getEntity() != null ? punisher.getEntity().getUUID() : net.minecraft.Util.NIL_UUID,
                punisher.getTextName(),
                reason,
                serverUuid
        );
    }

    public boolean isTemporary() {
        return this.duration > -1;
    }

    public boolean isExpired() {
        return this.isTemporary() && System.currentTimeMillis() > (this.timestamp + this.duration);
    }

    public Date getExpirationDate() {
        return this.isTemporary() ? new Date((this.timestamp() + this.duration)) : new Date(Long.MAX_VALUE -1);
    }

    public Date getDate() {
        return new Date(this.timestamp() * 1000);
    }

    public String getFormattedDate() {
        return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", this.getDate());
    }

    public String getFormattedExpirationDate() {
        return this.isTemporary() ? String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", this.getExpirationDate()) : "Never";
    }

    public String getFormattedExpirationTime() {
        if (this.duration() > -1) {
            long x = this.duration + this.timestamp - System.currentTimeMillis() / 1000;

            long seconds = x % 60;
            long minutes = (x / 60) % 60;
            long hours = (x / (60 * 60)) % 24;
            long days = x / (60 * 60 * 24) % 365;
            long years = x / (60 * 60 * 24 * 365);

            StringBuilder sb = new StringBuilder();

            if (years > 0) {
                sb.append(years).append("y ");
            }
            if (days > 0) {
                sb.append(days).append("d ");
            }
            if (hours > 0) {
                sb.append(hours).append("h ");
            }
            if (minutes > 0) {
                sb.append(minutes).append("m ");
            }
            if (seconds > 0) {
                sb.append(seconds).append("s");
            }

            return sb.toString();
        } else {
            return "Never";
        }
    }

    public String getDisconnectMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are ").append(this.type.name).append("ed");
        if (this.reason != null && !this.reason.isEmpty()) {
            sb.append(" for: ").append(this.reason);
        }
        if (this.isTemporary()) {
            sb.append("\nThis punishment will expire in: ").append(this.getFormattedExpirationTime());
        }
        return sb.toString();
    }

    public Component getDisconnectChatMessage() {
        return Component.literal(this.getDisconnectMessage());
    }
}
