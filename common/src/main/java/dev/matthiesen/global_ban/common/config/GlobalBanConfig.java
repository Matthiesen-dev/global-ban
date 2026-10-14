package dev.matthiesen.global_ban.common.config;

import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.def.SyncType;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
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

    private static final List<PunishmentRecord> punishmentCache = new ArrayList<>();

    public static List<PunishmentRecord> getPunished() {
        if (punishmentCache.isEmpty()) {
            punishmentCache.addAll(loadPunished());
        }
        return new ArrayList<>(punishmentCache);
    }

    public static void reloadPunished() {
        punishmentCache.clear();
        punishmentCache.addAll(loadPunished());
    }

    public static void invalidatePunished() {
        punishmentCache.clear();
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
        if (GlobalBanConfig.SERVER_CONFIG.scheduler_apiSyncEnabled.getAsBoolean()) {
            appendPunishmentToPendingSync(punishment, SyncType.ADD);
        }
        PUNISHMENTS.punished.set(bannedPlayers.stream().map(PunishmentRecord::serialize).toList());
        PUNISHMENTS.punished.save();
        reloadPunished();
    }

    public static int removePunishment(PunishmentRecord punishment) {
        List<PunishmentRecord> bannedPlayers = getPunished();
        if (!bannedPlayers.contains(punishment)) return 0; // Not banned, no need to remove
        bannedPlayers.remove(punishment);
        if (GlobalBanConfig.SERVER_CONFIG.scheduler_apiSyncEnabled.getAsBoolean()) {
            appendPunishmentToPendingSync(punishment, SyncType.REMOVE);
        }
        PUNISHMENTS.punished.set(bannedPlayers.stream().map(PunishmentRecord::serialize).toList());
        PUNISHMENTS.punished.save();
        reloadPunished();
        return 1;
    }

    public static int clearPunishments(UUID player) {
        List<PunishmentRecord> bannedPlayers = getPunished();
        List<PunishmentRecord> updatedList = bannedPlayers.stream()
                .filter(punishment -> !punishment.playerUuid().equals(player))
                .toList();

        if (GlobalBanConfig.SERVER_CONFIG.scheduler_apiSyncEnabled.getAsBoolean()) {
            var removedPunishments = bannedPlayers.stream()
                    .filter(punishment -> punishment.playerUuid().equals(player))
                    .toList();
            removedPunishments.forEach(punishment -> appendPunishmentToPendingSync(punishment, SyncType.REMOVE));
        }

        int difference = bannedPlayers.size() - updatedList.size();
        PUNISHMENTS.punished.set(updatedList.stream().map(PunishmentRecord::serialize).toList());
        PUNISHMENTS.punished.save();
        reloadPunished();
        return difference;
    }

    public static int clearIpPunishments(String ipAddress) {
        List<PunishmentRecord> bannedPlayers = getPunished();
        List<PunishmentRecord> updatedList = bannedPlayers.stream()
                .filter(punishment -> !punishment.playerIp().equals(ipAddress))
                .toList();

        if (GlobalBanConfig.SERVER_CONFIG.scheduler_apiSyncEnabled.getAsBoolean()) {
            var removedPunishments = bannedPlayers.stream()
                    .filter(punishment -> punishment.playerIp().equals(ipAddress))
                    .toList();
            removedPunishments.forEach(punishment -> appendPunishmentToPendingSync(punishment, SyncType.REMOVE));
        }

        int difference = bannedPlayers.size() - updatedList.size();
        PUNISHMENTS.punished.set(updatedList.stream().map(PunishmentRecord::serialize).toList());
        PUNISHMENTS.punished.save();
        reloadPunished();
        return difference;
    }

    public static boolean hasPendingSyncPunishments() {
        return !PUNISHMENTS.punished_pendingSync.get().isEmpty();
    }

    public static List<PunishmentRecord.Sync> getPendingSyncPunishments() {
        return PUNISHMENTS.punished_pendingSync.get().stream()
                .map(PunishmentRecord.Sync::deserialize)
                .toList();
    }

    public static void appendPunishmentToPendingSync(PunishmentRecord punishment, SyncType syncType) {
        List<PunishmentRecord.Sync> pendingSyncList = new ArrayList<>(getPendingSyncPunishments());
        pendingSyncList.add(new PunishmentRecord.Sync(punishment, syncType));
        PUNISHMENTS.punished_pendingSync.set(pendingSyncList.stream().map(PunishmentRecord.Sync::serialize).toList());
        PUNISHMENTS.punished_pendingSync.save();
    }

    public static void clearPendingSyncPunishments() {
        PUNISHMENTS.punished_pendingSync.set(new ArrayList<>());
        PUNISHMENTS.punished_pendingSync.save();
    }

    /**
     * This method is intended to be called when a server is first registered with a Remote API server, and already has punishments in it's cache.
     * This method will take all current punishments in the server's cache and add them to the pending sync list, so that they can be sent to the Remote API server on the next sync.
     */
    public static void createForceSyncRequest() {
        List<PunishmentRecord> currentPunishments = getPunished();
        List<PunishmentRecord.Sync> pendingSyncList = new ArrayList<>(getPendingSyncPunishments());
        for (PunishmentRecord punishment : currentPunishments) {
            pendingSyncList.add(new PunishmentRecord.Sync(punishment, SyncType.ADD));
        }
        PUNISHMENTS.punished_pendingSync.set(pendingSyncList.stream().map(PunishmentRecord.Sync::serialize).toList());
        PUNISHMENTS.punished_pendingSync.save();
    }

    public static UUID getServerUUID() {
        return UUID.fromString(SERVER_CONFIG.serverUUID.get());
    }
}
