package top.ydog01.mmagic.spell.node_implementations;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.SpellRegistry;

public final class UtilityNodes {
    private UtilityNodes() {}

    public static class LightningNode extends SpellNode {
        public static final String ID = "lightning";
        private boolean executed = false;

        public LightningNode() {
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
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            bolt.setPos(ctx.getCurrentPosition());
            level.addFreshEntity(bolt);
            executed = true;
            return ExecutionResult.continueTo(0);
        }
    }

    public static class TeleportNode extends SpellNode {
        public static final String ID = "teleport";
        private boolean executed = false;

        public TeleportNode() {
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
            Vec3 at = ctx.getCurrentPosition();
            ctx.caster().teleportTo(at.x, at.y, at.z);
            executed = true;
            return ExecutionResult.continueTo(0);
        }
    }

    public static class LaunchNode extends SpellNode {
        public static final String ID = "launch";
        private boolean executed = false;

        public LaunchNode() {
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
            LivingEntity caster = ctx.caster();
            caster.setDeltaMovement(caster.getDeltaMovement().add(0, 0.9, 0));
            caster.hurtMarked = true;
            if (caster instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
            }
            executed = true;
            return ExecutionResult.continueTo(0);
        }
    }

    public static class KnockbackPulseNode extends SpellNode {
        public static final String ID = "knockback_pulse";
        private boolean executed = false;

        public KnockbackPulseNode() {
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
            Vec3 at = ctx.getCurrentPosition();
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(6.0),
                    ent -> ent.isAlive() && ent != ctx.caster())) {
                double dx = e.getX() - at.x;
                double dz = e.getZ() - at.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.001) {
                    // LivingEntity#knockback subtracts the supplied vector, so
                    // invert it to push entities away from the pulse center.
                    e.knockback(1.4, -dx / len, -dz / len);
                }
            }
            executed = true;
            return ExecutionResult.continueTo(0);
        }
    }
}