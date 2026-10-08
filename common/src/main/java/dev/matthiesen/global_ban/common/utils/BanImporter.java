package dev.matthiesen.global_ban.common.utils;

import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.GlobalBanCommon;
import dev.matthiesen.global_ban.common.config.GlobalBanConfig;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.global_ban.common.def.PunishmentType;
import dev.matthiesen.global_ban.common.mixins.accessors.StoredUserEntryAccessor;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unchecked")
public final class BanImporter {
    private static MinecraftServer getServer() {
        return GlobalBanCommon.INSTANCE.getCommonUtils().getServer();
    }

    public static boolean importVanillaBans(boolean cleanup) {
        try {
            handlePlayerBans(cleanup);
            handleIpBans(cleanup);
            return true;
        } catch (Exception e) {
            GlobalBanCommon.INSTANCE.createErrorLog("Failed to import vanilla bans", e);
            return false;
        }
    }

    public static void handlePlayerBans(boolean cleanup) {
        UserBanList banList = getServer().getPlayerList().getBans();
        for (UserBanListEntry entry : banList.getEntries()) {
            try {
                GameProfile profile = ((StoredUserEntryAccessor<GameProfile>) entry).getKeyServer();
                PunishmentRecord punishment = getPunishment(entry, profile);
                GlobalBanConfig.addPunishment(punishment);
                if (cleanup) {
                    banList.remove(profile);
                }
            } catch (Exception e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to handle player ban entry", e);
            }
        }
    }

    public static void handleIpBans(boolean cleanup) {
        IpBanList ipBanList = getServer().getPlayerList().getIpBans();
        for (IpBanListEntry entry : ipBanList.getEntries()) {
            try {
                String ip = ((StoredUserEntryAccessor<String>) entry).getKeyServer();
                PunishmentRecord punishment = getPunishment(entry, ip);
                GlobalBanConfig.addPunishment(punishment);
                if (cleanup) {
                    ipBanList.remove(ip);
                }
            } catch (Exception e) {
                GlobalBanCommon.INSTANCE.createErrorLog("Failed to handle IP ban entry", e);
            }
        }
    }

    private static @NotNull PunishmentRecord getPunishment(UserBanListEntry entry, GameProfile profile) {
        long creation = entry.getCreated().getTime() / 1000;
        long expiration;
        try {
            assert entry.getExpires() != null;
            expiration = entry.getExpires().getTime() / 1000;
        } catch (Exception e) {
            expiration = PunishmentRecord.NON_EXPIRING_PUNISHMENT;
        }
        return new PunishmentRecord(
                PunishmentType.BAN,
                creation,
                expiration,
                profile.getId(),
                PunishmentRecord.UNDEFINED_IP_ADDRESS,
                profile.getName(),
                Util.NIL_UUID,
                entry.getSource(),
                entry.getReason(),
                GlobalBanConfig.getServerUUID()
        );
    }

    private static @NotNull PunishmentRecord getPunishment(IpBanListEntry entry, String ip) {
        long creation = entry.getCreated().getTime() / 1000;
        long expiration;
        try {
            assert entry.getExpires() != null;
            expiration = entry.getExpires().getTime() / 1000;
        } catch (Exception e) {
            expiration = PunishmentRecord.NON_EXPIRING_PUNISHMENT;
        }
        return new PunishmentRecord(
                PunishmentType.IP_BAN,
                creation,
                expiration,
                Util.NIL_UUID,
                ip,
                PunishmentRecord.UNKNOWN_PLAYER_NAME,
                Util.NIL_UUID,
                entry.getSource(),
                entry.getReason(),
                GlobalBanConfig.getServerUUID()
        );
    }
}
