package org.spigotmc;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

public class SpigotWorldConfig
{

    private final String worldName;
    private final YamlConfiguration config;
    private boolean verbose;

    public SpigotWorldConfig(String worldName)
    {
        this.worldName = worldName;
        this.config = SpigotConfig.config;
        init();
    }

    public void init()
    {
        this.verbose = getBoolean( "verbose", true );

        log( "-------- World Settings For [" + worldName + "] --------" );
        SpigotConfig.readConfig( SpigotWorldConfig.class, this );
    }

    private void log(String s)
    {
        if ( verbose )
        {
            Bukkit.getLogger().info( s );
        }
    }

    private void set(String path, Object val)
    {
        config.set( "world-settings.default." + path, val );
    }

    private boolean getBoolean(String path, boolean def)
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getBoolean( "world-settings." + worldName + "." + path, config.getBoolean( "world-settings.default." + path ) );
    }

    private double getDouble(String path, double def)
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getDouble( "world-settings." + worldName + "." + path, config.getDouble( "world-settings.default." + path ) );
    }

    private int getInt(String path, int def)
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getInt( "world-settings." + worldName + "." + path, config.getInt( "world-settings.default." + path ) );
    }

    private <T> List getList(String path, T def)
    {
        config.addDefault( "world-settings.default." + path, def );
        return (List<T>) config.getList( "world-settings." + worldName + "." + path, config.getList( "world-settings.default." + path ) );
    }

    private String getString(String path, String def)
    {
        config.addDefault( "world-settings.default." + path, def );
        return config.getString( "world-settings." + worldName + "." + path, config.getString( "world-settings.default." + path ) );
    }

    public int chunksPerTick;
    public boolean clearChunksOnTick;
    private void chunksPerTick()
    {
        chunksPerTick = getInt( "chunks-per-tick", 650 );
        log( "Chunks to Grow per Tick: " + chunksPerTick );

        clearChunksOnTick = getBoolean( "clear-tick-list", false );
        log( "Clear tick list: " + clearChunksOnTick );
    }

    // Crop growth rates
    public int cactusModifier;
    public int caneModifier;
    public int melonModifier;
    public int mushroomModifier;
    public int pumpkinModifier;
    public int saplingModifier;
    public int wheatModifier;
    private int getAndValidateGrowth(String crop)
    {
        int modifier = getInt( "growth." + crop.toLowerCase() + "-modifier", 100 );
        if ( modifier == 0 )
        {
            log( "Cannot set " + crop + " growth to zero, defaulting to 100" );
            modifier = 100;
        }
        log( crop + " Growth Modifier: " + modifier + "%" );

        return modifier;
    }
    private void growthModifiers()
    {
        cactusModifier = getAndValidateGrowth( "Cactus" );
        caneModifier = getAndValidateGrowth( "Cane" );
        melonModifier = getAndValidateGrowth( "Melon" );
        mushroomModifier = getAndValidateGrowth( "Mushroom" );
        pumpkinModifier = getAndValidateGrowth( "Pumpkin" );
        saplingModifier = getAndValidateGrowth( "Sapling" );
        wheatModifier = getAndValidateGrowth( "Wheat" );
    }

    public double itemMerge;
    private void itemMerge()
    {
        itemMerge = getDouble("merge-radius.item", 3.5 );
        log( "Item Merge Radius: " + itemMerge );
    }

    public double expMerge;
    private void expMerge()
    {
        expMerge = getDouble("merge-radius.exp", 6.0 );
        log( "Experience Merge Radius: " + expMerge );
    }

    public int viewDistance;
    private void viewDistance()
    {
        viewDistance = getInt( "view-distance", Bukkit.getViewDistance() );
        log( "View Distance: " + viewDistance );
    }

    public byte mobSpawnRange;
    private void mobSpawnRange()
    {
        mobSpawnRange = (byte) getInt( "mob-spawn-range", 5 );
        log( "Mob Spawn Range: " + mobSpawnRange );
    }

    public int animalActivationRange = 32;
    public int monsterActivationRange = 32;
    public int miscActivationRange = 16;
    private void activationRange()
    {
        animalActivationRange = getInt( "entity-activation-range.animals", animalActivationRange );
        monsterActivationRange = getInt( "entity-activation-range.monsters", monsterActivationRange );
        miscActivationRange = getInt( "entity-activation-range.misc", miscActivationRange );
        log( "Entity Activation Range: An " + animalActivationRange + " / Mo " + monsterActivationRange + " / Mi " + miscActivationRange );
    }

    public int playerTrackingRange = 48;
    public int animalTrackingRange = 48;
    public int monsterTrackingRange = 48;
    public int miscTrackingRange = 16;
    public int otherTrackingRange = 64;
    private void trackingRange()
    {
        playerTrackingRange = getInt( "entity-tracking-range.players", playerTrackingRange );
        animalTrackingRange = getInt( "entity-tracking-range.animals", animalTrackingRange );
        monsterTrackingRange = getInt( "entity-tracking-range.monsters", monsterTrackingRange );
        miscTrackingRange = getInt( "entity-tracking-range.misc", miscTrackingRange );
        otherTrackingRange = getInt( "entity-tracking-range.other", otherTrackingRange );
        log( "Entity Tracking Range: Pl " + playerTrackingRange + " / An " + animalTrackingRange + " / Mo " + monsterTrackingRange + " / Mi " + miscTrackingRange + " / Other " + otherTrackingRange );
    }

    public boolean altHopperTicking;
    public int hopperTransfer;
    public int hopperCheck;
    public int hopperAmount;
    private void hoppers()
    {
        // Alternate ticking method. Uses inventory changes, redstone updates etc.
        // to update hoppers. Hopper-check is disabled when this is true.
        boolean prev = altHopperTicking;
        altHopperTicking = getBoolean( "hopper-alt-ticking", false );
        // Necessary for the reload command
        if (prev != altHopperTicking) {
            net.minecraft.server.World world = (net.minecraft.server.World) Bukkit.getWorld(this.worldName);
            if (world != null) {
                if (altHopperTicking) {
                    for (Object o : world.tileEntityList) {
                        if (o instanceof net.minecraft.server.TileEntityHopper) {
                            ((net.minecraft.server.TileEntityHopper) o).convertToScheduling();
                        }
                    }
                } else {
                    for (Object o : world.tileEntityList) {
                        if (o instanceof net.minecraft.server.TileEntityHopper) {
                            ((net.minecraft.server.TileEntityHopper) o).convertToPolling();
                        }
                    }
                }
            }
        }
        // Set the tick delay between hopper item movements
        hopperTransfer = getInt( "ticks-per.hopper-transfer", 8 );
        // Set the tick delay between checking for items after the associated
        // container is empty. Default to the hopperTransfer value to prevent
        // hopper sorting machines from becoming out of sync.
        hopperCheck = getInt( "ticks-per.hopper-check", hopperTransfer );
        hopperAmount = getInt( "hopper-amount", 1 );
        log( "Alternative Hopper Ticking: " + altHopperTicking );
        log( "Hopper Transfer: " + hopperTransfer + " Hopper Check: " + hopperCheck + " Hopper Amount: " + hopperAmount );
    }

    public boolean randomLightUpdates;
    private void lightUpdates()
    {
        randomLightUpdates = getBoolean( "random-light-updates", false );
        log( "Random Lighting Updates: " + randomLightUpdates );
    }

    public boolean saveStructureInfo;
    private void structureInfo()
    {
        saveStructureInfo = getBoolean( "save-structure-info", true );
        log( "Structure Info Saving: " + saveStructureInfo );
        if ( !saveStructureInfo )
        {
            log( "*** WARNING *** You have selected to NOT save structure info. This may cause structures such as fortresses to not spawn mobs when updating to 1.7!" );
            log( "*** WARNING *** Please use this option with caution, SpigotMC is not responsible for any issues this option may cause in the future!" );
        }
    }

    public int itemDespawnRate;
    private void itemDespawnRate()
    {
        itemDespawnRate = getInt( "item-despawn-rate", 5000 );
        log( "Item Despawn Rate: " + itemDespawnRate );
    }

    public int arrowDespawnRate;
    private void arrowDespawnRate()
    {
        arrowDespawnRate = getInt( "arrow-despawn-rate", 300  );
        log( "Arrow Despawn Rate: " + arrowDespawnRate );
    }
    
    public boolean antiXray;
    public int engineMode;
    public List<Integer> hiddenBlocks;
    public List<Integer> replaceBlocks;
    public AntiXray antiXrayInstance;
    private void antiXray()
    {
        antiXray = getBoolean( "anti-xray.enabled", false );
        log( "Anti X-Ray: " + antiXray );

        engineMode = getInt( "anti-xray.engine-mode", 1 );
        log( "\tEngine Mode: " + engineMode );

        if ( SpigotConfig.version < 5 )
        {
            set( "anti-xray.blocks", null );
        }
        hiddenBlocks = getList( "anti-xray.hide-blocks", Arrays.asList( new Integer[]
        {
            14, 15, 16, 21, 48, 49, 54, 56, 73, 74, 82, 129, 130
        } ) );
        log( "\tHidden Blocks: " + hiddenBlocks );

        replaceBlocks = getList( "anti-xray.replace-blocks", Arrays.asList( new Integer[]
        {
            1, 5
        } ) );
        log( "\tReplace Blocks: " + replaceBlocks );

        antiXrayInstance = new AntiXray( this );
    }

    public boolean zombieAggressiveTowardsVillager;
    private void zombieAggressiveTowardsVillager()
    {
        zombieAggressiveTowardsVillager = getBoolean( "zombie-aggressive-towards-villager", true );
        log( "Zombie Aggressive Towards Villager: " + zombieAggressiveTowardsVillager );
    }

    public boolean nerfSpawnerMobs;
    private void nerfSpawnerMobs()
    {
        nerfSpawnerMobs = getBoolean( "nerf-spawner-mobs", true );
        log( "Nerfing mobs spawned from spawners: " + nerfSpawnerMobs );
    }

    public boolean enableZombiePigmenPortalSpawns;
    private void enableZombiePigmenPortalSpawns()
    {
        enableZombiePigmenPortalSpawns = getBoolean( "enable-zombie-pigmen-portal-spawns", true );
        log( "Allow Zombie Pigmen to spawn from portal blocks: " + enableZombiePigmenPortalSpawns );
    }

    public int maxBulkChunk;
    private void bulkChunkCount()
    {
        maxBulkChunk = getInt( "max-bulk-chunks", 5 );
        log( "Sending up to " + maxBulkChunk + " chunks per packet" );
    }

    public int maxCollisionsPerEntity;
    private void maxEntityCollision()
    {
        maxCollisionsPerEntity = getInt( "max-entity-collisions", 6 );
        log( "Max Entity Collisions: " + maxCollisionsPerEntity );
    }

    public int dragonDeathSoundRadius;
    private void keepDragonDeathPerWorld()
    {
        dragonDeathSoundRadius = getInt( "dragon-death-sound-radius", 0 );
    }

    public int witherSpawnSoundRadius;
    private void witherSpawnSoundRadius()
    {
        witherSpawnSoundRadius = getInt( "wither-spawn-sound-radius", 0 );
    }

    public int villageSeed;
    public int largeFeatureSeed;
    private void initWorldGenSeeds()
    {
        villageSeed = getInt( "seed-village", 10387312 );
        largeFeatureSeed = getInt( "seed-feature", 14357617 );
        log( "Custom Map Seeds:  Village: " + villageSeed + " Feature: " + largeFeatureSeed );
    }

    public float walkExhaustion;
    public float sprintExhaustion;
    public float combatExhaustion;
    public float regenExhaustion;
    private void initHunger()
    {
        walkExhaustion = (float) getDouble( "hunger.walk-exhaustion", 0.2 );
        sprintExhaustion = (float) getDouble( "hunger.sprint-exhaustion", 0.6 );
        combatExhaustion = (float) getDouble( "hunger.combat-exhaustion", 0.3 );
        regenExhaustion = (float) getDouble( "hunger.regen-exhaustion", 2.4 );
    }

    public int currentPrimedTnt = 0;
    public int maxTntTicksPerTick;
    private void maxTntPerTick() {
        if ( SpigotConfig.version < 7 )
        {
            set( "max-tnt-per-tick", 100 );
        }
        maxTntTicksPerTick = getInt( "max-tnt-per-tick", 100 );
        log( "Max TNT Explosions: " + maxTntTicksPerTick );
    }

    public int hangingTickFrequency;
    private void hangingTickFrequency()
    {
        hangingTickFrequency = getInt( "hanging-tick-frequency", 100 );
    }

    public int expDespawnRate;

    private void expDespawnRate() {
        expDespawnRate = getInt( "exp-despawn-rate", 6000 );
        log( "Experience Orb Despawn Rate: " + expDespawnRate );
    }

    public boolean mobsEnabled;

    private void mobsEnabled() {
        mobsEnabled = getBoolean("mobs-enabled", true);
        log("Mobs enabled: " + mobsEnabled);
    }

    // CobelPvP start
    public boolean enderPearlsCanPassNonSolidBlocks;
    private void enderPearlsCanPassNonSolidBlocks() {
        enderPearlsCanPassNonSolidBlocks = getBoolean("enderPearlsCanPassNonSolidBlocks", false);
        log("Enderpearls can pass non-solid blocks: " + enderPearlsCanPassNonSolidBlocks);
    }

    public boolean updateMapItemsInPlayerInventory;
    private void dontUpdateMapItemsInPlayerInventory() {
        updateMapItemsInPlayerInventory = getBoolean( "updateMapItemsInPlayerInventory" , false);
    }

    public boolean useAlternateEndSpawn;
    private void useAlternateEndSpawn() {
        useAlternateEndSpawn = getBoolean( "useAlternateEndSpawn", true);
    }
    // CobelPvP end

    public double knockbackSprintVertical = 0.1D;
    public double knockbackSprintHorizontal = 0.5D;
    public boolean knockbackResetFallDistance = false;
    public double knockbackHeight = 0.4D;
    public double knockbackHorizontal = 0.4D;
    public double knockbackFriction = 2.0D;
    public double knockbackVerticalLimit = 0.4D;
    private void knockbackSetup()
    {
        knockbackSprintVertical = getDouble( "knockback.sprint-vertical", knockbackSprintVertical );
        knockbackSprintHorizontal = getDouble( "knockback.sprint-horizontal", knockbackSprintHorizontal );
        knockbackResetFallDistance = getBoolean( "knockback.reset-fall-distance", knockbackResetFallDistance );
        knockbackHeight = getDouble( "knockback.height", knockbackHeight );
        knockbackHorizontal = getDouble( "knockback.horizontal", knockbackHorizontal );
        knockbackFriction = getDouble( "knockback.friction", knockbackFriction );
        knockbackVerticalLimit = getDouble( "knockback.vertical-limit", knockbackVerticalLimit );
    }
}
