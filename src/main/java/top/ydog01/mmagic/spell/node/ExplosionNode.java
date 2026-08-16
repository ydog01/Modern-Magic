package top.ydog01.mmagic.spell.node;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellModifiers;
import top.ydog01.mmagic.spell.SpellNode;

import java.util.List;

public class ExplosionNode extends SpellNode {
    @Override
    public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {

        float radius = paramFloat("radius");
        float damage = paramFloat("damage") * damageMult;
        boolean terrain = paramBool("destroy_terrain");
        Level.ExplosionInteraction interaction =
                terrain ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
        ctx.level().explode(ctx.caster(),
                ctx.caster().damageSources().explosion(ctx.caster(), ctx.caster()),
                new FixedDamageCalculator(damage),
                at.x, at.y, at.z, radius, false, interaction);
        return null;
    }

    @Override
    public boolean keepVelocity() {
        return false;
    }

    private static final class FixedDamageCalculator extends ExplosionDamageCalculator {
        private final float damage;

        FixedDamageCalculator(float damage) {
            this.damage = damage;
        }

        @Override
        public float getEntityDamageAmount(Explosion explosion, Entity entity) {
            return damage;
        }
    }
}
