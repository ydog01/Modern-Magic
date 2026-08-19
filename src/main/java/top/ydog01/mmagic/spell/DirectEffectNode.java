package top.ydog01.mmagic.spell;

public abstract class DirectEffectNode extends SpellNode {
    
    public DirectEffectNode(String id, SpellNodeType type) {
        super(id, type);
    }
    
    @Override
    public abstract ExecutionResult execute(SpellContext ctx);
    
    @Override
    public SpellNode tryModify(SpellNode target, SpellContext ctx) {
        return target;
    }
}