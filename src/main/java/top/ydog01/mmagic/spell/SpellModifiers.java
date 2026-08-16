package top.ydog01.mmagic.spell;

public final class SpellModifiers {
    public static final int FIRE = 1;
    public static final int ICE = 1 << 1;
    public static final int POISON = 1 << 2;
    public static final int WITHER = 1 << 3;
    public static final int LEVITATE = 1 << 4;
    public static final int HEAL = 1 << 5;
    public static final int WATER = 1 << 6;
    public static final int HOMING = 1 << 7;
    public static final int BURST = 1 << 8;
    public static final int GRAVITY = 1 << 9;
    public static final int PIERCE_BLOCK = 1 << 10;

    private final int bits;
    private final float healAmount;
    private final float burstRadius;
    private final int bounces;
    private final int pierces;

    public SpellModifiers() {
        this(0, 4.0f, 2.0f, 0, 0);
    }

    public SpellModifiers(int bits, float healAmount, float burstRadius, int bounces, int pierces) {
        this.bits = bits;
        this.healAmount = healAmount;
        this.burstRadius = burstRadius;
        this.bounces = bounces;
        this.pierces = pierces;
    }

    public SpellModifiers with(int flag) {
        return new SpellModifiers(bits | flag, healAmount, burstRadius, bounces, pierces);
    }

    public SpellModifiers withHeal(float amount) {
        return new SpellModifiers(bits | HEAL, amount, burstRadius, bounces, pierces);
    }

    public SpellModifiers withBurst(float radius) {
        return new SpellModifiers(bits | BURST, healAmount, radius, bounces, pierces);
    }

    public SpellModifiers withBounce(int count) {
        return new SpellModifiers(bits, healAmount, burstRadius, bounces + count, pierces);
    }

    public SpellModifiers withPierce(int count) {
        return new SpellModifiers(bits, healAmount, burstRadius, bounces, pierces + count);
    }

    public boolean has(int flag) {
        return (bits & flag) != 0;
    }

    public int bits() {
        return bits;
    }

    public float healAmount() {
        return healAmount;
    }

    public float burstRadius() {
        return burstRadius;
    }

    public int bounces() {
        return bounces;
    }

    public int pierces() {
        return pierces;
    }
}
