package top.ydog01.mmagic.spell.node_api;

import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellNodeType;

import java.util.UUID;

public abstract class ModifierNode extends SpellNode {
    
    private UUID modifiedTargetUuid;
    private boolean targetRemoved = false;
    private boolean completed = false;

    public ModifierNode(String id, SpellNodeType type) {
        super(id, type);
    }

    public void setModifiedTarget(UUID targetUuid) {
        this.modifiedTargetUuid = targetUuid;
    }

    public UUID getModifiedTarget() {
        return modifiedTargetUuid;
    }

    public boolean isTargetRemoved() {
        return targetRemoved;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

    @Override
    public ExecutionResult onUpdate(SpellContext ctx) {
        if (completed) return ExecutionResult.terminate();
        if (modifiedTargetUuid != null && ctx.isNodeRemoved(modifiedTargetUuid)) {
            this.targetRemoved = true;
            return ExecutionResult.terminate();
        }
        return ExecutionResult.empty();
    }

    @Override
    public abstract SpellNode tryModify(SpellNode target, SpellContext ctx);

    @Override
    public ExecutionResult execute(SpellContext ctx) {
        return ExecutionResult.continueTo(0);
    }
}