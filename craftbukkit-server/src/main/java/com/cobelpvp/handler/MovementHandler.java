package com.cobelpvp.handler;

import net.minecraft.server.PacketPlayInFlying;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface MovementHandler {
    void handleUpdateLocation(Player var1, Location var2, Location var3, PacketPlayInFlying var4);

    void handleUpdateRotation(Player var1, Location var2, Location var3, PacketPlayInFlying var4);
}
