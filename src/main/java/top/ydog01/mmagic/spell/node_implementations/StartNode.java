package top.ydog01.mmagic.spell.node_implementations;

import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.SpellRegistry;

public class StartNode extends SpellNode {
    public static final String ID = "start";
    
    public StartNode() {
        super(ID, SpellRegistry.get(ID));
    }
    
    @Override
    public ExecutionResult execute(SpellContext ctx) {
        return ExecutionResult.empty();
    }
    
    @Override
    public ExecutionResult tick(SpellContext ctx) {
        return ExecutionResult.continueTo(0);
    }
}