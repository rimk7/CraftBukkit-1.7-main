package org.bukkit.event.entity;

import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class EnderpearlLandEvent extends EntityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Reason reason;

    private final Entity hit;

    private boolean cancel;

    public EnderpearlLandEvent(EnderPearl enderPearl, Reason reason) {
        this(enderPearl, reason, null);
    }

    public EnderpearlLandEvent(EnderPearl enderPearl, Reason reason, Entity hit) {
        super((Entity)enderPearl);
        this.reason = reason;
        this.hit = hit;
    }

    public EnderPearl getEntity() {
        return (EnderPearl)this.entity;
    }

    public Reason getReason() {
        return this.reason;
    }

    public Entity getHit() {
        return this.hit;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public boolean isCancelled() {
        return this.cancel;
    }

    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public enum Reason {
        BLOCK, ENTITY;
    }
}
