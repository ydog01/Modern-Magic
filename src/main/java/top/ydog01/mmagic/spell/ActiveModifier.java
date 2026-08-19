package top.ydog01.mmagic.spell;

public class ActiveModifier implements ActiveNode {
    
    private final ModifierNode node;
    private boolean alive = true;
    private int remainingTicks = -1;
    
    public ActiveModifier(ModifierNode node) {
        this.node = node;
    }
    
    public ActiveModifier(ModifierNode node, int durationTicks) {
        this.node = node;
        this.remainingTicks = durationTicks;
    }
    
    private ActiveModifier(ModifierNode node, boolean alive, int remainingTicks) {
        this.node = node;
        this.alive = alive;
        this.remainingTicks = remainingTicks;
    }
    
    @Override
    public SpellNode getNode() {
        return node;
    }
    
    @Override
    public ExecutionResult onUpdate(SpellContext ctx) {
        if (!alive) return ExecutionResult.terminate();
        if (remainingTicks > 0) {
            remainingTicks--;
            if (remainingTicks == 0) {
                alive = false;
                return ExecutionResult.terminate();
            }
        }
        return node.onUpdate(ctx);
    }
    
    @Override
    public SpellNode tryModify(SpellNode target, SpellContext ctx) {
        if (!alive) return target;
        return node.tryModify(target, ctx);
    }
    
    @Override
    public ActiveNode clone() {
        return new ActiveModifier(node, alive, remainingTicks);
    }
}