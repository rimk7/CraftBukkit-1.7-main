package com.cobelpvp.handler;

import net.minecraft.server.Packet;
import net.minecraft.server.PlayerConnection;

public interface PacketHandler {
    void handleReceivedPacket(PlayerConnection var1, Packet var2);

    void handleSentPacket(PlayerConnection var1, Packet var2);

    default boolean handleSentPacketCancellable(PlayerConnection connection, Packet packet) {
        return true;
    }
}
