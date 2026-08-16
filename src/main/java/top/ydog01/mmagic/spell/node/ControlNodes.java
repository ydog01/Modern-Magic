package top.ydog01.mmagic.spell.node;

import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellModifiers;
import top.ydog01.mmagic.spell.SpellNode;

import java.util.ArrayList;
import java.util.List;

public final class ControlNodes {
    private ControlNodes() {
    }

    public static final class MultiCastNode extends SpellNode {
        @Override
        public int outputCount() {
            return paramInt("outputs");
        }

        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < outputCount(); i++) {
                list.add(i);
            }
            return list;
        }
    }

    public static final class RandomCastNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return ctx.level().random.nextBoolean() ? List.of(0) : List.of(1);
        }
    }

    public static final class AmplifierNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public float outputDamageMult(float in) {
            return in * paramFloat("mult");
        }
    }

    public static final class AcceleratorNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public float outputSpeedMult(float in) {
            return in * paramFloat("mult");
        }
    }

    public static final class DecelerateNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public float outputSpeedMult(float in) {
            return in * paramFloat("mult");
        }
    }

    public static final class SetSpeedNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public float outputSpeedMult(float in) {
            return paramFloat("speed");
        }
    }

    public static final class EchoNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < paramInt("times"); i++) {
                list.add(0);
            }
            return list;
        }
    }

    public static final class LoopNode extends SpellNode {
        @Override
        public int inputCount() {
            return paramInt("inputs");
        }

        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }
    }

    public static final class ConditionNode extends SpellNode {
        private int passes = 0;

        @Override
        public int outputCount() {
            return 2;
        }

        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            this.passes++;
            if (this.passes >= paramInt("min") && this.passes <= paramInt("max")) {
                return List.of(0);
            }
            return List.of(1);
        }
    }

    public static final class TerminateNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return null;
        }
    }

    public static final class GlobalTerminateNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            ctx.markTerminated();
            return null;
        }
    }

    public static final class RotateNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public Vec3 outputVelocity(Vec3 vel) {
            if (vel == null) {
                return null;
            }
            double angle = Math.toRadians(paramInt("angle"));
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            return new Vec3(vel.x * cos + vel.z * sin, vel.y, -vel.x * sin + vel.z * cos);
        }
    }

    public static final class DirectionNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public Vec3 outputVelocity(Vec3 vel) {
            if (vel == null) {
                return null;
            }
            Vec3 f = vel.lengthSqr() > 0.0001 ? vel.normalize() : new Vec3(0.0, 0.0, 1.0);
            double yaw = Math.toRadians(paramInt("yaw"));
            double pitch = Math.toRadians(paramInt("pitch"));
            double cy = Math.cos(yaw);
            double sy = Math.sin(yaw);
            Vec3 yawed = new Vec3(f.x * cy + f.z * sy, f.y, -f.x * sy + f.z * cy);
            double cp = Math.cos(pitch);
            double sp = Math.sin(pitch);
            double y2 = yawed.y * cp + yawed.z * sp;
            double z2 = -yawed.y * sp + yawed.z * cp;
            return new Vec3(yawed.x, y2, z2);
        }
    }

    public static final class SpeedReturnNode extends SpellNode {
        private Vec3 captured = null;

        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            this.captured = vel == null ? ctx.direction() : vel;
            return List.of(0);
        }

        @Override
        public Vec3 outputVelocity(Vec3 vel) {
            return this.captured == null ? vel : this.captured;
        }
    }

    public static final class WaitNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public int delayTicks() {
            return Math.max(1, paramInt("time"));
        }
    }

    public static final class OffsetUpNode extends SpellNode {
        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            return List.of(0);
        }

        @Override
        public Vec3 outputPosition(Vec3 at) {
            return at.add(0.0, paramInt("distance"), 0.0);
        }
    }

    public static final class OffsetForwardNode extends SpellNode {
        private Vec3 lastDir = new Vec3(0.0, 0.0, 1.0);

        @Override
        public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
            this.lastDir = vel == null ? ctx.direction() : vel;
            return List.of(0);
        }

        @Override
        public Vec3 outputPosition(Vec3 at) {
            return at.add(this.lastDir.scale(paramInt("distance")));
        }
    }
}
