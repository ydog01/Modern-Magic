package top.ydog01.mmagic.spell.node_implementations;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.SpellRegistry;

public class ExplosionNode extends SpellNode {
    public static final String ID = "explosion";

    private boolean executed = false;

    public ExplosionNode() {
        super(ID, SpellRegistry.get(ID));
    }

    @Override
    public ExecutionResult execute(SpellContext ctx) {
        return ExecutionResult.empty();
    }

    @Override
    public ExecutionResult tick(SpellContext ctx) {
        if (executed) {
            return ExecutionResult.continueTo(0);
        }

        if (!(ctx.level() instanceof ServerLevel level)) {
            return ExecutionResult.empty();
        }

        float radius = paramFloat("radius");
        float damage = paramFloat("damage");
        boolean destroyTerrain = paramBool("destroy_terrain");
        Vec3 pos = ctx.getCurrentPosition();

        Level.ExplosionInteraction interaction = destroyTerrain
                ? Level.ExplosionInteraction.BLOCK
                : Level.ExplosionInteraction.NONE;

        level.explode(
                ctx.caster(),
                ctx.caster().damageSources().explosion(ctx.caster(), ctx.caster()),
                new FixedDamageCalculator(damage),
                pos.x, pos.y, pos.z,
                radius,
                false,
                interaction
        );

        executed = true;
        return ExecutionResult.continueTo(0);
    }

    @Override
    public int getManaCost() {
        int base = super.getManaCost();
        float radius = paramFloat("radius");
        float damage = paramFloat("damage");
        boolean destroyTerrain = paramBool("destroy_terrain");

        int extra = 0;
        extra += Math.round(radius * 0.5f);
        extra += Math.round(damage * 0.2f);
        if (destroyTerrain) {
            extra += 2;
        }

        return base + extra;
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
