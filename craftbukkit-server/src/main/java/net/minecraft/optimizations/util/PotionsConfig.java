package net.minecraft.optimizations.util;

import java.io.File;
import java.util.*;

import org.bukkit.configuration.file.YamlConfiguration;

public class PotionsConfig {
    private static final YamlConfiguration conf = YamlConfiguration.loadConfiguration(new File("config/server", "potions.yml"));
    private static final List<PotionMatcher> disableBrewing = new ArrayList();
    private static final Map<Integer, Boolean> disableBrewingCache = new HashMap();

    static {
        List<?> disable = conf.getList("disable-brewing");
        if (disable != null) {
            Iterator var2 = disable.iterator();

            while(var2.hasNext()) {
                Object obj = var2.next();
                if (obj instanceof Map) {
                    disableBrewing.add(new PotionMatcher((Map)obj));
                }
            }
        }

    }

    public PotionsConfig() {
    }

    public static boolean isBrewingDisabled(int damage) {
        Boolean cached = (Boolean)disableBrewingCache.get(damage);
        if (cached != null) {
            return cached;
        } else {
            Iterator var3 = disableBrewing.iterator();

            while(var3.hasNext()) {
                PotionMatcher potion = (PotionMatcher)var3.next();
                if (potion.matches(damage)) {
                    disableBrewingCache.put(damage, true);
                    return true;
                }
            }

            disableBrewingCache.put(damage, false);
            return false;
        }
    }
}
