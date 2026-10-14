package dev.matthiesen.global_ban.common.def;

import com.electronwill.nightconfig.core.Config;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.utils.Helpers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PunishmentRecord {
    public static final long NON_EXPIRING_PUNISHMENT = -1L;
    public static final String UNKNOWN_PLAYER_NAME = "UnknownPlayer";
    public static final String UNDEFINED_IP_ADDRESS = "undefined";

    public PunishmentType type;
    public PunishmentType type() {
        return type;
    }
    public long timestamp;
    public long duration;
    public UUID playerUuid;
    public String playerIp;
    public String playerDisplayName;
    public UUID punisherUuid;
    public String punisherDisplayName;
    public String reason;
    public UUID serverUuid;
    public boolean synced;
    // Soft-delete marker: the record stays in the list until the removal has been synced. Not sent to the API.
    public transient boolean removed;

    public void markAsSynced() {
        this.synced = true;
    }

    public void markAsRemoved() {
        this.removed = true;
        this.synced = false;
    }

    public boolean matches(PunishmentRecord other) {
        return this.type == other.type
                && this.timestamp == other.timestamp
                && this.playerUuid.equals(other.playerUuid)
                && java.util.Objects.equals(this.playerIp, other.playerIp);
    }

    public PunishmentRecord(
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
        this.type = type;
        this.timestamp = timestamp;
        this.duration = duration;
        this.playerUuid = playerUuid;
        this.playerIp = playerIp;
        this.playerDisplayName = playerDisplayName;
        this.punisherUuid = punisherUuid;
        this.punisherDisplayName = punisherDisplayName;
        this.reason = reason;
        this.serverUuid = serverUuid;
    }

    public record Sync(PunishmentRecord record, SyncType type) {
    }

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
        boolean isSynced = config.get("synced") instanceof Boolean synced && synced;
        boolean isRemoved = config.get("removed") instanceof Boolean removed && removed;
        PunishmentRecord record = new PunishmentRecord(
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
        record.removed = isRemoved;
        record.synced = isSynced;
        return record;
    }

    public static boolean isValid(Config config) {
        if (!(config.get("type") instanceof String typeRaw) || PunishmentType.fromName(typeRaw) == null) {
            return false;
        }
        if (!(config.get("timestamp") instanceof Number)) {
            return false;
        }
        if (!(config.get("duration") instanceof Number)) {
            return false;
        }
        if (!(config.get("playerUuid") instanceof String playerUuidRaw)) {
            return false;
        }
        if (!(config.get("playerIp") instanceof String)) {
            return false;
        }
        if (!(config.get("playerDisplayName") instanceof String)) {
            return false;
        }
        if (!(config.get("punisherUuid") instanceof String punisherUuidRaw)) {
            return false;
        }
        if (!(config.get("punisherDisplayName") instanceof String)) {
            return false;
        }
        if (!(config.get("reason") instanceof String)) {
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

    public static PunishmentRecord create(ServerPlayer target, CommandSourceStack punisher, PunishmentType type, long duration, String reason) {
        return create(
                target.getUUID(),
                target.getIpAddress(),
                target.getGameProfile().getName(),
                punisher,
                type,
                duration,
                reason,
                GlobalBanConfig.getServerUUID()
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
        config.set("synced", this.synced);
        config.set("removed", this.removed);
        return config;
    }

    public boolean isTemporary() {
        return this.duration > NON_EXPIRING_PUNISHMENT;
    }

    public boolean isExpired() {
        return this.isTemporary() && System.currentTimeMillis() > (this.timestamp + this.duration);
    }

    public Date getExpirationDate() {
        return this.isTemporary() ? new Date((this.timestamp + this.duration)) : new Date(Long.MAX_VALUE - 1);
    }

    public Date getDate() {
        return new Date(this.timestamp * 1000);
    }

    public String getFormattedDate() {
        return String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", this.getDate());
    }

    public String getFormattedExpirationDate() {
        return this.isTemporary() ? String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS", this.getExpirationDate()) : "Never";
    }

    public String getFormattedExpirationTime() {
        if (this.duration > NON_EXPIRING_PUNISHMENT) {
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

    public Component getDisconnectScreenComponent() {
        var message = switch (this.type) {
            case BAN -> this.isTemporary() ? GlobalBanConfig.SERVER_CONFIG.messages_tempBanScreen.get() : GlobalBanConfig.SERVER_CONFIG.messages_banScreen.get();
            case IP_BAN -> this.isTemporary() ? GlobalBanConfig.SERVER_CONFIG.messages_tempIpBanScreen.get() : GlobalBanConfig.SERVER_CONFIG.messages_ipBanScreen.get();
            case KICK -> GlobalBanConfig.SERVER_CONFIG.messages_kickScreen.get();
        };
        String singleStringMessage = String.join("\n", message);
        var parsed = Helpers.processPlaceholders(singleStringMessage, this.getPlaceholders());
        return parsed.toComponent();
    }

    public Component getChatMessage() {
        var message = switch (this.type) {
            case BAN -> this.isTemporary() ? GlobalBanConfig.SERVER_CONFIG.messages_tempBanChatMessage.get() : GlobalBanConfig.SERVER_CONFIG.messages_banChatMessage.get();
            case IP_BAN -> this.isTemporary() ? GlobalBanConfig.SERVER_CONFIG.messages_tempIpBanChatMessage.get() : GlobalBanConfig.SERVER_CONFIG.messages_ipBanChatMessage.get();
            case KICK -> GlobalBanConfig.SERVER_CONFIG.messages_kickMessage.get();
        };
        String singleStringMessage = String.join("\n", message);
        var parsed = Helpers.processPlaceholders(singleStringMessage, this.getPlaceholders());
        return parsed.toComponent();
    }

    public Map<String, String> getPlaceholders() {
        HashMap<String, String> placeholders = new HashMap<>();

        placeholders.put("%operator%", this.punisherDisplayName);
        placeholders.put("%operator_uuid%", this.punisherUuid.toString());
        placeholders.put("%player%", this.playerDisplayName);
        placeholders.put("%player_uuid%", this.playerUuid.toString());
        placeholders.put("%reason%", this.reason);
        placeholders.put("%type%", this.type.name);
        placeholders.put("%date%", this.getFormattedDate());
        placeholders.put("%expiration_date%", this.getFormattedExpirationDate());
        placeholders.put("%expiration_time%", this.getFormattedExpirationTime());
        placeholders.put("%server_uuid%", this.serverUuid.toString());

        return placeholders;
    }
}
