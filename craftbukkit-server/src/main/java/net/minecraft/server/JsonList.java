package net.minecraft.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.util.com.google.common.base.Charsets;
import net.minecraft.util.com.google.common.collect.Maps;
import net.minecraft.util.com.google.common.io.Files;
import net.minecraft.util.com.google.gson.Gson;
import net.minecraft.util.com.google.gson.GsonBuilder;
import net.minecraft.util.com.google.gson.JsonObject;
import net.minecraft.util.org.apache.commons.io.IOUtils;
import org.spigotmc.SpigotConfig;

public class JsonList {

    protected static final Logger a = LogManager.getLogger();
    protected final Gson b;
    private final File c;
    private final Map d = Maps.newHashMap();
    private boolean e = true;
    private static final ParameterizedType f = new JsonListType();

    public JsonList(File file1) {
        this.c = file1;
        GsonBuilder gsonbuilder = (new GsonBuilder()).setPrettyPrinting();

        gsonbuilder.registerTypeHierarchyAdapter(JsonListEntry.class, new JsonListEntrySerializer(this, (JsonListType) null));
        this.b = gsonbuilder.create();
    }

    public boolean isEnabled() {
        return this.e;
    }

    public void a(boolean flag) {
        this.e = flag;
    }

    public File c() {
        return this.c;
    }

    public void add(JsonListEntry jsonlistentry) {
        this.d.put(this.a(jsonlistentry.getKey()), jsonlistentry);

        try {
            this.save();
        } catch (IOException ioexception) {
            a.warn("Could not save the list after adding a user.", ioexception);
        }
    }

    public JsonListEntry get(Object object) {
        this.h();
        return (JsonListEntry) this.d.get(this.a(object));
    }

    public void remove(Object object) {
        this.d.remove(this.a(object));

        try {
            this.save();
        } catch (IOException ioexception) {
            a.warn("Could not save the list after removing a user.", ioexception);
        }
    }

    public String[] getEntries() {
        return (String[]) this.d.keySet().toArray(new String[this.d.size()]);
    }

    // CraftBukkit start
    public Collection<JsonListEntry> getValues() {
        return this.d.values();
    }
    // CraftBukkit end

    public boolean isEmpty() {
        return this.d.size() < 1;
    }

    protected String a(Object object) {
        return object.toString();
    }

    protected boolean d(Object object) {
        return this.d.containsKey(this.a(object));
    }

    private void h() {
        Iterator<JsonListEntry> iterator = this.d.values().iterator();
        while (iterator.hasNext()) {
            JsonListEntry jsonlistentry = iterator.next();
            if (jsonlistentry.hasExpired()) this.d.remove(jsonlistentry.getKey());
        }
    }

    protected JsonListEntry a(JsonObject jsonobject) {
        return new JsonListEntry(null, jsonobject);
    }

    protected Map e() {
        return this.d;
    }

    public void save() throws IOException { // CraftBukkit - Added throws
        if (SpigotConfig.disableSaving) return; // CobelPvP
        Collection collection = this.d.values();
        String s = this.b.toJson(collection);
        BufferedWriter bufferedwriter = null;

        try {
            bufferedwriter = Files.newWriter(this.c, Charsets.UTF_8);
            bufferedwriter.write(s);
        } finally {
            IOUtils.closeQuietly(bufferedwriter);
        }
    }

    public void load() throws IOException { // CraftBukkit - Added throws
        Collection collection = null;
        BufferedReader bufferedreader = null;

        try {
            bufferedreader = Files.newReader(this.c, Charsets.UTF_8);
            collection = (Collection) this.b.fromJson(bufferedreader, f);
        // Spigot Start
        } catch ( java.io.FileNotFoundException ex )
        {
            org.bukkit.Bukkit.getLogger().log( java.util.logging.Level.INFO, "Unable to find file {0}, creating it.", this.c );
        } catch ( net.minecraft.util.com.google.gson.JsonSyntaxException ex )
        {
            org.bukkit.Bukkit.getLogger().log( java.util.logging.Level.WARNING, "Unable to read file {0}, backing it up to {0}.backup and creating new copy.", this.c );
            File backup = new File( this.c + ".backup" );
            this.c.renameTo( backup );
            this.c.delete();
        // Spigot End
        } finally {
            IOUtils.closeQuietly(bufferedreader);
        }

        if (collection != null) {
            this.d.clear();
            Iterator iterator = collection.iterator();

            while (iterator.hasNext()) {
                JsonListEntry jsonlistentry = (JsonListEntry) iterator.next();

                if (jsonlistentry.getKey() != null) {
                    this.d.put(this.a(jsonlistentry.getKey()), jsonlistentry);
                }
            }
        }
    }
}
