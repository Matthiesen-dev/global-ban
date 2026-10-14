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

    private static synchronized List<PunishmentRecord> getAllRecords() {
        if (punishmentCache.isEmpty()) {
            punishmentCache.addAll(loadPunished());
        }
        return new ArrayList<>(punishmentCache);
    }

    /**
     * Active punishments only; records that are soft-deleted while awaiting removal sync are excluded.
     */
    public static List<PunishmentRecord> getPunished() {
        return getAllRecords().stream()
                .filter(punishment -> !punishment.removed)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    public static synchronized void reloadPunished() {
        punishmentCache.clear();
        punishmentCache.addAll(loadPunished());
    }

    public static synchronized void invalidatePunished() {
        punishmentCache.clear();
    }

    private static List<PunishmentRecord> loadPunished() {
        return PUNISHMENTS.punished.get().stream()
                .map(PunishmentRecord::deserialize)
                .toList();
    }

    private static void saveRecords(List<PunishmentRecord> records) {
        PUNISHMENTS.punished.set(records.stream().map(PunishmentRecord::serialize).toList());
        PUNISHMENTS.punished.save();
        reloadPunished();
    }

    private static boolean isSyncEnabled() {
        return GlobalBanConfig.SERVER_CONFIG.scheduler_apiSyncEnabled.getAsBoolean();
    }

    /**
     * Removes the matching records, or soft-deletes them when the remote API still needs to be told about the removal.
     */
    private static int removeMatching(List<PunishmentRecord> all, java.util.function.Predicate<PunishmentRecord> filter, boolean fromRemote) {
        int count = 0;
        for (PunishmentRecord record : new ArrayList<>(all)) {
            if (record.removed || !filter.test(record)) continue;
            count++;
            if (!fromRemote && isSyncEnabled() && record.synced) {
                record.markAsRemoved();
            } else {
                all.remove(record);
            }
        }
        return count;
    }

    public static List<PunishmentRecord> isPlayerUUIDPunished(UUID player) {
        return getPunished().stream()
                .filter(punishment -> punishment.playerUuid.equals(player) && punishment.type() == PunishmentType.BAN)
                .toList();
    }

    public static List<PunishmentRecord> isIpPunished(String ipAddress) {
        return getPunished().stream()
                .filter(punishment -> punishment.playerIp.equals(ipAddress) && punishment.type() == PunishmentType.IP_BAN)
                .toList();
    }

    public static synchronized void addPunishment(PunishmentRecord punishment) {
        List<PunishmentRecord> records = getAllRecords();
        if (records.stream().anyMatch(record -> !record.removed && record.matches(punishment))) return; // Already banned, no need to add again
        records.removeIf(record -> record.removed && record.matches(punishment));
        records.add(punishment);
        saveRecords(records);
    }

    public static int removePunishment(PunishmentRecord punishment) {
        return removePunishment(punishment, false);
    }

    /**
     * @param fromRemote true when the removal originated from the remote API, so it must not be synced back
     */
    public static synchronized int removePunishment(PunishmentRecord punishment, boolean fromRemote) {
        List<PunishmentRecord> records = getAllRecords();
        int removed = removeMatching(records, record -> record.matches(punishment), fromRemote);
        if (removed == 0) return 0; // Not banned, no need to remove
        saveRecords(records);
        return 1;
    }

    public static synchronized int clearPunishments(UUID player) {
        List<PunishmentRecord> records = getAllRecords();
        int difference = removeMatching(records, punishment -> punishment.playerUuid.equals(player), false);
        saveRecords(records);
        return difference;
    }

    public static synchronized int clearIpPunishments(String ipAddress) {
        List<PunishmentRecord> records = getAllRecords();
        int difference = removeMatching(records, punishment -> punishment.playerIp.equals(ipAddress), false);
        saveRecords(records);
        return difference;
    }

    public static boolean hasPendingSyncPunishments() {
        return getAllRecords().stream().anyMatch(record -> !record.synced);
    }

    public static List<PunishmentRecord.Sync> getPendingSyncPunishments() {
        return getAllRecords().stream()
                .filter(record -> !record.synced)
                .map(record -> new PunishmentRecord.Sync(record, record.removed ? SyncType.REMOVE : SyncType.ADD))
                .toList();
    }

    /**
     * Marks the given (previously sent) pending records as synced, and purges removed records whose removal has been synced.
     */
    public static synchronized void markPendingSynced(List<PunishmentRecord.Sync> sent) {
        List<PunishmentRecord> records = getAllRecords();
        for (PunishmentRecord record : records) {
            if (record.synced) continue;
            for (PunishmentRecord.Sync sync : sent) {
                if (sync.record().matches(record) && (sync.type() == SyncType.REMOVE) == record.removed) {
                    record.markAsSynced();
                    break;
                }
            }
        }
        records.removeIf(record -> record.removed && record.synced);
        saveRecords(records);
    }

    /**
     * This method is intended to be called when a server is first registered with a Remote API server, and already has punishments in it's cache.
     * This method will flag all current punishments as unsynced, so that they are sent to the Remote API server on the next sync.
     */
    public static synchronized void createForceSyncRequest() {
        List<PunishmentRecord> records = getAllRecords();
        records.forEach(record -> record.synced = false);
        saveRecords(records);
    }

    public static UUID getServerUUID() {
        return UUID.fromString(SERVER_CONFIG.serverUUID.get());
    }
}
