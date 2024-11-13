package com.cobelpvp;

import com.cobelpvp.handler.MovementHandler;
import com.cobelpvp.handler.PacketHandler;
import org.apache.commons.math3.util.FastMath;
import net.minecraft.server.World;
import org.bukkit.Bukkit;

import java.util.HashSet;
import java.util.Set;

public enum CobelSpigot {
    INSTANCE("INSTANCE", 0);

    private static CobelPvPThread thread;

    private CobelSpigot(final String s, final int n) {
        this.packetHandlers = new HashSet<PacketHandler>();
        this.movementHandlers = new HashSet<MovementHandler>();
    }

    private Set<PacketHandler> packetHandlers;
    private Set<MovementHandler> movementHandlers;

    public static CobelSpigot getInstance() {
        return INSTANCE;
    }

    public static void init() {
        (thread = new CobelPvPThread(Runtime.getRuntime().availableProcessors() * 2)).loadAsyncThreads();
    }

    public Set<PacketHandler> getPacketHandlers() {
        return this.packetHandlers;
    }

    public Set<MovementHandler> getMovementHandlers() {
        return this.movementHandlers;
    }

    public void addPacketHandler(final PacketHandler handler) {
        Bukkit.getLogger().info("Adding packet handler: " + handler.getClass().getPackage().getName() + "." + handler.getClass().getName());
        this.packetHandlers.add(handler);
    }

    public void addMovementHandler(final MovementHandler handler) {
        Bukkit.getLogger().info("Adding movement handler: " + handler.getClass().getPackage().getName() + "." + handler.getClass().getName());
        this.movementHandlers.add(handler);
    }

    public static boolean isPositionOfTileEntityInUse(World world, double d0, double d1, double d2) {
        int i = (int) FastMath.floor(d0);
        int j = (int) FastMath.floor(d1);
        int k = (int) FastMath.floor(d2);
        return (world.isLoaded(i, j, k) && !unloadQueueContains(world, i, j, k));
    }

    public static boolean unloadQueueContains(World world, int x, int y, int z) {
        return world != null &&
                world.chunkProviderServer != null &&
                world.chunkProviderServer.unloadQueue != null &&
                world.chunkProviderServer.unloadQueue.contains(x >> 4, z >> 4);
    }

    public CobelPvPThread getThread() {
        return thread;
    }
}
