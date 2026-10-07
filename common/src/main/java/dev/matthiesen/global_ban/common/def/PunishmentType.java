package dev.matthiesen.global_ban.common.def;

public enum PunishmentType {
    BAN("ban", true, false, true),
    IP_BAN("ip_ban", true, true, true),
    KICK("kick", true, false, true);

    public final String name;
    public final boolean shouldKick;
    public final boolean ipBased;
    public final boolean shouldRecord;

    PunishmentType(String name, boolean shouldKick, boolean ipBased, boolean shouldRecord) {
        this.name = name;
        this.shouldKick = shouldKick;
        this.ipBased = ipBased;
        this.shouldRecord = shouldRecord;
    }

    public static PunishmentType fromName(String name) {
        for (PunishmentType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
