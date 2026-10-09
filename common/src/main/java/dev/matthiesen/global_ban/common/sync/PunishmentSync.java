package dev.matthiesen.global_ban.common.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

public final class PunishmentSync {
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

    public record SyncData(List<PunishmentRecord.Sync> punishments) {
        public static SyncData fromJson(String json) {
            return GSON.fromJson(json, SyncData.class);
        }

        public String toJson() {
            return GSON.toJson(this);
        }

        private static final Gson GSON = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
    }
}
