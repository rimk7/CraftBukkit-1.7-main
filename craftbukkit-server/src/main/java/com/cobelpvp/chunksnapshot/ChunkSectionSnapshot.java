package com.cobelpvp.chunksnapshot;

import net.minecraft.server.NibbleArray;

public class ChunkSectionSnapshot
{
    private final int nonEmptyBlockCount;
    private final int tickingBlockCount;
    private final byte[] blockIds;
    private final NibbleArray blockData;
    private final NibbleArray emittedLight;
    private final NibbleArray skyLight;
    private final int compactId;
    private final byte compactData;
    private final byte compactEmitted;
    private final byte compactSky;

    public ChunkSectionSnapshot(final int nonEmptyBlockCount, final int tickingBlockCount, final byte[] blockIds, final NibbleArray blockData, final NibbleArray emittedLight, final NibbleArray skyLight, final int compactId, final byte compactData, final byte compactEmitted, final byte compactSky) {
        this.nonEmptyBlockCount = nonEmptyBlockCount;
        this.tickingBlockCount = tickingBlockCount;
        this.blockIds = blockIds;
        this.blockData = blockData;
        this.emittedLight = emittedLight;
        this.skyLight = skyLight;
        this.compactId = compactId;
        this.compactData = compactData;
        this.compactEmitted = compactEmitted;
        this.compactSky = compactSky;
    }

    public final int getNonEmptyBlockCount() {
        return this.nonEmptyBlockCount;
    }

    public final int getTickingBlockCount() {
        return this.tickingBlockCount;
    }

    public final byte[] getBlockIds() {
        return this.blockIds;
    }

    public final NibbleArray getBlockData() {
        return this.blockData;
    }

    public final NibbleArray getEmittedLight() {
        return this.emittedLight;
    }

    public final NibbleArray getSkyLight() {
        return this.skyLight;
    }

    public final int getCompactId() {
        return this.compactId;
    }

    public final byte getCompactData() {
        return this.compactData;
    }

    public final byte getCompactEmitted() {
        return this.compactEmitted;
    }

    public final byte getCompactSky() {
        return this.compactSky;
    }
}
