package net.minecraft.server;

import net.minecraft.optimizations.pathsearch.PositionPathSearchType;
import org.apache.commons.math3.util.FastMath;

import java.util.Random;

public class PathfinderGoalFleeSun extends PathfinderGoal {

    private EntityCreature a;
    private double b;
    private double c;
    private double d;
    private double e;
    private World f;

    public PathfinderGoalFleeSun(EntityCreature entitycreature, double d0) {
        this.a = entitycreature;
        this.e = d0;
        this.f = entitycreature.world;
        this.a(1);
    }

    public boolean a() {
        if (!this.f.w()) {
            return false;
        } else if (!this.a.isBurning()) {
            return false;
        } else if (!this.f.i((int) FastMath.floor(this.a.locX), (int) this.a.boundingBox.b, (int) FastMath.floor(this.a.locZ))) {
            return false;
        } else {
            Vec3D vec3d = this.f();

            if (vec3d == null) {
                return false;
            } else {
                this.b = vec3d.a;
                this.c = vec3d.b;
                this.d = vec3d.c;
                return true;
            }
        }
    }

    public boolean b() {
        return !this.a.getNavigation().g();
    }

    public void c() {
        this.a.getNavigation().a(PositionPathSearchType.FLEESUN, this.b, this.c, this.d, this.e); // CobelPvP
    }

    private Vec3D f() {
        Random random = this.a.aI();

        for (int i = 0; i < 10; ++i) {
            int j = (int) FastMath.floor(this.a.locX + (double) random.nextInt(20) - 10.0D);
            int k = (int) FastMath.floor(this.a.boundingBox.b + (double) random.nextInt(6) - 3.0D);
            int l = (int) FastMath.floor(this.a.locZ + (double) random.nextInt(20) - 10.0D);

            if (!this.f.i(j, k, l) && this.a.a(j, k, l) < 0.0F) {
                return Vec3D.a((double) j, (double) k, (double) l);
            }
        }

        return null;
    }
}
