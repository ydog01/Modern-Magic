package top.ydog01.mmagic.spell;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Visual trail presets contributed by spell modifiers.
 *
 * <p>Nodes override {@link top.ydog01.mmagic.spell.node_api.SpellNode#trailEffect()}
 * to add one of these to the {@link SpellContext}. When a magic missile is launched
 * the collected trails are copied onto the projectile, which then emits them every
 * tick (and in a larger burst on impact).</p>
 */
public enum SpellTrail {
    /** Every missile gets a faint arcane sparkle trail. */
    MAGIC(ParticleTypes.END_ROD, 1, 0.02, 0.0),
    FLAME(ParticleTypes.FLAME, 1, 0.02, 0.005),
    FROST(ParticleTypes.SNOWFLAKE, 1, 0.03, 0.0),
    POISON(ParticleTypes.ITEM_SLIME, 1, 0.02, 0.0),
    WITHER(ParticleTypes.SMOKE, 1, 0.02, 0.0),
    LEVITATION(ParticleTypes.END_ROD, 1, 0.02, 0.01),
    WATER(ParticleTypes.SPLASH, 1, 0.03, 0.0),
    HEAL(ParticleTypes.HEART, 1, 0.02, 0.0),
    HAPPY(ParticleTypes.HAPPY_VILLAGER, 1, 0.02, 0.0),
    ENCHANT(ParticleTypes.ENCHANT, 2, 0.05, 0.1),
    CRIT(ParticleTypes.CRIT, 2, 0.03, 0.0),
    PORTAL(ParticleTypes.PORTAL, 2, 0.05, 0.1),
    CLOUD(ParticleTypes.CLOUD, 1, 0.02, 0.0),
    SOUL(ParticleTypes.SOUL_FIRE_FLAME, 1, 0.02, 0.0),
    ELECTRIC(ParticleTypes.ELECTRIC_SPARK, 2, 0.04, 0.0);

    private final ParticleOptions particle;
    private final int count;
    private final double spread;
    private final double speed;

    SpellTrail(ParticleOptions particle, int count, double spread, double speed) {
        this.particle = particle;
        this.count = count;
        this.spread = spread;
        this.speed = speed;
    }

    /** Emit one segment of the trail at the given position. */
    public void spawn(ServerLevel level, double x, double y, double z) {
        level.sendParticles(particle, x, y, z, count, spread, spread, spread, speed);
    }

    /** Emit a larger burst, used when the missile hits something. */
    public void burst(ServerLevel level, double x, double y, double z) {
        level.sendParticles(particle, x, y, z, count * 6, spread * 4.0, spread * 4.0, spread * 4.0, speed + 0.05);
    }
}