package top.ydog01.mmagic.spell.node;

import top.ydog01.mmagic.spell.ControlNode;
import top.ydog01.mmagic.spell.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellRegistry;
import java.util.ArrayList;
import java.util.List;

public final class ControlNodes {
    private ControlNodes() {}

    public static class MultiCastNode extends ControlNode {
        public MultiCastNode() { super("multi_cast", SpellRegistry.get("multi_cast")); }
        
        @Override 
        public ExecutionResult execute(SpellContext ctx) {
            return ExecutionResult.empty();
        }
        
        @Override 
        public ExecutionResult tick(SpellContext ctx) {
            int count = paramInt("outputs");
            List<Integer> ports = new ArrayList<>();
            for (int i = 0; i < count; i++) ports.add(i);
            return ExecutionResult.cloneTo(count, ports.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    public static class RandomCastNode extends ControlNode {
        public RandomCastNode() { super("random_cast", SpellRegistry.get("random_cast")); }
        
        @Override 
        public ExecutionResult execute(SpellContext ctx) {
            return ExecutionResult.empty();
        }
        
        @Override 
        public ExecutionResult tick(SpellContext ctx) {
            return ctx.level().random.nextBoolean() ? ExecutionResult.continueTo(0) : ExecutionResult.continueTo(1);
        }
    }

    public static class EchoNode extends ControlNode {
        public EchoNode() { super("echo", SpellRegistry.get("echo")); }
        
        @Override 
        public ExecutionResult execute(SpellContext ctx) {
            return ExecutionResult.empty();
        }
        
        @Override 
        public ExecutionResult tick(SpellContext ctx) {
            int times = paramInt("times");
            List<Integer> ports = new ArrayList<>();
            for (int i = 0; i < times; i++) ports.add(0);
            return ExecutionResult.cloneTo(times, ports.stream().mapToInt(Integer::intValue).toArray());
        }
    }

    public static class TerminateNode extends ControlNode {
        public TerminateNode() { super("terminate", SpellRegistry.get("terminate")); }
        
        @Override 
        public ExecutionResult execute(SpellContext ctx) {
            return ExecutionResult.terminate();
        }
        
        @Override 
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.terminate();
        }
    }

    public static class WaitNode extends ControlNode {
        private int ticks = 0;
        
        public WaitNode() { super("wait", SpellRegistry.get("wait")); }
        
        @Override 
        public int getDelayTicks() { return Math.max(1, paramInt("time")); }
        
        @Override 
        public ExecutionResult execute(SpellContext ctx) {
            return ExecutionResult.empty();
        }
        
        @Override 
        public ExecutionResult tick(SpellContext ctx) {
            ticks++;
            if (ticks < Math.max(1, paramInt("time"))) {
                return null;
            }
            return ExecutionResult.continueTo(0);
        }
    }

    public static class ConditionNode extends ControlNode {
        private int passes = 0;
        
        public ConditionNode() { super("condition", SpellRegistry.get("condition")); }
        
        @Override 
        public ExecutionResult execute(SpellContext ctx) {
            return ExecutionResult.empty();
        }
        
        @Override 
        public ExecutionResult tick(SpellContext ctx) {
            passes++;
            return (passes >= paramInt("min") && passes <= paramInt("max")) ? ExecutionResult.continueTo(0) : ExecutionResult.continueTo(1);
        }
    }
}