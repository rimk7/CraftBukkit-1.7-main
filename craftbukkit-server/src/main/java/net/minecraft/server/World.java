package net.minecraft.server;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

// PaperSpigot start
import java.util.HashMap;
import java.util.Map;

import com.cobelpvp.generator.GeneratorConfig;
// PaperSpigot end

// CraftBukkit start
import net.minecraft.optimizations.*;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.generator.ChunkGenerator;

import com.google.common.base.Function;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
// CraftBukkit end

// CobelPvP start
import net.minecraft.optimizations.ThreadingManager.TaskQueueWorker;
import com.cobelpvp.autosave.AutoSaveWorldData;
// CobelPvP end

public abstract class World implements IBlockAccess {

    public boolean d;
    // Spigot start - guard entity list from removals
    public final List<Entity> entityList = new RemoveAllFix<Entity>()
    {
        @Override
        public Entity remove(int index)
        {
            guard();
            return super.remove( index );
        }

        @Override
        public boolean remove(Object o)
        {
            guard();
            return super.remove( o );
        }

        private void guard()
        {
            if ( guardEntityList )
            {
                throw new java.util.ConcurrentModificationException();
            }
        }
    };
    // Spigot end
    protected List f = new ArrayList();
    public final List<TileEntity> tileEntityList = new RemoveAllFix<>(); // CraftBukkit - ArrayList -> HashSet
    private List a = new ArrayList();
    private List b = new ArrayList();
    public List players = new ArrayList();
    public List i = new ArrayList();
    private long c = 16777215L;
    public int j;
    protected int k = (new Random()).nextInt();
    protected final int l = 1013904223;
    protected float m;
    protected float n;
    protected float o;
    protected float p;
    public int q;
    public EnumDifficulty difficulty;
    public Random random = new Random();
    public WorldProvider worldProvider; // CraftBukkit - remove final
    protected List u = new ArrayList();
    public IChunkProvider chunkProvider; // CraftBukkit - public
    protected final IDataManager dataManager;
    public WorldData worldData; // CraftBukkit - public
    public boolean isLoading;
    public PersistentCollection worldMaps;
    public final PersistentVillage villages;
    protected final VillageSiege siegeManager = new VillageSiege(this);
    public final MethodProfiler methodProfiler;
    private final Calendar J = Calendar.getInstance();
    public Scoreboard scoreboard = new Scoreboard(); // CraftBukkit - protected -> public
    public boolean isStatic;
    // CraftBukkit start - public, longhashset
    // protected LongHashSet chunkTickList = new LongHashSet(); // Spigot
    private int K;
    public boolean allowMonsters;
    public boolean allowAnimals;
    // Added the following
    public boolean captureBlockStates = false;
    public boolean captureTreeGeneration = false;
    public ArrayList<BlockState> capturedBlockStates= new ArrayList<BlockState>();
    public long ticksPerAnimalSpawns;
    public long ticksPerMonsterSpawns;
    public boolean populating;
    private int tickPosition;
    // CraftBukkit end
    private ArrayList L;
    private boolean M;
    int[] I;

    // Spigot start
    private boolean guardEntityList;
    protected final net.minecraft.util.gnu.trove.map.hash.TLongShortHashMap chunkTickList;
    protected float growthOdds = 100;
    protected float modifiedOdds = 100;
    private final byte chunkTickRadius;
    public static boolean haveWeSilencedAPhysicsCrash;
    public static String blockLocation;
    public List<TileEntity> triggerHoppersList = new ArrayList<TileEntity>(); // Spigot, When altHopperTicking, tile entities being added go through here.
    // CobelPvP start
    private LightingUpdater lightingUpdater = new LightingUpdater();
    private TaskQueueWorker lightingQueue = ThreadingManager.createTaskQueue();
    // CobelPvP end
    public final Map<Explosion.CacheKey, Float> explosionDensityCache = new HashMap<Explosion.CacheKey, Float>(); // PaperSpigot - Optimize explosions

    public double hardDespawnDistance = -1D; // CobelPvP

    public static long chunkToKey(int x, int z)
    {
        long k = ( ( ( (long) x ) & 0xFFFF0000L ) << 16 ) | ( ( ( (long) x ) & 0x0000FFFFL ) << 0 );
        k     |= ( ( ( (long) z ) & 0xFFFF0000L ) << 32 ) | ( ( ( (long) z ) & 0x0000FFFFL ) << 16 );
        return k;
    }

    public static int keyToX(long k)
    {
        return (int) ( ( ( k >> 16 ) & 0xFFFF0000 ) | ( k & 0x0000FFFF ) );
    }

    public static int keyToZ(long k)
    {
        return (int) ( ( ( k >> 32 ) & 0xFFFF0000L ) | ( ( k >> 16 ) & 0x0000FFFF ) );
    }

    // Spigot Start - Hoppers need to be born ticking.
    private void initializeHoppers() {
        if (this.spigotConfig.altHopperTicking) {
            for (TileEntity o : this.triggerHoppersList) {
                o.scheduleTicks();
                if (o instanceof TileEntityHopper) {
                    ((TileEntityHopper) o).convertToScheduling();
                    ((TileEntityHopper) o).scheduleHopperTick();
                }
            }
        }
        triggerHoppersList.clear();
    }
    
    // Helper method for altHopperTicking. Updates chests at the specified location,
    // accounting for double chests. Updating the chest will update adjacent hoppers.
    public void updateChestAndHoppers(int a, int b, int c) {
        Block block = this.getType(a, b, c);
        if (block instanceof BlockChest) {
            TileEntity tile = this.getTileEntity(a, b, c);
            if (tile instanceof TileEntityChest) {
                tile.scheduleTicks();
            }
            for (int i = 2; i < 6; i++) {
            	// Facing class provides arrays for direction offset.
                if (this.getType(a + Facing.b[i], b, c + Facing.d[i]) == block) {
                    tile = this.getTileEntity(a + Facing.b[i], b, c + Facing.d[i]);
                    if (tile instanceof TileEntityChest) {
                        tile.scheduleTicks();
                    }
                    break;
                }
            }
        }
    }
    // Spigot end

    // CobelPvP start
    public final PlayerMap playerMap = new PlayerMap();
    // CobelPvP end

    public BiomeBase getBiome(int i, int j) {
        if (this.isLoaded(i, 0, j)) {
            Chunk chunk = this.getChunkAtWorldCoords(i, j);

            try {
                return chunk.getBiome(i & 15, j & 15, this.worldProvider.e);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.a(throwable, "Getting biome");
                CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Coordinates of biome request");

                crashreportsystemdetails.a("Location", (Callable) (new CrashReportWorldLocation(this, i, j)));
                throw new ReportedException(crashreport);
            }
        } else {
            return this.worldProvider.e.getBiome(i, j);
        }
    }

    public WorldChunkManager getWorldChunkManager() {
        return this.worldProvider.e;
    }


    // CobelPvP start
    public ChunkProviderServer chunkProviderServer; // moved here from WorldServer
    private final AutoSaveWorldData autoSaveWorldData;

    public AutoSaveWorldData getAutoSaveWorldData() {
        return this.autoSaveWorldData;
    }
    // CobelPvP end

    // CraftBukkit start
    private final CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public ChunkGenerator generator;
    public final org.spigotmc.SpigotWorldConfig spigotConfig; // Spigot
    public final org.github.paperspigot.PaperSpigotWorldConfig paperSpigotConfig; // PaperSpigot
    public final net.tacospigot.TacoSpigotWorldConfig tacoSpigotWorldConfig;
    public final SpigotTimings.WorldTimingsHandler timings; // Spigot

    public CraftWorld getWorld() {
        return this.world;
    }

    public CraftServer getServer() {
        return (CraftServer) Bukkit.getServer();
    }

    public Chunk getChunkIfLoaded(int x, int z) {
        return ((ChunkProviderServer) this.chunkProvider).getChunkIfLoaded(x, z);
    }

    public final GeneratorConfig generatorConfig;// CobelPvP

    // Changed signature - added gen and env
    public World(IDataManager idatamanager, String s, WorldSettings worldsettings, WorldProvider worldprovider, MethodProfiler methodprofiler, ChunkGenerator gen, org.bukkit.World.Environment env) {
        this.spigotConfig = new org.spigotmc.SpigotWorldConfig( s ); // Spigot
        this.paperSpigotConfig = new org.github.paperspigot.PaperSpigotWorldConfig( s ); // PaperSpigot
        this.generatorConfig = new GeneratorConfig(s); // CobelPvP
        this.tacoSpigotWorldConfig = new net.tacospigot.TacoSpigotWorldConfig(s);
        this.generator = gen;
        this.world = new CraftWorld((WorldServer) this, gen, env);
        this.ticksPerAnimalSpawns = this.getServer().getTicksPerAnimalSpawns(); // CraftBukkit
        this.ticksPerMonsterSpawns = this.getServer().getTicksPerMonsterSpawns(); // CraftBukkit
        // CraftBukkit end
        this.keepSpawnInMemory = this.paperSpigotConfig.keepSpawnInMemory; // PaperSpigot
        // Spigot start
        this.chunkTickRadius = (byte) ( ( this.getServer().getViewDistance() < 7 ) ? this.getServer().getViewDistance() : 7 );
        this.chunkTickList = new net.minecraft.util.gnu.trove.map.hash.TLongShortHashMap( spigotConfig.chunksPerTick * 5, 0.7f, Long.MIN_VALUE, Short.MIN_VALUE );
        this.chunkTickList.setAutoCompactionFactor( 0 );
        // Spigot end

        this.K = this.random.nextInt(12000);
        this.allowMonsters = true;
        this.allowAnimals = true;
        this.L = new ArrayList();
        this.I = new int['\u8000'];
        this.dataManager = idatamanager;
        this.methodProfiler = methodprofiler;
        this.worldMaps = new PersistentCollection(idatamanager);
        this.worldData = idatamanager.getWorldData();
        if (worldprovider != null) {
            this.worldProvider = worldprovider;
        } else if (this.worldData != null && this.worldData.j() != 0) {
            this.worldProvider = WorldProvider.byDimension(this.worldData.j());
        } else {
            this.worldProvider = WorldProvider.byDimension(0);
        }

        if (this.worldData == null) {
            this.worldData = new WorldData(worldsettings, s);
        } else {
            this.worldData.setName(s);
        }

        this.worldProvider.a(this);
        this.chunkProvider = this.j();
        timings = new SpigotTimings.WorldTimingsHandler(this); // Spigot - code below can generate new world and access timings
        if (!this.worldData.isInitialized()) {
            try {
                this.a(worldsettings);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.a(throwable, "Exception initializing level");

                try {
                    this.a(crashreport);
                } catch (Throwable throwable1) {
                    ;
                }

                throw new ReportedException(crashreport);
            }

            this.worldData.d(true);
        }

        PersistentVillage persistentvillage = (PersistentVillage) this.worldMaps.get(PersistentVillage.class, "villages");

        if (persistentvillage == null) {
            this.villages = new PersistentVillage(this);
            this.worldMaps.a("villages", this.villages);
        } else {
            this.villages = persistentvillage;
            this.villages.a(this);
        }

        this.B();
        this.a();

        this.getServer().addWorld(this.world); // CraftBukkit

        // CobelPvP - Set spawn flags based on mobsEnabled.
        if (!spigotConfig.mobsEnabled) {
            this.world.setSpawnFlags(false, false);
        }

        this.autoSaveWorldData = new AutoSaveWorldData(this); // CobelPvP
    }

    protected abstract IChunkProvider j();

    protected void a(WorldSettings worldsettings) {
        this.worldData.d(true);
    }

    public Block b(int i, int j) {
        int k;

        for (k = 63; !this.isEmpty(i, k + 1, j); ++k) {
            ;
        }

        return this.getType(i, k, j);
    }

    // Spigot start
    public Block getType(int i, int j, int k)
    {
        return getType( i, j, k, true );
    }

    public Block getType(int i, int j, int k, boolean useCaptured) {
        // CraftBukkit start - tree generation
        if (captureTreeGeneration && useCaptured) {
    // Spigot end
            Iterator<BlockState> it = capturedBlockStates.iterator();
            while (it.hasNext()) {
                BlockState previous = it.next();
                if (previous.getX() == i && previous.getY() == j && previous.getZ() == k) {
                    return CraftMagicNumbers.getBlock(previous.getTypeId());
                }
            }
        }
        // CraftBukkit end
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000 && j >= 0 && j < 256) {
            Chunk chunk = null;

            try {
                chunk = this.getChunkAt(i >> 4, k >> 4);
                return chunk.getType(i & 15, j, k & 15);
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.a(throwable, "Exception getting block type in world");
                CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Requested block coordinates");

                crashreportsystemdetails.a("Found chunk", Boolean.valueOf(chunk == null));
                crashreportsystemdetails.a("Location", CrashReportSystemDetails.a(i, j, k));
                throw new ReportedException(crashreport);
            }
        } else {
            return Blocks.AIR;
        }
    }

    public boolean isEmpty(int i, int j, int k) {
        return this.getType(i, j, k).getMaterial() == Material.AIR;
    }

    public boolean isLoaded(int i, int j, int k) {
        return j >= 0 && j < 256 ? this.isChunkLoaded(i >> 4, k >> 4) : false;
    }

    public boolean areChunksLoaded(int i, int j, int k, int l) {
        return this.b(i - l, j - l, k - l, i + l, j + l, k + l);
    }

    public boolean b(int i, int j, int k, int l, int i1, int j1) {
        if (i1 >= 0 && j < 256) {
            i >>= 4;
            k >>= 4;
            l >>= 4;
            j1 >>= 4;

            for (int k1 = i; k1 <= l; ++k1) {
                for (int l1 = k; l1 <= j1; ++l1) {
                    if (!this.isChunkLoaded(k1, l1)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    protected boolean isChunkLoaded(int i, int j) {
        return this.chunkProvider.isChunkLoaded(i, j);
    }

    public Chunk getChunkAtWorldCoords(int i, int j) {
        return this.getChunkAt(i >> 4, j >> 4);
    }

    public Chunk getChunkAt(int i, int j) {
        return this.chunkProvider.getOrCreateChunk(i, j);
    }

    public boolean setTypeAndData(int i, int j, int k, Block block, int l, int i1) {
        // CraftBukkit start - tree generation
        if (this.captureTreeGeneration) {
            BlockState blockstate = null;
            Iterator<BlockState> it = capturedBlockStates.iterator();
            while (it.hasNext()) {
                BlockState previous = it.next();
                if (previous.getX() == i && previous.getY() == j && previous.getZ() == k) {
                    blockstate = previous;
                    it.remove();
                    break;
                }
            }
            if (blockstate == null) {
                blockstate = org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(this, i, j, k, i1);
            }
            blockstate.setTypeId(CraftMagicNumbers.getId(block));
            blockstate.setRawData((byte) l);
            this.capturedBlockStates.add(blockstate);
            return true;
        }
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j < 0) {
                return false;
            } else if (j >= 256) {
                return false;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);
                Block block1 = null;

                if ((i1 & 1) != 0) {
                    block1 = chunk.getType(i & 15, j, k & 15);
                }

                // CraftBukkit start - capture blockstates
                BlockState blockstate = null;
                if (this.captureBlockStates) {
                    blockstate = org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(this, i, j, k, i1);
                    this.capturedBlockStates.add(blockstate);
                }
                // CraftBukkit end

                boolean flag = chunk.a(i & 15, j, k & 15, block, l);

                // CraftBukkit start - remove blockstate if failed
                if (!flag && this.captureBlockStates) {
                    this.capturedBlockStates.remove(blockstate);
                }
                // CraftBukkit end

                this.methodProfiler.a("checkLight");
                this.t(i, j, k);
                this.methodProfiler.b();
                // CraftBukkit start
                if (flag && !this.captureBlockStates) { // Don't notify clients or update physics while capturing blockstates
                    // Modularize client and physic updates
                    this.notifyAndUpdatePhysics(i, j, k, chunk, block1, block, i1);
                // CraftBukkit end
                }
                // Spigot start - If this block is changing to that which a chest beneath it
                // becomes able to be opened, then the chest must be updated.
                // block1 is the old block. block is the new block. r returns true if the block type
                // prevents access to a chest.
                if (this.spigotConfig.altHopperTicking && block1 != null && block1.r() && !block.r()) {
                    this.updateChestAndHoppers(i, j - 1, k);
                }
                // Spigot end

                return flag;
            }
        } else {
            return false;
        }
    }

    // CraftBukkit start - Split off from original setTypeAndData(int i, int j, int k, Block block, int l, int i1) method in order to directly send client and physic updates
    public void notifyAndUpdatePhysics(int i, int j, int k, Chunk chunk, Block oldBlock, Block newBlock, int flag)
    {
        // should be isReady()
        if ((flag & 2) != 0 && (!this.isStatic || (flag & 4) == 0) && (chunk == null || chunk.isReady())) { // allow chunk to be null here as chunk.isReady() is false when we send our notification during block placement
            this.notify(i, j, k);
        }

        if ((flag & 1) != 0) {
            this.update(i, j, k, oldBlock);
            if (newBlock.isComplexRedstone()) {
                this.updateAdjacentComparators(i, j, k, newBlock);
            }
        }
    }
    // CraftBukkit end

    public int getData(int i, int j, int k) {
        // CraftBukkit start - tree generation
        if (captureTreeGeneration) {
            Iterator<BlockState> it = capturedBlockStates.iterator();
            while (it.hasNext()) {
                BlockState previous = it.next();
                if (previous.getX() == i && previous.getY() == j && previous.getZ() == k) {
                    return previous.getRawData();
                }
            }
        }
        // CraftBukkit end
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j < 0) {
                return 0;
            } else if (j >= 256) {
                return 0;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                i &= 15;
                k &= 15;
                return chunk.getData(i, j, k);
            }
        } else {
            return 0;
        }
    }

    public boolean setData(int i, int j, int k, int l, int i1) {
        // CraftBukkit start - tree generation
        if (this.captureTreeGeneration) {
            BlockState blockstate = null;
            Iterator<BlockState> it = capturedBlockStates.iterator();
            while (it.hasNext()) {
                BlockState previous = it.next();
                if (previous.getX() == i && previous.getY() == j && previous.getZ() == k) {
                    blockstate = previous;
                    it.remove();
                    break;
                }
            }
            if (blockstate == null) {
                blockstate = org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(this, i, j, k, i1);
            }
            blockstate.setRawData((byte) l);
            this.capturedBlockStates.add(blockstate);
            return true;
        }
        // CraftBukkit end
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j < 0) {
                return false;
            } else if (j >= 256) {
                return false;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);
                int j1 = i & 15;
                int k1 = k & 15;
                boolean flag = chunk.a(j1, j, k1, l);

                if (flag) {
                    Block block = chunk.getType(j1, j, k1);

                    if ((i1 & 2) != 0 && (!this.isStatic || (i1 & 4) == 0) && chunk.isReady()) {
                        this.notify(i, j, k);
                    }

                    if (!this.isStatic && (i1 & 1) != 0) {
                        this.update(i, j, k, block);
                        if (block.isComplexRedstone()) {
                            this.updateAdjacentComparators(i, j, k, block);
                        }
                    }
                }

                return flag;
            }
        } else {
            return false;
        }
    }

    public boolean setAir(int i, int j, int k) {
        return this.setTypeAndData(i, j, k, Blocks.AIR, 0, 3);
    }

    public boolean setAir(int i, int j, int k, boolean flag) {
        Block block = this.getType(i, j, k);

        if (block.getMaterial() == Material.AIR) {
            return false;
        } else {
            int l = this.getData(i, j, k);

            this.triggerEffect(2001, i, j, k, Block.getId(block) + (l << 12));
            if (flag) {
                block.b(this, i, j, k, l, 0);
            }

            return this.setTypeAndData(i, j, k, Blocks.AIR, 0, 3);
        }
    }

    public boolean setTypeUpdate(int i, int j, int k, Block block) {
        return this.setTypeAndData(i, j, k, block, 0, 3);
    }

    public void notify(int i, int j, int k) {
        for (int l = 0; l < this.u.size(); ++l) {
            ((IWorldAccess) this.u.get(l)).a(i, j, k);
        }
    }

    public void update(int i, int j, int k, Block block) {
        // CraftBukkit start
        if (this.populating) {
            return;
        }
        // CraftBukkit end
        this.applyPhysics(i, j, k, block);
    }

    public void b(int i, int j, int k, int l) {
        int i1;

        if (k > l) {
            i1 = l;
            l = k;
            k = i1;
        }

        if (!this.worldProvider.g) {
            for (i1 = k; i1 <= l; ++i1) {
                this.updateLight(EnumSkyBlock.SKY, i, i1, j); // PaperSpigot - Asynchronous lighting updates
            }
        }

        this.c(i, k, j, i, l, j);
    }

    public void c(int i, int j, int k, int l, int i1, int j1) {
        for (int k1 = 0; k1 < this.u.size(); ++k1) {
            ((IWorldAccess) this.u.get(k1)).a(i, j, k, l, i1, j1);
        }
    }

    public void applyPhysics(int i, int j, int k, Block block) {
        if (captureBlockStates) return;
        this.e(i - 1, j, k, block);
        this.e(i + 1, j, k, block);
        this.e(i, j - 1, k, block);
        this.e(i, j + 1, k, block);
        this.e(i, j, k - 1, block);
        this.e(i, j, k + 1, block);
        spigotConfig.antiXrayInstance.updateNearbyBlocks(this, i, j, k); // Spigot
    }

    public void b(int i, int j, int k, Block block, int l) {
        if (l != 4) {
            this.e(i - 1, j, k, block);
        }

        if (l != 5) {
            this.e(i + 1, j, k, block);
        }

        if (l != 0) {
            this.e(i, j - 1, k, block);
        }

        if (l != 1) {
            this.e(i, j + 1, k, block);
        }

        if (l != 2) {
            this.e(i, j, k - 1, block);
        }

        if (l != 3) {
            this.e(i, j, k + 1, block);
        }
    }

    public void e(int i, int j, int k, Block block) {
        if (!this.isStatic) {
            Block block1 = this.getType(i, j, k);

            try {
                // CraftBukkit start
                CraftWorld world = ((WorldServer) this).getWorld();
                if (world != null) {
                    BlockPhysicsEvent event = new BlockPhysicsEvent(world.getBlockAt(i, j, k), CraftMagicNumbers.getId(block));
                    this.getServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return;
                    }
                }
                // CraftBukkit end

                block1.doPhysics(this, i, j, k, block);
            } catch (StackOverflowError stackoverflowerror) { // Spigot Start
                haveWeSilencedAPhysicsCrash = true;
                blockLocation = i + ", " + j + ", " + k; // Spigot End
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.a(throwable, "Exception while updating neighbours");
                CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Block being updated");

                int l;

                try {
                    l = this.getData(i, j, k);
                } catch (Throwable throwable1) {
                    l = -1;
                }

                crashreportsystemdetails.a("Source block type", (Callable) (new CrashReportSourceBlockType(this, block)));
                CrashReportSystemDetails.a(crashreportsystemdetails, i, j, k, block1, l);
                throw new ReportedException(crashreport);
            }
        }
    }

    public boolean a(int i, int j, int k, Block block) {
        return false;
    }

    public boolean i(int i, int j, int k) {
        return this.getChunkAt(i >> 4, k >> 4).d(i & 15, j, k & 15);
    }

    public int j(int i, int j, int k) {
        if (j < 0) {
            return 0;
        } else {
            if (j >= 256) {
                j = 255;
            }

            return this.getChunkAt(i >> 4, k >> 4).b(i & 15, j, k & 15, 0);
        }
    }

    public int getLightLevel(int i, int j, int k) {
        return this.b(i, j, k, true);
    }

    public int b(int i, int j, int k, boolean flag) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (flag && this.getType(i, j, k).n()) {
                int l = this.b(i, j + 1, k, false);
                int i1 = this.b(i + 1, j, k, false);
                int j1 = this.b(i - 1, j, k, false);
                int k1 = this.b(i, j, k + 1, false);
                int l1 = this.b(i, j, k - 1, false);

                if (i1 > l) {
                    l = i1;
                }

                if (j1 > l) {
                    l = j1;
                }

                if (k1 > l) {
                    l = k1;
                }

                if (l1 > l) {
                    l = l1;
                }

                return l;
            } else if (j < 0) {
                return 0;
            } else {
                if (j >= 256) {
                    j = 255;
                }

                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                i &= 15;
                k &= 15;
                return chunk.b(i, j, k, this.j);
            }
        } else {
            return 15;
        }
    }

    // CobelPvP start
    public boolean isLightLevel(int i, int j, int k, int level) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (this.getType(i, j, k).n()) {
                if (this.b(i, j + 1, k, false) >= level) {
                    return true;
                }
                if (this.b(i + 1, j, k, false) >= level) {
                    return true;
                }
                if (this.b(i - 1, j, k, false) >= level) {
                    return true;
                }
                if (this.b(i, j, k + 1, false) >= level) {
                    return true;
                }
                if (this.b(i, j, k - 1, false) >= level) {
                    return true;
                }
                return false;
            } else {
                if (j >= 256) {
                    j = 255;
                }

                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                i &= 15;
                k &= 15;
                return chunk.b(i, j, k, this.j) >= level;
            }
        } else {
            return true;
        }
    }
    // CobelPvP end

    public int getHighestBlockYAt(int i, int j) {
    // CobelPvP start
        return this.getHighestBlockYAt(i, j, false);
    }

    public int getHighestBlockYAt(int i, int j, boolean chunksHaveAlreadyBeenChecked) {
    // CobelPvP end
        if (i >= -30000000 && j >= -30000000 && i < 30000000 && j < 30000000) {
            if (!chunksHaveAlreadyBeenChecked && !this.isChunkLoaded(i >> 4, j >> 4)) { // CobelPvP
                return 0;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, j >> 4);

                return chunk.b(i & 15, j & 15);
            }
        } else {
            return 64;
        }
    }

    public int g(int i, int j) {
    // CobelPvP start
        return this.g(i, j, false);
    }

    public int g(int i, int j, boolean chunksHaveAlreadyBeenChecked) {
    // CobelPvP end
        if (i >= -30000000 && j >= -30000000 && i < 30000000 && j < 30000000) {
            if (!chunksHaveAlreadyBeenChecked && !this.isChunkLoaded(i >> 4, j >> 4)) { // CobelPvP
                return 0;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, j >> 4);

                return chunk.r;
            }
        } else {
            return 64;
        }
    }

    public int b(EnumSkyBlock enumskyblock, int i, int j, int k) {
        if (j < 0) {
            j = 0;
        }

        if (j >= 256) {
            j = 255;
        }

        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            int l = i >> 4;
            int i1 = k >> 4;

            if (!this.isChunkLoaded(l, i1)) {
                return enumskyblock.c;
            } else {
                Chunk chunk = this.getChunkAt(l, i1);

                return chunk.getBrightness(enumskyblock, i & 15, j, k & 15);
            }
        } else {
            return enumskyblock.c;
        }
    }

    public void b(EnumSkyBlock enumskyblock, int i, int j, int k, int l) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j >= 0) {
                if (j < 256) {
                    if (this.isChunkLoaded(i >> 4, k >> 4)) {
                        Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                        chunk.a(enumskyblock, i & 15, j, k & 15, l);

                        for (int i1 = 0; i1 < this.u.size(); ++i1) {
                            ((IWorldAccess) this.u.get(i1)).b(i, j, k);
                        }
                    }
                }
            }
        }
    }

    public void m(int i, int j, int k) {
        for (int l = 0; l < this.u.size(); ++l) {
            ((IWorldAccess) this.u.get(l)).b(i, j, k);
        }
    }

    public float n(int i, int j, int k) {
        return this.worldProvider.h[this.getLightLevel(i, j, k)];
    }

    public boolean w() {
        return this.j < 4;
    }

    public MovingObjectPosition a(Vec3D vec3d, Vec3D vec3d1) {
        return this.rayTrace(vec3d, vec3d1, false, false, false);
    }

    public MovingObjectPosition rayTrace(Vec3D vec3d, Vec3D vec3d1, boolean flag) {
        return this.rayTrace(vec3d, vec3d1, flag, false, false);
    }

    public MovingObjectPosition rayTrace(Vec3D position, Vec3D motion, boolean flag, boolean flag1, boolean flag2) {
        if (!Double.isNaN(position.a) && !Double.isNaN(position.b) && !Double.isNaN(position.c)) {
            if (!Double.isNaN(motion.a) && !Double.isNaN(motion.b) && !Double.isNaN(motion.c)) {
                int i = (int) FastMath.floor(motion.a);
                int j = (int) FastMath.floor(motion.b);
                int k = (int) FastMath.floor(motion.c);
                int l = (int) FastMath.floor(position.a);
                int i1 = (int) FastMath.floor(position.b);
                int j1 = (int) FastMath.floor(position.c);
                Block block = this.getType(l, i1, j1);
                int k1 = this.getData(l, i1, j1);

                if ((!flag1 || block.a(this, l, i1, j1) != null) && block.a(k1, flag)) {
                    MovingObjectPosition movingobjectposition = block.a(this, l, i1, j1, position, motion);

                    if (movingobjectposition != null) {
                        return movingobjectposition;
                    }
                }

                MovingObjectPosition movingobjectposition1 = null;

                k1 = 200;

                while (k1-- >= 0) {
                    if (Double.isNaN(position.a) || Double.isNaN(position.b) || Double.isNaN(position.c)) {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k) {
                        return flag2 ? movingobjectposition1 : null;
                    }

                    boolean flag3 = true;
                    boolean flag4 = true;
                    boolean flag5 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l) {
                        d0 = (double) l + 1.0D;
                    } else if (i < l) {
                        d0 = (double) l + 0.0D;
                    } else {
                        flag3 = false;
                    }

                    if (j > i1) {
                        d1 = (double) i1 + 1.0D;
                    } else if (j < i1) {
                        d1 = (double) i1 + 0.0D;
                    } else {
                        flag4 = false;
                    }

                    if (k > j1) {
                        d2 = (double) j1 + 1.0D;
                    } else if (k < j1) {
                        d2 = (double) j1 + 0.0D;
                    } else {
                        flag5 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = motion.a - position.a;
                    double d7 = motion.b - position.b;
                    double d8 = motion.c - position.c;

                    if (flag3) {
                        d3 = (d0 - position.a) / d6;
                    }

                    if (flag4) {
                        d4 = (d1 - position.b) / d7;
                    }

                    if (flag5) {
                        d5 = (d2 - position.c) / d8;
                    }

                    boolean flag6 = false;
                    byte b0;

                    if (d3 < d4 && d3 < d5) {
                        if (i > l) {
                            b0 = 4;
                        } else {
                            b0 = 5;
                        }

                        position.a = d0;
                        position.b += d7 * d3;
                        position.c += d8 * d3;
                    } else if (d4 < d5) {
                        if (j > i1) {
                            b0 = 0;
                        } else {
                            b0 = 1;
                        }

                        position.a += d6 * d4;
                        position.b = d1;
                        position.c += d8 * d4;
                    } else {
                        if (k > j1) {
                            b0 = 2;
                        } else {
                            b0 = 3;
                        }

                        position.a += d6 * d5;
                        position.b += d7 * d5;
                        position.c = d2;
                    }

                    Vec3D vec3d2 = Vec3D.a(position.a, position.b, position.c);

                    l = (int) (vec3d2.a = (double) FastMath.floor(position.a));
                    if (b0 == 5) {
                        --l;
                        ++vec3d2.a;
                    }

                    i1 = (int) (vec3d2.b = (double) FastMath.floor(position.b));
                    if (b0 == 1) {
                        --i1;
                        ++vec3d2.b;
                    }

                    j1 = (int) (vec3d2.c = (double) FastMath.floor(position.c));
                    if (b0 == 3) {
                        --j1;
                        ++vec3d2.c;
                    }

                    Block block1 = this.getType(l, i1, j1);
                    int l1 = this.getData(l, i1, j1);

                    if (!flag1 || block1.a(this, l, i1, j1) != null) {
                        if (block1.a(l1, flag)) {
                            MovingObjectPosition movingobjectposition2 = block1.a(this, l, i1, j1, position, motion);

                            if (movingobjectposition2 != null) {
                                return movingobjectposition2;
                            }
                        } else {
                            movingobjectposition1 = new MovingObjectPosition(l, i1, j1, b0, position, false);
                        }
                    }
                }

                return flag2 ? movingobjectposition1 : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void makeSound(Entity entity, String s, float f, float f1) {
        for (int i = 0; i < this.u.size(); ++i) {
            ((IWorldAccess) this.u.get(i)).a(s, entity.locX, entity.locY - (double) entity.height, entity.locZ, f, f1);
        }
    }

    public void a(EntityHuman entityhuman, String s, float f, float f1) {
        for (int i = 0; i < this.u.size(); ++i) {
            ((IWorldAccess) this.u.get(i)).a(entityhuman, s, entityhuman.locX, entityhuman.locY - (double) entityhuman.height, entityhuman.locZ, f, f1);
        }
    }

    // CobelPvP start - hack to silence sounds from cancelled block place
    private boolean interceptSounds = false;
    private final List<Runnable> interceptedSounds = new ArrayList<Runnable>();
    public void interceptSounds() {
        interceptSounds = true;
    }
    public void sendInterceptedSounds() {
        for (Runnable r : interceptedSounds) {
            r.run();
        }
        interceptedSounds.clear();
        interceptSounds = false;
    }
    public void clearInterceptedSounds() {
        interceptedSounds.clear();
        interceptSounds = false;
    }
    public void makeSound(final double d0, final double d1, final double d2, final String s, final float f, final float f1) {
        if (interceptSounds && org.bukkit.Bukkit.isPrimaryThread()) {
            interceptedSounds.add(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < World.this.u.size(); ++i) {
                        ((IWorldAccess) World.this.u.get(i)).a(s, d0, d1, d2, f, f1);
                    }
                }
            });
            return;
        }
        // CobelPvP end
        for (int i = 0; i < this.u.size(); ++i) {
            ((IWorldAccess) this.u.get(i)).a(s, d0, d1, d2, f, f1);
        }
    }

    public void a(double d0, double d1, double d2, String s, float f, float f1, boolean flag) {}

    public void a(String s, int i, int j, int k) {
        for (int l = 0; l < this.u.size(); ++l) {
            ((IWorldAccess) this.u.get(l)).a(s, i, j, k);
        }
    }

    public void addParticle(String s, double d0, double d1, double d2, double d3, double d4, double d5) {
        for (int i = 0; i < this.u.size(); ++i) {
            ((IWorldAccess) this.u.get(i)).a(s, d0, d1, d2, d3, d4, d5);
        }
    }

    public boolean strikeLightning(Entity entity) {
        this.i.add(entity);
        return true;
    }

    public boolean addEntity(Entity entity) {
        // CraftBukkit start - Used for entities other than creatures
        return this.addEntity(entity, SpawnReason.DEFAULT); // Set reason as DEFAULT
    }

    public boolean addEntity(Entity entity, SpawnReason spawnReason) { // Changed signature, added SpawnReason
        org.spigotmc.AsyncCatcher.catchOp( "entity add"); // Spigot
        if (entity == null) return false;
        // CraftBukkit end

        int i = (int) FastMath.floor(entity.locX / 16.0D);
        int j = (int) FastMath.floor(entity.locZ / 16.0D);
        boolean flag = entity.attachedToPlayer;

        if (entity instanceof EntityHuman) {
            flag = true;
        }

        // CraftBukkit start
        org.bukkit.event.Cancellable event = null;
        if (entity instanceof EntityLiving && !(entity instanceof EntityPlayer)) {
            boolean isAnimal = entity instanceof EntityAnimal || entity instanceof EntityWaterAnimal || entity instanceof EntityGolem;
            boolean isMonster = entity instanceof EntityMonster || entity instanceof EntityGhast || entity instanceof EntitySlime;

            if (spawnReason != SpawnReason.CUSTOM) {
                if (isAnimal && !allowAnimals || isMonster && !allowMonsters)  {
                    entity.dead = true;
                    return false;
                }
            }

            event = CraftEventFactory.callCreatureSpawnEvent((EntityLiving) entity, spawnReason);
        } else if (entity instanceof EntityItem) {
            event = CraftEventFactory.callItemSpawnEvent((EntityItem) entity);
        } else if (entity.getBukkitEntity() instanceof org.bukkit.entity.Projectile) {
            // Not all projectiles extend EntityProjectile, so check for Bukkit interface instead
            event = CraftEventFactory.callProjectileLaunchEvent(entity);
        }
        // Spigot start
        else if (entity instanceof EntityExperienceOrb) {
            EntityExperienceOrb xp = (EntityExperienceOrb) entity;
            double radius = spigotConfig.expMerge;
            if (radius > 0) {
                List<Entity> entities = this.getEntities(entity, entity.boundingBox.grow(radius, radius, radius));
                for (Entity e : entities) {
                    if (e instanceof EntityExperienceOrb) {
                        EntityExperienceOrb loopItem = (EntityExperienceOrb) e;
                        if (!loopItem.dead) {
                            xp.value += loopItem.value;
                            loopItem.die();
                        }
                    }
                }
            }
        } // Spigot end

        if (event != null && (event.isCancelled() || entity.dead)) {
            entity.dead = true;
            return false;
        }
        // CraftBukkit end

        if (!flag && !this.isChunkLoaded(i, j)) {
            entity.dead = true; // CraftBukkit
            return false;
        } else {
            if (entity instanceof EntityHuman) {
                EntityHuman entityhuman = (EntityHuman) entity;

                this.players.add(entityhuman);
                this.playerMap.add((EntityPlayer) entityhuman); // CobelPvP 
                this.everyoneSleeping();
                this.b(entity);
            }

            this.getChunkAt(i, j).a(entity);
            this.entityList.add(entity);
            this.a(entity);
            return true;
        }
    }

    protected void a(Entity entity) {
        for (int i = 0; i < this.u.size(); ++i) {
            ((IWorldAccess) this.u.get(i)).a(entity);
        }

        entity.valid = true; // CraftBukkit
    }

    protected void b(Entity entity) {
        for (int i = 0; i < this.u.size(); ++i) {
            ((IWorldAccess) this.u.get(i)).b(entity);
        }

        entity.valid = false; // CraftBukkit
    }

    public void kill(Entity entity) {
        if (entity.passenger != null) {
            entity.passenger.mount((Entity) null);
        }

        if (entity.vehicle != null) {
            entity.mount((Entity) null);
        }

        entity.die();
        if (entity instanceof EntityHuman) {
            this.players.remove(entity);
            this.playerMap.remove((EntityPlayer) entity); // CobelPvP
            // Spigot start
            for ( Object o : worldMaps.c )
            {
                if ( o instanceof WorldMap )
                {
                    WorldMap map = (WorldMap) o;
                    map.i.remove( entity );
                    for ( Iterator<WorldMapHumanTracker> iter = (Iterator<WorldMapHumanTracker>) map.f.iterator(); iter.hasNext(); )
                    {
                        if ( iter.next().trackee == entity )
                        {
                            iter.remove();
                        }
                    }
                }
            }
            // Spigot end
            this.everyoneSleeping();
        }
    }

    public void removeEntity(Entity entity) {
        org.spigotmc.AsyncCatcher.catchOp( "entity remove"); // Spigot
        entity.die();
        if (entity instanceof EntityHuman) {
            this.players.remove(entity);
            this.playerMap.remove((EntityPlayer) entity); // CobelPvP
            this.everyoneSleeping();
        }
        // Spigot start
        if (!guardEntityList) { // It will get removed after the tick if we are ticking
            int i = entity.ah;
            int j = entity.aj;
            if (entity.ag && this.isChunkLoaded(i, j)) {
                this.getChunkAt(i, j).b(entity);
            }
            // CraftBukkit start - Decrement loop variable field if we've already ticked this entity
            int index = this.entityList.indexOf(entity);
            if (index != -1) {
                if (index <= this.tickPosition) {
                    this.tickPosition--;
                }
                this.entityList.remove(index);
            }
            // CraftBukkit end
        }
        // Spigot end

        this.b(entity);
    }

    public void addIWorldAccess(IWorldAccess iworldaccess) {
        this.u.add(iworldaccess);
    }

    public List getCubes(Entity entity, AxisAlignedBB axisalignedbb) {
        this.L.clear();
        int i = (int) FastMath.floor(axisalignedbb.a);
        int j = (int) FastMath.floor(axisalignedbb.d + 1.0D);
        int k = (int) FastMath.floor(axisalignedbb.b);
        int l = (int) FastMath.floor(axisalignedbb.e + 1.0D);
        int i1 = (int) FastMath.floor(axisalignedbb.c);
        int j1 = (int) FastMath.floor(axisalignedbb.f + 1.0D);

        // Spigot start
        int ystart = ( ( k - 1 ) < 0 ) ? 0 : ( k - 1 );
        for ( int chunkx = ( i >> 4 ); chunkx <= ( ( j - 1 ) >> 4 ); chunkx++ )
        {
            int cx = chunkx << 4;
            for ( int chunkz = ( i1 >> 4 ); chunkz <= ( ( j1 - 1 ) >> 4 ); chunkz++ )
            {
                if ( !this.isChunkLoaded( chunkx, chunkz ) )
                {
                    entity.inUnloadedChunk = true; // PaperSpigot - Remove entities in unloaded chunks
                    continue;
                }
                int cz = chunkz << 4;
                Chunk chunk = this.getChunkAt( chunkx, chunkz );
                // Compute ranges within chunk
                int xstart = ( i < cx ) ? cx : i;
                int xend = ( j < ( cx + 16 ) ) ? j : ( cx + 16 );
                int zstart = ( i1 < cz ) ? cz : i1;
                int zend = ( j1 < ( cz + 16 ) ) ? j1 : ( cz + 16 );
                // Loop through blocks within chunk
                for ( int x = xstart; x < xend; x++ )
                {
                    for ( int z = zstart; z < zend; z++ )
                    {
                        for ( int y = ystart; y < l; y++ )
                        {
                            Block block = chunk.getType(x - cx, y, z - cz );
                            if ( block != null && block != Blocks.AIR) // CobelPvP
                            {
                                // PaperSpigot start - FallingBlocks and TNT collide with specific non-collidable blocks
                                if (entity.world.paperSpigotConfig.fallingBlocksCollideWithSigns && (entity instanceof EntityTNTPrimed || entity instanceof EntityFallingBlock) &&
                                        (block instanceof BlockSign || block instanceof BlockFenceGate || block instanceof BlockTorch || block instanceof BlockButtonAbstract || block instanceof BlockLever || block instanceof BlockTripwireHook || block instanceof BlockTripwire)) {
                                    AxisAlignedBB aabb = AxisAlignedBB.a(x, y, z, x + 1.0, y + 1.0, z + 1.0);
                                    if (axisalignedbb.b(aabb)) this.L.add(aabb);
                                } else {
                                    block.a( this, x, y, z, axisalignedbb, this.L, entity );
                                }
                                // PaperSpigot end
                            }
                        }
                    }
                }
            }
        }
        // Spigot end

        /*double d0 = 0.25D;
        List list = this.getEntities(entity, axisalignedbb.grow(d0, d0, d0));

        for (int j2 = 0; j2 < list.size(); ++j2) {
            AxisAlignedBB axisalignedbb1 = ((Entity) list.get(j2)).J();

            if (axisalignedbb1 != null && axisalignedbb1.b(axisalignedbb)) {
                this.L.add(axisalignedbb1);
            }

            axisalignedbb1 = entity.h((Entity) list.get(j2));
            if (axisalignedbb1 != null && axisalignedbb1.b(axisalignedbb)) {
                this.L.add(axisalignedbb1);
            }
        }*/

        return this.L;
    }

    public List a(AxisAlignedBB axisalignedbb) {
        this.L.clear();
        int i = (int) FastMath.floor(axisalignedbb.a);
        int j = (int) FastMath.floor(axisalignedbb.d + 1.0D);
        int k = (int) FastMath.floor(axisalignedbb.b);
        int l = (int) FastMath.floor(axisalignedbb.e + 1.0D);
        int i1 = (int) FastMath.floor(axisalignedbb.c);
        int j1 = (int) FastMath.floor(axisalignedbb.f + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                if (this.isLoaded(k1, 64, l1)) {
                    for (int i2 = k - 1; i2 < l; ++i2) {
                        Block block;

                        if (k1 >= -30000000 && k1 < 30000000 && l1 >= -30000000 && l1 < 30000000) {
                            block = this.getType(k1, i2, l1);
                        } else {
                            block = Blocks.BEDROCK;
                        }

                        if (block != Blocks.AIR) block.a(this, k1, i2, l1, axisalignedbb, this.L, (Entity) null);
                    }
                }
            }
        }

        return this.L;
    }

    public int a(float f) {
        float f1 = this.c(f);
        float f2 = 1.0F - ((int) FastMath.cos(f1 * 3.1415927F * 2.0F) * 2.0F + 0.5F);

        if (f2 < 0.0F) {
            f2 = 0.0F;
        }

        if (f2 > 1.0F) {
            f2 = 1.0F;
        }

        f2 = 1.0F - f2;
        f2 = (float) ((double) f2 * (1.0D - (double) (this.j(f) * 5.0F) / 16.0D));
        f2 = (float) ((double) f2 * (1.0D - (double) (this.h(f) * 5.0F) / 16.0D));
        f2 = 1.0F - f2;
        return (int) (f2 * 11.0F);
    }

    public float c(float f) {
        return this.worldProvider.a(this.worldData.getDayTime(), f);
    }

    public float y() {
        return WorldProvider.a[this.worldProvider.a(this.worldData.getDayTime())];
    }

    public float d(float f) {
        float f1 = this.c(f);

        return f1 * 3.1415927F * 2.0F;
    }

    public int h(int i, int j) {
        return this.getChunkAtWorldCoords(i, j).d(i & 15, j & 15);
    }

    public int i(int i, int j) {
        Chunk chunk = this.getChunkAtWorldCoords(i, j);
        int k = chunk.h() + 15;

        i &= 15;

        for (j &= 15; k > 0; --k) {
            Block block = chunk.getType(i, k, j);

            if (block.getMaterial().isSolid() && block.getMaterial() != Material.LEAVES) {
                return k + 1;
            }
        }

        return -1;
    }

    public void a(int i, int j, int k, Block block, int l) {}

    public void a(int i, int j, int k, Block block, int l, int i1) {}

    public void b(int i, int j, int k, Block block, int l, int i1) {}

    public void tickEntities() {
        this.methodProfiler.a("entities");
        this.methodProfiler.a("global");

        int i;
        Entity entity;
        CrashReport crashreport;
        CrashReportSystemDetails crashreportsystemdetails;

        for (i = 0; i < this.i.size(); ++i) {
            entity = (Entity) this.i.get(i);
            // CraftBukkit start - Fixed an NPE
            if (entity == null) {
                continue;
            }
            // CraftBukkit end

            try {
                ++entity.ticksLived;
                entity.h();
            } catch (Throwable throwable) {
                crashreport = CrashReport.a(throwable, "Ticking entity");
                crashreportsystemdetails = crashreport.a("Entity being ticked");
                if (entity == null) {
                    crashreportsystemdetails.a("Entity", "~~NULL~~");
                } else {
                    entity.a(crashreportsystemdetails);
                }

                throw new ReportedException(crashreport);
            }

            if (entity.dead) {
                this.i.remove(i--);
            }
        }

        this.methodProfiler.c("remove");
        this.entityList.removeAll(this.f);

        int j;
        int k;

        for (i = 0; i < this.f.size(); ++i) {
            entity = (Entity) this.f.get(i);
            j = entity.ah;
            k = entity.aj;
            if (entity.ag && this.isChunkLoaded(j, k)) {
                this.getChunkAt(j, k).b(entity);
            }
        }

        for (i = 0; i < this.f.size(); ++i) {
            this.b((Entity) this.f.get(i));
        }

        this.f.clear();
        this.methodProfiler.c("regular");

        org.spigotmc.ActivationRange.activateEntities(this); // Spigot
        timings.entityTick.startTiming(); // Spigot
        guardEntityList = true; // Spigot
        // CraftBukkit start - Use field for loop variable
        for (this.tickPosition = 0; this.tickPosition < this.entityList.size(); ++this.tickPosition) {
            entity = (Entity) this.entityList.get(this.tickPosition);
            if (entity.vehicle != null) {
                if (!entity.vehicle.dead && entity.vehicle.passenger == entity) {
                    continue;
                }

                entity.vehicle.passenger = null;
                entity.vehicle = null;
            }

            this.methodProfiler.a("tick");
            if (!entity.dead) {
                try {
                    SpigotTimings.tickEntityTimer.startTiming(); // Spigot
                    this.playerJoinedWorld(entity);
                    SpigotTimings.tickEntityTimer.stopTiming(); // Spigot
                } catch (Throwable throwable1) {
                    // PaperSpigot start
                    SpigotTimings.tickEntityTimer.stopTiming(); // Spigot
                    System.err.println("Entity threw exception at " + entity.world.getWorld().getName()+":"+entity.locX +"," + entity.locY+","+entity.locZ);
                    throwable1.printStackTrace();
                    entity.dead = true;
                    continue;
                    /*
                    crashreport = CrashReport.a(throwable1, "Ticking entity");
                    crashreportsystemdetails = crashreport.a("Entity being ticked");
                    entity.a(crashreportsystemdetails);
                    throw new ReportedException(crashreport);
                    */
                    // PaperSpigot end
                }
            }

            this.methodProfiler.b();
            this.methodProfiler.a("remove");
            if (entity.dead) {
                j = entity.ah;
                k = entity.aj;
                if (entity.ag && this.isChunkLoaded(j, k)) {
                    this.getChunkAt(j, k).b(entity);
                }

                guardEntityList = false; // Spigot
                this.entityList.remove(this.tickPosition--); // CraftBukkit - Use field for loop variable
                guardEntityList = true; // Spigot
                this.b(entity);
            }

            this.methodProfiler.b();
        }
        guardEntityList = false; // Spigot

        timings.entityTick.stopTiming(); // Spigot
        this.methodProfiler.c("blockEntities");
        timings.tileEntityTick.startTiming(); // Spigot
        this.M = true;
        // CraftBukkit start - From below, clean up tile entities before ticking them
        if (!this.b.isEmpty()) {
            this.tileEntityList.removeAll(this.b);
            this.b.clear();
        }
        // Spigot End

        this.initializeHoppers(); // Spigot - Initializes hoppers which have been added recently.
        Iterator iterator = this.tileEntityList.iterator();

        while (iterator.hasNext()) {
            TileEntity tileentity = (TileEntity) iterator.next();
            // Spigot start
            if (tileentity == null) {
                getServer().getLogger().severe("Spigot has detected a null entity and has removed it, preventing a crash");
                iterator.remove();
                continue;
            }
            // Spigot end

            if (!tileentity.r() && tileentity.o() && this.isLoaded(tileentity.x, tileentity.y, tileentity.z)) {
                try {
                    tileentity.tickTimer.startTiming(); // Spigot
                    tileentity.h();
                    tileentity.tickTimer.stopTiming(); // Spigot
                } catch (Throwable throwable2) {
                    // PaperSpigot start
                    tileentity.tickTimer.stopTiming(); // Spigot
                    System.err.println("TileEntity threw exception at " + tileentity.world.getWorld().getName()+":"+tileentity.x +"," + tileentity.y+","+tileentity.z);
                    throwable2.printStackTrace();
                    iterator.remove();
                    continue;
                    /*
                    crashreport = CrashReport.a(throwable2, "Ticking block entity");
                    crashreportsystemdetails = crashreport.a("Block entity being ticked");
                    tileentity.a(crashreportsystemdetails);
                    throw new ReportedException(crashreport);
                    */
                    // PaperSpigot end
                }
            }

            if (tileentity.r()) {
                iterator.remove();
                if (this.isChunkLoaded(tileentity.x >> 4, tileentity.z >> 4)) {
                    Chunk chunk = this.getChunkAt(tileentity.x >> 4, tileentity.z >> 4);

                    if (chunk != null) {
                        chunk.f(tileentity.x & 15, tileentity.y, tileentity.z & 15);
                    }
                }
            }
        }

        timings.tileEntityTick.stopTiming(); // Spigot
        timings.tileEntityPending.startTiming(); // Spigot
        this.M = false;
        /* CraftBukkit start - Moved up
        if (!this.b.isEmpty()) {
            this.tileEntityList.removeAll(this.b);
            this.b.clear();
        }
        */ // CraftBukkit end

        this.methodProfiler.c("pendingBlockEntities");
        if (!this.a.isEmpty()) {
            for (int l = 0; l < this.a.size(); ++l) {
                TileEntity tileentity1 = (TileEntity) this.a.get(l);

                if (!tileentity1.r()) {
                    /* CraftBukkit start - Order matters, moved down
                    if (!this.tileEntityList.contains(tileentity1)) {
                        this.tileEntityList.add(tileentity1);
                    }
                    // CraftBukkit end */

                    if (this.isChunkLoaded(tileentity1.x >> 4, tileentity1.z >> 4)) {
                        Chunk chunk1 = this.getChunkAt(tileentity1.x >> 4, tileentity1.z >> 4);

                        if (chunk1 != null) {
                            chunk1.a(tileentity1.x & 15, tileentity1.y, tileentity1.z & 15, tileentity1);
                            // CraftBukkit start - Moved down from above
                            if (!this.tileEntityList.contains(tileentity1)) {
                                this.tileEntityList.add(tileentity1);
                            }
                            // CraftBukkit end
                        }
                    }

                    this.notify(tileentity1.x, tileentity1.y, tileentity1.z);
                }
            }

            this.a.clear();
        }

        timings.tileEntityPending.stopTiming(); // Spigot
        this.methodProfiler.b();
        this.methodProfiler.b();
    }

    public void a(Collection collection) {
        if (this.M) {
            this.a.addAll(collection);
        } else {
            this.tileEntityList.addAll(collection);
        }
    }

    public void playerJoinedWorld(Entity entity) {
        this.entityJoinedWorld(entity, true);
    }

    public void entityJoinedWorld(Entity entity, boolean flag) {
        int i = (int) FastMath.floor(entity.locX);
        int j = (int) FastMath.floor(entity.locZ);

        MinecraftServer.getServer().entities++; // Kohi

        // Spigot start
        if (!org.spigotmc.ActivationRange.checkIfActive(entity)) {
            entity.ticksLived++;
            entity.inactiveTick();
            // PaperSpigot start - Remove entities in unloaded chunks
            if (!this.isChunkLoaded(i, j) && ((entity instanceof EntityEnderPearl && this.paperSpigotConfig.removeUnloadedEnderPearls) ||
                    (entity instanceof EntityFallingBlock && this.paperSpigotConfig.removeUnloadedFallingBlocks) ||
                    (entity instanceof EntityTNTPrimed && this.paperSpigotConfig.removeUnloadedTNTEntities))) {
                entity.inUnloadedChunk = true;
                entity.die();
            }
            // PaperSpigot end
        } else {
            MinecraftServer.getServer().activeEntities++; // Kohi
            entity.tickTimer.startTiming(); // Spigot
            // CraftBukkit end
            entity.S = entity.locX;
            entity.T = entity.locY;
            entity.U = entity.locZ;
            entity.lastYaw = entity.yaw;
            entity.lastPitch = entity.pitch;
            if (flag && entity.ag) {
                ++entity.ticksLived;
                if (entity.vehicle != null) {
                    entity.ab();
                } else {
                    entity.h();
                }
            }

            this.methodProfiler.a("chunkCheck");
            if (Double.isNaN(entity.locX) || Double.isInfinite(entity.locX)) {
                entity.locX = entity.S;
            }

            if (Double.isNaN(entity.locY) || Double.isInfinite(entity.locY)) {
                entity.locY = entity.T;
            }

            if (Double.isNaN(entity.locZ) || Double.isInfinite(entity.locZ)) {
                entity.locZ = entity.U;
            }

            if (Double.isNaN((double) entity.pitch) || Double.isInfinite((double) entity.pitch)) {
                entity.pitch = entity.lastPitch;
            }

            if (Double.isNaN((double) entity.yaw) || Double.isInfinite((double) entity.yaw)) {
                entity.yaw = entity.lastYaw;
            }

            int k = (int) FastMath.floor(entity.locX / 16.0D);
            int l = Math.min(15, Math.max(0, MathHelper.floor(entity.locY / 16.0D)));
            int i1 = (int) FastMath.floor(entity.locZ / 16.0D);

            if (!entity.ag || entity.ah != k || entity.ai != l || entity.aj != i1) {
                if (entity.loadChunks) entity.loadChunks(); // PaperSpigot - Force load chunks
                if (entity.ag && this.isChunkLoaded(entity.ah, entity.aj)) {
                    this.getChunkAt(entity.ah, entity.aj).a(entity, entity.ai);
                }

                final boolean loaded = entity.ag = this.isChunkLoaded(k, i1);
                if (loaded) {
                    this.getChunkAt(k, i1).a(entity);
                }
            }

            this.methodProfiler.b();
            if (flag && entity.ag && entity.passenger != null) {
                if (!entity.passenger.dead && entity.passenger.vehicle == entity) {
                    this.playerJoinedWorld(entity.passenger);
                } else {
                    entity.passenger.vehicle = null;
                    entity.passenger = null;
                }
            }
            entity.tickTimer.stopTiming(); // Spigot
        }
    }

    public boolean b(AxisAlignedBB axisalignedbb) {
        return this.a(axisalignedbb, (Entity) null);
    }

    public boolean a(AxisAlignedBB axisalignedbb, Entity entity) {
        List list = this.getEntities((Entity) null, axisalignedbb);

        for (int i = 0; i < list.size(); ++i) {
            Entity entity1 = (Entity) list.get(i);
            // PaperSpigot start - Allow block placement if the placer cannot see the blocker
            if (entity instanceof EntityPlayer && entity1 instanceof EntityPlayer) {
                if (!((EntityPlayer) entity).getBukkitEntity().canSee(((EntityPlayer) entity1).getBukkitEntity())) {
                    continue;
                }
            }
            // PaperSpigot end

            if (!entity1.dead && entity1.k && entity1 != entity) {
                return false;
            }
        }

        return true;
    }

    public boolean c(AxisAlignedBB axisalignedbb) {
        int i = (int) FastMath.floor(axisalignedbb.a);
        int j = (int) FastMath.floor(axisalignedbb.d + 1.0D);
        int k = (int) FastMath.floor(axisalignedbb.b);
        int l = (int) FastMath.floor(axisalignedbb.e + 1.0D);
        int i1 = (int) FastMath.floor(axisalignedbb.c);
        int j1 = (int) FastMath.floor(axisalignedbb.f + 1.0D);

        if (axisalignedbb.a < 0.0D) {
            --i;
        }

        if (axisalignedbb.b < 0.0D) {
            --k;
        }

        if (axisalignedbb.c < 0.0D) {
            --i1;
        }

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    Block block = this.getType(k1, l1, i2);

                    if (block.getMaterial() != Material.AIR) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean containsLiquid(AxisAlignedBB axisalignedbb) {
        int i = (int) FastMath.floor(axisalignedbb.a);
        int j = (int) FastMath.floor(axisalignedbb.d + 1.0D);
        int k = (int) FastMath.floor(axisalignedbb.b);
        int l = (int) FastMath.floor(axisalignedbb.e + 1.0D);
        int i1 = (int) FastMath.floor(axisalignedbb.c);
        int j1 = (int) FastMath.floor(axisalignedbb.f + 1.0D);

        if (axisalignedbb.a < 0.0D) {
            --i;
        }

        if (axisalignedbb.b < 0.0D) {
            --k;
        }

        if (axisalignedbb.c < 0.0D) {
            --i1;
        }

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    Block block = this.getType(k1, l1, i2);

                    if (block.getMaterial().isLiquid()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean e(AxisAlignedBB axisalignedbb) {
        int i = (int) FastMath.floor(axisalignedbb.a);
        int j = (int) FastMath.floor(axisalignedbb.d + 1.0D);
        int k = (int) FastMath.floor(axisalignedbb.b);
        int l = (int) FastMath.floor(axisalignedbb.e + 1.0D);
        int i1 = (int) FastMath.floor(axisalignedbb.c);
        int j1 = (int) FastMath.floor(axisalignedbb.f + 1.0D);

        if (this.b(i, k, i1, j, l, j1)) {
            for (int k1 = i; k1 < j; ++k1) {
                for (int l1 = k; l1 < l; ++l1) {
                    for (int i2 = i1; i2 < j1; ++i2) {
                        Block block = this.getType(k1, l1, i2);

                        if (block == Blocks.FIRE || block == Blocks.LAVA || block == Blocks.STATIONARY_LAVA) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean a(AxisAlignedBB axisalignedbb, Material material, Entity entity) {
        int i = (int) FastMath.floor(axisalignedbb.a);
        int j = (int) FastMath.floor(axisalignedbb.d + 1.0D);
        int k = (int) FastMath.floor(axisalignedbb.b);
        int l = (int) FastMath.floor(axisalignedbb.e + 1.0D);
        int i1 = (int) FastMath.floor(axisalignedbb.c);
        int j1 = (int) FastMath.floor(axisalignedbb.f + 1.0D);

        if (!this.b(i, k, i1, j, l, j1)) {
            return false;
        } else {
            boolean flag = false;
            Vec3D vec3d = Vec3D.a(0.0D, 0.0D, 0.0D);

            for (int k1 = i; k1 < j; ++k1) {
                for (int l1 = k; l1 < l; ++l1) {
                    for (int i2 = i1; i2 < j1; ++i2) {
                        Block block = this.getType(k1, l1, i2);

                        if (block.getMaterial() == material) {
                            double d0 = (double) ((float) (l1 + 1) - BlockFluids.b(this.getData(k1, l1, i2)));

                            if ((double) l >= d0) {
                                flag = true;
                                block.a(this, k1, l1, i2, entity, vec3d);
                            }
                        }
                    }
                }
            }

            if (vec3d.b() > 0.0D && entity.aC()) {
                vec3d = vec3d.a();
                double d1 = 0.014D;

                entity.motX += vec3d.a * d1;
                entity.motY += vec3d.b * d1;
                entity.motZ += vec3d.c * d1;
            }

            return flag;
        }
    }

    public boolean a(AxisAlignedBB axisalignedbb, Material material) {
    // CobelPvP start
        return this.a(axisalignedbb, material, this);
    }

    public boolean a(AxisAlignedBB axisalignedbb, Material material, IBlockAccess iblockaccess) {
    // CobelPvP end
        int i = (int) FastMath.floor(axisalignedbb.a);
        int j = (int) FastMath.floor(axisalignedbb.d + 1.0D);
        int k = (int) FastMath.floor(axisalignedbb.b);
        int l = (int) FastMath.floor(axisalignedbb.e + 1.0D);
        int i1 = (int) FastMath.floor(axisalignedbb.c);
        int j1 = (int) FastMath.floor(axisalignedbb.f + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    if (iblockaccess.getType(k1, l1, i2).getMaterial() == material) { // CobelPvP
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // Alfie start
    public boolean boundingBoxContainsMaterials(AxisAlignedBB boundingBox, Set<Block> matching) {
        int i = (int) FastMath.floor(boundingBox.a);
        int j = (int) FastMath.floor(boundingBox.d + 1.0D);
        int k = (int) FastMath.floor(boundingBox.b);
        int l = (int) FastMath.floor(boundingBox.e + 1.0D);
        int i1 = (int) FastMath.floor(boundingBox.c);
        int j1 = (int) FastMath.floor(boundingBox.f + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    if (matching.contains(getType(k1, l1, i2))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    // Alfie end

    public boolean b(AxisAlignedBB axisalignedbb, Material material) {
        int i = (int) FastMath.floor(axisalignedbb.a);
        int j = (int) FastMath.floor(axisalignedbb.d + 1.0D);
        int k = (int) FastMath.floor(axisalignedbb.b);
        int l = (int) FastMath.floor(axisalignedbb.e + 1.0D);
        int i1 = (int) FastMath.floor(axisalignedbb.c);
        int j1 = (int) FastMath.floor(axisalignedbb.f + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    Block block = this.getType(k1, l1, i2);

                    if (block.getMaterial() == material) {
                        int j2 = this.getData(k1, l1, i2);
                        double d0 = (double) (l1 + 1);

                        if (j2 < 8) {
                            d0 = (double) (l1 + 1) - (double) j2 / 8.0D;
                        }

                        if (d0 >= axisalignedbb.b) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public Explosion explode(Entity entity, double d0, double d1, double d2, float f, boolean flag) {
        return this.createExplosion(entity, d0, d1, d2, f, false, flag);
    }

    public Explosion createExplosion(Entity entity, double d0, double d1, double d2, float f, boolean flag, boolean flag1) {
        Explosion explosion = new Explosion(this, entity, d0, d1, d2, f);

        explosion.a = flag;
        explosion.b = flag1;
        explosion.a();
        explosion.a(true);
        return explosion;
    }

    public float a(Vec3D vec3d, AxisAlignedBB axisalignedbb) {
        double d0 = 1.0D / ((axisalignedbb.d - axisalignedbb.a) * 2.0D + 1.0D);
        double d1 = 1.0D / ((axisalignedbb.e - axisalignedbb.b) * 2.0D + 1.0D);
        double d2 = 1.0D / ((axisalignedbb.f - axisalignedbb.c) * 2.0D + 1.0D);

        // PaperSpigot start - Center TNT sample points for more accurate calculations
        // Shift the sample points so they are centered on the BB
        double xOffset = (1.0 - Math.floor(1.0 / d0) * d0) / 2.0;
        double zOffset = (1.0 - Math.floor(1.0 / d2) * d2) / 2.0;
        // PaperSpigot end

        if (d0 >= 0.0D && d1 >= 0.0D && d2 >= 0.0D) {
            int i = 0;
            int j = 0;

            Vec3D vec3d2 = vec3d.a(0, 0, 0); // CraftBukkit
            for (float f = 0.0F; f <= 1.0F; f = (float) ((double) f + d0)) {
                for (float f1 = 0.0F; f1 <= 1.0F; f1 = (float) ((double) f1 + d1)) {
                    for (float f2 = 0.0F; f2 <= 1.0F; f2 = (float) ((double) f2 + d2)) {
                        double d3 = axisalignedbb.a + (axisalignedbb.d - axisalignedbb.a) * (double) f;
                        double d4 = axisalignedbb.b + (axisalignedbb.e - axisalignedbb.b) * (double) f1;
                        double d5 = axisalignedbb.c + (axisalignedbb.f - axisalignedbb.c) * (double) f2;

                        if (this.a(vec3d2.b(xOffset + d3, d4, zOffset + d5), vec3d) == null) { // CraftBukkit // PaperSpigot
                            ++i;
                        }

                        ++j;
                    }
                }
            }

            return (float) i / (float) j;
        } else {
            return 0.0F;
        }
    }

    public boolean douseFire(EntityHuman entityhuman, int i, int j, int k, int l) {
        if (l == 0) {
            --j;
        }

        if (l == 1) {
            ++j;
        }

        if (l == 2) {
            --k;
        }

        if (l == 3) {
            ++k;
        }

        if (l == 4) {
            --i;
        }

        if (l == 5) {
            ++i;
        }

        if (this.getType(i, j, k) == Blocks.FIRE) {
            this.a(entityhuman, 1004, i, j, k, 0);
            this.setAir(i, j, k);
            return true;
        } else {
            return false;
        }
    }

    public TileEntity getTileEntity(int i, int j, int k) {
        if (j >= 0 && j < 256) {
            TileEntity tileentity = null;
            int l;
            TileEntity tileentity1;

            if (this.M) {
                for (l = 0; l < this.a.size(); ++l) {
                    tileentity1 = (TileEntity) this.a.get(l);
                    if (!tileentity1.r() && tileentity1.x == i && tileentity1.y == j && tileentity1.z == k) {
                        tileentity = tileentity1;
                        break;
                    }
                }
            }

            if (tileentity == null) {
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                if (chunk != null) {
                    tileentity = chunk.e(i & 15, j, k & 15);
                }
            }

            if (tileentity == null) {
                for (l = 0; l < this.a.size(); ++l) {
                    tileentity1 = (TileEntity) this.a.get(l);
                    if (!tileentity1.r() && tileentity1.x == i && tileentity1.y == j && tileentity1.z == k) {
                        tileentity = tileentity1;
                        break;
                    }
                }
            }

            return tileentity;
        } else {
            return null;
        }
    }

    public void setTileEntity(int i, int j, int k, TileEntity tileentity) {
        if (tileentity != null && !tileentity.r()) {
            if (this.M) {
                tileentity.x = i;
                tileentity.y = j;
                tileentity.z = k;
                Iterator iterator = this.a.iterator();

                while (iterator.hasNext()) {
                    TileEntity tileentity1 = (TileEntity) iterator.next();

                    if (tileentity1.x == i && tileentity1.y == j && tileentity1.z == k) {
                        tileentity1.s();
                        iterator.remove();
                    }
                }

                tileentity.a(this); // Spigot - No null worlds
                this.a.add(tileentity);
            } else {
                this.tileEntityList.add(tileentity);
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                if (chunk != null) {
                    chunk.a(i & 15, j, k & 15, tileentity);
                }
            }
        }
    }

    public void p(int i, int j, int k) {
        TileEntity tileentity = this.getTileEntity(i, j, k);

        if (tileentity != null && this.M) {
            tileentity.s();
            this.a.remove(tileentity);
        } else {
            if (tileentity != null) {
                this.a.remove(tileentity);
                this.tileEntityList.remove(tileentity);
            }

            Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

            if (chunk != null) {
                chunk.f(i & 15, j, k & 15);
            }
        }
    }

    public void a(TileEntity tileentity) {
        this.b.add(tileentity);
    }

    public boolean q(int i, int j, int k) {
        AxisAlignedBB axisalignedbb = this.getType(i, j, k).a(this, i, j, k);

        return axisalignedbb != null && axisalignedbb.a() >= 1.0D;
    }

    public static boolean a(IBlockAccess iblockaccess, int i, int j, int k) {
        Block block = iblockaccess.getType(i, j, k);
        int l = iblockaccess.getData(i, j, k);

        return block.getMaterial().k() && block.d() ? true : (block instanceof BlockStairs ? (l & 4) == 4 : (block instanceof BlockStepAbstract ? (l & 8) == 8 : (block instanceof BlockHopper ? true : (block instanceof BlockSnow ? (l & 7) == 7 : false))));
    }

    public boolean c(int i, int j, int k, boolean flag) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            Chunk chunk = this.chunkProvider.getOrCreateChunk(i >> 4, k >> 4);

            if (chunk != null && !chunk.isEmpty()) {
                Block block = this.getType(i, j, k);

                return block.getMaterial().k() && block.d();
            } else {
                return flag;
            }
        } else {
            return flag;
        }
    }

    public void B() {
        int i = this.a(1.0F);

        if (i != this.j) {
            this.j = i;
        }
    }

    public void setSpawnFlags(boolean flag, boolean flag1) {
        this.allowMonsters = flag;
        this.allowAnimals = flag1;
    }

    public void doTick() {
        this.o();
    }

    private void a() {
        if (this.worldData.hasStorm()) {
            this.n = 1.0F;
            if (this.worldData.isThundering()) {
                this.p = 1.0F;
            }
        }
    }

    protected void o() {
        if (!this.worldProvider.g) {
            if (!this.isStatic) {
                int i = this.worldData.getThunderDuration();

                if (i <= 0) {
                    if (this.worldData.isThundering()) {
                        this.worldData.setThunderDuration(this.random.nextInt(12000) + 3600);
                    } else {
                        this.worldData.setThunderDuration(this.random.nextInt(168000) + 12000);
                    }
                } else {
                    --i;
                    this.worldData.setThunderDuration(i);
                    if (i <= 0) {
                        // CraftBukkit start
                        ThunderChangeEvent thunder = new ThunderChangeEvent(this.getWorld(), !this.worldData.isThundering());
                        this.getServer().getPluginManager().callEvent(thunder);
                        if (!thunder.isCancelled()) {
                            this.worldData.setThundering(!this.worldData.isThundering());
                        }
                        // CraftBukkit end
                    }
                }

                this.o = this.p;
                if (this.worldData.isThundering()) {
                    this.p = (float) ((double) this.p + 0.01D);
                } else {
                    this.p = (float) ((double) this.p - 0.01D);
                }

                this.p = MathHelper.a(this.p, 0.0F, 1.0F);
                int j = this.worldData.getWeatherDuration();

                if (j <= 0) {
                    if (this.worldData.hasStorm()) {
                        this.worldData.setWeatherDuration(this.random.nextInt(12000) + 12000);
                    } else {
                        this.worldData.setWeatherDuration(this.random.nextInt(168000) + 12000);
                    }
                } else {
                    --j;
                    this.worldData.setWeatherDuration(j);
                    if (j <= 0) {
                        // CraftBukkit start
                        WeatherChangeEvent weather = new WeatherChangeEvent(this.getWorld(), !this.worldData.hasStorm());
                        this.getServer().getPluginManager().callEvent(weather);

                        if (!weather.isCancelled()) {
                            this.worldData.setStorm(!this.worldData.hasStorm());
                        }
                        // CraftBukkit end
                    }
                }

                this.m = this.n;
                if (this.worldData.hasStorm()) {
                    this.n = (float) ((double) this.n + 0.01D);
                } else {
                    this.n = (float) ((double) this.n - 0.01D);
                }

                this.n = MathHelper.a(this.n, 0.0F, 1.0F);
            }
        }
    }

    protected void C() {
        // this.chunkTickList.clear(); // CraftBukkit - removed
        this.methodProfiler.a("buildList");

        int i;
        EntityHuman entityhuman;
        int j;
        int k;
        int l;

        // Spigot start
        int optimalChunks = spigotConfig.chunksPerTick;
        // Quick conditions to allow us to exist early
        if ( optimalChunks <= 0 || players.isEmpty() )
        {
            return;
        }
        this.timings.doTickTiles_buildList.startTiming(); // CobelPvP
        // Keep chunks with growth inside of the optimal chunk range
        int chunksPerPlayer = Math.min( 200, Math.max( 1, (int) ( ( ( optimalChunks - players.size() ) / (double) players.size() ) + 0.5 ) ) );
        int randRange = 3 + chunksPerPlayer / 30;
        // Limit to normal tick radius - including view distance
        randRange = ( randRange > chunkTickRadius ) ? chunkTickRadius : randRange;
        // odds of growth happening vs growth happening in vanilla
        this.growthOdds = this.modifiedOdds = Math.max( 35, Math.min( 100, ( ( chunksPerPlayer + 1 ) * 100F ) / 15F ) );
        // Spigot end

        Chunk chunkObj = null; // CobelPvP

        for (i = 0; i < this.players.size(); ++i) {
            entityhuman = (EntityHuman) this.players.get(i);
            j = (int) FastMath.floor(entityhuman.locX / 16.0D);
            k = (int) FastMath.floor(entityhuman.locZ / 16.0D);
            l = this.p();

            // Spigot start - Always update the chunk the player is on
            long key = chunkToKey( j, k );
            int existingPlayers = Math.max( 0, chunkTickList.get( key ) ); // filter out -1
            chunkTickList.put(key, (short) (existingPlayers + 1));

            // Check and see if we update the chunks surrounding the player this tick
            for ( int chunk = 0; chunk < chunksPerPlayer; chunk++ )
            {
                int dx = ( random.nextBoolean() ? 1 : -1 ) * random.nextInt( randRange );
                int dz = ( random.nextBoolean() ? 1 : -1 ) * random.nextInt( randRange );
                long hash = chunkToKey( dx + j, dz + k );
                if ( !chunkTickList.contains( hash ) && ((chunkObj = this.getChunkIfLoaded(dx + j, dz + k)) != null) && chunkObj.areNeighborsLoaded(1) ) // CobelPvP
                {
                    chunkTickList.put( hash, (short) -1 ); // no players
                }
            }
            // Spigot End
        }

        this.methodProfiler.b();
        if (this.K > 0) {
            --this.K;
        }

        this.methodProfiler.a("playerCheckLight");
        if (spigotConfig.randomLightUpdates && !this.players.isEmpty()) { // Spigot
            i = this.random.nextInt(this.players.size());
            entityhuman = (EntityHuman) this.players.get(i);
            j = (int) FastMath.floor(entityhuman.locX) + this.random.nextInt(11) - 5;
            k = (int) FastMath.floor(entityhuman.locY) + this.random.nextInt(11) - 5;
            l = (int) FastMath.floor(entityhuman.locZ) + this.random.nextInt(11) - 5;
            this.t(j, k, l);
        }

        this.methodProfiler.b();
        this.timings.doTickTiles_buildList.stopTiming(); // CobelPvP
    }

    protected abstract int p();

    protected void a(int i, int j, Chunk chunk) {
        this.methodProfiler.c("moodSound");
        if (this.K == 0 && !this.isStatic) {
            this.k = this.k * 3 + 1013904223;
            int k = this.k >> 2;
            int l = k & 15;
            int i1 = k >> 8 & 15;
            int j1 = k >> 16 & 255;
            Block block = chunk.getType(l, j1, i1);

            l += i;
            i1 += j;
            if (block.getMaterial() == Material.AIR && this.j(l, j1, i1) <= this.random.nextInt(8) && this.b(EnumSkyBlock.SKY, l, j1, i1) <= 0) {
                EntityHuman entityhuman = this.findNearbyPlayer((double) l + 0.5D, (double) j1 + 0.5D, (double) i1 + 0.5D, 8.0D);

                if (entityhuman != null && entityhuman.e((double) l + 0.5D, (double) j1 + 0.5D, (double) i1 + 0.5D) > 4.0D) {
                    this.makeSound((double) l + 0.5D, (double) j1 + 0.5D, (double) i1 + 0.5D, "ambient.cave.cave", 0.7F, 0.8F + this.random.nextFloat() * 0.2F);
                    this.K = this.random.nextInt(12000) + 6000;
                }
            }
        }

        this.methodProfiler.c("checkLight");
        chunk.o();
    }

    protected void g() {
        this.C();
    }

    public boolean r(int i, int j, int k) {
        return this.d(i, j, k, false);
    }

    public boolean s(int i, int j, int k) {
        return this.d(i, j, k, true);
    }

    public boolean d(int i, int j, int k, boolean flag) {
        BiomeBase biomebase = this.getBiome(i, k);
        float f = biomebase.a(i, j, k);

        if (f > 0.15F) {
            return false;
        } else {
            if (j >= 0 && j < 256 && this.b(EnumSkyBlock.BLOCK, i, j, k) < 10) {
                Block block = this.getType(i, j, k);

                if ((block == Blocks.STATIONARY_WATER || block == Blocks.WATER) && this.getData(i, j, k) == 0) {
                    if (!flag) {
                        return true;
                    }

                    boolean flag1 = true;

                    if (flag1 && this.getType(i - 1, j, k).getMaterial() != Material.WATER) {
                        flag1 = false;
                    }

                    if (flag1 && this.getType(i + 1, j, k).getMaterial() != Material.WATER) {
                        flag1 = false;
                    }

                    if (flag1 && this.getType(i, j, k - 1).getMaterial() != Material.WATER) {
                        flag1 = false;
                    }

                    if (flag1 && this.getType(i, j, k + 1).getMaterial() != Material.WATER) {
                        flag1 = false;
                    }

                    if (!flag1) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public boolean e(int i, int j, int k, boolean flag) {
        BiomeBase biomebase = this.getBiome(i, k);
        float f = biomebase.a(i, j, k);

        if (f > 0.15F) {
            return false;
        } else if (!flag) {
            return true;
        } else {
            if (j >= 0 && j < 256 && this.b(EnumSkyBlock.BLOCK, i, j, k) < 10) {
                Block block = this.getType(i, j, k);

                if (block.getMaterial() == Material.AIR && Blocks.SNOW.canPlace(this, i, j, k)) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean t(int i, int j, int k) {
        boolean flag = false;

        if (!this.worldProvider.g) {
            flag |= this.updateLight(EnumSkyBlock.SKY, i, j, k); // PaperSpigot - Asynchronous lighting updates
        }

        flag |= this.updateLight(EnumSkyBlock.BLOCK, i, j, k); // PaperSpigot - Asynchronous lighting updates
        return flag;
    }

    private int a(int i, int j, int k, EnumSkyBlock enumskyblock) {
        if (enumskyblock == EnumSkyBlock.SKY && this.i(i, j, k)) {
            return 15;
        } else {
            Block block = this.getType(i, j, k);
            int l = enumskyblock == EnumSkyBlock.SKY ? 0 : block.m();
            int i1 = block.k();

            if (i1 >= 15 && block.m() > 0) {
                i1 = 1;
            }

            if (i1 < 1) {
                i1 = 1;
            }

            if (i1 >= 15) {
                return 0;
            } else if (l >= 14) {
                return l;
            } else {
                for (int j1 = 0; j1 < 6; ++j1) {
                    int k1 = i + Facing.b[j1];
                    int l1 = j + Facing.c[j1];
                    int i2 = k + Facing.d[j1];
                    int j2 = this.b(enumskyblock, k1, l1, i2) - i1;

                    if (j2 > l) {
                        l = j2;
                    }

                    if (l >= 14) {
                        return l;
                    }
                }

                return l;
            }
        }
    }

    public boolean c(EnumSkyBlock enumskyblock, int i, int j, int k, Chunk chunk, List<Chunk> neighbors) { // PaperSpigot
        // CraftBukkit start - Use neighbor cache instead of looking up
        //Chunk chunk = this.getChunkIfLoaded(i >> 4, k >> 4);
        if (chunk == null /*|| !chunk.areNeighborsLoaded(1)*/ /* !this.areChunksLoaded(i, j, k, 17)*/) {
            // CraftBukkit end
            return false;
        } else {
            int l = 0;
            int i1 = 0;

            this.methodProfiler.a("getBrightness");
            int j1 = this.b(enumskyblock, i, j, k);
            int k1 = this.a(i, j, k, enumskyblock);
            int l1;
            int i2;
            int j2;
            int k2;
            int l2;
            int i3;
            int j3;
            int k3;
            int l3;

            if (k1 > j1) {
                this.I[i1++] = 133152;
            } else if (k1 < j1) {
                this.I[i1++] = 133152 | j1 << 18;

                while (l < i1) {
                    l1 = this.I[l++];
                    i2 = (l1 & 63) - 32 + i;
                    j2 = (l1 >> 6 & 63) - 32 + j;
                    k2 = (l1 >> 12 & 63) - 32 + k;
                    l2 = l1 >> 18 & 15;
                    i3 = this.b(enumskyblock, i2, j2, k2);
                    if (i3 == l2) {
                        this.b(enumskyblock, i2, j2, k2, 0);
                        if (l2 > 0) {
                            j3 = MathHelper.a(i2 - i);
                            l3 = MathHelper.a(j2 - j);
                            k3 = MathHelper.a(k2 - k);
                            if (j3 + l3 + k3 < 17) {
                                for (int i4 = 0; i4 < 6; ++i4) {
                                    int j4 = i2 + Facing.b[i4];
                                    int k4 = j2 + Facing.c[i4];
                                    int l4 = k2 + Facing.d[i4];
                                    int i5 = Math.max(1, this.getType(j4, k4, l4).k());

                                    i3 = this.b(enumskyblock, j4, k4, l4);
                                    if (i3 == l2 - i5 && i1 < this.I.length) {
                                        this.I[i1++] = j4 - i + 32 | k4 - j + 32 << 6 | l4 - k + 32 << 12 | l2 - i5 << 18;
                                    }
                                }
                            }
                        }
                    }
                }

                l = 0;
            }

            this.methodProfiler.b();
            this.methodProfiler.a("checkedPosition < toCheckCount");

            while (l < i1) {
                l1 = this.I[l++];
                i2 = (l1 & 63) - 32 + i;
                j2 = (l1 >> 6 & 63) - 32 + j;
                k2 = (l1 >> 12 & 63) - 32 + k;
                l2 = this.b(enumskyblock, i2, j2, k2);
                i3 = this.a(i2, j2, k2, enumskyblock);
                if (i3 != l2) {
                    this.b(enumskyblock, i2, j2, k2, i3);
                    if (i3 > l2) {
                        j3 = Math.abs(i2 - i);
                        l3 = Math.abs(j2 - j);
                        k3 = Math.abs(k2 - k);
                        boolean flag = i1 < this.I.length - 6;

                        if (j3 + l3 + k3 < 17 && flag) {
                            if (this.b(enumskyblock, i2 - 1, j2, k2) < i3) {
                                this.I[i1++] = i2 - 1 - i + 32 + (j2 - j + 32 << 6) + (k2 - k + 32 << 12);
                            }

                            if (this.b(enumskyblock, i2 + 1, j2, k2) < i3) {
                                this.I[i1++] = i2 + 1 - i + 32 + (j2 - j + 32 << 6) + (k2 - k + 32 << 12);
                            }

                            if (this.b(enumskyblock, i2, j2 - 1, k2) < i3) {
                                this.I[i1++] = i2 - i + 32 + (j2 - 1 - j + 32 << 6) + (k2 - k + 32 << 12);
                            }

                            if (this.b(enumskyblock, i2, j2 + 1, k2) < i3) {
                                this.I[i1++] = i2 - i + 32 + (j2 + 1 - j + 32 << 6) + (k2 - k + 32 << 12);
                            }

                            if (this.b(enumskyblock, i2, j2, k2 - 1) < i3) {
                                this.I[i1++] = i2 - i + 32 + (j2 - j + 32 << 6) + (k2 - 1 - k + 32 << 12);
                            }

                            if (this.b(enumskyblock, i2, j2, k2 + 1) < i3) {
                                this.I[i1++] = i2 - i + 32 + (j2 - j + 32 << 6) + (k2 + 1 - k + 32 << 12);
                            }
                        }
                    }
                }
            }

            // PaperSpigot start - Asynchronous light updates
            if (chunk.world.paperSpigotConfig.useAsyncLighting) {
                chunk.pendingLightUpdates.decrementAndGet();
                if (neighbors != null) {
                    for (Chunk neighbor : neighbors) {
                        neighbor.pendingLightUpdates.decrementAndGet();
                    }
                }
            }
            // PaperSpigot end
            this.methodProfiler.b();
            return true;
        }
    }

    // PaperSpigot start - Asynchronous lighting updates
    public boolean updateLight(final EnumSkyBlock enumskyblock, final int x, final int y, final int z) {
        final Chunk chunk = this.getChunkIfLoaded(x >> 4, z >> 4);
        if (chunk == null || !chunk.areNeighborsLoaded(2)) { // CobelPvP - radius 2
            return false;
        }

        if (!chunk.world.paperSpigotConfig.useAsyncLighting) {
            return this.c(enumskyblock, x, y, z, chunk, null);
        }

        chunk.pendingLightUpdates.incrementAndGet();
        chunk.lightUpdateTime = chunk.world.getTime();

        final List<Chunk> neighbors = new ArrayList<Chunk>();
        // CobelPvP start
        int chunkx = chunk.locX;
        int chunkz = chunk.locZ;
        int radius = 2;
        for (int cx = chunkx - radius; cx <= chunkx + radius; ++cx) {
            for (int cz = chunkz - radius; cz <= chunkz + radius; ++cz) {
                if(cx != chunkx || cz != chunkz) {
        // CobelPvP end
                    Chunk neighbor = this.getChunkIfLoaded(cx, cz);
                    if (neighbor != null) {
                        neighbor.pendingLightUpdates.incrementAndGet();
                        neighbor.lightUpdateTime = chunk.world.getTime();
                        neighbors.add(neighbor);
                    }
                }
            }
        }

        if (!Bukkit.isPrimaryThread()) {
            return this.c(enumskyblock, x, y, z, chunk, neighbors);
        }

        // CobelPvP start
        this.lightingQueue.queueTask(new Runnable() {
            @Override
            public void run() {
                try {
                    World.this.lightingUpdater.c(enumskyblock, x, y, z, chunk, neighbors);
                } catch (Exception e) {
                    MinecraftServer.getLogger().error("Thread " + Thread.currentThread().getName() + " encountered an exception: " + e.getMessage(), e);
                }
            }
        });
        // CobelPvP end
        return true;
    }
    // PaperSpigot end

    public boolean a(boolean flag) {
        return false;
    }

    public List a(Chunk chunk, boolean flag) {
        return null;
    }

    public List getEntities(Entity entity, AxisAlignedBB axisalignedbb) {
        return this.getEntities(entity, axisalignedbb, (IEntitySelector) null);
    }

    public List getEntities(Entity entity, AxisAlignedBB axisalignedbb, IEntitySelector ientityselector) {
        ArrayList arraylist = new ArrayList();
        int i = (int) FastMath.floor((axisalignedbb.a - 2.0D) / 16.0D);
        int j = (int) FastMath.floor((axisalignedbb.d + 2.0D) / 16.0D);
        int k = (int) FastMath.floor((axisalignedbb.c - 2.0D) / 16.0D);
        int l = (int) FastMath.floor((axisalignedbb.f + 2.0D) / 16.0D);

        for (int i1 = i; i1 <= j; ++i1) {
            for (int j1 = k; j1 <= l; ++j1) {
                if (this.isChunkLoaded(i1, j1)) {
                    this.getChunkAt(i1, j1).a(entity, axisalignedbb, arraylist, ientityselector);
                }
            }
        }

        return arraylist;
    }

    public List a(Class oclass, AxisAlignedBB axisalignedbb) {
        return this.a(oclass, axisalignedbb, (IEntitySelector) null);
    }

    public List a(Class oclass, AxisAlignedBB axisalignedbb, IEntitySelector ientityselector) {
        int i = (int) FastMath.floor((axisalignedbb.a - 2.0D) / 16.0D);
        int j = (int) FastMath.floor((axisalignedbb.d + 2.0D) / 16.0D);
        int k = (int) FastMath.floor((axisalignedbb.c - 2.0D) / 16.0D);
        int l = (int) FastMath.floor((axisalignedbb.f + 2.0D) / 16.0D);
        ArrayList arraylist = new ArrayList();

        for (int i1 = i; i1 <= j; ++i1) {
            for (int j1 = k; j1 <= l; ++j1) {
                if (this.isChunkLoaded(i1, j1)) {
                    this.getChunkAt(i1, j1).a(oclass, axisalignedbb, arraylist, ientityselector);
                }
            }
        }

        return arraylist;
    }

    public Entity a(Class oclass, AxisAlignedBB axisalignedbb, Entity entity) {
        List list = this.a(oclass, axisalignedbb);
        Entity entity1 = null;
        double d0 = Double.MAX_VALUE;

        for (int i = 0; i < list.size(); ++i) {
            Entity entity2 = (Entity) list.get(i);

            if (entity2 != entity) {
                double d1 = entity.f(entity2);

                if (d1 <= d0) {
                    entity1 = entity2;
                    d0 = d1;
                }
            }
        }

        return entity1;
    }

    public abstract Entity getEntity(int i);

    public void b(int i, int j, int k, TileEntity tileentity) {
        if (this.isLoaded(i, j, k)) {
            this.getChunkAtWorldCoords(i, k).e();
        }
    }

    public int a(Class oclass) {
        int i = 0;

        for (int j = 0; j < this.entityList.size(); ++j) {
            Entity entity = (Entity) this.entityList.get(j);

            // CraftBukkit start - Split out persistent check, don't apply it to special persistent mobs
            if (entity instanceof EntityInsentient) {
                EntityInsentient entityinsentient = (EntityInsentient) entity;
                if (entityinsentient.isTypeNotPersistent() && entityinsentient.isPersistent()) {
                    continue;
                }
            }

            if (oclass.isAssignableFrom(entity.getClass())) {
            // if ((!(entity instanceof EntityInsentient) || !((EntityInsentient) entity).isPersistent()) && oclass.isAssignableFrom(entity.getClass())) {
                // CraftBukkit end
                ++i;
            }
        }

        return i;
    }

    public void a(List list) {
        org.spigotmc.AsyncCatcher.catchOp( "entity world add"); // Spigot
        // CraftBukkit start
        // this.entityList.addAll(list);
        Entity entity = null;

        for (int i = 0; i < list.size(); ++i) {
            entity = (Entity) list.get(i);
            if (entity == null) {
                continue;
            }
            this.entityList.add(entity);
            // CraftBukkit end
            this.a((Entity) list.get(i));
        }
    }

    public void b(List list) {
        this.f.addAll(list);
    }

    public boolean mayPlace(Block block, int i, int j, int k, boolean flag, int l, Entity entity, ItemStack itemstack) {
        Block block1 = this.getType(i, j, k);
        AxisAlignedBB axisalignedbb = flag ? null : block.a(this, i, j, k);

        // CraftBukkit start - store default return
        boolean defaultReturn = axisalignedbb != null && !this.a(axisalignedbb, entity) ? false : (block1.getMaterial() == Material.ORIENTABLE && block == Blocks.ANVIL ? true : block1.getMaterial().isReplaceable() && block.canPlace(this, i, j, k, l, itemstack));

        // CraftBukkit start
        BlockCanBuildEvent event = new BlockCanBuildEvent(this.getWorld().getBlockAt(i, j, k), CraftMagicNumbers.getId(block), defaultReturn);
        this.getServer().getPluginManager().callEvent(event);

        return event.isBuildable();
        // CraftBukkit end
    }

    public PathEntity findPath(Entity entity, Entity entity1, float f, boolean flag, boolean flag1, boolean flag2, boolean flag3) {
        this.methodProfiler.a("pathfind");
        int i = (int) FastMath.floor(entity.locX);
        int j = (int) FastMath.floor(entity.locY + 1.0D);
        int k = (int) FastMath.floor(entity.locZ);
        int l = (int) (f + 16.0F);
        int i1 = i - l;
        int j1 = j - l;
        int k1 = k - l;
        int l1 = i + l;
        int i2 = j + l;
        int j2 = k + l;
        WeakChunkCache chunkcache = new WeakChunkCache(this, i1, j1, k1, l1, i2, j2, 0); // CobelPvP
        PathEntity pathentity = (new Pathfinder(chunkcache, flag, flag1, flag2, flag3)).a(entity, entity1, f);

        this.methodProfiler.b();
        return pathentity;
    }

    public PathEntity a(Entity entity, int i, int j, int k, float f, boolean flag, boolean flag1, boolean flag2, boolean flag3) {
        this.methodProfiler.a("pathfind");
        int l = (int) FastMath.floor(entity.locX);
        int i1 = (int) FastMath.floor(entity.locY);
        int j1 = (int) FastMath.floor(entity.locZ);
        int k1 = (int) (f + 8.0F);
        int l1 = l - k1;
        int i2 = i1 - k1;
        int j2 = j1 - k1;
        int k2 = l + k1;
        int l2 = i1 + k1;
        int i3 = j1 + k1;
        WeakChunkCache chunkcache = new WeakChunkCache(this, l1, i2, j2, k2, l2, i3, 0); // CobelPvP
        PathEntity pathentity = (new Pathfinder(chunkcache, flag, flag1, flag2, flag3)).a(entity, i, j, k, f);

        this.methodProfiler.b();
        return pathentity;
    }

    public int getBlockPower(int i, int j, int k, int l) {
        return this.getType(i, j, k).c(this, i, j, k, l);
    }

    public int getBlockPower(int i, int j, int k) {
        byte b0 = 0;
        int l = Math.max(b0, this.getBlockPower(i, j - 1, k, 0));

        if (l >= 15) {
            return l;
        } else {
            l = Math.max(l, this.getBlockPower(i, j + 1, k, 1));
            if (l >= 15) {
                return l;
            } else {
                l = Math.max(l, this.getBlockPower(i, j, k - 1, 2));
                if (l >= 15) {
                    return l;
                } else {
                    l = Math.max(l, this.getBlockPower(i, j, k + 1, 3));
                    if (l >= 15) {
                        return l;
                    } else {
                        l = Math.max(l, this.getBlockPower(i - 1, j, k, 4));
                        if (l >= 15) {
                            return l;
                        } else {
                            l = Math.max(l, this.getBlockPower(i + 1, j, k, 5));
                            return l >= 15 ? l : l;
                        }
                    }
                }
            }
        }
    }

    public boolean isBlockFacePowered(int i, int j, int k, int l) {
        return this.getBlockFacePower(i, j, k, l) > 0;
    }

    public int getBlockFacePower(int i, int j, int k, int l) {
        return this.getType(i, j, k).r() ? this.getBlockPower(i, j, k) : this.getType(i, j, k).b(this, i, j, k, l);
    }

    public boolean isBlockIndirectlyPowered(int i, int j, int k) {
        return this.getBlockFacePower(i, j - 1, k, 0) > 0 ? true : (this.getBlockFacePower(i, j + 1, k, 1) > 0 ? true : (this.getBlockFacePower(i, j, k - 1, 2) > 0 ? true : (this.getBlockFacePower(i, j, k + 1, 3) > 0 ? true : (this.getBlockFacePower(i - 1, j, k, 4) > 0 ? true : this.getBlockFacePower(i + 1, j, k, 5) > 0))));
    }

    public int getHighestNeighborSignal(int i, int j, int k) {
        int l = 0;

        for (int i1 = 0; i1 < 6; ++i1) {
            int j1 = this.getBlockFacePower(i + Facing.b[i1], j + Facing.c[i1], k + Facing.d[i1], i1);

            if (j1 >= 15) {
                return 15;
            }

            if (j1 > l) {
                l = j1;
            }
        }

        return l;
    }

    public EntityHuman findNearbyPlayer(Entity entity, double d0) {
        return this.findNearbyPlayer(entity.locX, entity.locY, entity.locZ, d0);
    }

    public EntityHuman findNearbyPlayer(double d0, double d1, double d2, double d3) {
        // CobelPvP start
        if (0 <= d3 && d3 <= 64) {
            return this.playerMap.getNearestPlayer(d0, d1, d2, d3);
        }
        // CobelPvP end
        double d4 = -1.0D;
        EntityHuman entityhuman = null;

        for (Object o : a(EntityHuman.class, AxisAlignedBB.a(d0 - d3, d1 - d3, d2 - d3, d0 + d3, d1 + d3, d2 + d3))) {
            EntityHuman entityhuman1 = (EntityHuman) o;
            // CraftBukkit start - Fixed an NPE
            if (entityhuman1 == null || entityhuman1.dead) {
                continue;
            }
            // CraftBukkit end
            double d5 = entityhuman1.e(d0, d1, d2);

            if ((d3 < 0.0D || d5 < d3 * d3) && (d4 == -1.0D || d5 < d4)) {
                d4 = d5;
                entityhuman = entityhuman1;
            }
        }

        return entityhuman;
    }

    public EntityHuman findNearbyVulnerablePlayer(Entity entity, double d0) {
        return this.findNearbyVulnerablePlayer(entity.locX, entity.locY, entity.locZ, d0);
    }

    // CobelPvP start
    private static final Function<EntityHuman, Double> invisibilityFunction = new Function<EntityHuman, Double>() {
        @Override
        public Double apply(EntityHuman entityHuman) {

            if (entityHuman.isInvisible()) {
                float f = entityHuman.bE();

                if (f < 0.1F) {
                    f = 0.1F;
                }

                return (double) (0.7F * f);
            }

            return null;
        }
    };
    // CobelPvP end

    public EntityHuman findNearbyVulnerablePlayer(double d0, double d1, double d2, double d3) {
        // CobelPvP start
        if (0 <= d3 && d3 <= 64.0D) {
            return this.playerMap.getNearestAttackablePlayer(d0, d1, d2, d3, d3, invisibilityFunction);
        }
        // CobelPvP end
        double d4 = -1.0D;
        EntityHuman entityhuman = null;

        for (Object o : a(EntityHuman.class, AxisAlignedBB.a(d0 - d3, d1 - d3, d2 - d3, d0 + d3, d1 + d3, d2 + d3))) {
            EntityHuman entityhuman1 = (EntityHuman) o;
            // CraftBukkit start - Fixed an NPE
            if (entityhuman1 == null || entityhuman1.dead) {
                continue;
            }
            // CraftBukkit end

            if (!entityhuman1.abilities.isInvulnerable && entityhuman1.isAlive()) {
                double d5 = entityhuman1.e(d0, d1, d2);
                double d6 = d3;

                if (entityhuman1.isSneaking()) {
                    d6 = d3 * 0.800000011920929D;
                }

                if (entityhuman1.isInvisible()) {
                    float f = entityhuman1.bE();

                    if (f < 0.1F) {
                        f = 0.1F;
                    }

                    d6 *= (double) (0.7F * f);
                }

                if ((d3 < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4)) {
                    d4 = d5;
                    entityhuman = entityhuman1;
                }
            }
        }

        return entityhuman;
    }

    // PaperSpigot start - Find players with the spawning flag
    public EntityHuman findNearbyPlayerWhoAffectsSpawning(Entity entity, double radius) {
        return this.findNearbyPlayerWhoAffectsSpawning(entity.locX, entity.locY, entity.locZ, radius);
    }

    public EntityHuman findNearbyPlayerWhoAffectsSpawning(double x, double y, double z, double radius) {
        // CobelPvP start
        if (0 <= radius && radius <= 64.0) {
            return this.playerMap.getNearbyPlayer(x, y, z, radius, true);
        }
        // CobelPvP end
        double nearestRadius = - 1.0D;
        EntityHuman entityHuman = null;

        for (int i = 0; i < this.players.size(); ++i) {
            EntityHuman nearestPlayer = (EntityHuman) this.players.get(i);

            if (nearestPlayer == null || nearestPlayer.dead || !nearestPlayer.affectsSpawning) {
                continue;
            }

            double distance = nearestPlayer.e(x, y, z);

            if ((radius < 0.0D || distance < radius * radius) && (nearestRadius == -1.0D || distance < nearestRadius)) {
                nearestRadius = distance;
                entityHuman = nearestPlayer;
            }
        }

        return entityHuman;
    }
    // PaperSpigot end

    public EntityHuman a(String s) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityHuman entityhuman = (EntityHuman) this.players.get(i);

            if (s.equals(entityhuman.getName())) {
                return entityhuman;
            }
        }

        return null;
    }

    public EntityHuman a(UUID uuid) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityHuman entityhuman = (EntityHuman) this.players.get(i);

            if (uuid.equals(entityhuman.getUniqueID())) {
                return entityhuman;
            }
        }

        return null;
    }

    public void G() throws ExceptionWorldConflict { // CraftBukkit - added throws
        this.dataManager.checkSession();
    }

    public long getSeed() {
        return this.worldData.getSeed();
    }

    public long getTime() {
        return this.worldData.getTime();
    }

    public long getDayTime() {
        return this.worldData.getDayTime();
    }

    public void setDayTime(long i) {
        this.worldData.setDayTime(i);
    }

    public ChunkCoordinates getSpawn() {
        return new ChunkCoordinates(this.worldData.c(), this.worldData.d(), this.worldData.e());
    }

    public void x(int i, int j, int k) {
        this.worldData.setSpawn(i, j, k);
    }

    // CobelPvP start
    public void setSpawn(int i, int j, int k, float yaw, float pitch) {
        this.worldData.setSpawn(i, j, k, yaw, pitch);
    }
    // CobelPvP end

    public boolean a(EntityHuman entityhuman, int i, int j, int k) {
        return true;
    }

    public void broadcastEntityEffect(Entity entity, byte b0) {}

    public IChunkProvider L() {
        return this.chunkProvider;
    }

    public void playBlockAction(int i, int j, int k, Block block, int l, int i1) {
        block.a(this, i, j, k, l, i1);
    }

    public IDataManager getDataManager() {
        return this.dataManager;
    }

    public WorldData getWorldData() {
        return this.worldData;
    }

    public GameRules getGameRules() {
        return this.worldData.getGameRules();
    }

    public void everyoneSleeping() {}

    // CraftBukkit start
    // Calls the method that checks to see if players are sleeping
    // Called by CraftPlayer.setPermanentSleeping()
    public void checkSleepStatus() {
        if (!this.isStatic) {
            this.everyoneSleeping();
        }
    }
    // CraftBukkit end

    public float h(float f) {
        return (this.o + (this.p - this.o) * f) * this.j(f);
    }

    public float j(float f) {
        return this.m + (this.n - this.m) * f;
    }

    public boolean P() {
        return (double) this.h(1.0F) > 0.9D;
    }

    public boolean Q() {
        return (double) this.j(1.0F) > 0.2D;
    }

    public boolean isRainingAt(int i, int j, int k) {
        if (!this.Q()) {
            return false;
        } else if (!this.i(i, j, k)) {
            return false;
        } else if (this.h(i, k) > j) {
            return false;
        } else {
            BiomeBase biomebase = this.getBiome(i, k);

            return biomebase.d() ? false : (this.e(i, j, k, false) ? false : biomebase.e());
        }
    }

    public boolean z(int i, int j, int k) {
        BiomeBase biomebase = this.getBiome(i, k);

        return biomebase.f();
    }

    public void a(String s, PersistentBase persistentbase) {
        this.worldMaps.a(s, persistentbase);
    }

    public PersistentBase a(Class oclass, String s) {
        return this.worldMaps.get(oclass, s);
    }

    public int b(String s) {
        return this.worldMaps.a(s);
    }

    public void b(int i, int j, int k, int l, int i1) {
        for (int j1 = 0; j1 < this.u.size(); ++j1) {
            ((IWorldAccess) this.u.get(j1)).a(i, j, k, l, i1);
        }
    }

    public void triggerEffect(int i, int j, int k, int l, int i1) {
        this.a((EntityHuman) null, i, j, k, l, i1);
    }

    public void a(EntityHuman entityhuman, int i, int j, int k, int l, int i1) {
        try {
            for (int j1 = 0; j1 < this.u.size(); ++j1) {
                ((IWorldAccess) this.u.get(j1)).a(entityhuman, i, j, k, l, i1);
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Playing level event");
            CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Level event being played");

            crashreportsystemdetails.a("Block coordinates", CrashReportSystemDetails.a(j, k, l));
            crashreportsystemdetails.a("Event source", entityhuman);
            crashreportsystemdetails.a("Event type", Integer.valueOf(i));
            crashreportsystemdetails.a("Event data", Integer.valueOf(i1));
            throw new ReportedException(crashreport);
        }
    }

    public int getHeight() {
        return 256;
    }

    public int S() {
        return this.worldProvider.g ? 128 : 256;
    }

    public Random A(int i, int j, int k) {
        long l = (long) i * 341873128712L + (long) j * 132897987541L + this.getWorldData().getSeed() + (long) k;

        this.random.setSeed(l);
        return this.random;
    }

    public ChunkPosition b(String s, int i, int j, int k) {
        return this.L().findNearestMapFeature(this, s, i, j, k);
    }

    public CrashReportSystemDetails a(CrashReport crashreport) {
        CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Affected level", 1);

        crashreportsystemdetails.a("Level name", (this.worldData == null ? "????" : this.worldData.getName()));
        crashreportsystemdetails.a("All players", (Callable) (new CrashReportPlayers(this)));
        crashreportsystemdetails.a("Chunk stats", (Callable) (new CrashReportChunkStats(this)));

        try {
            this.worldData.a(crashreportsystemdetails);
        } catch (Throwable throwable) {
            crashreportsystemdetails.a("Level Data Unobtainable", throwable);
        }

        return crashreportsystemdetails;
    }

    public void d(int i, int j, int k, int l, int i1) {
        for (int j1 = 0; j1 < this.u.size(); ++j1) {
            IWorldAccess iworldaccess = (IWorldAccess) this.u.get(j1);

            iworldaccess.b(i, j, k, l, i1);
        }
    }

    public Calendar V() {
        if (this.getTime() % 600L == 0L) {
            this.J.setTimeInMillis(MinecraftServer.ar());
        }

        return this.J;
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    public void updateAdjacentComparators(int i, int j, int k, Block block) {
        for (int l = 0; l < 4; ++l) {
            int i1 = i + Direction.a[l];
            int j1 = k + Direction.b[l];
            Block block1 = this.getType(i1, j, j1);

            if (Blocks.REDSTONE_COMPARATOR_OFF.e(block1)) {
                block1.doPhysics(this, i1, j, j1, block);
            } else if (block1.r()) {
                i1 += Direction.a[l];
                j1 += Direction.b[l];
                Block block2 = this.getType(i1, j, j1);

                if (Blocks.REDSTONE_COMPARATOR_OFF.e(block2)) {
                    block2.doPhysics(this, i1, j, j1, block);
                }
            }
        }
    }

    public float b(double d0, double d1, double d2) {
        return this.B((int) FastMath.floor(d0), (int) FastMath.floor(d1), (int) FastMath.floor(d2));
    }

    public float B(int i, int j, int k) {
        float f = 0.0F;
        boolean flag = this.difficulty == EnumDifficulty.HARD;

        if (this.isLoaded(i, j, k)) {
            float f1 = this.y();

            f += MathHelper.a((float) this.getChunkAtWorldCoords(i, k).s / 3600000.0F, 0.0F, 1.0F) * (flag ? 1.0F : 0.75F);
            f += f1 * 0.25F;
        }

        if (this.difficulty == EnumDifficulty.EASY || this.difficulty == EnumDifficulty.PEACEFUL) {
            f *= (float) this.difficulty.a() / 2.0F;
        }

        return MathHelper.a(f, 0.0F, flag ? 1.5F : 1.0F);
    }

    public void X() {
        Iterator iterator = this.u.iterator();

        while (iterator.hasNext()) {
            IWorldAccess iworldaccess = (IWorldAccess) iterator.next();

            iworldaccess.b();
        }
    }

    // CobelPvP start - chunk unload queue slowness
    public long obtainLock, pendingSavesPut, fileIOThreadAddition, writeStartNBT, writeSections, writeBiomes, writeEntities, writeTileEntities, writeTileTicks;
    public void printTimings() {
        MinecraftServer.getLogger().warn("Obtain lock: " + obtainLock);
        MinecraftServer.getLogger().warn("Pending saves put: " + pendingSavesPut);
        MinecraftServer.getLogger().warn("File IO thread addition: " + fileIOThreadAddition);
        MinecraftServer.getLogger().warn("Write start NBT: " + writeStartNBT);
        MinecraftServer.getLogger().warn("Write sections: " + writeSections);
        MinecraftServer.getLogger().warn("Write biomes: " + writeBiomes);
        MinecraftServer.getLogger().warn("Write entities: " + writeEntities);
        MinecraftServer.getLogger().warn("Write tile entities: " + writeTileEntities);
        MinecraftServer.getLogger().warn("Write tile ticks: " + writeTileTicks);
        
    }

    public void clearTimings() {
        this.obtainLock = 0;
        this.pendingSavesPut = 0;
        this.fileIOThreadAddition = 0;
        this.writeStartNBT = 0;
        this.writeSections = 0;
        this.writeBiomes = 0;
        this.writeEntities = 0;
        this.writeTileEntities = 0;
        this.writeTileTicks = 0;
    }
    // CobelPvP end
}
