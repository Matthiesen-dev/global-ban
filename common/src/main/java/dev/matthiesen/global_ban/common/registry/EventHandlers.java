package dev.matthiesen.global_ban.common.registry;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.config.ServerConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.matthiesen_core.common.api.events.config.ConfigEvent;
import dev.matthiesen.matthiesen_core.common.api.events.server.PlayerEvent;
import dev.matthiesen.matthiesen_core.common.api.events.server.ServerEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class EventHandlers {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
            .create();

    public static void onServerStarting(ServerEvent.Starting starting) {
        try {
            ConcurrentHashMap<String, String> cache = GlobalBanCommon.IP_CACHE_FILE.exists()
                    ? GSON.fromJson(new FileReader(GlobalBanCommon.IP_CACHE_FILE), new TypeToken<ConcurrentHashMap<String, String>>() {}.getType())
                    : null;

            if (cache != null) {
                for (var entry : cache.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getValue());
                        String ip = entry.getKey();

                        GlobalBanCommon.UUID_TO_IP_CACHE.put(uuid, ip);
                        GlobalBanCommon.IP_TO_UUID_CACHE.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                    } catch (Exception e) {
                        // Ignore invalid entries
                    }
                }
            }

        } catch (FileNotFoundException e) {
            GlobalBanCommon.INSTANCE.createWarnLog("IP cache file not found, creating a new one...");
        }
    }

    public static void onServerStarted(ServerEvent.Started started) {
        Schedulers.register();
        GlobalBanCommon.INSTANCE.createInfoLog("Global Ban is running...");
    }

    public static void onServerStopping(ServerEvent.Stopping stopping) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(GlobalBanCommon.IP_CACHE_FILE));

            var ipCache = new HashMap<>();

            for (var entry : GlobalBanCommon.UUID_TO_IP_CACHE.entrySet()) {
                ipCache.put(entry.getKey().toString(), entry.getValue());
            }

            writer.write(GSON.toJson(ipCache));
            writer.close();
        } catch (IOException exception) {
            GlobalBanCommon.INSTANCE.createErrorLog("Failed to save IP cache file: " + exception.getMessage(), exception);
        }
    }

    public static void onConfigReloading(ConfigEvent.Reloading reloading) {
        GlobalBanConfig.invalidatePunished();
        GlobalBanCommon.INSTANCE.createInfoLog("Reloaded Global Ban configuration");
    }

    public static void onPlayerJoin(PlayerEvent.Join join) {
        MinecraftServer server = GlobalBanCommon.INSTANCE.getCommonUtils().getServer();
        ServerConfig serverConfig = GlobalBanConfig.SERVER_CONFIG;

        if (!serverConfig.showKnownAccounts.getAsBoolean()) {
            return;
        }

        String ip = join.player().getIpAddress();
        Set<UUID> knownAccounts = GlobalBanCommon.IP_TO_UUID_CACHE.get(ip);

        if (knownAccounts == null || knownAccounts.isEmpty()) return;

        List<Component> playerMessages = new LinkedList<>();

        for (UUID player : knownAccounts) {
            var profileCache = server.getProfileCache();
            String name;
            if (profileCache != null) {
                name = profileCache.get(player).map(GameProfile::getName).orElse(player.toString());
            } else {
                name = player.toString();
            }
            MutableComponent messageText = Component.literal("[" + name + "]");
            List<PunishmentRecord> punishments = GlobalBanConfig.isPlayerUUIDPunished(player);

            if (server.getPlayerList().getPlayer(player) != null || player.equals(join.player().getUUID())) {
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
        server.sendSystemMessage(message);

        if (serverConfig.showPunishActionsInChat.getAsBoolean()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (PermissionRegistry.checkPermission(player, PermissionRegistry.SEE_KNOWN_ACCOUNTS_PERMISSION)) {
                    player.sendSystemMessage(message);
                }
            }
        }
    }
}
