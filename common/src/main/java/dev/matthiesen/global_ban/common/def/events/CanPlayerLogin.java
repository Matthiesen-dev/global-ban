package dev.matthiesen.global_ban.common.def.events;

import com.mojang.authlib.GameProfile;

import java.net.SocketAddress;

public final class CanPlayerLogin {
    public record Pre(SocketAddress socketAddress, GameProfile gameProfile) {

    }

    public record Post(SocketAddress socketAddress, GameProfile gameProfile) {

    }
}
