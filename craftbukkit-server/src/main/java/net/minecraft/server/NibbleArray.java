package net.minecraft.server;

public class NibbleArray {

    public final byte[] a;

    public NibbleArray(int i, int j) {
        this.a = new byte[i >> 1];
        // this.b = j; // CobelPvP
        // this.c = j + 4; // CobelPvP
    }

    public NibbleArray(byte[] abyte, int i) {
        // CobelPvP start
        if (abyte.length != 2048 || i != 4) {
            throw new IllegalStateException("NibbleArrays should be 2048 in length with 4 bits per nibble.");
        }
        // CobelPvP end
        this.a = abyte;
    }

    public int a(int i, int j, int k) {
        // CobelPvP start
        int position = j << 8 | k << 4 | i;
        return this.a[position >> 1] >> ((position & 1) << 2) & 15;
        // CobelPvP end
    }

    public void a(int i, int j, int k, int l) {
        // CobelPvP start
        int position = j << 8 | k << 4 | i; // CobelPvP
        int shift = (position & 1) << 2;
        this.a[position >> 1] = (byte) (this.a[position >> 1] & ~(15 << shift) | (l & 15) << shift);
        // CobelPvP end
    }

    // CobelPvP start - chunk snapshot api
    public NibbleArray clone() {
        return new NibbleArray(a.clone(), 4);
    }
    // CobelPvP end
}
