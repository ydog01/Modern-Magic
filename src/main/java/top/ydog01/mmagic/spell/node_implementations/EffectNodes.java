package top.ydog01.mmagic.spell.node_implementations;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.SpellRegistry;
import top.ydog01.mmagic.spell.SpellNodeType;

public final class EffectNodes {
    private EffectNodes() {}

    public abstract static class SelfEffectNode extends SpellNode {
        protected abstract Holder<MobEffect> getEffect();

        public SelfEffectNode(String id, SpellNodeType type) {
            super(id, type);
        }

        @Override
        public ExecutionResult execute(SpellContext ctx) {
            return ExecutionResult.empty();
        }

        @Override
        public ExecutionResult tick(SpellContext ctx) {
            if (ctx.caster() instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(
                    getEffect(),
                    Math.max(1, paramInt("duration")),
                    paramInt("level")
                ));
            }
            return ExecutionResult.continueTo(0);
        }

        @Override
        public int getManaCost() {
            return super.getManaCost() + paramInt("duration") / 40 + paramInt("level");
        }
    }

    public static class HealNode extends SpellNode {
        public static final String ID = "heal";

        public HealNode() {
            super(ID, SpellRegistry.get(ID));
        }

        @Override
        public ExecutionResult execute(SpellContext ctx) {
            return ExecutionResult.empty();
        }

        @Override
        public ExecutionResult tick(SpellContext ctx) {
            if (ctx.caster() instanceof LivingEntity living) {
                living.heal(paramInt("amount"));
            }
            return ExecutionResult.continueTo(0);
        }

        @Override
        public int getManaCost() {
            return super.getManaCost() + paramInt("amount") / 2;
        }
    }

    public static class SpeedNode extends SelfEffectNode {
        public static final String ID = "speed";

        public SpeedNode() {
            super(ID, SpellRegistry.get(ID));
        }

        @Override
        protected Holder<MobEffect> getEffect() {
            return MobEffects.MOVEMENT_SPEED;
        }
    }

    public static class StrengthNode extends SelfEffectNode {
        public static final String ID = "strength";

        public StrengthNode() {
            super(ID, SpellRegistry.get(ID));
        }

        @Override
        protected Holder<MobEffect> getEffect() {
            return MobEffects.DAMAGE_BOOST;
        }
    }

    public static class AbsorptionNode extends SelfEffectNode {
        public static final String ID = "absorption";

        public AbsorptionNode() {
            super(ID, SpellRegistry.get(ID));
        }

        @Override
        protected Holder<MobEffect> getEffect() {
            return MobEffects.ABSORPTION;
        }
    }
}
