package dev.matthiesen.global_ban.common.def;

public enum SyncType {
    ADD,
    REMOVE;

    public static SyncType fromName(String name) {
        for (SyncType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
