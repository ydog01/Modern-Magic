package top.ydog01.mmagic.util;

public final class SpellCost {
    public static final int BASE_COOLDOWN = 200;

    private SpellCost() {
    }

    public static int totalCost(int maxMana, double regen, int cooldown) {
        return manaCost(maxMana) + regenCost(regen) + cooldownCost(cooldown);
    }

    public static int manaCost(int maxMana) {
        return tri(Math.max(0, maxMana / 10));
    }

    public static int regenCost(double regen) {
        return tri(Math.max(0, (int) Math.round(regen * 2.0)));
    }

    public static int cooldownCost(int cooldown) {
        return tri(Math.max(0, (BASE_COOLDOWN - cooldown) / 5));
    }

    private static int tri(int n) {
        return n * (n + 1) / 2;
    }

    public static int charge(int curMaxMana, double curRegen, int curCooldown,
                             int newMaxMana, double newRegen, int newCooldown) {
        int diff = totalCost(newMaxMana, newRegen, newCooldown)
                - totalCost(curMaxMana, curRegen, curCooldown);
        return Math.max(1, diff);
    }
}
