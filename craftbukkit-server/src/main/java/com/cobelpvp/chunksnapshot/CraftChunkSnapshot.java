package com.cobelpvp.chunksnapshot;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.NBTTagCompound;

public class CraftChunkSnapshot implements ChunkSnapshot
{
    private final ChunkSectionSnapshot[] sections;
    private final List<NBTTagCompound> tileEntities;

    public CraftChunkSnapshot() {
        this.sections = new ChunkSectionSnapshot[16];
        this.tileEntities = new ArrayList<NBTTagCompound>();
    }

    public ChunkSectionSnapshot[] getSections() {
        return this.sections;
    }

    public List<NBTTagCompound> getTileEntities() {
        return this.tileEntities;
    }
}
