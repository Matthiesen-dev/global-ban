package dev.matthiesen.global_ban.common;

import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.matthiesen_core.common.AbstractCommonMod;
import dev.matthiesen.matthiesen_core.common.api.events.PlatformEvents;
import dev.matthiesen.matthiesen_core.common.api.platform.loader.ModConfigType;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class GlobalBanCommon extends AbstractCommonMod {
    public static final String MOD_ID = "global_ban";
    public static final String MOD_NAME = "Global Ban";
    public static @Token final String METRICS_TOKEN = "382f15e125d207a935a0999a3d268084";
    public static final GlobalBanCommon INSTANCE = new GlobalBanCommon();

    public static ConcurrentHashMap<UUID, String> UUID_TO_IP_CACHE = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Set<UUID>> IP_TO_UUID_CACHE = new ConcurrentHashMap<>();

    public GlobalBanCommon() {
        super(MOD_ID, MOD_NAME);
    }

    @Override
    public @Token @NotNull String getMetricsToken() {
        return METRICS_TOKEN;
    }

    public static String modConfig(String path) {
        return MOD_ID + "/" + path + ".toml";
    }

    public void initialize() {
        super.initialize();

        registerModConfig(MOD_ID, ModConfigType.COMMON, GlobalBanConfig.SERVER_SPEC, modConfig("server-config"));
        registerModConfig(MOD_ID, ModConfigType.COMMON, GlobalBanConfig.PUNISHMENTS_SPEC, modConfig("punished-accounts"));

        PlatformEvents.SERVER_STARTED.subscribe(server -> {
            registerAsyncScheduler();
            createInfoLog("Global Ban is running...");
        });

        createInfoLog("Initialized");
    }

    public void registerAsyncScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(this::handleExpiredPunishments, 5L, TimeUnit.MINUTES);
    }

    public void punishPlayer(PunishmentRecord record) {
        if (record.type().shouldKick && record.type().ipBased) {
            boolean alreadyBanned = false;

            for (ServerPlayer player : getCommonUtils().getServer().getPlayerList().getPlayers()) {
                if (player.getIpAddress().equals(record.playerIp())) {
                    player.connection.disconnect(record.getDisconnectChatMessage());

                    if (GlobalBanConfig.SERVER_CONFIG.autoBanPlayersByIP.getAsBoolean() && record.type() == PunishmentType.IP_BAN) {
                        PunishmentRecord newPunishment = PunishmentRecord.punishAlt(player.getUUID(), player.getName().getString(), record);

                        if (player.getUUID() == record.playerUuid()) {
                            alreadyBanned = true;
                        }

                        punishPlayer(newPunishment);
                    }
                }
            }

            if (GlobalBanConfig.SERVER_CONFIG.autoBanPlayersByIP.getAsBoolean() && record.type() == PunishmentType.IP_BAN && !alreadyBanned) {
                PunishmentRecord newPunishment = PunishmentRecord.punishAlt(record.playerUuid(), record.playerDisplayName(), record);
                punishPlayer(newPunishment);
            }
        } else if (record.type().shouldKick) {
            ServerPlayer player = getCommonUtils().getServer().getPlayerList().getPlayer(record.playerUuid());

            if (player != null) {
                player.connection.disconnect(record.getDisconnectChatMessage());
            }
        }

        createInfoLog("Punished player " + record.playerDisplayName() + " (" + record.playerUuid() + ") with type " + record.type().name);

        if (record.type().shouldRecord) {
            GlobalBanConfig.addPunishment(record);
        }
    }

    public void unpunishPlayer(PunishmentRecord record) {
        unpunishPlayer(record, false);
    }

    public void unpunishPlayer(PunishmentRecord record, boolean expired) {
        createInfoLog("Unpunished player " + record.playerDisplayName() + " (" + record.playerUuid() + ") with type " + record.type().name + (expired ? " (expired)" : ""));
        GlobalBanConfig.removePunishment(record);
    }

    public void handleExpiredPunishments() {
        Set<PunishmentRecord> expiredPunishments = ConcurrentHashMap.newKeySet();

        for (PunishmentRecord record : GlobalBanConfig.getPunished()) {
            if (record.isExpired()) {
                expiredPunishments.add(record);
            }
        }

        if (expiredPunishments.isEmpty()) return;

        getCommonUtils().getServer().execute(() -> {
            for (PunishmentRecord record : expiredPunishments) {
                unpunishPlayer(record, true);
            }
        });
    }
}
