package net.minecraft.server;

import org.apache.commons.math3.util.FastMath;

public class PacketPlayOutSpawnEntity extends Packet {

    public int a;
    public int b;
    public int c;
    public int d;
    private int e;
    private int f;
    private int g;
    private int h;
    private int i;
    public int j;
    private int k;

    public PacketPlayOutSpawnEntity() {}

    public PacketPlayOutSpawnEntity(Entity entity, int i) {
        this(entity, i, 0);
    }

    public PacketPlayOutSpawnEntity(Entity entity, int i, int j) {
        this.a = entity.getId();
        this.b = (int) FastMath.floor(entity.locX * 32.0D);
        this.c = (int) FastMath.floor(entity.locY * 32.0D);
        this.d = (int) FastMath.floor(entity.locZ * 32.0D);
        this.h = MathHelper.d(entity.pitch * 256.0F / 360.0F);
        this.i = MathHelper.d(entity.yaw * 256.0F / 360.0F);
        this.j = i;
        this.k = j;
        if (j > 0) {
            double d0 = entity.motX;
            double d1 = entity.motY;
            double d2 = entity.motZ;
            double d3 = 3.9D;

            if (d0 < -d3) {
                d0 = -d3;
            }

            if (d1 < -d3) {
                d1 = -d3;
            }

            if (d2 < -d3) {
                d2 = -d3;
            }

            if (d0 > d3) {
                d0 = d3;
            }

            if (d1 > d3) {
                d1 = d3;
            }

            if (d2 > d3) {
                d2 = d3;
            }

            this.e = (int) (d0 * 8000.0D);
            this.f = (int) (d1 * 8000.0D);
            this.g = (int) (d2 * 8000.0D);
        }
    }

    public void a(PacketDataSerializer packetdataserializer) {
        this.a = packetdataserializer.a();
        this.j = packetdataserializer.readByte();
        this.b = packetdataserializer.readInt();
        this.c = packetdataserializer.readInt();
        this.d = packetdataserializer.readInt();
        this.h = packetdataserializer.readByte();
        this.i = packetdataserializer.readByte();
        this.k = packetdataserializer.readInt();
        if (this.k > 0) {
            this.e = packetdataserializer.readShort();
            this.f = packetdataserializer.readShort();
            this.g = packetdataserializer.readShort();
        }
    }

    public void b(PacketDataSerializer packetdataserializer) {
        packetdataserializer.b(this.a);
        packetdataserializer.writeByte(this.j);
        // Spigot start - protocol patch
        if (j == 71 && packetdataserializer.version >= 28 )
        {
            // North: 0   256
            // West:  64  192
            // South: 128 128
            // East:  192 320
            switch ( k) {
                case 0:
                    d += 32;
                    i = 0;
                    break;
                case 1:
                    b -= 32;
                    i = 64;
                    break;
                case 2:
                    d -= 32;
                    i = 128;
                    break;
                case 3:
                    b += 32;
                    i = 192;
                    break;
            }
        }
        if (j == 70 && packetdataserializer.version >= 36 )
        {
            int id = k & 0xFFFF;
            int data = k >> 16;
            k = id | ( data << 12 );
        }

        if (( j == 50 || j == 70 || j == 74 ) && packetdataserializer.version >= 16 ) // Spigot Update - 20140916a
        {
            c -= 16;
        }

        // Spigot end
        packetdataserializer.writeInt(this.b);
        packetdataserializer.writeInt(this.c);
        packetdataserializer.writeInt(this.d);
        packetdataserializer.writeByte(this.h);
        packetdataserializer.writeByte(this.i);
        packetdataserializer.writeInt(this.k);
        if (this.k > 0) {
            packetdataserializer.writeShort(this.e);
            packetdataserializer.writeShort(this.f);
            packetdataserializer.writeShort(this.g);
        }
    }

    public void a(PacketPlayOutListener packetplayoutlistener) {
        packetplayoutlistener.a(this);
    }

    public String b() {
        return String.format("id=%d, type=%d, x=%.2f, y=%.2f, z=%.2f", new Object[] { Integer.valueOf(this.a), Integer.valueOf(this.j), Float.valueOf((float) this.b / 32.0F), Float.valueOf((float) this.c / 32.0F), Float.valueOf((float) this.d / 32.0F)});
    }

    public void a(int i) {
        this.b = i;
    }

    public void b(int i) {
        this.c = i;
    }

    public void c(int i) {
        this.d = i;
    }

    public void d(int i) {
        this.e = i;
    }

    public void e(int i) {
        this.f = i;
    }

    public void f(int i) {
        this.g = i;
    }

    public void handle(PacketListener packetlistener) {
        this.a((PacketPlayOutListener) packetlistener);
    }
}

