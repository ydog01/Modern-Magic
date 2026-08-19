package top.ydog01.mmagic.spell.node;

import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.ControlNode;
import top.ydog01.mmagic.spell.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellRegistry;

public final class MotionNodes {
    private MotionNodes() {}

    public static class AmplifierNode extends ControlNode {
        public static final String ID = "amplifier";
        public AmplifierNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public float outputDamageMult(float in, SpellContext ctx) { return in * paramFloat("mult"); }
        
        @Override public int getManaCost() { return super.getManaCost() + Math.round(paramFloat("mult")); }
    }

    public static class AcceleratorNode extends ControlNode {
        public static final String ID = "accelerator";
        public AcceleratorNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public float outputSpeedMult(float in, SpellContext ctx) { return in * paramFloat("mult"); }
        
        @Override public int getManaCost() { return super.getManaCost() + Math.round(paramFloat("mult")); }
    }

    public static class RotateNode extends ControlNode {
        public static final String ID = "rotate";
        public RotateNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public Vec3 outputVelocity(Vec3 vel, SpellContext ctx) {
            if (vel == null) return ctx.getCurrentVelocity();
            double angle = Math.toRadians(paramInt("angle"));
            double cos = Math.cos(angle), sin = Math.sin(angle);
            return new Vec3(vel.x * cos + vel.z * sin, vel.y, -vel.x * sin + vel.z * cos);
        }
        
        @Override public int getManaCost() { return super.getManaCost() + Math.abs(paramInt("angle")) / 30; }
    }

    public static class DirectionNode extends ControlNode {
        public static final String ID = "direction";
        public DirectionNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public Vec3 outputVelocity(Vec3 vel, SpellContext ctx) {
            if (vel == null) return ctx.getCurrentVelocity();
            Vec3 f = vel.lengthSqr() > 0.0001 ? vel.normalize() : new Vec3(0, 0, 1);
            double yaw = Math.toRadians(paramInt("yaw")), pitch = Math.toRadians(paramInt("pitch"));
            double cy = Math.cos(yaw), sy = Math.sin(yaw);
            Vec3 yawed = new Vec3(f.x * cy + f.z * sy, f.y, -f.x * sy + f.z * cy);
            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            return new Vec3(yawed.x, yawed.y * cp + yawed.z * sp, -yawed.y * sp + yawed.z * cp);
        }
        
        @Override public int getManaCost() { return super.getManaCost() + (Math.abs(paramInt("yaw")) + Math.abs(paramInt("pitch"))) / 30; }
    }

    public static class SpeedReturnNode extends ControlNode {
        public static final String ID = "speed_return";
        public SpeedReturnNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public Vec3 outputVelocity(Vec3 vel, SpellContext ctx) { return vel; }
    }

    public static class SetSpeedNode extends ControlNode {
        public static final String ID = "set_speed";
        public SetSpeedNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public float outputSpeedMult(float in, SpellContext ctx) { return paramFloat("speed"); }
        
        @Override public int getManaCost() { return super.getManaCost() + Math.round(paramFloat("speed")); }
    }

    public static class DecelerateNode extends ControlNode {
        public static final String ID = "decelerate";
        public DecelerateNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public float outputSpeedMult(float in, SpellContext ctx) { return in * paramFloat("mult"); }
        
        @Override public int getManaCost() { return super.getManaCost() + Math.round((1f - paramFloat("mult")) * 3); }
    }

    public static class OffsetUpNode extends ControlNode {
        public static final String ID = "offset_up";
        public OffsetUpNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public Vec3 outputPosition(Vec3 at, SpellContext ctx) { return at.add(0, paramInt("distance"), 0); }
        
        @Override public int getManaCost() { return super.getManaCost() + paramInt("distance") / 2; }
    }

    public static class OffsetForwardNode extends ControlNode {
        public static final String ID = "offset_forward";
        public OffsetForwardNode() { super(ID, SpellRegistry.get(ID)); }
        
        @Override public ExecutionResult execute(SpellContext ctx) { return ExecutionResult.empty(); }
        
        @Override public ExecutionResult tick(SpellContext ctx) { return ExecutionResult.continueTo(0); }
        
        @Override public Vec3 outputPosition(Vec3 at, SpellContext ctx) {
            Vec3 dir = ctx.getCurrentVelocity();
            if (dir == null || dir.lengthSqr() < 0.0001) dir = ctx.direction();
            return at.add(dir.normalize().scale(paramInt("distance")));
        }
        
        @Override public int getManaCost() { return super.getManaCost() + paramInt("distance") / 2; }
    }
}