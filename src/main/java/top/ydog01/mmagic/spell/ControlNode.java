package top.ydog01.mmagic.spell;

public abstract class ControlNode extends SpellNode {
    
    public ControlNode(String id, SpellNodeType type) {
        super(id, type);
    }
    
    @Override
    public abstract ExecutionResult execute(SpellContext ctx);
    
    @Override
    public SpellNode tryModify(SpellNode target, SpellContext ctx) {
        return target;
    }
}
