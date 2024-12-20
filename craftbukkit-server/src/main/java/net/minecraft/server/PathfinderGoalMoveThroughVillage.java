package net.minecraft.server;

import net.minecraft.optimizations.pathsearch.PositionPathSearchType;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PathfinderGoalMoveThroughVillage extends PathfinderGoal {

    private EntityCreature a;
    private double b;
    private PathEntity c;
    private VillageDoor d;
    private boolean e;
    private List f = new ArrayList();

    public PathfinderGoalMoveThroughVillage(EntityCreature entitycreature, double d0, boolean flag) {
        this.a = entitycreature;
        this.b = d0;
        this.e = flag;
        this.a(1);
    }

    public boolean a() {
        this.f();
        if (this.e && this.a.world.w()) {
            return false;
        } else {
            Village village = this.a.world.villages.getClosestVillage((int) FastMath.floor(this.a.locX), (int) FastMath.floor(this.a.locY), (int) FastMath.floor(this.a.locZ), 0);

            if (village == null) {
                return false;
            } else {
                this.d = this.a(village);
                if (this.d == null) {
                    return false;
                } else {
                    boolean flag = this.a.getNavigation().c();

                    this.a.getNavigation().b(false);
                    this.c = this.a.getNavigation().a(PositionPathSearchType.MOVETHROUGHVILLAGE, (double) this.d.locX, (double) this.d.locY, (double) this.d.locZ); // CobelPvP
                    this.a.getNavigation().b(flag);
                    if (this.c != null) {
                        return true;
                    } else {
                        Vec3D vec3d = RandomPositionGenerator.a(this.a, 10, 7, Vec3D.a((double) this.d.locX, (double) this.d.locY, (double) this.d.locZ));

                        if (vec3d == null) {
                            return false;
                        } else {
                            this.a.getNavigation().b(false);
                            this.c = this.a.getNavigation().a(PositionPathSearchType.MOVETHROUGHVILLAGE, vec3d.a, vec3d.b, vec3d.c); // CobelPvP
                            this.a.getNavigation().b(flag);
                            return this.c != null;
                        }
                    }
                }
            }
        }
    }

    public boolean b() {
        if (this.a.getNavigation().g()) {
            return false;
        } else {
            float f = this.a.width + 4.0F;

            return this.a.e((double) this.d.locX, (double) this.d.locY, (double) this.d.locZ) > (double) (f * f);
        }
    }

    public void c() {
        this.a.getNavigation().a(this.c, this.b);
    }

    public void d() {
        if (this.a.getNavigation().g() || this.a.e((double) this.d.locX, (double) this.d.locY, (double) this.d.locZ) < 16.0D) {
            this.f.add(this.d);
        }
    }

    private VillageDoor a(Village village) {
        VillageDoor villagedoor = null;
        int i = Integer.MAX_VALUE;
        List list = village.getDoors();
        Iterator iterator = list.iterator();

        while (iterator.hasNext()) {
            VillageDoor villagedoor1 = (VillageDoor) iterator.next();
            int j = villagedoor1.b((int) FastMath.floor(this.a.locX), (int) FastMath.floor(this.a.locY), (int) FastMath.floor(this.a.locZ));

            if (j < i && !this.a(villagedoor1)) {
                villagedoor = villagedoor1;
                i = j;
            }
        }

        return villagedoor;
    }

    private boolean a(VillageDoor villagedoor) {
        Iterator iterator = this.f.iterator();

        VillageDoor villagedoor1;

        do {
            if (!iterator.hasNext()) {
                return false;
            }

            villagedoor1 = (VillageDoor) iterator.next();
        } while (villagedoor.locX != villagedoor1.locX || villagedoor.locY != villagedoor1.locY || villagedoor.locZ != villagedoor1.locZ);

        return true;
    }

    private void f() {
        if (this.f.size() > 15) {
            this.f.remove(0);
        }
    }
}
