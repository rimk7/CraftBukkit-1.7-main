package net.minecraft.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.cobelpvp.CobelSpigot;
import net.minecraft.optimizations.RemoveAllFix.Marker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.spigotmc.CustomTimingsHandler; // Spigot
import org.bukkit.inventory.InventoryHolder; // CraftBukkit

public class TileEntity implements Marker {

    private static final Logger a = LogManager.getLogger();
    private static final Map i = new HashMap();
    private static final Map j = new HashMap();
    // Optimized TileEntity Tick changes
    private static int tileEntityCounter = 0;

    private boolean needsRemoved = false;

    static {
        a(TileEntityFurnace.class, "Furnace");
        a(TileEntityChest.class, "Chest");
        a(TileEntityEnderChest.class, "EnderChest");
        a(TileEntityRecordPlayer.class, "RecordPlayer");
        a(TileEntityDispenser.class, "Trap");
        a(TileEntityDropper.class, "Dropper");
        a(TileEntitySign.class, "Sign");
        a(TileEntityMobSpawner.class, "MobSpawner");
        a(TileEntityNote.class, "Music");
        a(TileEntityPiston.class, "Piston");
        a(TileEntityBrewingStand.class, "Cauldron");
        a(TileEntityEnchantTable.class, "EnchantTable");
        a(TileEntityEnderPortal.class, "Airportal");
        a(TileEntityCommand.class, "Control");
        a(TileEntityBeacon.class, "Beacon");
        a(TileEntitySkull.class, "Skull");
        a(TileEntityLightDetector.class, "DLDetector");
        a(TileEntityHopper.class, "Hopper");
        a(TileEntityComparator.class, "Comparator");
        a(TileEntityFlowerPot.class, "FlowerPot");
    }

    public CustomTimingsHandler tickTimer = org.bukkit.craftbukkit.SpigotTimings.getTileEntityTimings(this); // Spigot
    public int x;
    public int y;
    public int z;
    public int g = -1;
    public Block h;
    public boolean isAdded = false;
    public int tileId = tileEntityCounter++;
    protected World world;
    protected boolean f;


    // Spigot end

    public TileEntity() {
    }

    private static void a(Class oclass, String s) {
        if (i.containsKey(s)) {
            throw new IllegalArgumentException("Duplicate id: " + s);
        } else {
            i.put(s, oclass);
            j.put(oclass, s);
        }
    }

    public static TileEntity c(NBTTagCompound nbttagcompound) {
        TileEntity tileentity = null;

        try {
            Class oclass = (Class) i.get(nbttagcompound.getString("id"));

            if (oclass != null) {
                tileentity = (TileEntity) oclass.newInstance();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        if (tileentity != null) {
            tileentity.a(nbttagcompound);
        } else {
            a.warn("Skipping BlockEntity with id " + nbttagcompound.getString("id"));
        }

        return tileentity;
    }

    static Map v() {
        return j;
    }

    // Spigot start
    // Helper method for scheduleTicks. If the hopper at x0, y0, z0 is pointed
    // at this tile entity, then make it active.
    private void scheduleTick(int x0, int y0, int z0) {
        TileEntity tileEntity = this.world.getTileEntity(x0, y0, z0);
        if (tileEntity instanceof TileEntityHopper && tileEntity.world != null) {
            // i is the metadeta assoiated with the direction the hopper faces.
            int i = BlockHopper.b(tileEntity.p());
            // Facing class provides arrays for direction offset.
            if (tileEntity.x + Facing.b[i] == this.x && tileEntity.y + Facing.c[i] == this.y && tileEntity.z + Facing.d[i] == this.z) {
                ((TileEntityHopper) tileEntity).makeTick();
            }
        }
    }

    // Called from update when the contents have changed, so hoppers need updates.
    // Check all 6 faces.
    public void scheduleTicks() {
        if (this.world != null && this.world.spigotConfig.altHopperTicking) {
            if (!CobelSpigot.isPositionOfTileEntityInUse(world, x, y, z)) return;
            // Check the top
            this.scheduleTick(this.x, this.y + 1, this.z);
            // Check the sides
            for (int i = 2; i < 6; i++) {
                this.scheduleTick(this.x + Facing.b[i], this.y, this.z + Facing.d[i]);
            }
            // Check the bottom.
            TileEntity tileEntity = this.world.getTileEntity(this.x, this.y - 1, this.z);
            if (tileEntity instanceof TileEntityHopper && tileEntity.world != null) {
                ((TileEntityHopper) tileEntity).makeTick();
            }
        }
    }

    public World getWorld() {
        return this.world;
    }

    public void a(World world) {
        this.world = world;
    }

    public boolean o() {
        return this.world != null;
    }

    public void a(NBTTagCompound nbttagcompound) {
        this.x = nbttagcompound.getInt("x");
        this.y = nbttagcompound.getInt("y");
        this.z = nbttagcompound.getInt("z");
    }

    public void b(NBTTagCompound nbttagcompound) {
        String s = (String) j.get(this.getClass());

        if (s == null) {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        } else {
            nbttagcompound.setString("id", s);
            nbttagcompound.setInt("x", this.x);
            nbttagcompound.setInt("y", this.y);
            nbttagcompound.setInt("z", this.z);
        }
    }

    public void h() {
    }

    public int p() {
        if (this.g == -1) {
            this.g = this.world.getData(this.x, this.y, this.z);
        }

        return this.g;
    }

    public void update() {
        if (this.world != null) {
            if (!CobelSpigot.isPositionOfTileEntityInUse(world, x, y, z)) return;
            this.g = this.world.getData(this.x, this.y, this.z);
            this.world.b(this.x, this.y, this.z, this);
            if (this.q() != Blocks.AIR) {
                this.world.updateAdjacentComparators(this.x, this.y, this.z, this.q());
            }
            // Spigot start - Called when the contents have changed, so hoppers around this
            // tile need updating.
            this.scheduleTicks();
            // Spigot end
        }
    }

    public Block q() {
        if (this.h == null) {
            this.h = this.world.getType(this.x, this.y, this.z);
        }

        return this.h;
    }

    public Packet getUpdatePacket() {
        return null;
    }

    public boolean r() {
        return this.f;
    }

    public void s() {
        this.f = true;
    }

    public void t() {
        this.f = false;
    }

    public boolean c(int i, int j) {
        return false;
    }

    public void u() {
        this.h = null;
        this.g = -1;
    }

    public void a(CrashReportSystemDetails crashreportsystemdetails) {
        crashreportsystemdetails.a("Name", new CrashReportTileEntityName(this));
        Block block = this.q(); // PaperSpigot
        if (block != null) { // PaperSpigot
            CrashReportSystemDetails.a(crashreportsystemdetails, this.x, this.y, this.z, this.q(), this.p());
        } // PaperSpigot
        crashreportsystemdetails.a("Actual block type", new CrashReportTileEntityType(this));
        crashreportsystemdetails.a("Actual block data value", new CrashReportTileEntityData(this));
    }

    // CraftBukkit start - add method
    public InventoryHolder getOwner() {
        // Spigot start
        org.bukkit.block.Block block = world.getWorld().getBlockAt(x, y, z);
        if (block == null) {
            org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.WARNING, "No block for owner at %s %d %d %d", new Object[]{world.getWorld(), x, y, z});
            return null;
        }
        // Spigot end
        org.bukkit.block.BlockState state = block.getState();
        if (state instanceof InventoryHolder) return (InventoryHolder) state;
        return null;
    }
    // CraftBukkit end

    public boolean isToBeRemoved() {
        return needsRemoved;
    }

    public void setRemovalState(boolean removalState) {
        needsRemoved = removalState;
    }
}
