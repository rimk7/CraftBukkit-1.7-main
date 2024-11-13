package net.minecraft.server;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class RegionFile {

    private static final byte[] a = new byte[4096]; // Spigot - note: if this ever changes to not be 4096 bytes, update constructor! // PAIL: empty 4k block
    private final File b;
    private RandomAccessFile c;
    private final int[] d = new int[1024];
    private final int[] e = new int[1024];
    private ArrayList f;
    private int g;
    private long h;

    // CobelPvP start
    private Boolean[] existingChunkCache = new Boolean[1024];

    private boolean isExistingChunkCacheEntrySet(int i, int j) {
        return this.existingChunkCache[i + j * 32] != null;
    }

    private boolean checkExistingChunkCache(int i, int j) {
        return this.existingChunkCache[i + j * 32].booleanValue();
    }

    private void addCoordinatesToCache(int i, int j, boolean result) {
        Boolean a = this.existingChunkCache[i + j * 32];
        if(a == null || a.booleanValue() != result) {
            this.existingChunkCache[i + j * 32] = new Boolean(result);
        }
    }
    // CobelPvP end

    public RegionFile(File file1) {
        this.b = file1;
        this.g = 0;

        try {
            if (file1.exists()) {
                this.h = file1.lastModified();
            }

            this.c = new RandomAccessFile(file1, "rw");
            int i;

            if (this.c.length() < 4096L) {
                // Spigot start - more effecient chunk zero'ing
                this.c.write(RegionFile.a);
                this.c.write(RegionFile.a);
                // Spigot end

                this.g += 8192;
            }

            if ((this.c.length() & 4095L) != 0L) {
                for (i = 0; (long) i < (this.c.length() & 4095L); ++i) {
                    this.c.write(0);
                }
            }

            i = (int) this.c.length() / 4096;
            this.f = new ArrayList(i);

            int j;

            for (j = 0; j < i; ++j) {
                this.f.add(Boolean.valueOf(true));
            }

            this.f.set(0, Boolean.valueOf(false));
            this.f.set(1, Boolean.valueOf(false));
            this.c.seek(0L);

            int k;

            // CobelPvP start
            ByteBuffer header = ByteBuffer.allocate(8192);
            while (header.hasRemaining())  {
                if (this.c.getChannel().read(header) == -1) throw new EOFException();
            }
            header.clear();
            IntBuffer headerAsInts = header.asIntBuffer();
            // CobelPvP end
            for (j = 0; j < 1024; ++j) {
                k = headerAsInts.get(); // CobelPvP
                this.d[j] = k;
                if (k != 0 && (k >> 8) + (k & 255) <= this.f.size()) {
                    for (int l = 0; l < (k & 255); ++l) {
                        this.f.set((k >> 8) + l, Boolean.valueOf(false));
                    }
                }
            }

            for (j = 0; j < 1024; ++j) {
                k = headerAsInts.get(); // CobelPvP
                this.e[j] = k;
            }
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    // CraftBukkit start - This is a copy (sort of) of the method below it, make sure they stay in sync
    public boolean chunkExists(int i, int j) { // CobelPvP - move synchronization inside method
        if (this.d(i, j)) {
            return false;
        } else {
            // CobelPvP start
            if(this.isExistingChunkCacheEntrySet(i, j)) {
                return this.checkExistingChunkCache(i, j);
            }
            synchronized(this) {
            // CobelPvP end
            try {
                int k = this.e(i, j);

                if (k == 0) {
                    return false;
                } else {
                    int l = k >> 8;
                    int i1 = k & 255;

                    if (l + i1 > this.f.size()) {
                        return false;
                    }

                    this.c.seek((long) (l * 4096));
                    int j1 = this.c.readInt();

                    if (j1 > 4096 * i1 || j1 <= 0) {
                        return false;
                    }

                    byte b0 = this.c.readByte();
                    // CobelPvP start
                    boolean foundChunk = (b0 == 1 || b0 == 2);
                    this.addCoordinatesToCache(i, j, foundChunk);
                    return foundChunk;
                    // CobelPvP end
                }
            } catch (IOException ioexception) {
                return false;
            }
            }
        }
    }
    // CraftBukkit end

    public synchronized DataInputStream a(int i, int j) {
        if (this.d(i, j)) {
            return null;
        } else {
            try {
                int k = this.e(i, j);

                if (k == 0) {
                    return null;
                } else {
                    int l = k >> 8;
                    int i1 = k & 255;

                    if (l + i1 > this.f.size()) {
                        return null;
                    } else {
                        this.c.seek((long) (l * 4096));
                        int j1 = this.c.readInt();

                        if (j1 > 4096 * i1) {
                            return null;
                        } else if (j1 <= 0) {
                            return null;
                        } else {
                            byte b0 = this.c.readByte();
                            byte[] abyte;

                            if (b0 == 1) {
                                this.addCoordinatesToCache(i, j, true); // CobelPvP
                                abyte = new byte[j1 - 1];
                                this.c.read(abyte);
                                return new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(abyte))));
                            } else if (b0 == 2) {
                                this.addCoordinatesToCache(i, j, true); // CobelPvP
                                abyte = new byte[j1 - 1];
                                this.c.read(abyte);
                                return new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(abyte))));
                            } else {
                                this.addCoordinatesToCache(i, j, false); // CobelPvP
                                return null;
                            }
                        }
                    }
                }
            } catch (IOException ioexception) {
                return null;
            }
        }
    }

    public DataOutputStream b(int i, int j) {
        return this.d(i, j) ? null : new DataOutputStream(new java.io.BufferedOutputStream(new DeflaterOutputStream(new ChunkBuffer(this, i, j)))); // Spigot - use a BufferedOutputStream to greatly improve file write performance
    }

    protected synchronized void a(int i, int j, byte[] abyte, int k) {
        try {
            int l = this.e(i, j);
            int i1 = l >> 8;
            int j1 = l & 255;
            int k1 = (k + 5) / 4096 + 1;

            if (k1 >= 256) {
                return;
            }

            if (i1 != 0 && j1 == k1) {
                this.a(i1, abyte, k);
            } else {
                int l1;

                for (l1 = 0; l1 < j1; ++l1) {
                    this.f.set(i1 + l1, Boolean.valueOf(true));
                }

                l1 = this.f.indexOf(Boolean.valueOf(true));
                int i2 = 0;
                int j2;

                if (l1 != -1) {
                    for (j2 = l1; j2 < this.f.size(); ++j2) {
                        if (i2 != 0) {
                            if (((Boolean) this.f.get(j2)).booleanValue()) {
                                ++i2;
                            } else {
                                i2 = 0;
                            }
                        } else if (((Boolean) this.f.get(j2)).booleanValue()) {
                            l1 = j2;
                            i2 = 1;
                        }

                        if (i2 >= k1) {
                            break;
                        }
                    }
                }

                if (i2 >= k1) {
                    i1 = l1;
                    this.a(i, j, l1 << 8 | k1);

                    for (j2 = 0; j2 < k1; ++j2) {
                        this.f.set(i1 + j2, Boolean.valueOf(false));
                    }

                    this.a(i1, abyte, k);
                } else {
                    this.c.seek(this.c.length());
                    i1 = this.f.size();

                    for (j2 = 0; j2 < k1; ++j2) {
                        this.c.write(a);
                        this.f.add(Boolean.valueOf(false));
                    }

                    this.g += 4096 * k1;
                    this.a(i1, abyte, k);
                    this.a(i, j, i1 << 8 | k1);
                }
            }

            this.b(i, j, (int) (MinecraftServer.ar() / 1000L));
            this.addCoordinatesToCache(i, j, true); // CobelPvP
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    private void a(int i, byte[] abyte, int j) throws IOException { // CraftBukkit - added throws
        this.c.seek((long) (i * 4096));
        this.c.writeInt(j + 1);
        this.c.writeByte(2);
        this.c.write(abyte, 0, j);
    }

    private boolean d(int i, int j) {
        return i < 0 || i >= 32 || j < 0 || j >= 32;
    }

    private int e(int i, int j) {
        return this.d[i + j * 32];
    }

    public boolean c(int i, int j) {
        return this.e(i, j) != 0;
    }

    private void a(int i, int j, int k) throws IOException { // CraftBukkit - added throws
        this.d[i + j * 32] = k;
        this.c.seek((long) ((i + j * 32) * 4));
        this.c.writeInt(k);
    }

    private void b(int i, int j, int k) throws IOException { // CraftBukkit - added throws
        this.e[i + j * 32] = k;
        this.c.seek((long) (4096 + (i + j * 32) * 4));
        this.c.writeInt(k);
    }

    public void c() throws IOException { // CraftBukkit - added throws
        if (this.c != null) {
            this.c.close();
        }
    }
}
