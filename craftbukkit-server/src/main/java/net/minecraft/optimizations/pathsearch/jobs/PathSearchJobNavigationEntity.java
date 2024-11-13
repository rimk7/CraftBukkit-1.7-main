package net.minecraft.optimizations.pathsearch.jobs;

import java.util.UUID;

import net.minecraft.optimizations.pathsearch.AsyncPathfinder;
import net.minecraft.optimizations.pathsearch.cache.SearchCacheEntryEntity;
import net.minecraft.optimizations.pathsearch.cache.SearchCacheEntry;

import net.minecraft.server.Entity;
import net.minecraft.server.EntityInsentient;

public class PathSearchJobNavigationEntity extends PathSearchJob {

    private Entity target;

    public PathSearchJobNavigationEntity(EntityInsentient entity, Entity target, float range, boolean b1, boolean b2, boolean b3, boolean b4) {
        super(entity, range, b1, b2, b3, b4);
        this.target = target;
    }

    @Override
    public PathSearchJob call() throws Exception {
        if(!this.isEntityStillValid()) {
            this.cancel();
        } else if(!this.issued) {
            this.issued = true;
            this.pathEntity = (new AsyncPathfinder(this.chunkCache, this.b1, this.b2, this.b3, this.b4)).a(entity, this.target, this.range);
            this.entity.getNavigation().setSearchResult(this);
            this.cleanup();
        }
        return this;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.target = null;
    }

    public UUID getCacheEntryKey() {
        return this.target.getUniqueID();
    }

    public SearchCacheEntry getCacheEntryValue() {
        return new SearchCacheEntryEntity(this.entity, this.target, this.pathEntity);
    }

    @Override
    public void cancel() {
        this.entity.getNavigation().cancelSearch(this);
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || !(o instanceof PathSearchJobNavigationEntity)) {
            return false;
        }
        return this.getSearchHash() == ((PathSearchJobNavigationEntity)o).getSearchHash();
    }
}