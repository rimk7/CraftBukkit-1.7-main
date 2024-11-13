package com.cobelpvp.packets;

import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PotionPreSplashEvent extends Event implements Cancellable {
    private final HandlerList handlers = new HandlerList();
    ThrownPotion potion;
    private boolean cancelled;

    public PotionPreSplashEvent(ThrownPotion potion) {
        this.potion = potion;
    }

    public HandlerList getHandlers() {
        return this.handlers;
    }

    public ThrownPotion getPotion() {
        return this.potion;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
