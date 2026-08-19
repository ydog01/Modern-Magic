package top.ydog01.mmagic.spell.node_implementations;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import top.ydog01.mmagic.entity.MagicMissileEntity;
import top.ydog01.mmagic.init.ModEntityTypes;
import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.SpellRegistry;

import java.util.ArrayList;
import java.util.List;

public class TriggerMissileNode extends SpellNode {
    public static final String ID = "trigger_missile";

    private MagicMissileEntity missile;
    private boolean launched = false;
    private List<SpellNode.Connection> continuation = new ArrayList<>();

    public TriggerMissileNode() {
        super(ID, SpellRegistry.get(ID));
    }

    @Override
    public ExecutionResult execute(SpellContext ctx) {
        launched = false;
        missile = null;
        continuation.clear();
        return null;
    }

    @Override
    public ExecutionResult tick(SpellContext ctx) {
        if (!launched) {
            if (!(ctx.level() instanceof ServerLevel level)) {
                return ExecutionResult.empty();
            }

            missile = new MagicMissileEntity(
                    ModEntityTypes.MAGIC_MISSILE.get(),
                    ctx.caster(),
                    level);

            float damage = paramFloat("damage");
            float speed = paramFloat("speed");

            missile.setDamage(damage);
            missile.setPos(ctx.getCurrentPosition());
            missile.setDeltaMovement(ctx.getCurrentVelocity().scale(speed));

            continuation.clear();
            for (int i = 0; i < getOutputCount(); i++) {
                continuation.addAll(getConnections(i));
            }
            missile.setTrigger(true, MagicMissileEntity.MAX_LIFETIME_TICKS,
                    ctx.caster().getUUID(), ctx.wandId(), continuation);

            level.addFreshEntity(missile);
            launched = true;
            return null;
        }

        return null;
    }

    @Override
    public ExecutionResult onCollideEntity(Entity entity, SpellContext ctx) {
        if (entity instanceof LivingEntity living) {
            float damage = paramFloat("damage");
            living.hurt(living.damageSources().magic(), damage);
            if (missile != null && !missile.isRemoved()) {
                missile.discard();
            }
            return ExecutionResult.continueToAlive(0);
        }
        return ExecutionResult.empty();
    }

    @Override
    public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
        if (missile != null && !missile.isRemoved()) {
            missile.discard();
        }
        return ExecutionResult.continueToAlive(0);
    }

    @Override
    public ExecutionResult onUpdate(SpellContext ctx) {
        return ExecutionResult.empty();
    }

    @Override
    public int getManaCost() {
        return super.getManaCost() + Math.round(paramFloat("damage") * 0.5f) + Math.round(paramFloat("speed"));
    }
}