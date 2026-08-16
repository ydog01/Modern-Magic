package top.ydog01.mmagic.spell.node;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellModifiers;
import top.ydog01.mmagic.spell.SpellNode;

import java.util.List;

public final class EffectNodes {
    private EffectNodes() {
    }

    public abstract static class SelfEffectNode extends SpellNode {
        protected abstract Holder<MobEffect> effect();

        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            if (ctx.caster() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(effect(),
                        Math.max(1, paramInt("duration")), paramInt("level")));
            }
            return null;
        }
    }

    public static final class HealNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            if (ctx.caster() instanceof LivingEntity living) {
                living.heal(paramInt("amount"));
            }
            return null;
        }
    }

    public static final class StabilizeNode extends SpellNode {
        private static final net.minecraft.resources.ResourceLocation MOD_ID =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("modern_magic", "stabilize");

        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            LivingEntity caster = ctx.caster();
            net.minecraft.world.entity.ai.attributes.AttributeInstance inst =
                    caster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
            if (inst != null) {
                inst.removeModifier(MOD_ID);
                inst.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(MOD_ID,
                        1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                int duration = Math.max(20, paramInt("duration"));
                top.ydog01.mmagic.spell.SpellScheduler.schedule(duration, () -> {
                    net.minecraft.world.entity.ai.attributes.AttributeInstance i =
                            caster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
                    if (i != null) {
                        i.removeModifier(MOD_ID);
                    }
                });
            }
            return null;
        }
    }

    public static final class SpeedNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED; }
    }

    public static final class StrengthNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.DAMAGE_BOOST; }
    }

    public static final class InvisibilityNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.INVISIBILITY; }
    }

    public static final class FireResistNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE; }
    }

    public static final class RegenNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.REGENERATION; }
    }

    public static final class NightVisionNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.NIGHT_VISION; }
    }

    public static final class JumpBoostNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.JUMP; }
    }

    public static final class SlowFallNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.SLOW_FALLING; }
    }

    public static final class WaterBreathNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.WATER_BREATHING; }
    }

    public static final class AbsorptionNode extends SelfEffectNode {
        @Override protected Holder<MobEffect> effect() { return net.minecraft.world.effect.MobEffects.ABSORPTION; }
    }
}
