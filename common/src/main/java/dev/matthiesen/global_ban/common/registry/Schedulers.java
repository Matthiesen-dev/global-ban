package dev.matthiesen.global_ban.common.registry;

import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.*;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.events.UnPunishPlayerEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Schedulers {
    public static void register() {
        ServerConfig serverConfig = GlobalBanConfig.SERVER_CONFIG;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(serverConfig.scheduler_cpuCorePoolSize.getAsInt());

        scheduler.schedule(Schedulers::handleExpiredPunishments, serverConfig.scheduler_expiredPunishmentsCheckInterval.getAsLong(), TimeUnit.MINUTES);
         if (serverConfig.scheduler_apiSyncEnabled.getAsBoolean()) {
            scheduler.schedule(PunishmentSync::run, serverConfig.scheduler_apiSyncInterval.getAsLong(), TimeUnit.MINUTES);
         }
    }

    public static void handleExpiredPunishments() {
        Set<PunishmentRecord> expiredPunishments = ConcurrentHashMap.newKeySet();

        for (PunishmentRecord record : GlobalBanConfig.getPunished()) {
            if (record.isExpired()) {
                expiredPunishments.add(record);
            }
        }

        if (expiredPunishments.isEmpty()) return;

        GlobalBanCommon.INSTANCE.getCommonUtils().getServer().execute(() -> {
            for (PunishmentRecord record : expiredPunishments) {
                GlobalBanCommon.Events.UNPUNISH_PLAYER.emit(new UnPunishPlayerEvent(record, true));
            }
        });
    }
}
