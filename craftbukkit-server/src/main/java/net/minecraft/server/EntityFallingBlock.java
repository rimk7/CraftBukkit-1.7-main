package net.minecraft.server;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

import java.util.ArrayList;
import java.util.Iterator;

public class EntityFallingBlock extends Entity {

    public Block id; // CraftBukkit - private -> public
    public int data;
    public int ticksLived;
    public boolean dropItem;
    private boolean f;
    private boolean hurtEntities;
    private int fallHurtMax;
    private float fallHurtAmount;
    public NBTTagCompound tileEntityData;
    public org.bukkit.Location sourceLoc; // PaperSpigot

    // PaperSpigot start - Add FallingBlock and TNT source location API
    public EntityFallingBlock(World world) {
        this(null, world);
    }

    public EntityFallingBlock(org.bukkit.Location loc, World world) {
        super(world);
        sourceLoc = loc;
        // PaperSpigot end
        this.dropItem = true;
        this.fallHurtMax = 40;
        this.fallHurtAmount = 2.0F;
        this.loadChunks = world.paperSpigotConfig.loadUnloadedFallingBlocks; // PaperSpigot
    }

    // PaperSpigot start - Add FallingBlock and TNT source location API
    public EntityFallingBlock(org.bukkit.Location loc, World world, double d0, double d1, double d2, Block block) {
        this(loc, world, d0, d1, d2, block, 0);
    }

    public EntityFallingBlock(org.bukkit.Location loc, World world, double d0, double d1, double d2, Block block, int i) {
        super(world);
        sourceLoc = loc;
        // PaperSpigot end
        this.dropItem = true;
        this.fallHurtMax = 40;
        this.fallHurtAmount = 2.0F;
        this.id = block;
        this.data = i;
        this.k = true;
        this.a(0.98F, 0.98F);
        this.height = this.length / 2.0F;
        this.setPosition(d0, d1, d2);
        this.motX = 0.0D;
        this.motY = 0.0D;
        this.motZ = 0.0D;
        this.lastX = d0;
        this.lastY = d1;
        this.lastZ = d2;
        this.loadChunks = world.paperSpigotConfig.loadUnloadedFallingBlocks; // PaperSpigot
    }

    protected boolean g_() {
        return false;
    }

    protected void c() {}

    public boolean R() {
        return !this.dead;
    }

    public void h() {
        if (this.id.getMaterial() == Material.AIR) {
            this.die();
        } else {
            this.lastX = this.locX;
            this.lastY = this.locY;
            this.lastZ = this.locZ;
            ++this.ticksLived;
            this.motY -= 0.03999999910593033D;
            this.move(this.motX, this.motY, this.motZ);
            // PaperSpigot start - Remove entities in unloaded chunks
            if (this.inUnloadedChunk && world.paperSpigotConfig.removeUnloadedFallingBlocks) {
                this.die();
            }
            // PaperSpigot end

            // PaperSpigot start - Drop falling blocks above the specified height
            if (this.world.paperSpigotConfig.fallingBlockHeightNerf != 0 && this.locY > this.world.paperSpigotConfig.fallingBlockHeightNerf) {
                if (this.dropItem) {
                    this.a(new ItemStack(this.id, 1, this.id.getDropData(this.data)), 0.0F);
                }

                this.die();
            }
            // PaperSpigot end

            this.motX *= 0.9800000190734863D;
            this.motY *= 0.9800000190734863D;
            this.motZ *= 0.9800000190734863D;
            if (!this.world.isStatic) {
                int i = (int) FastMath.floor(this.locX);
                int j = (int) FastMath.floor(this.locY);
                int k = (int) FastMath.floor(this.locZ);

                if (this.ticksLived == 1) {
                    // CraftBukkit - compare data and call event
                    if (this.ticksLived != 1 || this.world.getType(i, j, k) != this.id || this.world.getData(i, j, k) != this.data || CraftEventFactory.callEntityChangeBlockEvent(this, i, j, k, Blocks.AIR, 0).isCancelled()) {
                        this.die();
                        return;
                    }

                    this.world.setAir(i, j, k);
                    world.spigotConfig.antiXrayInstance.updateNearbyBlocks(world, i, j, k); // Spigot
                }

                if (this.onGround) {
                    this.motX *= 0.699999988079071D;
                    this.motZ *= 0.699999988079071D;
                    this.motY *= -0.5D;
                    if (this.world.getType(i, j, k) != Blocks.PISTON_MOVING) {
                        this.die();
                        // CraftBukkit start - fire EntityChangeBlockEvent
                        if (!this.f && this.world.mayPlace(this.id, i, j, k, true, 1, (Entity) null, (ItemStack) null) && !BlockFalling.canFall(this.world, i, j - 1, k) /* mimic the false conditions of setTypeIdAndData */ && i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000 && j > 0 && j < 256 && !(this.world.getType(i, j, k) == this.id && this.world.getData(i, j, k) == this.data)) {
                            if (CraftEventFactory.callEntityChangeBlockEvent(this, i, j, k, this.id, this.data).isCancelled()) {
                                return;
                            }
                            this.world.setTypeAndData(i, j, k, this.id, this.data, 3);
                            // CraftBukkit end
                            world.spigotConfig.antiXrayInstance.updateNearbyBlocks(world, i, j, k); // Spigot

                            if (this.id instanceof BlockFalling) {
                                ((BlockFalling) this.id).a(this.world, i, j, k, this.data);
                            }

                            if (this.tileEntityData != null && this.id instanceof IContainer) {
                                TileEntity tileentity = this.world.getTileEntity(i, j, k);

                                if (tileentity != null) {
                                    NBTTagCompound nbttagcompound = new NBTTagCompound();

                                    tileentity.b(nbttagcompound);
                                    Iterator iterator = this.tileEntityData.c().iterator();

                                    while (iterator.hasNext()) {
                                        String s = (String) iterator.next();
                                        NBTBase nbtbase = this.tileEntityData.get(s);

                                        if (!s.equals("x") && !s.equals("y") && !s.equals("z")) {
                                            nbttagcompound.set(s, nbtbase.clone());
                                        }
                                    }

                                    tileentity.a(nbttagcompound);
                                    tileentity.update();
                                }
                            }
                        } else if (this.dropItem && !this.f) {
                            this.a(new ItemStack(this.id, 1, this.id.getDropData(this.data)), 0.0F);
                        }
                    }
                } else if (this.ticksLived > 100 && !this.world.isStatic && (j < 1 || j > 256) || this.ticksLived > 600) {
                    if (this.dropItem) {
                        this.a(new ItemStack(this.id, 1, this.id.getDropData(this.data)), 0.0F);
                    }

                    this.die();
                }
            }
        }
    }

    protected void b(float f) {
        if (this.hurtEntities) {
            int i = MathHelper.f(f - 1.0F);

            if (i > 0) {
                ArrayList arraylist = new ArrayList(this.world.getEntities(this, this.boundingBox));
                boolean flag = this.id == Blocks.ANVIL;
                DamageSource damagesource = flag ? DamageSource.ANVIL : DamageSource.FALLING_BLOCK;
                Iterator iterator = arraylist.iterator();

                while (iterator.hasNext()) {
                    Entity entity = (Entity) iterator.next();

                    CraftEventFactory.entityDamage = this; // CraftBukkit
                    entity.damageEntity(damagesource, (float) FastMath.min(MathHelper.d((float) i * this.fallHurtAmount), this.fallHurtMax));
                    CraftEventFactory.entityDamage = null; // CraftBukkit
                }

                if (flag && (double) this.random.nextFloat() < 0.05000000074505806D + (double) i * 0.05D) {
                    int j = this.data >> 2;
                    int k = this.data & 3;

                    ++j;
                    if (j > 2) {
                        this.f = true;
                    } else {
                        this.data = k | j << 2;
                    }
                }
            }
        }
    }

    protected void b(NBTTagCompound nbttagcompound) {
        nbttagcompound.setByte("Tile", (byte) Block.getId(this.id));
        nbttagcompound.setInt("TileID", Block.getId(this.id));
        nbttagcompound.setByte("Data", (byte) this.data);
        nbttagcompound.setByte("Time", (byte) this.ticksLived);
        nbttagcompound.setBoolean("DropItem", this.dropItem);
        nbttagcompound.setBoolean("HurtEntities", this.hurtEntities);
        nbttagcompound.setFloat("FallHurtAmount", this.fallHurtAmount);
        nbttagcompound.setInt("FallHurtMax", this.fallHurtMax);
        if (this.tileEntityData != null) {
            nbttagcompound.set("TileEntityData", this.tileEntityData);
        }
        // PaperSpigot start - Add FallingBlock and TNT source location API
        if (sourceLoc != null) {
            nbttagcompound.setInt("SourceLoc_x", sourceLoc.getBlockX());
            nbttagcompound.setInt("SourceLoc_y", sourceLoc.getBlockY());
            nbttagcompound.setInt("SourceLoc_z", sourceLoc.getBlockZ());
        }
        // PaperSpigot end
    }

    protected void a(NBTTagCompound nbttagcompound) {
        if (nbttagcompound.hasKeyOfType("TileID", 99)) {
            this.id = Block.getById(nbttagcompound.getInt("TileID"));
        } else {
            this.id = Block.getById(nbttagcompound.getByte("Tile") & 255);
        }

        this.data = nbttagcompound.getByte("Data") & 255;
        this.ticksLived = nbttagcompound.getByte("Time") & 255;
        if (nbttagcompound.hasKeyOfType("HurtEntities", 99)) {
            this.hurtEntities = nbttagcompound.getBoolean("HurtEntities");
            this.fallHurtAmount = nbttagcompound.getFloat("FallHurtAmount");
            this.fallHurtMax = nbttagcompound.getInt("FallHurtMax");
        } else if (this.id == Blocks.ANVIL) {
            this.hurtEntities = true;
        }

        if (nbttagcompound.hasKeyOfType("DropItem", 99)) {
            this.dropItem = nbttagcompound.getBoolean("DropItem");
        }

        if (nbttagcompound.hasKeyOfType("TileEntityData", 10)) {
            this.tileEntityData = nbttagcompound.getCompound("TileEntityData");
        }

        if (this.id.getMaterial() == Material.AIR) {
            this.id = Blocks.SAND;
        }
        // PaperSpigot start - Add FallingBlock and TNT source location API
        if (nbttagcompound.hasKey("SourceLoc_x")) {
            int srcX = nbttagcompound.getInt("SourceLoc_x");
            int srcY = nbttagcompound.getInt("SourceLoc_y");
            int srcZ = nbttagcompound.getInt("SourceLoc_z");
            sourceLoc = new org.bukkit.Location(world.getWorld(), srcX, srcY, srcZ);
        }
        // PaperSpigot end
    }

    public void a(boolean flag) {
        this.hurtEntities = flag;
    }

    public void a(CrashReportSystemDetails crashreportsystemdetails) {
        super.a(crashreportsystemdetails);
        crashreportsystemdetails.a("Immitating block ID", Integer.valueOf(Block.getId(this.id)));
        crashreportsystemdetails.a("Immitating block data", Integer.valueOf(this.data));
    }

    public Block f() {
        return this.id;
    }
}
