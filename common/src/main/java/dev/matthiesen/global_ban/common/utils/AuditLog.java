package dev.matthiesen.global_ban.common.utils;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

public final class AuditLog {
    private static final String LOG_DIR = "logs";
    private static final String LOG_NAME = "audit.log";
    private static final String ROLLED_PREFIX = "audit-";
    private static final String ROLLED_SUFFIX = ".log";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object LOCK = new Object();
    private static LocalDate lastMaintenance;

    private AuditLog() {
    }

    private static Path getAuditLogPath() {
        return GlobalBanCommon.INSTANCE.getCommonUtils().getModConfig(GlobalBanCommon.MOD_ID, LOG_DIR + "/" + LOG_NAME);
    }

    private static String buildAuditLogString(String message) {
        return "[" + LocalDateTime.now().format(TIMESTAMP) + "] " + message + System.lineSeparator();
    }

    public static void appendToAuditLog(String message) {
        synchronized (LOCK) {
            try {
                Path path = getAuditLogPath();
                Path dir = path.toAbsolutePath().getParent();
                if (dir != null) {
                    Files.createDirectories(dir);
                }
                runMaintenance(path, dir);
                Files.writeString(path, buildAuditLogString(message), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException | RuntimeException e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to write to " + LOG_NAME + ": " + e.getMessage(), e);
            }
        }
    }

    // Rolls a stale audit.log into a dated file and prunes old ones, at most once per day.
    private static void runMaintenance(Path path, Path dir) throws IOException {
        LocalDate today = LocalDate.now();
        if (today.equals(lastMaintenance)) {
            return;
        }

        if (Files.exists(path)) {
            LocalDate modified = Instant.ofEpochMilli(Files.getLastModifiedTime(path).toMillis())
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (modified.isBefore(today)) {
                Path rolled = path.resolveSibling(ROLLED_PREFIX + modified + ROLLED_SUFFIX);
                if (Files.exists(rolled)) {
                    Files.write(rolled, Files.readAllBytes(path), StandardOpenOption.APPEND);
                    Files.delete(path);
                } else {
                    Files.move(path, rolled, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        if (dir != null) {
            pruneOldLogs(dir, today.minusDays(GlobalBanConfig.SERVER_CONFIG.security_auditLoggingMaxDays.get()));
        }
        lastMaintenance = today;
    }

    private static void pruneOldLogs(Path dir, LocalDate cutoff) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(file -> {
                String name = file.getFileName().toString();
                if (!name.startsWith(ROLLED_PREFIX) || !name.endsWith(ROLLED_SUFFIX)) {
                    return;
                }
                String date = name.substring(ROLLED_PREFIX.length(), name.length() - ROLLED_SUFFIX.length());
                try {
                    if (LocalDate.parse(date).isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                    }
                } catch (DateTimeParseException | IOException e) {
                    GlobalBanCommon.INSTANCE.createErrorLog("Failed to prune audit log " + name + ": " + e.getMessage(), e);
                }
            });
        }
    }
}
