package net.minecraft.server;

import org.apache.commons.math3.util.FastMath;
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.craftbukkit.util.LongObjectHashMap;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
// CraftBukkit end

public final class SpawnerCreature {

    private LongObjectHashMap<Boolean> a = new LongObjectHashMap<Boolean>(); // CraftBukkit - HashMap -> LongObjectHashMap

    public SpawnerCreature() {}

    protected static ChunkPosition getRandomPosition(World world, int i, int j) {
        Chunk chunk = world.getChunkAt(i, j);
        int k = i * 16 + world.random.nextInt(16);
        int l = j * 16 + world.random.nextInt(16);
        int i1 = world.random.nextInt(chunk == null ? world.S() : chunk.h() + 16 - 1);

        return new ChunkPosition(k, i1, l);
    }

    // Spigot start - get entity count only from chunks being processed in b
    private int getEntityCount(WorldServer server, Class oClass)
    {
        int i = 0;
        for ( Long coord : this.a.keySet() )
        {
            int x = LongHash.msw( coord );
            int z = LongHash.lsw( coord );
            if (!server.chunkProviderServer.unloadQueue.contains( coord ) && server.isChunkLoaded( x, z ) )
            {
                i += server.getChunkAt( x, z ).entityCount.get( oClass );
            }
        }
        return i;
    }
    // Spigot end

    public int spawnEntities(WorldServer worldserver, boolean flag, boolean flag1, boolean flag2) {
        if (!flag && !flag1) {
            return 0;
        } else {
            this.a.clear();

            int i;
            int j;

            for (i = 0; i < worldserver.players.size(); ++i) {
                EntityHuman entityhuman = (EntityHuman) worldserver.players.get(i);
                // PaperSpigot start - Affects spawning API
                if (!entityhuman.affectsSpawning)
                    continue;
                // PaperSpigot end
                int k = (int) FastMath.floor(entityhuman.locX / 16.0D);

                j = (int) FastMath.floor(entityhuman.locZ / 16.0D);
                byte b0 = 8;
                // Spigot Start
                b0 = worldserver.spigotConfig.mobSpawnRange;
                b0 = ( b0 > worldserver.spigotConfig.viewDistance ) ? (byte) worldserver.spigotConfig.viewDistance : b0;
                b0 = ( b0 > 8 ) ? 8 : b0;
                // Spigot End

                for (int l = -b0; l <= b0; ++l) {
                    for (int i1 = -b0; i1 <= b0; ++i1) {
                        boolean flag3 = l == -b0 || l == b0 || i1 == -b0 || i1 == b0;

                        // CraftBukkit start - use LongHash and LongObjectHashMap
                        long chunkCoords = LongHash.toLong(l + k, i1 + j);

                        if (!flag3 && worldserver.isChunkLoaded((i1 + l) >> 4, (k + j) >> 4)) { // CobelPvP
                            this.a.put(chunkCoords, false);
                        } else if (!this.a.containsKey(chunkCoords)) {
                            this.a.put(chunkCoords, true);
                        }
                        // CraftBukkit end
                    }
                }
            }

            i = 0;
            ChunkCoordinates chunkcoordinates = worldserver.getSpawn();
            EnumCreatureType[] aenumcreaturetype = EnumCreatureType.values();

            j = aenumcreaturetype.length;

            for (int j1 = 0; j1 < j; ++j1) {
                EnumCreatureType enumcreaturetype = aenumcreaturetype[j1];

                // CraftBukkit start - Use per-world spawn limits
                int limit = enumcreaturetype.b();
                switch (enumcreaturetype) {
                    case MONSTER:
                        limit = worldserver.getWorld().getMonsterSpawnLimit();
                        break;
                    case CREATURE:
                        limit = worldserver.getWorld().getAnimalSpawnLimit();
                        break;
                    case WATER_CREATURE:
                        limit = worldserver.getWorld().getWaterAnimalSpawnLimit();
                        break;
                    case AMBIENT:
                        limit = worldserver.getWorld().getAmbientSpawnLimit();
                        break;
                }

                if (limit == 0) {
                    continue;
                }
                int mobcnt = 0;
                // CraftBukkit end

                if ((!enumcreaturetype.d() || flag1) && (enumcreaturetype.d() || flag) && (!enumcreaturetype.e() || flag2) && (mobcnt = getEntityCount(worldserver, enumcreaturetype.a())) <= limit * this.a.size() / 256) { // Spigot - use per-world limits and use all loaded chunks
                    Iterator iterator = this.a.keySet().iterator();

                    int moblimit = (limit * this.a.size() / 256) - mobcnt + 1; // Spigot - up to 1 more than limit
                    label110:
                    while (iterator.hasNext() && (moblimit > 0)) { // Spigot - while more allowed
                        // CraftBukkit start = use LongHash and LongObjectHashMap
                        long key = ((Long) iterator.next()).longValue();

                        if (!this.a.get(key)) {
                            ChunkPosition chunkposition = getRandomPosition(worldserver, LongHash.msw(key), LongHash.lsw(key));
                            // CraftBukkit end
                            int k1 = chunkposition.x;
                            int l1 = chunkposition.y;
                            int i2 = chunkposition.z;

                            if (!worldserver.getType(k1, l1, i2).r() && worldserver.getType(k1, l1, i2).getMaterial() == enumcreaturetype.c()) {
                                int j2 = 0;
                                int k2 = 0;

                                while (k2 < 3) {
                                    int l2 = k1;
                                    int i3 = l1;
                                    int j3 = i2;
                                    byte b1 = 6;
                                    BiomeMeta biomemeta = null;
                                    GroupDataEntity groupdataentity = null;
                                    int k3 = 0;

                                    while (true) {
                                        if (k3 < 4) {
                                            label103: {
                                                l2 += worldserver.random.nextInt(b1) - worldserver.random.nextInt(b1);
                                                i3 += worldserver.random.nextInt(1) - worldserver.random.nextInt(1);
                                                j3 += worldserver.random.nextInt(b1) - worldserver.random.nextInt(b1);
                                                if (a(enumcreaturetype, worldserver, l2, i3, j3)) {
                                                    float f = (float) l2 + 0.5F;
                                                    float f1 = (float) i3;
                                                    float f2 = (float) j3 + 0.5F;

                                                    if (worldserver.findNearbyPlayerWhoAffectsSpawning((double) f, (double) f1, (double) f2, 24.0D) == null) { // PaperSpigot
                                                        float f3 = f - (float) chunkcoordinates.x;
                                                        float f4 = f1 - (float) chunkcoordinates.y;
                                                        float f5 = f2 - (float) chunkcoordinates.z;
                                                        float f6 = f3 * f3 + f4 * f4 + f5 * f5;

                                                        if (f6 >= 576.0F) {
                                                            if (biomemeta == null) {
                                                                biomemeta = worldserver.a(enumcreaturetype, l2, i3, j3);
                                                                if (biomemeta == null) {
                                                                    break label103;
                                                                }
                                                            }

                                                            EntityInsentient entityinsentient;

                                                            try {
                                                                entityinsentient = (EntityInsentient) biomemeta.b.getConstructor(new Class[] { World.class}).newInstance(new Object[] { worldserver});
                                                            } catch (Exception exception) {
                                                                exception.printStackTrace();
                                                                return i;
                                                            }

                                                            entityinsentient.setPositionRotation((double) f, (double) f1, (double) f2, worldserver.random.nextFloat() * 360.0F, 0.0F);
                                                            if (entityinsentient.canSpawn()) {
                                                                ++j2;
                                                                // CraftBukkit start - Added a reason for spawning this creature, moved entityinsentient.a(groupdataentity) up
                                                                groupdataentity = entityinsentient.prepare(groupdataentity);
                                                                worldserver.addEntity(entityinsentient, SpawnReason.NATURAL);
                                                                // CraftBukkit end
                                                                // Spigot start
                                                                if (--moblimit <= 0 )
                                                                {
                                                                    // If we're past limit, stop spawn
                                                                    continue label110;
                                                                }
                                                                // Spigot end
                                                                if (j2 >= entityinsentient.bB()) {
                                                                    continue label110;
                                                                }
                                                            }

                                                            i += j2;
                                                        }
                                                    }
                                                }

                                                ++k3;
                                                continue;
                                            }
                                        }

                                        ++k2;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return i;
        }
    }

    public static boolean a(EnumCreatureType enumcreaturetype, World world, int i, int j, int k) {
        if (enumcreaturetype.c() == Material.WATER) {
            return world.getType(i, j, k).getMaterial().isLiquid() && world.getType(i, j - 1, k).getMaterial().isLiquid() && !world.getType(i, j + 1, k).r();
        } else if (!World.a((IBlockAccess) world, i, j - 1, k)) {
            return false;
        } else {
            Block block = world.getType(i, j - 1, k);

            return block != Blocks.BEDROCK && !world.getType(i, j, k).r() && !world.getType(i, j, k).getMaterial().isLiquid() && !world.getType(i, j + 1, k).r();
        }
    }

    public static void a(World world, BiomeBase biomebase, int i, int j, int k, int l, Random random) {
        List list = biomebase.getMobs(EnumCreatureType.CREATURE);

        if (!list.isEmpty()) {
            while (random.nextFloat() < biomebase.g()) {
                BiomeMeta biomemeta = (BiomeMeta) WeightedRandom.a(world.random, (Collection) list);
                GroupDataEntity groupdataentity = null;
                int i1 = biomemeta.c + random.nextInt(1 + biomemeta.d - biomemeta.c);
                int j1 = i + random.nextInt(k);
                int k1 = j + random.nextInt(l);
                int l1 = j1;
                int i2 = k1;

                for (int j2 = 0; j2 < i1; ++j2) {
                    boolean flag = false;

                    for (int k2 = 0; !flag && k2 < 4; ++k2) {
                        int l2 = world.i(j1, k1);

                        if (a(EnumCreatureType.CREATURE, world, j1, l2, k1)) {
                            float f = (float) j1 + 0.5F;
                            float f1 = (float) l2;
                            float f2 = (float) k1 + 0.5F;

                            EntityInsentient entityinsentient;

                            try {
                                entityinsentient = (EntityInsentient) biomemeta.b.getConstructor(new Class[] { World.class}).newInstance(new Object[] { world});
                            } catch (Exception exception) {
                                exception.printStackTrace();
                                continue;
                            }

                            entityinsentient.setPositionRotation((double) f, (double) f1, (double) f2, random.nextFloat() * 360.0F, 0.0F);
                            // CraftBukkit start - Added a reason for spawning this creature, moved entityinsentient.a(groupdataentity) up
                            groupdataentity = entityinsentient.prepare(groupdataentity);
                            world.addEntity(entityinsentient, SpawnReason.CHUNK_GEN);
                            // CraftBukkit end
                            flag = true;
                        }

                        j1 += random.nextInt(5) - random.nextInt(5);

                        for (k1 += random.nextInt(5) - random.nextInt(5); j1 < i || j1 >= i + k || k1 < j || k1 >= j + k; k1 = i2 + random.nextInt(5) - random.nextInt(5)) {
                            j1 = l1 + random.nextInt(5) - random.nextInt(5);
                        }
                    }
                }
            }
        }
    }
}
