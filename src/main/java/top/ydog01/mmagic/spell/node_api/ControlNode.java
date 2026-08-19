package top.ydog01.mmagic.spell.node_api;

import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellNodeType;

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
