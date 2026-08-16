package top.ydog01.mmagic.spell.node;

import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellModifiers;
import top.ydog01.mmagic.spell.SpellNode;

import java.util.List;

public final class ModifierNodes {
    private ModifierNodes() {
    }

    public abstract static class ModifierNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult,
                                     SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public abstract SpellModifiers outputModifiers(SpellModifiers in);
    }

    public static final class FireModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.FIRE);
        }
    }

    public static final class IceModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.ICE);
        }
    }

    public static final class PoisonModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.POISON);
        }
    }

    public static final class WitherModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.WITHER);
        }
    }

    public static final class LevitateModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.LEVITATE);
        }
    }

    public static final class WaterModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.WATER);
        }
    }

    public static final class HomingModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.HOMING);
        }
    }

    public static final class HealModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.withHeal(paramFloat("amount"));
        }
    }

    public static final class BurstModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.withBurst(paramFloat("radius"));
        }
    }

    public static final class BounceModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.withBounce(paramInt("times"));
        }
    }

    public static final class PierceModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.withPierce(paramInt("times"));
        }
    }

    public static final class GravityModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.GRAVITY);
        }
    }

    public static final class PierceWallModifierNode extends ModifierNode {
        @Override
        public SpellModifiers outputModifiers(SpellModifiers in) {
            return in.with(SpellModifiers.PIERCE_BLOCK);
        }
    }
}
