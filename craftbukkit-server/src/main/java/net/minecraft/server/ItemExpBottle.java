package net.minecraft.server;

public class ItemExpBottle extends Item {

    public ItemExpBottle() {
        this.a(CreativeModeTab.f);
    }

    public ItemStack a(ItemStack itemstack, World world, EntityHuman entityhuman) {

        // CobelPvP start
        if (!world.isStatic && world.addEntity(new EntityThrownExpBottle(world, entityhuman))) {
            if (!entityhuman.abilities.canInstantlyBuild) {
                --itemstack.count;
            }

            world.makeSound(entityhuman, "random.bow", 0.5F, 0.4F / (g.nextFloat() * 0.4F + 0.8F));
        }
        // CobelPvP end

        return itemstack;
    }
}