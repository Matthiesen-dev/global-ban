package dev.matthiesen.global_ban.common;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.registry.CommandRegistry;
import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.Helpers;
import dev.matthiesen.libs.faststats.Token;
import dev.matthiesen.matthiesen_core.common.AbstractCommonMod;
import dev.matthiesen.matthiesen_core.common.api.events.PlatformEvents;
import dev.matthiesen.matthiesen_core.common.api.events.config.ConfigEvent;
import dev.matthiesen.matthiesen_core.common.api.events.server.PlayerEvent;
import dev.matthiesen.matthiesen_core.common.api.events.server.ServerEvent;
import dev.matthiesen.matthiesen_core.common.api.platform.loader.ModConfigType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
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
    private static final File IP_CACHE_FILE = Paths.get("ipcache.json").toFile();

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
        registerModConfig(MOD_ID, ModConfigType.STARTUP, GlobalBanConfig.PERMISSIONS_SPEC, modConfig("permissions"));

        PermissionRegistry.init();
        CommandRegistry.init();

        PlatformEvents.SERVER_STARTING.subscribe(this::onServerStarting);
        PlatformEvents.SERVER_STARTED.subscribe(this::onServerStarted);
        PlatformEvents.SERVER_STOPPING.subscribe(this::onServerStopping);
        PlatformEvents.CONFIG_RELOADING(MOD_ID).subscribe(this::onConfigReloading);

        PlatformEvents.PLAYER_JOIN.subscribe(this::onPlayerJoin);

        createInfoLog("Initialized");
    }

    public boolean isGooeyLibsLoaded() {
        return getCommonUtils().isModLoaded("gooeylibs");
    }

    private void onConfigReloading(ConfigEvent.Reloading reloading) {
        GlobalBanConfig.invalidatePunished();
        createInfoLog("Reloaded Global Ban configuration");
    }

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
            .create();

    private void onServerStarting(ServerEvent.Starting starting) {
        try {
            ConcurrentHashMap<String, String> cache = IP_CACHE_FILE.exists()
                    ? GSON.fromJson(new FileReader(IP_CACHE_FILE), new TypeToken<ConcurrentHashMap<String, String>>() {}.getType())
                    : null;

            if (cache != null) {
                for (var entry : cache.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getValue());
                        String ip = entry.getKey();

                        UUID_TO_IP_CACHE.put(uuid, ip);
                        IP_TO_UUID_CACHE.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                    } catch (Exception e) {
                        // Ignore invalid entries
                    }
                }
            }

        } catch (FileNotFoundException e) {
            createWarnLog("IP cache file not found, creating a new one...");
        }
    }

    private void onServerStarted(ServerEvent.Started started) {
        registerAsyncScheduler();
        createInfoLog("Global Ban is running...");
    }

    private void onServerStopping(ServerEvent.Stopping stopping) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(IP_CACHE_FILE));

            var ipCache = new HashMap<>();

            for (var entry : UUID_TO_IP_CACHE.entrySet()) {
                ipCache.put(entry.getKey().toString(), entry.getValue());
            }

            writer.write(GSON.toJson(ipCache));
            writer.close();
        } catch (IOException exception) {
            createErrorLog("Failed to save IP cache file: " + exception.getMessage(), exception);
        }
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
                    player.connection.disconnect(record.getDisconnectScreenComponent());

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
                player.connection.disconnect(record.getDisconnectScreenComponent());
            }
        }

        getCommonUtils().getServer().sendSystemMessage(record.getChatMessage());
        for (ServerPlayer player : getCommonUtils().getServer().getPlayerList().getPlayers()) {
            if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_BANS_PERMISSION)) {
                player.sendSystemMessage(record.getChatMessage());
            }
        }

        if (record.type().shouldRecord) {
            GlobalBanConfig.addPunishment(record);
        }
    }

    private void onPlayerJoin(PlayerEvent.Join join) {
        if (!GlobalBanConfig.SERVER_CONFIG.showKnownAccounts.getAsBoolean()) {
            return;
        }

        String ip = join.player().getIpAddress();
        Set<UUID> knownAccounts = GlobalBanCommon.IP_TO_UUID_CACHE.get(ip);

        if (knownAccounts == null || knownAccounts.isEmpty()) return;

        List<Component> playerMessages = new LinkedList<>();

        for (UUID player : knownAccounts) {
            var profileCache = getCommonUtils().getServer().getProfileCache();
            String name;
            if (profileCache != null) {
                name = profileCache.get(player).map(GameProfile::getName).orElse(player.toString());
            } else {
                name = player.toString();
            }
            MutableComponent messageText = Component.literal("[" + name + "]");
            List<PunishmentRecord> punishments = GlobalBanConfig.isPlayerUUIDPunished(player);

            if (getCommonUtils().getServer().getPlayerList().getPlayer(player) != null || player.equals(join.player().getUUID())) {
                messageText.withStyle(ChatFormatting.GREEN);
            } else if (punishments.isEmpty()) {
                messageText.withStyle(ChatFormatting.GRAY);
            } else {
                messageText.withStyle(ChatFormatting.RED);
                PunishmentRecord punishment = punishments.getFirst();

                messageText.withStyle(style ->
                        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, punishment.getChatMessage()))
                );
            }

            playerMessages.add(messageText);
        }

        Component message = ComponentUtils.formatList(playerMessages, Component.literal(" "));
        getCommonUtils().getServer().sendSystemMessage(message);

        for (ServerPlayer player : getCommonUtils().getServer().getPlayerList().getPlayers()) {
            if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_KNOWN_ACCOUNTS_PERMISSION)) {
                player.sendSystemMessage(message);
            }
        }
    }

    public void unpunishPlayer(PunishmentRecord record) {
        unpunishPlayer(record, false);
    }

    public void unpunishPlayer(PunishmentRecord record, boolean expired) {
        if (!expired) {
            var message = switch (record.type()) {
                case BAN -> GlobalBanConfig.SERVER_CONFIG.messages_unbanChatMessage.get();
                case IP_BAN -> GlobalBanConfig.SERVER_CONFIG.messages_ipUnbanChatMessage.get();
                case KICK -> null;
            };
            if (message != null) {
                String singleStringMessage = String.join("\n", message);
                var parsed = Helpers.processPlaceholders(singleStringMessage, record.getPlaceholders());
                getCommonUtils().getServer().sendSystemMessage(parsed.toComponent());
                for (ServerPlayer player : getCommonUtils().getServer().getPlayerList().getPlayers()) {
                    if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_BANS_PERMISSION)) {
                        player.sendSystemMessage(parsed.toComponent());
                    }
                }
            }
        } else {
            createInfoLog("Punishment expired: " + record.toString());
        }
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
