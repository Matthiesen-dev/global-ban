package dev.matthiesen.global_ban.common.def.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;

import java.util.List;
import java.util.UUID;

public record SyncPunishmentPayload(
        List<PunishmentRecord.Sync> punishments,
        UUID serverId
) {
    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();

    public static SyncPunishmentPayload fromJsonPayload(String jsonPayload) {
        return GSON.fromJson(jsonPayload, SyncPunishmentPayload.class);
    }

    public static SyncPunishmentPayload create(List<PunishmentRecord.Sync> punishments) {
        return new SyncPunishmentPayload(punishments, GlobalBanConfig.getServerUUID());
    }

    public String toJsonPayload() {
        return GSON.toJson(this);
    }
}
