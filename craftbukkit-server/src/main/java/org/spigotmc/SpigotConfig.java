package org.spigotmc;

import com.cobelpvp.commands.*;
import com.google.common.base.Throwables;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import net.minecraft.server.AttributeRanged;
import net.minecraft.server.GenericAttributes;
import net.minecraft.util.gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class SpigotConfig
{
    private static final File CONFIG_FILE = new File( "config/server", "spigot.yml" ); // CobelPvP - Dedicated config directory
    private static final String HEADER = "This is the main configuration file for Spigot.\n"
            + "As you can see, there's tons to configure. Some options may impact gameplay, so use\n"
            + "with caution, and make sure you know what each option does before configuring.\n"
            + "For a reference for any variable inside this file, check out the Spigot wiki at\n"
            + "http://www.spigotmc.org/wiki/spigot-configuration/\n"
            + "\n"
            + "If you need help with the configuration or have any questions related to Spigot,\n"
            + "join us at the IRC or drop by our forums and leave a post.\n"
            + "\n"
            + "IRC: #spigot @ irc.spi.gt ( http://www.spigotmc.org/pages/irc/ )\n"
            + "Forums: http://www.spigotmc.org/\n";
    /*========================================================================*/
    public static YamlConfiguration config;
    static int version;
    static Map<String, Command> commands;
    /*========================================================================*/

    public static void init()
    {
        config = new YamlConfiguration();
        try
        {
            config.load( CONFIG_FILE );
        } catch ( IOException ex )
        {
        } catch ( InvalidConfigurationException ex )
        {
            Bukkit.getLogger().log( Level.SEVERE, "Could not load spigot.yml, please correct your syntax errors", ex );
            throw Throwables.propagate( ex );
        }

        config.options().header( HEADER );
        config.options().copyDefaults( true );

        commands = new HashMap<String, Command>();

        version = getInt( "config-version", 8 );
        set( "config-version", 8 );
        readConfig( SpigotConfig.class, null );
    }

    public static void registerCommands()
    {
        for ( Map.Entry<String, Command> entry : commands.entrySet() )
        {
            MinecraftServer.getServer().server.getCommandMap().register( entry.getKey(), "Spigot", entry.getValue() );
        }
    }

    static void readConfig(Class<?> clazz, Object instance)
    {
        for ( Method method : clazz.getDeclaredMethods() )
        {
            if ( Modifier.isPrivate( method.getModifiers() ) )
            {
                if ( method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE )
                {
                    try
                    {
                        method.setAccessible( true );
                        method.invoke( instance );
                    } catch ( InvocationTargetException ex )
                    {
                        throw Throwables.propagate( ex.getCause() );
                    } catch ( Exception ex )
                    {
                        Bukkit.getLogger().log( Level.SEVERE, "Error invoking " + method, ex );
                    }
                }
            }
        }

        try
        {
            config.save( CONFIG_FILE );
        } catch ( IOException ex )
        {
            Bukkit.getLogger().log( Level.SEVERE, "Could not save " + CONFIG_FILE, ex );
        }
    }

    private static void set(String path, Object val)
    {
        config.set( path, val );
    }

    private static boolean getBoolean(String path, boolean def)
    {
        config.addDefault( path, def );
        return config.getBoolean( path, config.getBoolean( path ) );
    }

    private static int getInt(String path, int def)
    {
        config.addDefault( path, def );
        return config.getInt( path, config.getInt( path ) );
    }

    private static <T> List getList(String path, T def)
    {
        config.addDefault( path, def );
        return (List<T>) config.getList( path, config.getList( path ) );
    }

    private static String getString(String path, String def)
    {
        config.addDefault( path, def );
        return config.getString( path, config.getString( path ) );
    }

    private static double getDouble(String path, double def)
    {
        config.addDefault( path, def );
        return config.getDouble( path, config.getDouble( path ) );
    }

    public static boolean logCommands;
    private static void logCommands()
    {
        logCommands = getBoolean( "commands.log", true );
    }

    public static int tabComplete;
    private static void tabComplete()
    {
        if ( version < 6 )
        {
            boolean oldValue = getBoolean( "commands.tab-complete", true );
            if ( oldValue )
            {
                set( "commands.tab-complete", 0 );
            } else
            {
                set( "commands.tab-complete", -1 );
            }
        }
        tabComplete = getInt( "commands.tab-complete", 0 );
    }

    public static String whitelistMessage;
    public static String unknownCommandMessage;
    public static String serverFullMessage;
    public static String outdatedClientMessage = "Outdated client! Please use {0}";
    public static String outdatedServerMessage = "Outdated server! I\'m still on {0}";
    private static String transform(String s)
    {
        return ChatColor.translateAlternateColorCodes( '&', s ).replaceAll( "\\n", "\n" );
    }
    private static void messages()
    {
        if (version < 8)
        {
            set( "messages.outdated-client", outdatedClientMessage );
            set( "messages.outdated-server", outdatedServerMessage );
        }

        whitelistMessage = transform( getString( "messages.whitelist", "You are not whitelisted on this server!" ) );
        unknownCommandMessage = transform( getString( "messages.unknown-command", "Unknown command. Type \"/help\" for help." ) );
        serverFullMessage = transform( getString( "messages.server-full", "The server is full!" ) );
        outdatedClientMessage = transform( getString( "messages.outdated-client", outdatedClientMessage ) );
        outdatedServerMessage = transform( getString( "messages.outdated-server", outdatedServerMessage ) );
    }

    public static int timeoutTime = 10;
    public static boolean restartOnCrash = true;
    public static String restartScript = "./start.sh";
    public static String restartMessage;
    private static void watchdog()
    {
        timeoutTime = getInt( "settings.timeout-time", timeoutTime );
        restartOnCrash = getBoolean( "settings.restart-on-crash", restartOnCrash );
        restartScript = getString( "settings.restart-script", restartScript );
        restartMessage = transform( getString( "messages.restart", "Server is restarting" ) );
        commands.put( "restart", new RestartCommand( "restart" ) );
        WatchdogThread.doStart( timeoutTime, restartOnCrash );
    }

    public static boolean bungee;
    private static void bungee() {
        if ( version < 4 )
        {
            set( "settings.bungeecord", false );
            System.out.println( "Oudated config, disabling BungeeCord support!" );
        }
        bungee = getBoolean( "settings.bungeecord", false );
    }

    private static void nettyThreads()
    {
        int count = getInt( "settings.netty-threads", 4 );
        System.setProperty( "io.netty.eventLoopThreads", Integer.toString( count ) );
        Bukkit.getLogger().log( Level.INFO, "Using {0} threads for Netty based IO", count );
    }

    public static boolean lateBind;
    private static void lateBind() {
        lateBind = getBoolean( "settings.late-bind", true );
    }

    public static boolean disableStatSaving = true;
    public static TObjectIntHashMap<String> forcedStats = new TObjectIntHashMap<String>();
    private static void stats()
    {
        if ( !config.contains( "stats.forced-stats" ) ) {
            config.createSection( "stats.forced-stats" );
        }

        ConfigurationSection section = config.getConfigurationSection( "stats.forced-stats" );
        for ( String name : section.getKeys( true ) )
        {
            if ( section.isInt( name ) )
            {
                forcedStats.put( name, section.getInt( name ) );
            }
        }

        if ( disableStatSaving && section.getInt( "achievement.openInventory", 0 ) < 1 )
        {
            forcedStats.put("achievement.openInventory", 1);
        }
    }

    private static void tpsCommand()
    {
        commands.put( "tps", new TicksPerSecondCommand( "tps" ) );
    }

    public static int playerSample;
    private static void playerSample()
    {
        playerSample = getInt( "settings.sample-count", 12 );
        System.out.println( "Server Ping Player Sample Count: " + playerSample );
    }

    public static int playerShuffle;
    private static void playerShuffle()
    {
        playerShuffle = getInt( "settings.player-shuffle", 0 );
    }

    public static List<String> spamExclusions;
    private static void spamExclusions()
    {
        spamExclusions = getList( "commands.spam-exclusions", Arrays.asList( new String[]
        {
                "/skill"
        } ) );
    }

    public static boolean silentCommandBlocks;
    private static void silentCommandBlocks()
    {
        silentCommandBlocks = getBoolean( "commands.silent-commandblock-console", false );
    }

    public static boolean filterCreativeItems;
    private static void filterCreativeItems()
    {
        filterCreativeItems = getBoolean( "settings.filter-creative-items", true );
    }

    public static Set<String> replaceCommands;
    private static void replaceCommands()
    {
        if ( config.contains( "replace-commands" ) )
        {
            set( "commands.replace-commands", config.getStringList( "replace-commands" ) );
            config.set( "replace-commands", null );
        }
        replaceCommands = new HashSet<String>( (List<String>) getList( "commands.replace-commands",
                Arrays.asList( "setblock", "summon", "testforblock", "tellraw" ) ) );
    }
    
    public static int userCacheCap;
    private static void userCacheCap()
    {
        userCacheCap = getInt( "settings.user-cache-size", 1000 );
    }
    
    public static boolean saveUserCacheOnStopOnly;
    private static void saveUserCacheOnStopOnly()
    {
        saveUserCacheOnStopOnly = getBoolean( "settings.save-user-cache-on-stop-only", false );
    }

    public static int intCacheLimit;
    private static void intCacheLimit()
    {
        intCacheLimit = getInt( "settings.int-cache-limit", 1024 );
    }

    public static double movedWronglyThreshold;
    private static void movedWronglyThreshold()
    {
        movedWronglyThreshold = getDouble( "settings.moved-wrongly-threshold", 0.0625D );
    }

    public static double movedTooQuicklyThreshold;
    private static void movedTooQuicklyThreshold()
    {
        movedTooQuicklyThreshold = getDouble( "settings.moved-too-quickly-threshold", 100.0D );
    }

    public static double maxHealth = 2048;
    public static double movementSpeed = 2048;
    public static double attackDamage = 2048;
    private static void attributeMaxes()
    {
        maxHealth = getDouble( "settings.attribute.maxHealth.max", maxHealth );
        ( (AttributeRanged) GenericAttributes.maxHealth ).b = maxHealth;
        movementSpeed = getDouble( "settings.attribute.movementSpeed.max", movementSpeed );
        ( (AttributeRanged) GenericAttributes.d ).b = movementSpeed;
        attackDamage = getDouble( "settings.attribute.attackDamage.max", attackDamage );
        ( (AttributeRanged) GenericAttributes.e ).b = attackDamage;
    }

    private static void globalAPICache()
    {
        if ( getBoolean( "settings.global-api-cache", false ) && !CachedStreamHandlerFactory.isSet )
        {
            Bukkit.getLogger().info( "Global API cache enabled - All requests to Mojang's API will be " +
                    "handled by Spigot" );
            CachedStreamHandlerFactory.isSet = true;
            URL.setURLStreamHandlerFactory(new CachedStreamHandlerFactory());
        }
    }

    public static boolean debug;
    private static void debug()
    {
        debug = getBoolean( "settings.debug", false );

        if ( debug && !LogManager.getRootLogger().isTraceEnabled() )
        {
            // Enable debug logging
            LoggerContext ctx = (LoggerContext) LogManager.getContext( false );
            Configuration conf = ctx.getConfiguration();
            conf.getLoggerConfig( LogManager.ROOT_LOGGER_NAME ).setLevel( org.apache.logging.log4j.Level.ALL );
            ctx.updateLoggers( conf );
        }

        if ( LogManager.getRootLogger().isTraceEnabled() )
        {
            Bukkit.getLogger().info( "Debug logging is enabled" );
        } else
        {
            Bukkit.getLogger().info( "Debug logging is disabled" );
        }
    }

    // CobelPvP start
    public static boolean disablePlayerFileSaving;
    private static void playerFiles() {
        disablePlayerFileSaving = getBoolean( "settings.disable-player-file-saving", false );
        if (disablePlayerFileSaving) {
            disableStatSaving = true;
        }
    }

    public static boolean logRemainingAsyncThreadsDuringShutdown;
    private static void logRemainingAsyncThreadsDuringShutdown() {
        logRemainingAsyncThreadsDuringShutdown = getBoolean( "settings.logRemainingAsyncThreadsDuringShutdown" , true);
    }

    private static void worldstatsCommand() {
        commands.put( "worldstats", new WorldStatsCommand( "worldstats" ) );
    }

    private static void setviewdistanceCommand() {
        commands.put( "setviewdistance", new SetViewDistanceCommand( "setviewdistance" ) );
    }

    public static int playersPerChunkIOThread;
    private static void playersPerChunkIOThread() {
        playersPerChunkIOThread = Math.max(1, getInt( "settings.chunkio.players-per-thread", 150) );
    }

    public static int autoSaveChunksPerTick;
    private static void autoSaveChunksPerTick() {
        autoSaveChunksPerTick = getInt( "settings.autosave.chunks-per-tick" , 200 );
    }

    public static boolean autoSaveFireWorldSaveEvent;
    private static void autoSaveFireWorldSaveEvent() {
        autoSaveFireWorldSaveEvent = getBoolean ( "settings.autosave.fire-WorldSaveEvent", false);
    }

    public static boolean autoSaveClearRegionFileCache;
    private static void autoSaveClearRegionFileCache() {
        autoSaveClearRegionFileCache = getBoolean ( "settings.autosave.clear-RegionFileCache", false);
    }

    public static boolean lagSpikeLoggerEnabled;
    private static void lagSpikeLoggerEnabled() {
        lagSpikeLoggerEnabled = getBoolean ( "settings.lagSpikeLogger.enabled", true);
    }

    public static long lagSpikeLoggerTickLimitNanos;
    private static void lagSpikeLoggerTickLimitNanos() {
        lagSpikeLoggerTickLimitNanos = ((long) getInt( "settings.lagSpikeLogger.tickLimitInMilliseconds", 100)) * 1000000L;
    }
    // CobelPvP end

    public static int brewingMultiplier;
    private static void brewingMultiplier() {
        brewingMultiplier = getInt("settings.brewingMultiplier", 1);
    }

    public static int smeltingMultiplier;
    private static void smeltingMultiplier() {
        smeltingMultiplier = getInt("settings.smeltingMultiplier", 1);
    }

    public static boolean instantRespawn;
    private static void instantRespawn()  {
        instantRespawn = getBoolean("settings.instantRespawn", false);
    }

    // CobelPvP start
    private static void noTrackCommand() {
        commands.put( "notrack", new NoTrackCommand( "notrack" ) );
    }

    private static void PvPArmorCommand() {
        commands.put("pvparmor", new PvPArmorCommand("pvparmor"));
    }

    private static void GCCommand() {
        commands.put("garbageclear", new PvPArmorCommand("garbageclear"));
    }

    public static boolean disableTracking;
    private static void disableTracking() {
        disableTracking = getBoolean("settings.disable.entityTracking", false);
    }

    public static boolean disableBlockTicking;
    private static void disableBlockTicking() {
        disableBlockTicking = getBoolean("settings.disable.ticking.blocks", false);
    }

    public static boolean disableVillageTicking;
    private static void disableVillageTicking() {
        disableVillageTicking = getBoolean("settings.disable.ticking.villages", false);
    }

    public static boolean disableWeatherTicking;
    private static void disableWeatherTicking() {
        disableWeatherTicking = getBoolean("settings.disable.ticking.weather", false);
    }

    public static boolean disableSleepCheck;
    private static void disableSleepCheck() {
        disableSleepCheck = getBoolean("settings.disable.general.sleepcheck", false);
    }

    public static boolean disableEntityCollisions;
    private static void disableEntityCollisions() {
        disableEntityCollisions = getBoolean("settings.disable.general.entity-collisions", false);
    }

    public static boolean cacheChunkMaps;
    private static void cacheChunkMaps() {
        cacheChunkMaps = getBoolean("settings.cache-chunk-maps", false);
    }

    public static boolean disableSaving;
    private static void disableSaving() {
        disableSaving = getBoolean("settings.disableSaving", false);
    }

    public static boolean playerListPackets;
    public static boolean updatePingOnTablist;
    public static boolean onlyCustomTab;
    private static void packets() {
        onlyCustomTab = getBoolean("settings.only-custom-tab", false);

        if (!onlyCustomTab) {
            playerListPackets = !getBoolean("settings.disable.player-list-packets", false);
            updatePingOnTablist = getBoolean("settings.disable.ping-update-packets", false);
        }
    }
    // CobelPvP end

    public static boolean reduceArmorDamage;
    private static void reduceArmorDamage() {
        reduceArmorDamage = getBoolean("settings.reduce-armor-damage", true);
    }

    public static boolean pearlThroughGatesAndTripwire = false;
    private static void pearls() {
        pearlThroughGatesAndTripwire = getBoolean("settings.pearl-through-tripwire", true);
    }

    public static double knockbackFriction = 2.0D;
    public static double knockbackHorizontal = 0.35D;
    public static double knockbackVertical = 0.35D;
    public static double knockbackVerticalLimit = 0.4D;
    public static double knockbackExtraHorizontal = 0.425D;
    public static double knockbackExtraVertical = 0.085D;
    
}
