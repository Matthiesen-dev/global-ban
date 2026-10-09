package dev.matthiesen.global_ban.common.def;

public enum SyncType {
    ADD("add"),
    REMOVE("remove");

    public final String name;

    SyncType(String name) {
        this.name = name;
    }

    public static SyncType fromName(String name) {
        for (SyncType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
