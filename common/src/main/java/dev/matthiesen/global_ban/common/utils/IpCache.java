package dev.matthiesen.global_ban.common.utils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.matthiesen.global_ban.common.GlobalBanCommon;

import java.io.*;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class IpCache {
    private static final ConcurrentHashMap<UUID, String> UUID_TO_IP_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<UUID>> IP_TO_UUID_CACHE = new ConcurrentHashMap<>();
    private static final File IP_CACHE_FILE = Paths.get("ipcache.json").toFile();

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
            .create();

    public static ConcurrentHashMap<String, String> getCacheFromFile() {
        try {
            return GSON.fromJson(new FileReader(IP_CACHE_FILE), new TypeToken<ConcurrentHashMap<String, String>>() {}.getType());
        } catch (FileNotFoundException e) {
            GlobalBanCommon.INSTANCE.createWarnLog("Failed to load ipcache.json, creating a new one.");
            return null;
        }
    }

    public static void handleServerStarting() {
        var cache = getCacheFromFile();
        if (cache != null) {
            for (var entry : cache.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getValue());
                    String ip = entry.getKey();

                    UUID_TO_IP_CACHE.put(uuid, ip);
                    IP_TO_UUID_CACHE.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                } catch (Exception e) {
                    // Ignore invalid UUIDs in the cache
                }
            }
        }
    }

    public static void writeCacheToFile() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(IP_CACHE_FILE));
            ConcurrentHashMap<String, String> cacheToWrite = new ConcurrentHashMap<>();
            for (var entry : UUID_TO_IP_CACHE.entrySet()) {
                cacheToWrite.put(entry.getValue(), entry.getKey().toString());
            }
            writer.write(GSON.toJson(cacheToWrite));
            writer.close();
        } catch (IOException e) {
            GlobalBanCommon.INSTANCE.createErrorLog("Failed to write ipcache.json: " + e.getMessage(), e);
        }
    }

    public static Set<UUID> getUuidByIp(String ip) {
        return IP_TO_UUID_CACHE.get(ip);
    }

    public static void addToCache(UUID uuid, String ip) {
        UUID_TO_IP_CACHE.put(uuid, ip);
        IP_TO_UUID_CACHE.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
    }
}
