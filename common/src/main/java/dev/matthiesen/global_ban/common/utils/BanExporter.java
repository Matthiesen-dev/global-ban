package dev.matthiesen.global_ban.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public final class BanExporter {
    public static final String EXPORT_DIR = "exports";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static void exportBans() {
        List<PunishmentRecord> punishments = GlobalBanConfig.getPunished();
        PlayerBanList playerBanList = PlayerBanList.fromPunishments(punishments);
        IpBanList ipBanList = IpBanList.fromPunishments(punishments);

        if (new File(EXPORT_DIR).mkdir()) {
            GlobalBanCommon.INSTANCE.createDebugLog("Created export directory: " + EXPORT_DIR);
        }
        playerBanList.exportToFile();
        ipBanList.exportToFile();
    }

    public record PlayerBanList(List<Entry> entries) {
        public static final File EXPORT_FILE_PATH = new File(EXPORT_DIR, "banned-players.json");

        public static PlayerBanList fromPunishments(List<PunishmentRecord> punishments) {
            List<Entry> entries = punishments.stream()
                    .filter(punishment -> punishment.type == PunishmentType.BAN)
                    .map(punishment -> new Entry(
                            punishment.playerUuid.toString(),
                            punishment.playerDisplayName,
                            punishment.getDate().toString(),
                            punishment.punisherDisplayName,
                            punishment.isTemporary() ? punishment.getExpirationDate().toString() : "forever",
                            punishment.reason
                    ))
                    .toList();
            return new PlayerBanList(entries);
        }

        public void exportToFile() {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(EXPORT_FILE_PATH));
                writer.write(GSON.toJson(this));
                writer.close();
            } catch (Exception e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to export banned players to file: " + e.getMessage(), e);
            }
        }

        public record Entry(
                String UUID,
                String name,
                String created,
                String source,
                String expires,
                String reason
        ) {
        }
    }

    public record IpBanList(List<Entry> entries) {
        public static final File EXPORT_FILE_PATH = new File(EXPORT_DIR, "banned-ips.json");

        public static IpBanList fromPunishments(List<PunishmentRecord> punishments) {
            List<Entry> entries = punishments.stream()
                    .filter(punishment -> punishment.type == PunishmentType.BAN)
                    .map(punishment -> new Entry(
                            punishment.playerIp,
                            punishment.getDate().toString(),
                            punishment.punisherDisplayName,
                            punishment.isTemporary() ? punishment.getExpirationDate().toString() : "forever",
                            punishment.reason
                    ))
                    .toList();
            return new IpBanList(entries);
        }

        public void exportToFile() {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(EXPORT_FILE_PATH));
                writer.write(GSON.toJson(this));
                writer.close();
            } catch (Exception e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to export banned IPs to file: " + e.getMessage(), e);
            }
        }

        public record Entry(
                String ip,
                String created,
                String source,
                String expires,
                String reason
        ) {
        }
    }
}
