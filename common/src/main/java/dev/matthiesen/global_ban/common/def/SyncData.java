package dev.matthiesen.global_ban.common.def;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

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