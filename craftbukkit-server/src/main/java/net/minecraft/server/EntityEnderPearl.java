package net.minecraft.server;

import java.util.*;
import com.google.common.collect.Sets;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.player.PlayerPearlRefundEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.material.Gate;
import org.bukkit.material.Openable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.spigotmc.SpigotConfig;

public class EntityEnderPearl extends EntityProjectile
{
    private Location lastValidTeleport;
    private Item toRefundPearl;
    private EntityLiving c;
    private static Set<Block> PROHIBITED_PEARL_BLOCKS;
    public static List<String> pearlAbleType;
    public static List<Material> forwardTypes;

    static {
        EntityEnderPearl.PROHIBITED_PEARL_BLOCKS = Sets.newHashSet(Block.getById(85), Block.getById(107));
        EntityEnderPearl.pearlAbleType = Arrays.asList("STEP", "STAIR");
        EntityEnderPearl.forwardTypes = Collections.singletonList(Material.ENDER_PORTAL_FRAME);
    }

    public EntityEnderPearl(final World world) {
        super(world);
        this.toRefundPearl = null;
        this.loadChunks = world.paperSpigotConfig.loadUnloadedEnderPearls;
    }

    public EntityEnderPearl(final World world, final EntityLiving entityliving) {
        super(world, entityliving);
        this.toRefundPearl = null;
        this.c = entityliving;
        this.loadChunks = world.paperSpigotConfig.loadUnloadedEnderPearls;
    }

    @Override
    protected void a(final MovingObjectPosition movingobjectposition) {
        if (SpigotConfig.pearlThroughGatesAndTripwire) {
            final Block block = this.world.getType(movingobjectposition.b, movingobjectposition.c, movingobjectposition.d);
            if (block == Blocks.TRIPWIRE) {
                return;
            }
            if (block == Blocks.FENCE_GATE) {
                BlockIterator bi = null;
                try {
                    final Vector l = new Vector(this.locX, this.locY, this.locZ);
                    final Vector l2 = new Vector(this.locX + this.motX, this.locY + this.motY, this.locZ + this.motZ);
                    final Vector dir = new Vector(l2.getX() - l.getX(), l2.getY() - l.getY(), l2.getZ() - l.getZ()).normalize();
                    bi = new BlockIterator(this.world.getWorld(), l, dir, 0.0, 1);
                }
                catch (IllegalStateException ex) {}
                if (bi != null) {
                    boolean open = true;
                    boolean hasSolidBlock = false;
                    while (bi.hasNext()) {
                        final org.bukkit.block.Block b = bi.next();
                        if (b.getType().isSolid() && b.getType().isOccluding()) {
                            hasSolidBlock = true;
                        }
                        if (b.getState().getData() instanceof Gate && !((Gate)b.getState().getData()).isOpen()) {
                            open = false;
                            break;
                        }
                    }
                    if (open && !hasSolidBlock) {
                        return;
                    }
                }
            }
        }
        if (movingobjectposition.entity != null) {
            if (movingobjectposition.entity == this.c) {
                return;
            }
            movingobjectposition.entity.damageEntity(DamageSource.projectile(this, this.getShooter()), 0.0f);
        }
        if (this.inUnloadedChunk && this.world.paperSpigotConfig.removeUnloadedEnderPearls) {
            this.die();
        }
        for (int i = 0; i < 32; ++i) {
            this.world.addParticle("portal", this.locX, this.locY + this.random.nextDouble() * 2.0, this.locZ, this.random.nextGaussian(), 0.0, this.random.nextGaussian());
        }
        if (!this.world.isStatic) {
            if (this.getShooter() != null && this.getShooter() instanceof EntityPlayer) {
                final EntityPlayer entityplayer = (EntityPlayer)this.getShooter();
                if (entityplayer.playerConnection.b().isConnected() && entityplayer.world == this.world) {
                    if (this.lastValidTeleport != null) {
                        final CraftPlayer player = entityplayer.getBukkitEntity();
                        final Location location = this.lastValidTeleport;
                        location.setPitch(player.getLocation().getPitch());
                        location.setYaw(player.getLocation().getYaw());
                        final PlayerTeleportEvent teleEvent = new PlayerTeleportEvent(player, player.getLocation(), location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
                        Bukkit.getPluginManager().callEvent(teleEvent);
                        if (!teleEvent.isCancelled() && !entityplayer.playerConnection.isDisconnected()) {
                            if (this.getShooter().am()) {
                                this.getShooter().mount(null);
                            }
                            entityplayer.playerConnection.teleport(teleEvent.getTo());
                            this.getShooter().fallDistance = 0.0f;
                            CraftEventFactory.entityDamage = this;
                            this.getShooter().damageEntity(DamageSource.FALL, 5.0f);
                            CraftEventFactory.entityDamage = null;
                        }
                    }
                    else {
                        Bukkit.getPluginManager().callEvent(new PlayerPearlRefundEvent(entityplayer.getBukkitEntity()));
                    }
                }
            }
            this.die();
        }
    }

    @Override
    public void h() {
        final EntityLiving shooter = this.getShooter();
        if (shooter != null && !shooter.isAlive()) {
            this.die();
        }
        else {
            final AxisAlignedBB newBoundingBox = AxisAlignedBB.a(this.locX - 0.3, this.locY - 0.05, this.locZ - 0.3, this.locX + 0.3, this.locY + 0.5, this.locZ + 0.3);
            if (!this.world.boundingBoxContainsMaterials(this.boundingBox.grow(0.25, 0.0, 0.25), EntityEnderPearl.PROHIBITED_PEARL_BLOCKS) && this.world.getCubes(this, newBoundingBox).isEmpty()) {
                this.lastValidTeleport = this.getBukkitEntity().getLocation();
            }
            final org.bukkit.block.Block block = this.world.getWorld().getBlockAt((int) FastMath.floor(this.locX), (int) FastMath.floor(this.locY), (int) FastMath.floor(this.locZ));
            final Material typeHere = this.world.getWorld().getBlockAt((int) FastMath.floor(this.locX), (int) FastMath.floor(this.locY), (int) FastMath.floor(this.locZ)).getType();
            if (EntityEnderPearl.pearlAbleType.stream().anyMatch(it -> typeHere.name().contains(it))) {
                this.lastValidTeleport = this.getBukkitEntity().getLocation();
            }
            if (shooter != null && EntityEnderPearl.forwardTypes.stream().anyMatch(it -> block.getType() == it)) {
                this.lastValidTeleport = this.getBukkitEntity().getLocation();
            }
            if (typeHere == Material.FENCE_GATE && ((Openable)block.getState().getData()).isOpen()) {
                this.lastValidTeleport = this.getBukkitEntity().getLocation();
            }
            if (shooter != null) {
                final org.bukkit.block.Block newTrap = block.getRelative(BlockFace.DOWN);
                if (newTrap.getType() == Material.COBBLE_WALL || newTrap.getType() == Material.FENCE) {
                    this.lastValidTeleport = newTrap.getLocation();
                }
            }
            super.h();
        }
    }

    public Item getToRefundPearl() {
        return this.toRefundPearl;
    }

    public void setToRefundPearl(final Item pearl) {
        this.toRefundPearl = pearl;
    }
}
