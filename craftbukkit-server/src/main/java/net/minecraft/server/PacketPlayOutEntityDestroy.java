package net.minecraft.server;

public class PacketPlayOutEntityDestroy extends Packet {
    public int[] a;

    public PacketPlayOutEntityDestroy() {
    }

    public PacketPlayOutEntityDestroy(int... aint) {
        this.a = aint;
    }

    public void a(PacketDataSerializer packetdataserializer) {
        this.a = new int[packetdataserializer.readByte()];

        for(int i = 0; i < this.a.length; ++i) {
            this.a[i] = packetdataserializer.readInt();
        }

    }

    public void b(PacketDataSerializer packetdataserializer) {
        int i;
        if (packetdataserializer.version < 16) {
            packetdataserializer.writeByte(this.a.length);

            for(i = 0; i < this.a.length; ++i) {
                packetdataserializer.writeInt(this.a[i]);
            }
        } else {
            packetdataserializer.b(this.a.length);
            int[] var5;
            int var4 = (var5 = this.a).length;

            for(int var3 = 0; var3 < var4; ++var3) {
                i = var5[var3];
                packetdataserializer.b(i);
            }
        }

    }

    public void a(PacketPlayOutListener packetplayoutlistener) {
        packetplayoutlistener.a(this);
    }

    public String b() {
        StringBuilder stringbuilder = new StringBuilder();

        for(int i = 0; i < this.a.length; ++i) {
            if (i > 0) {
                stringbuilder.append(", ");
            }

            stringbuilder.append(this.a[i]);
        }

        return String.format("entities=%d[%s]", this.a.length, stringbuilder);
    }

    public void handle(PacketListener packetlistener) {
        this.a((PacketPlayOutListener)packetlistener);
    }
}
