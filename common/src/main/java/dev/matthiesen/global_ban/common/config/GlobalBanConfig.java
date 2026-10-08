package dev.matthiesen.global_ban.common.config;

import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;

public final class GlobalBanConfig {
    public static final ServerConfig SERVER_CONFIG;
    public static final ModConfigSpec SERVER_SPEC;

    public static final Punishments PUNISHMENTS;
    public static final ModConfigSpec PUNISHMENTS_SPEC;

    public static final PermissionsConfig PERMISSIONS_CONFIG;
    public static final ModConfigSpec PERMISSIONS_SPEC;

    static {
        Pair<ServerConfig, ModConfigSpec> serverSpecPair = new ModConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_CONFIG = serverSpecPair.getLeft();
        SERVER_SPEC = serverSpecPair.getRight();

        Pair<Punishments, ModConfigSpec> punishmentsPair = new ModConfigSpec.Builder().configure(Punishments::new);
        PUNISHMENTS = punishmentsPair.getLeft();
        PUNISHMENTS_SPEC = punishmentsPair.getRight();

        Pair<PermissionsConfig, ModConfigSpec> permissionsSpecPair = new ModConfigSpec.Builder().configure(PermissionsConfig::new);
        PERMISSIONS_CONFIG = permissionsSpecPair.getLeft();
        PERMISSIONS_SPEC = permissionsSpecPair.getRight();
    }

    private static List<PunishmentRecord> punishmentCache = null;

    public static List<PunishmentRecord> getPunished() {
        if (punishmentCache == null) {
            punishmentCache = loadPunished();
        }
        return punishmentCache;
    }

    public static void reloadPunished() {
        punishmentCache = loadPunished();
    }

    public static void invalidatePunished() {
        punishmentCache = null;
    }

    private static List<PunishmentRecord> loadPunished() {
        return PUNISHMENTS.punished.get().stream()
                .map(PunishmentRecord::deserialize)
                .toList();
    }

    public static List<PunishmentRecord> isPlayerUUIDPunished(UUID player) {
        return getPunished().stream()
                .filter(punishment -> punishment.playerUuid().equals(player) && punishment.type() == PunishmentType.BAN)
                .toList();
    }

    public static List<PunishmentRecord> isIpPunished(String ipAddress) {
        return getPunished().stream()
                .filter(punishment -> punishment.playerIp().equals(ipAddress) && punishment.type() == PunishmentType.IP_BAN)
                .toList();
    }

    public static void addPunishment(PunishmentRecord punishment) {
        List<PunishmentRecord> bannedPlayers = getPunished();
        if (bannedPlayers.contains(punishment)) return; // Already banned, no need to add again
        bannedPlayers.add(punishment);
        PUNISHMENTS.punished.set(bannedPlayers.stream().map(PunishmentRecord::serialize).toList());
        reloadPunished();
    }

    public static int removePunishment(PunishmentRecord punishment) {
        List<PunishmentRecord> bannedPlayers = getPunished();
        if (!bannedPlayers.contains(punishment)) return 0; // Not banned, no need to remove
        bannedPlayers.remove(punishment);
        PUNISHMENTS.punished.set(bannedPlayers.stream().map(PunishmentRecord::serialize).toList());
        reloadPunished();
        return 1;
    }

    public static int clearPunishments(UUID player) {
        List<PunishmentRecord> bannedPlayers = getPunished();
        List<PunishmentRecord> updatedList = bannedPlayers.stream()
                .filter(punishment -> !punishment.playerUuid().equals(player))
                .toList();
        int difference = bannedPlayers.size() - updatedList.size();
        PUNISHMENTS.punished.set(updatedList.stream().map(PunishmentRecord::serialize).toList());
        reloadPunished();
        return difference;
    }

    public static int clearIpPunishments(String ipAddress) {
        List<PunishmentRecord> bannedPlayers = getPunished();
        List<PunishmentRecord> updatedList = bannedPlayers.stream()
                .filter(punishment -> !punishment.playerIp().equals(ipAddress))
                .toList();
        int difference = bannedPlayers.size() - updatedList.size();
        PUNISHMENTS.punished.set(updatedList.stream().map(PunishmentRecord::serialize).toList());
        reloadPunished();
        return difference;
    }

    public static void syncPunishments() {
        // TODO: Implement syncing logic to remote API once that system is in place. For now, this is a placeholder to indicate where syncing would occur.
    }

    public static UUID getServerUUID() {
        return UUID.fromString(SERVER_CONFIG.serverUUID.get());
    }
}
