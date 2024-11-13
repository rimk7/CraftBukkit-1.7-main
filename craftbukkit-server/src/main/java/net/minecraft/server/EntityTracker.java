package net.minecraft.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.optimizations.util.IndexedLinkedHashSet;
import org.spigotmc.AsyncCatcher;
import org.spigotmc.TrackingRange;

public class EntityTracker {
    private IndexedLinkedHashSet<EntityTrackerEntry> c = new IndexedLinkedHashSet();

    public IntHashMap trackedEntities = new IntHashMap();

    private int noTrackDistance = 0;

    private WorldServer worldServer;

    private int e;

    public int getNoTrackDistance() {
        return this.noTrackDistance;
    }

    public void setNoTrackDistance(int noTrackDistance) {
        this.noTrackDistance = noTrackDistance;
    }

    public EntityTracker(WorldServer worldserver) {
        this.worldServer = worldserver;
        this.e = 128;
    }

    public WorldServer getWorldServer() {
        return this.worldServer;
    }

    public void track(Entity entity) {
        if (entity instanceof EntityPlayer) {
            addEntity(entity, 512, 2);
        } else if (entity instanceof EntityFishingHook) {
            addEntity(entity, 64, 5, true);
        } else if (entity instanceof EntityArrow) {
            addEntity(entity, 64, 20, false);
        } else if (entity instanceof EntitySmallFireball) {
            addEntity(entity, 64, 10, false);
        } else if (entity instanceof EntityFireball) {
            addEntity(entity, 64, 10, false);
        } else if (entity instanceof EntitySnowball) {
            addEntity(entity, 64, 10, true);
        } else if (entity instanceof EntityEnderPearl) {
            addEntity(entity, 64, 2, true);
        } else if (entity instanceof EntityEnderSignal) {
            addEntity(entity, 64, 4, true);
        } else if (entity instanceof EntityEgg) {
            addEntity(entity, 64, 10, true);
        } else if (entity instanceof EntityPotion) {
            addEntity(entity, 64, 10, true);
        } else if (entity instanceof EntityThrownExpBottle) {
            addEntity(entity, 64, 10, true);
        } else if (entity instanceof EntityFireworks) {
            addEntity(entity, 64, 10, true);
        } else if (entity instanceof EntityItem) {
            addEntity(entity, 64, 20, true);
        } else if (entity instanceof EntityMinecartAbstract) {
            addEntity(entity, 80, 3, true);
        } else if (entity instanceof EntityBoat) {
            addEntity(entity, 80, 3, true);
        } else if (entity instanceof EntitySquid) {
            addEntity(entity, 64, 3, true);
        } else if (entity instanceof EntityWither) {
            addEntity(entity, 80, 3, false);
        } else if (entity instanceof EntityBat) {
            addEntity(entity, 80, 3, false);
        } else if (entity instanceof IAnimal) {
            addEntity(entity, 80, 3, true);
        } else if (entity instanceof EntityEnderDragon) {
            addEntity(entity, 160, 3, true);
        } else if (entity instanceof EntityTNTPrimed) {
            addEntity(entity, 160, 10, true);
        } else if (entity instanceof EntityFallingBlock) {
            addEntity(entity, 160, 20, true);
        } else if (entity instanceof EntityHanging) {
            addEntity(entity, 160, 2147483647, false);
        } else if (entity instanceof EntityExperienceOrb) {
            addEntity(entity, 160, 20, true);
        } else if (entity instanceof EntityEnderCrystal) {
            addEntity(entity, 256, 2147483647, false);
        }
    }

    public void addEntity(Entity entity, int i, int j) {
        addEntity(entity, i, j, false);
    }

    public void addEntity(Entity entity, int i, int j, boolean flag) {
        AsyncCatcher.catchOp("entity track");
        i = TrackingRange.getEntityTrackingRange(entity, i);
        if (i > this.e)
            i = this.e;
        try {
            if (this.trackedEntities.b(entity.getId()))
                throw new IllegalStateException("Entity is already tracked!");
            EntityTrackerEntry entitytrackerentry = new EntityTrackerEntry(this, entity, i, j, flag);
            this.c.add(entitytrackerentry);
            this.trackedEntities.a(entity.getId(), entitytrackerentry);
            entitytrackerentry.addNearPlayers();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void untrackEntity(Entity entity) {
        AsyncCatcher.catchOp("entity untrack");
        if (entity instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer)entity;
            Iterator<EntityTrackerEntry> iterator = this.c.iterator();
            while (iterator.hasNext()) {
                EntityTrackerEntry entitytrackerentry = iterator.next();
                entitytrackerentry.a(entityplayer);
            }
        }
        EntityTrackerEntry entitytrackerentry1 = (EntityTrackerEntry)this.trackedEntities.d(entity.getId());
        if (entitytrackerentry1 != null) {
            this.c.remove(entitytrackerentry1);
            entitytrackerentry1.a();
        }
    }

    private static int trackerThreads = 4;

    private static ExecutorService pool = Executors.newFixedThreadPool(trackerThreads - 1, (new ThreadFactoryBuilder()).setNameFormat("entity-tracker-%d").build());

    public void updatePlayers() {
        int offset = 0;
        final CountDownLatch latch = new CountDownLatch(trackerThreads);
        for (int i = 1; i <= trackerThreads; i++) {
            final int localOffset = offset++;
            Runnable runnable = new Runnable() {
                public void run() {
                    for (int i = localOffset; i < EntityTracker.this.c.size(); i += EntityTracker.trackerThreads)
                        ((EntityTrackerEntry)EntityTracker.this.c.get(i)).update();
                    latch.countDown();
                }
            };
            if (i < trackerThreads) {
                pool.execute(runnable);
            } else {
                runnable.run();
            }
        }
        try {
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void a(Entity entity, Packet packet) {
        EntityTrackerEntry entitytrackerentry = (EntityTrackerEntry)this.trackedEntities.get(entity.getId());
        if (entitytrackerentry != null)
            entitytrackerentry.broadcast(packet);
    }

    public void sendPacketToEntity(Entity entity, Packet packet) {
        EntityTrackerEntry entitytrackerentry = (EntityTrackerEntry)this.trackedEntities.get(entity.getId());
        if (entitytrackerentry != null)
            entitytrackerentry.broadcastIncludingSelf(packet);
    }

    public void untrackPlayer(EntityPlayer entityplayer) {
        Iterator<EntityTrackerEntry> iterator = this.c.iterator();
        while (iterator.hasNext()) {
            EntityTrackerEntry entitytrackerentry = iterator.next();
            entitytrackerentry.clear(entityplayer);
        }
    }
}
