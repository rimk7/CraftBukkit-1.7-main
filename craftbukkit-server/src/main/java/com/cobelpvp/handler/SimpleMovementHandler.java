package com.cobelpvp.handler;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface SimpleMovementHandler {

    void onPlayerMove(Player player, Location to, Location from);
}
