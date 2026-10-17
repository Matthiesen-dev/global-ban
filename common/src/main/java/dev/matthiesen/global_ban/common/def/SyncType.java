package dev.matthiesen.global_ban.common.def;

public enum SyncType {
    ADD("add"),
    REMOVE("remove");

    public final String name;

    SyncType(String name) {
        this.name = name;
    }
}
