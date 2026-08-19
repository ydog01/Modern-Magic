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
    public static final int PICKUP = 1 << 11;
    public static final int DIG = 1 << 12;
    public static final int CHAIN_DIG = 1 << 13;
    public static final int SILK_TOUCH = 1 << 14;
    public static final int LOOTING = 1 << 15;

    private final int bits;
    private final float healAmount;
    private final float burstRadius;
    private final int bounces;
    private final int pierces;
    private final float pickupRadius;
    private final float digRadius;
    private final int digLevel;
    private final boolean digDrops;
    private final float chainRadius;
    private final int chainLevel;
    private final boolean chainDrops;
    private final int lootingLevel;

    public SpellModifiers() { this(0, 4.0f, 2.0f, 0, 0); }
    public SpellModifiers(int bits, float healAmount, float burstRadius, int bounces, int pierces) {
        this(bits, healAmount, burstRadius, bounces, pierces, 4.0f, 2.0f, 1, true, 8.0f, 1, true, 0);
    }
    public SpellModifiers(int bits, float healAmount, float burstRadius, int bounces, int pierces,
                          float pickupRadius, float digRadius, int digLevel, boolean digDrops,
                          float chainRadius, int chainLevel, boolean chainDrops, int lootingLevel) {
        this.bits = bits;
        this.healAmount = healAmount;
        this.burstRadius = burstRadius;
        this.bounces = bounces;
        this.pierces = pierces;
        this.pickupRadius = pickupRadius;
        this.digRadius = digRadius;
        this.digLevel = digLevel;
        this.digDrops = digDrops;
        this.chainRadius = chainRadius;
        this.chainLevel = chainLevel;
        this.chainDrops = chainDrops;
        this.lootingLevel = lootingLevel;
    }
    public int bits() { return bits; }
    public float healAmount() { return healAmount; }
    public float burstRadius() { return burstRadius; }
    public int bounces() { return bounces; }
    public int pierces() { return pierces; }
    public float pickupRadius() { return pickupRadius; }
    public float digRadius() { return digRadius; }
    public int digLevel() { return digLevel; }
    public boolean digDrops() { return digDrops; }
    public float chainRadius() { return chainRadius; }
    public int chainLevel() { return chainLevel; }
    public boolean chainDrops() { return chainDrops; }
    public int lootingLevel() { return lootingLevel; }
    public boolean has(int flag) { return (bits & flag) != 0; }
}
