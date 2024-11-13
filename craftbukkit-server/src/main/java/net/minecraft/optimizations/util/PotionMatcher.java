package net.minecraft.optimizations.util;

import java.util.Map;

import org.bukkit.potion.PotionType;

public class PotionMatcher {
    private PotionType type;
    private Integer level;
    private Boolean extended;
    private Boolean splash;

    public PotionMatcher(Map conf) {
        if (conf.containsKey("type")) {
            try {
                this.type = PotionType.valueOf((String)conf.get("type"));
            } catch (IllegalArgumentException var2) {
            }
        }

        if (conf.containsKey("level")) {
            this.level = (Integer)conf.get("level");
        }

        if (conf.containsKey("extended")) {
            this.extended = (Boolean)conf.get("extended");
        }

        if (conf.containsKey("splash")) {
            this.splash = (Boolean)conf.get("splash");
        }

    }

    public boolean matches(int damage) {
        if (this.type != null && this.type.getDamageValue() != (damage & 15)) {
            return false;
        } else if (this.level != null && this.level != (damage >> 5 & 1) + 1) {
            return false;
        } else if (this.extended != null && this.extended != ((damage >> 6 & 1) == 1)) {
            return false;
        } else {
            return this.splash == null || this.splash == ((damage >> 14 & 1) == 1);
        }
    }
}
