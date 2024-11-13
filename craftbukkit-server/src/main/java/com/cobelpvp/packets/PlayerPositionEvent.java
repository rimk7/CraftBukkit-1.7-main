package com.cobelpvp.packets;

import net.minecraft.server.Packet;
import net.minecraft.server.PacketPlayInFlying;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerPositionEvent extends PacketReceiveEvent {
    public PlayerPositionEvent(Player player, Location from, Location to, boolean fromGround, boolean toGround, PacketPlayInFlying packet, Phase phase) {
        super(player, (Packet)packet);
        this.phase = phase;
        this.from = from;
        this.to = to;
        this.fromGround = fromGround;
        this.toGround = toGround;
    }

    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public Phase getPhase() {
        return this.phase;
    }

    public Location getFrom() {
        return this.from;
    }

    public Location getTo() {
        return this.to;
    }

    public boolean isFromGround() {
        return this.fromGround;
    }

    public boolean isToGround() {
        return this.toGround;
    }

    private static HandlerList handlerList = new HandlerList();

    private final Phase phase;

    private Location from;

    private Location to;

    private boolean fromGround;

    private boolean toGround;

    public enum Phase {
        PRE, POST;
    }
}
