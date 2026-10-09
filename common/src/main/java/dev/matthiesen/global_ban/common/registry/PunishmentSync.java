package dev.matthiesen.global_ban.common.registry;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.SyncData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class PunishmentSync {
    public static List<PunishmentRecord.Sync> getPendingSyncPunishments() {
        if (GlobalBanConfig.hasPendingSyncPunishments())
            return GlobalBanConfig.getPendingSyncPunishments();
        return List.of();
    }

    public static void run() {
        String apiUrl = GlobalBanConfig.SERVER_CONFIG.scheduler_apiSyncUrl.get();
        if (apiUrl == null || apiUrl.isEmpty()) {
            GlobalBanCommon.INSTANCE.createWarnLog("Sync API URL is not configured. Skipping punishment sync.");
            return;
        }

        HttpRequest request = createPostRequest(apiUrl, getPendingSyncPunishments());
        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Failed to sync punishments: " + response.statusCode() + " - " + response.body());
                    }
                    GlobalBanConfig.clearPendingSyncPunishments();
                    return SyncData.fromJson(response.body());
                })
                .thenAccept(syncData -> {
                    if (syncData.punishments().isEmpty()) {
                        GlobalBanCommon.INSTANCE.createInfoLog("No punishments received from the sync API.");
                    } else {
                        GlobalBanCommon.INSTANCE.createInfoLog("Received " + syncData.punishments().size() + " punishments from the sync API.");
                        for (var newPunishment : syncData.punishments()) {
                            switch (newPunishment.type()) {
                                case ADD -> GlobalBanCommon.INSTANCE.punishPlayer(newPunishment.record());
                                case REMOVE -> GlobalBanCommon.INSTANCE.unpunishPlayer(newPunishment.record());
                            }
                        }
                    }
                })
                .exceptionally(throwable -> {
                    GlobalBanCommon.INSTANCE.createErrorLog("Error during punishment sync: " + throwable.getMessage());
                    return null;
                });
    }

    public static HttpRequest createPostRequest(String url, List<PunishmentRecord.Sync> punishments) {
        SyncData syncData = new SyncData(punishments);
        String jsonPayload = syncData.toJson();
        return getRequestBuilder(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
    }

    private static HttpRequest.Builder getRequestBuilder(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "GlobalBan/1.0 (Matthiesen-Dev https://github.com/Matthiesen-dev/global-ban)")
                .header("X-Global-Ban-UUID", GlobalBanConfig.getServerUUID().toString());
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
}
