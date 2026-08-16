package top.ydog01.mmagic.spell.node;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellModifiers;
import top.ydog01.mmagic.spell.SpellNode;

import java.util.List;

public final class UtilityNodes {
    private UtilityNodes() {
    }

    public static final class LightningNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            ServerLevel level = ctx.level();
            LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            bolt.setPos(at);
            level.addFreshEntity(bolt);
            return null;
        }
    }

    public static final class TeleportNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            ctx.caster().teleportTo(at.x, at.y, at.z);
            return null;
        }
    }

    public static final class KnockbackPulseNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            AABB box = new AABB(at, at).inflate(6.0);
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != ctx.caster())) {
                double dx = e.getX() - at.x;
                double dz = e.getZ() - at.z;
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.001) {
                    e.knockback(1.4, dx / len, dz / len);
                }
            }
            return null;
        }
    }

    public static final class PullPulseNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            AABB box = new AABB(at, at).inflate(8.0);
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != ctx.caster())) {
                Vec3 dir = at.subtract(e.position());
                if (dir.lengthSqr() > 0.001) {
                    e.setDeltaMovement(e.getDeltaMovement().add(dir.normalize().scale(0.9)));
                }
            }
            return null;
        }
    }

    public static final class FreezePulseNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            AABB box = new AABB(at, at).inflate(5.0);
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != ctx.caster())) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            }
            return null;
        }
    }

    public static final class AreaDamageNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            AABB box = new AABB(at, at).inflate(4.0);
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != ctx.caster())) {
                e.hurt(ctx.level().damageSources().magic(), 6.0f * damageMult);
            }
            return null;
        }
    }

    public static final class FireNovaNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            AABB box = new AABB(at, at).inflate(4.0);
            for (LivingEntity e : ctx.level().getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != ctx.caster())) {
                e.setRemainingFireTicks(100);
            }
            return null;
        }
    }

    public static final class LaunchNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            ctx.caster().setDeltaMovement(ctx.caster().getDeltaMovement().add(0.0, 0.9, 0.0));
            return null;
        }
    }
}
