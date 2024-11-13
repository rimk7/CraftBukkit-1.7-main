package net.minecraft.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.math3.util.FastMath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import org.spigotmc.AsyncCatcher;
import org.spigotmc.SpigotConfig;
import org.spigotmc.TrackingRange;

public class EntityTrackerEntry {
    private static final Logger p = LogManager.getLogger();

    public Entity tracker;

    public int b;

    public int c;

    public int xLoc;

    public int yLoc;

    public int zLoc;

    public int yRot;

    public int xRot;

    public int i;

    public double j;

    public double k;

    public double l;

    public int m;

    private double q;

    private double r;

    private double s;

    private boolean isMoving;

    private boolean u;

    private int v;

    private Entity w;

    private boolean x;

    public boolean n;

    public Set trackedPlayers = new LinkedHashSet();

    private List<EntityPlayer> toRemove = new ArrayList<>();

    private EntityTracker entityTracker;

    private int addRemoveRate;

    private int addRemoveCooldown;

    private boolean withinNoTrack = false;

    private final Consumer<EntityPlayer> addNearPlayersConsumer;

    public boolean equals(Object object) {
        return (object instanceof EntityTrackerEntry) ? ((((EntityTrackerEntry)object).tracker.getId() == this.tracker.getId())) : false;
    }

    public int hashCode() {
        return this.tracker.getId();
    }

    public void update() {
        this.withinNoTrack = withinNoTrack();
        if (--this.addRemoveCooldown <= 0) {
            removeFarPlayers();
            addNearPlayers();
            this.addRemoveCooldown = this.addRemoveRate;
        }
        track(null);
    }

    private void removeFarPlayers() {
        if (this.withinNoTrack) {
            this.toRemove.addAll(this.trackedPlayers);
            this.processToRemove();
        } else {
            Iterator var2 = this.trackedPlayers.iterator();

            while(true) {
                EntityPlayer entityplayer;
                double d0;
                double d1;
                int range;
                do {
                    if (!var2.hasNext()) {
                        this.processToRemove();
                        return;
                    }

                    entityplayer = (EntityPlayer)var2.next();
                    d0 = entityplayer.locX - this.tracker.locX;
                    d1 = entityplayer.locZ - this.tracker.locZ;
                    range = this.getRange();
                } while(d0 >= (double)(-range) && d0 <= (double)range && d1 >= (double)(-range) && d1 <= (double)range && !this.withinNoTrack());

                this.toRemove.add(entityplayer);
            }
        }
    }

    public void processToRemove() {
        for (EntityPlayer entityPlayer : this.toRemove) {
            entityPlayer.d(this.tracker);
            this.trackedPlayers.remove(entityPlayer);
        }
        this.toRemove.clear();
    }

    public void addNearPlayers() {
        addNearPlayers(false);
    }

    private void addNearPlayers(boolean updateCooldown) {
        if (this.withinNoTrack)
            return;
        if (updateCooldown)
            this.addRemoveCooldown = this.addRemoveRate;
        this.tracker.world.playerMap.forEachNearby(this.tracker.locX, this.tracker.locY, this.tracker.locZ, getRange(), false, this.addNearPlayersConsumer);
    }

    private boolean withinNoTrack() {
        return withinNoTrack(this.tracker);
    }

    private boolean withinNoTrack(Entity entity) {
        if (!(entity instanceof EntityPlayer))
            return false;
        if (this.entityTracker.getNoTrackDistance() == 0)
            return false;
        WorldServer worldServer = this.entityTracker.getWorldServer();
        return (entity.locX >= ((worldServer.getSpawn()).x - this.entityTracker.getNoTrackDistance()) &&
                entity.locX <= ((worldServer.getSpawn()).x + this.entityTracker.getNoTrackDistance()) &&
                entity.locZ >= ((worldServer.getSpawn()).z - this.entityTracker.getNoTrackDistance()) &&
                entity.locZ <= ((worldServer.getSpawn()).z + this.entityTracker.getNoTrackDistance()));
    }

    public EntityTrackerEntry(EntityTracker entityTracker, Entity entity, int i, int j, boolean flag) {
        this.addNearPlayersConsumer = new Consumer<EntityPlayer>() {
            public void accept(EntityPlayer entityPlayer) {
                if (!SpigotConfig.disableTracking || EntityTrackerEntry.this.tracker.passenger == entityPlayer)
                    EntityTrackerEntry.this.updatePlayer(entityPlayer);
            }
        };
        this.entityTracker = entityTracker;
        this.tracker = entity;
        this.b = i;
        this.c = j;
        this.u = flag;
        this.xLoc = (int) FastMath.floor(entity.locX * 32.0D);
        this.yLoc = (int) FastMath.floor(entity.locY * 32.0D);
        this.zLoc = (int) FastMath.floor(entity.locZ * 32.0D);
        this.yRot = MathHelper.d(entity.yaw * 256.0F / 360.0F);
        this.xRot = MathHelper.d(entity.pitch * 256.0F / 360.0F);
        this.i = MathHelper.d(entity.getHeadRotation() * 256.0F / 360.0F);
        if (SpigotConfig.disableTracking) {
            this.addRemoveRate = 100;
        } else if (this.tracker instanceof EntityArrow || this.tracker instanceof EntityProjectile) {
            this.addRemoveRate = 5;
        } else if (this.tracker instanceof EntityPlayer) {
            this.addRemoveRate = 5;
        } else {
            this.addRemoveRate = 10;
        }
        this.addRemoveCooldown = this.tracker.getId() % this.addRemoveRate;
    }

    public void track(List list) {
        this.n = false;
        if (!this.isMoving || this.tracker.e(this.q, this.r, this.s) > 16.0D) {
            this.q = this.tracker.locX;
            this.r = this.tracker.locY;
            this.s = this.tracker.locZ;
            this.isMoving = true;
            this.n = true;
        }
        if (this.w != this.tracker.vehicle || (this.tracker.vehicle != null && this.m % 60 == 0)) {
            this.w = this.tracker.vehicle;
            broadcast(new PacketPlayOutAttachEntity(0, this.tracker, this.tracker.vehicle));
        }
        if (this.tracker instanceof EntityItemFrame) {
            EntityItemFrame i3 = (EntityItemFrame)this.tracker;
            ItemStack i4 = i3.getItem();
            if (this.m % 10 == 0 && i4 != null && i4.getItem() instanceof ItemWorldMap) {
                WorldMap i6 = Items.MAP.getSavedMap(i4, this.tracker.world);
                Iterator<EntityHuman> i7 = this.trackedPlayers.iterator();
                while (i7.hasNext()) {
                    EntityHuman i8 = i7.next();
                    EntityPlayer i9 = (EntityPlayer)i8;
                    i6.a(i9, i4);
                    Packet j0 = Items.MAP.c(i4, this.tracker.world, i9);
                    if (j0 != null)
                        i9.playerConnection.sendPacket(j0);
                }
            }
            b();
        } else if (this.m % this.c == 0 || this.tracker.al || this.tracker.getDataWatcher().a()) {
            if (this.tracker.vehicle == null) {
                this.v++;
                int m = this.tracker.as.a(this.tracker.locX);
                int j = (int) FastMath.floor(this.tracker.locY * 32.0D);
                int k = this.tracker.as.a(this.tracker.locZ);
                int l = MathHelper.d(this.tracker.yaw * 256.0F / 360.0F);
                int i1 = MathHelper.d(this.tracker.pitch * 256.0F / 360.0F);
                int j1 = m - this.xLoc;
                int k1 = j - this.yLoc;
                int l1 = k - this.zLoc;
                Object object = null;
                boolean flag = !(Math.abs(j1) < 4 && Math.abs(k1) < 4 && Math.abs(l1) < 4 && this.m % 60 != 0);
                boolean flag1 = !(Math.abs(l - this.yRot) < 4 && Math.abs(i1 - this.xRot) < 4);
                if (flag) {
                    this.xLoc = m;
                    this.yLoc = j;
                    this.zLoc = k;
                }
                if (flag1) {
                    this.yRot = l;
                    this.xRot = i1;
                }
                if (this.m > 0 || this.tracker instanceof EntityArrow)
                    if (j1 >= -128 && j1 < 128 && k1 >= -128 && k1 < 128 && l1 >= -128 && l1 < 128 && this.v <= 50 && !this.x) {
                        if (flag && flag1) {
                            object = new PacketPlayOutRelEntityMoveLook(this.tracker.getId(), (byte)j1, (byte)k1, (byte)l1, (byte)l, (byte)i1, this.tracker.onGround);
                        } else if (flag) {
                            object = new PacketPlayOutRelEntityMove(this.tracker.getId(), (byte)j1, (byte)k1, (byte)l1, this.tracker.onGround);
                        } else if (flag1) {
                            object = new PacketPlayOutEntityLook(this.tracker.getId(), (byte)l, (byte)i1, this.tracker.onGround);
                        }
                    } else {
                        this.v = 0;
                        object = new PacketPlayOutEntityTeleport(this.tracker.getId(), m, j, k, (byte)l, (byte)i1, this.tracker.onGround, !(!(this.tracker instanceof EntityFallingBlock) && !(this.tracker instanceof EntityTNTPrimed)));
                    }
                if (this.u) {
                    double d0 = this.tracker.motX - this.j;
                    double d1 = this.tracker.motY - this.k;
                    double d2 = this.tracker.motZ - this.l;
                    double d3 = 0.02D;
                    double d4 = d0 * d0 + d1 * d1 + d2 * d2;
                    if (d4 > d3 * d3 || (d4 > 0.0D && this.tracker.motX == 0.0D && this.tracker.motY == 0.0D && this.tracker.motZ == 0.0D)) {
                        this.j = this.tracker.motX;
                        this.k = this.tracker.motY;
                        this.l = this.tracker.motZ;
                        broadcast(new PacketPlayOutEntityVelocity(this.tracker.getId(), this.j, this.k, this.l));
                    }
                }
                if (object != null)
                    broadcast((Packet)object);
                b();
                this.x = false;
            } else {
                int k = MathHelper.d(this.tracker.yaw * 256.0F / 360.0F);
                int j = MathHelper.d(this.tracker.pitch * 256.0F / 360.0F);
                boolean flag2 = !(Math.abs(k - this.yRot) < 4 && Math.abs(j - this.xRot) < 4);
                if (flag2) {
                    broadcast(new PacketPlayOutEntityLook(this.tracker.getId(), (byte)k, (byte)j, this.tracker.onGround));
                    this.yRot = k;
                    this.xRot = j;
                }
                this.xLoc = this.tracker.as.a(this.tracker.locX);
                this.yLoc = (int) FastMath.floor(this.tracker.locY * 32.0D);
                this.zLoc = this.tracker.as.a(this.tracker.locZ);
                b();
                this.x = true;
            }
            int i = MathHelper.d(this.tracker.getHeadRotation() * 256.0F / 360.0F);
            if (Math.abs(i - this.i) >= 4) {
                broadcast(new PacketPlayOutEntityHeadRotation(this.tracker, (byte)i));
                this.i = i;
            }
            this.tracker.al = false;
        }
        this.m++;
        if (this.tracker.velocityChanged) {
            boolean cancelled = false;
            if (this.tracker instanceof EntityPlayer) {
                Player player = (Player)this.tracker.getBukkitEntity();
                Vector velocity = player.getVelocity();
                PlayerVelocityEvent event = new PlayerVelocityEvent(player, velocity);
                this.tracker.world.getServer().getPluginManager().callEvent((Event)event);
                if (event.isCancelled()) {
                    cancelled = true;
                } else if (!velocity.equals(event.getVelocity())) {
                    player.setVelocity(velocity);
                }
            }
            if (!cancelled)
                if (this.tracker instanceof EntityPlayer) {
                    ((EntityPlayer)this.tracker).playerConnection.sendPacket(new PacketPlayOutEntityVelocity(this.tracker));
                } else if (this.tracker instanceof EntityArrow || this.tracker instanceof EntityProjectile) {
                    broadcast(new PacketPlayOutEntityVelocity(this.tracker));
                }
            this.tracker.velocityChanged = false;
        }
    }

    private void b() {
        DataWatcher datawatcher = this.tracker.getDataWatcher();
        if (datawatcher.a()) {
            List<?> changedMetadata = datawatcher.b();
            if (doHealthObfuscation()) {
                PacketPlayOutEntityMetadata metadataPacket = (new PacketPlayOutEntityMetadata(this.tracker.getId(), new ArrayList(changedMetadata), false)).obfuscateHealth();
                if (!metadataPacket.didFindHealth() || 1 < metadataPacket.getMetadata().size())
                    broadcast(metadataPacket);
            } else {
                broadcast(new PacketPlayOutEntityMetadata(this.tracker.getId(), changedMetadata, false));
            }
            if (this.tracker instanceof EntityPlayer)
                ((EntityPlayer)this.tracker).playerConnection.sendPacket(new PacketPlayOutEntityMetadata(this.tracker.getId(), changedMetadata, false));
        }
        if (this.tracker instanceof EntityLiving) {
            AttributeMapServer attributemapserver = (AttributeMapServer)((EntityLiving)this.tracker).getAttributeMap();
            Set set = attributemapserver.getAttributes();
            if (!set.isEmpty()) {
                if (this.tracker instanceof EntityPlayer) {
                    ((EntityPlayer)this.tracker).getBukkitEntity().injectScaledMaxHealth(set, false);
                    ((EntityPlayer)this.tracker).playerConnection.sendPacket(new PacketPlayOutUpdateAttributes(this.tracker.getId(), set));
                }
                if (this.tracker.passenger instanceof EntityPlayer)
                    ((EntityPlayer)this.tracker.passenger).playerConnection.sendPacket(new PacketPlayOutUpdateAttributes(this.tracker.getId(), set));
            }
            set.clear();
        }
    }

    public void broadcast(Packet packet) {
        Iterator<EntityPlayer> iterator = this.trackedPlayers.iterator();
        while (iterator.hasNext()) {
            EntityPlayer entityplayer = iterator.next();
            entityplayer.playerConnection.sendPacket(packet);
        }
    }

    public void broadcastIncludingSelf(Packet packet) {
        broadcast(packet);
        if (this.tracker instanceof EntityPlayer)
            ((EntityPlayer)this.tracker).playerConnection.sendPacket(packet);
    }

    public void a() {
        Iterator<EntityPlayer> iterator = this.trackedPlayers.iterator();
        while (iterator.hasNext()) {
            EntityPlayer entityplayer = iterator.next();
            entityplayer.d(this.tracker);
        }
    }

    public void a(EntityPlayer entityplayer) {
        if (this.trackedPlayers.contains(entityplayer)) {
            entityplayer.d(this.tracker);
            this.trackedPlayers.remove(entityplayer);
        }
    }

    public void updatePlayer(EntityPlayer entityplayer) {
        if (entityplayer != this.tracker) {
            double d0 = entityplayer.locX - this.tracker.locX;
            double d1 = entityplayer.locZ - this.tracker.locZ;
            int range = getRange();
            if (d0 >= -range && d0 <= range && d1 >= -range && d1 <= range) {
                if (!this.trackedPlayers.contains(entityplayer) && (d(entityplayer) || this.tracker.attachedToPlayer)) {
                    if (this.tracker instanceof EntityPlayer && withinNoTrack())
                        return;
                    if (this.tracker instanceof EntityPlayer) {
                        CraftPlayer craftPlayer = ((EntityPlayer)this.tracker).getBukkitEntity();
                        if (!entityplayer.getBukkitEntity().canSee((Player)craftPlayer))
                            return;
                    }
                    this.trackedPlayers.add(entityplayer);
                    Packet packet = c();
                    boolean isTarget18 = (entityplayer.playerConnection.networkManager.getVersion() > 28);
                    boolean trackerInstanceOf = this.tracker instanceof EntityPlayer;
                    if (isTarget18) {
                        if (trackerInstanceOf) {
                            entityplayer.playerConnection.sendPacket(PacketPlayOutPlayerInfo.addPlayer((EntityPlayer)this.tracker));
                            entityplayer.playerConnection.sendPacket(PacketPlayOutPlayerInfo.updateDisplayName((EntityPlayer)this.tracker));
                        }
                        entityplayer.playerConnection.sendPacket(packet);
                        if (trackerInstanceOf && SpigotConfig.onlyCustomTab)
                            entityplayer.playerConnection.sendPacket(PacketPlayOutPlayerInfo.removePlayer((EntityPlayer)this.tracker));
                    } else {
                        if (this.tracker instanceof EntityPlayer && !SpigotConfig.onlyCustomTab)
                            entityplayer.playerConnection.sendPacket(PacketPlayOutPlayerInfo.addPlayer((EntityPlayer)this.tracker));
                        entityplayer.playerConnection.sendPacket(packet);
                    }
                    if (!this.tracker.getDataWatcher().d()) {
                        PacketPlayOutEntityMetadata metadataPacket = new PacketPlayOutEntityMetadata(this.tracker.getId(), this.tracker.getDataWatcher(), true);
                        if (doHealthObfuscation())
                            metadataPacket.obfuscateHealth();
                        entityplayer.playerConnection.sendPacket(metadataPacket);
                    }
                    this.j = this.tracker.motX;
                    this.k = this.tracker.motY;
                    this.l = this.tracker.motZ;
                    if (this.u && !(packet instanceof PacketPlayOutSpawnEntityLiving))
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityVelocity(this.tracker.getId(), this.tracker.motX, this.tracker.motY, this.tracker.motZ));
                    if (this.tracker.vehicle != null)
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutAttachEntity(0, this.tracker, this.tracker.vehicle));
                    if (this.tracker.passenger != null)
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutAttachEntity(0, this.tracker.passenger, this.tracker));
                    if (this.tracker instanceof EntityInsentient && ((EntityInsentient)this.tracker).getLeashHolder() != null)
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutAttachEntity(1, this.tracker, ((EntityInsentient)this.tracker).getLeashHolder()));
                    if (this.tracker instanceof EntityLiving)
                        for (int i = 0; i < 5; i++) {
                            ItemStack itemstack = ((EntityLiving)this.tracker).getEquipment(i);
                            if (itemstack != null)
                                entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEquipment(this.tracker.getId(), i, itemstack));
                        }
                    if (this.tracker instanceof EntityHuman) {
                        EntityHuman entityhuman = (EntityHuman)this.tracker;
                        if (entityhuman.isSleeping())
                            entityplayer.playerConnection.sendPacket(new PacketPlayOutBed(entityhuman, (int) FastMath.floor(this.tracker.locX), (int) FastMath.floor(this.tracker.locY), (int) FastMath.floor(this.tracker.locZ)));
                    }
                    if (this.tracker instanceof EntityLiving)
                        entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityHeadRotation(this.tracker, (byte)MathHelper.d(this.tracker.getHeadRotation() * 256.0F / 360.0F)));
                    if (this.tracker instanceof EntityLiving) {
                        EntityLiving entityliving = (EntityLiving)this.tracker;
                        Iterator<MobEffect> iterator = entityliving.getEffects().iterator();
                        while (iterator.hasNext()) {
                            MobEffect mobeffect = iterator.next();
                            entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityEffect(this.tracker.getId(), mobeffect));
                        }
                    }
                }
            } else if (this.trackedPlayers.contains(entityplayer)) {
                this.trackedPlayers.remove(entityplayer);
                entityplayer.d(this.tracker);
            }
        }
    }

    private boolean d(EntityPlayer entityplayer) {
        return entityplayer.r().getPlayerChunkMap().a(entityplayer, this.tracker.ah, this.tracker.aj);
    }

    private Packet c() {
        if (this.tracker.dead)
            return null;
        if (this.tracker instanceof EntityItem)
            return new PacketPlayOutSpawnEntity(this.tracker, 2, 1);
        if (this.tracker instanceof EntityPlayer)
            return new PacketPlayOutNamedEntitySpawn((EntityHuman)this.tracker);
        if (this.tracker instanceof EntityMinecartAbstract) {
            EntityMinecartAbstract entityminecartabstract = (EntityMinecartAbstract)this.tracker;
            return new PacketPlayOutSpawnEntity(this.tracker, 10, entityminecartabstract.m());
        }
        if (this.tracker instanceof EntityBoat)
            return new PacketPlayOutSpawnEntity(this.tracker, 1);
        if (!(this.tracker instanceof IAnimal) && !(this.tracker instanceof EntityEnderDragon)) {
            if (this.tracker instanceof EntityFishingHook) {
                EntityHuman entityhuman = ((EntityFishingHook)this.tracker).owner;
                return new PacketPlayOutSpawnEntity(this.tracker, 90, (entityhuman != null) ? entityhuman.getId() : this.tracker.getId());
            }
            if (this.tracker instanceof EntityArrow) {
                Entity entity = ((EntityArrow)this.tracker).shooter;
                return new PacketPlayOutSpawnEntity(this.tracker, 60, (entity != null) ? entity.getId() : this.tracker.getId());
            }
            if (this.tracker instanceof EntitySnowball)
                return new PacketPlayOutSpawnEntity(this.tracker, 61);
            if (this.tracker instanceof EntityPotion)
                return new PacketPlayOutSpawnEntity(this.tracker, 73, ((EntityPotion)this.tracker).getPotionValue());
            if (this.tracker instanceof EntityThrownExpBottle)
                return new PacketPlayOutSpawnEntity(this.tracker, 75);
            if (this.tracker instanceof EntityEnderPearl)
                return new PacketPlayOutSpawnEntity(this.tracker, 65);
            if (this.tracker instanceof EntityEnderSignal)
                return new PacketPlayOutSpawnEntity(this.tracker, 72);
            if (this.tracker instanceof EntityFireworks)
                return new PacketPlayOutSpawnEntity(this.tracker, 76);
            if (this.tracker instanceof EntityFireball) {
                EntityFireball entityfireball = (EntityFireball)this.tracker;
                PacketPlayOutSpawnEntity packetplayoutspawnentity = null;
                byte b0 = 63;
                if (this.tracker instanceof EntitySmallFireball) {
                    b0 = 64;
                } else if (this.tracker instanceof EntityWitherSkull) {
                    b0 = 66;
                }
                if (entityfireball.shooter != null) {
                    packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, b0, ((EntityFireball)this.tracker).shooter.getId());
                } else {
                    packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, b0, 0);
                }
                packetplayoutspawnentity.d((int)(entityfireball.dirX * 8000.0D));
                packetplayoutspawnentity.e((int)(entityfireball.dirY * 8000.0D));
                packetplayoutspawnentity.f((int)(entityfireball.dirZ * 8000.0D));
                return packetplayoutspawnentity;
            }
            if (this.tracker instanceof EntityEgg)
                return new PacketPlayOutSpawnEntity(this.tracker, 62);
            if (this.tracker instanceof EntityTNTPrimed)
                return new PacketPlayOutSpawnEntity(this.tracker, 50);
            if (this.tracker instanceof EntityEnderCrystal)
                return new PacketPlayOutSpawnEntity(this.tracker, 51);
            if (this.tracker instanceof EntityFallingBlock) {
                EntityFallingBlock entityfallingblock = (EntityFallingBlock)this.tracker;
                return new PacketPlayOutSpawnEntity(this.tracker, 70, Block.getId(entityfallingblock.f()) | entityfallingblock.data << 16);
            }
            if (this.tracker instanceof EntityPainting)
                return new PacketPlayOutSpawnEntityPainting((EntityPainting)this.tracker);
            if (this.tracker instanceof EntityItemFrame) {
                EntityItemFrame entityitemframe = (EntityItemFrame)this.tracker;
                PacketPlayOutSpawnEntity packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, 71, entityitemframe.direction);
                packetplayoutspawnentity.a(MathHelper.d((entityitemframe.x * 32)));
                packetplayoutspawnentity.b(MathHelper.d((entityitemframe.y * 32)));
                packetplayoutspawnentity.c(MathHelper.d((entityitemframe.z * 32)));
                return packetplayoutspawnentity;
            }
            if (this.tracker instanceof EntityLeash) {
                EntityLeash entityleash = (EntityLeash)this.tracker;
                PacketPlayOutSpawnEntity packetplayoutspawnentity = new PacketPlayOutSpawnEntity(this.tracker, 77);
                packetplayoutspawnentity.a(MathHelper.d((entityleash.x * 32)));
                packetplayoutspawnentity.b(MathHelper.d((entityleash.y * 32)));
                packetplayoutspawnentity.c(MathHelper.d((entityleash.z * 32)));
                return packetplayoutspawnentity;
            }
            if (this.tracker instanceof EntityExperienceOrb)
                return new PacketPlayOutSpawnEntityExperienceOrb((EntityExperienceOrb)this.tracker);
            throw new IllegalArgumentException("Don't know how to add " + this.tracker.getClass() + "!");
        }
        this.i = MathHelper.d(this.tracker.getHeadRotation() * 256.0F / 360.0F);
        return new PacketPlayOutSpawnEntityLiving((EntityLiving)this.tracker);
    }

    public void clear(EntityPlayer entityplayer) {
        AsyncCatcher.catchOp("player tracker clear");
        if (this.trackedPlayers.remove(entityplayer))
            entityplayer.d(this.tracker);
    }

    public boolean doHealthObfuscation() {
        return (this.tracker.isAlive() && this.tracker instanceof EntityPlayer);
    }

    public int getRange() {
        if (this.tracker.passenger == null)
            return this.b;
        return Math.max(this.b, TrackingRange.getEntityTrackingRange(this.tracker.passenger, 0));
    }
}
