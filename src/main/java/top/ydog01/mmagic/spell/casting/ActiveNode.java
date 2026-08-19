package top.ydog01.mmagic.spell.casting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.ModifierNode;
import top.ydog01.mmagic.spell.node_api.ProjectileNode;
import top.ydog01.mmagic.spell.node_api.SpellNode;

import java.util.Optional;

public interface ActiveNode {
    
    SpellNode getNode();
    
    default SpellNode getModifier() {
        return getNode();
    }
    
    default boolean isModifier() {
        return getNode() instanceof ModifierNode;
    }
    
    default boolean isProjectile() {
        return getNode() instanceof ProjectileNode;
    }
    
    default SpellNode tryModify(SpellNode target, SpellContext ctx) {
        return getNode().tryModify(target, ctx);
    }
    
    default ExecutionResult onUpdate(SpellContext ctx) {
        return getNode().onUpdate(ctx);
    }
    
    default ExecutionResult onCollideEntity(Entity entity, SpellContext ctx) {
        return getNode().onCollideEntity(entity, ctx);
    }
    
    default ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
        return getNode().onCollideBlock(pos, ctx);
    }
    
    default Optional<Entity> checkEntityCollision(SpellContext ctx) {
        return Optional.empty();
    }
    
    default Optional<BlockPos> checkBlockCollision(SpellContext ctx) {
        return Optional.empty();
    }
    
    ActiveNode clone();
}