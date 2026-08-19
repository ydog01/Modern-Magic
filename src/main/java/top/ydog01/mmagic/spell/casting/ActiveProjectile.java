package top.ydog01.mmagic.spell.casting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.ProjectileNode;
import top.ydog01.mmagic.spell.node_api.SpellNode;

import java.util.Optional;

public class ActiveProjectile implements ActiveNode {
    
    private final ProjectileNode node;
    private Vec3 position;
    private Vec3 velocity;
    private boolean alive = true;
    private final double radius = 0.3;
    
    public ActiveProjectile(ProjectileNode node, SpellContext ctx) {
        this.node = node;
        if (ctx != null) {
            this.position = ctx.getCurrentPosition();
            this.velocity = ctx.getCurrentVelocity().scale(node.getSpeed());
        } else {
            this.position = Vec3.ZERO;
            this.velocity = Vec3.ZERO;
        }
    }
    
    private ActiveProjectile(ProjectileNode node, Vec3 position, Vec3 velocity, boolean alive) {
        this.node = node;
        this.position = position;
        this.velocity = velocity;
        this.alive = alive;
    }
    
    @Override
    public SpellNode getNode() {
        return node;
    }
    
    @Override
    public ExecutionResult onUpdate(SpellContext ctx) {
        if (!alive) return ExecutionResult.terminate();
        
        position = position.add(velocity);
        if (node.hasGravity()) {
            velocity = velocity.add(0, -0.04, 0);
        }
        ctx.setCurrentPosition(position);
        ctx.setCurrentVelocity(velocity);
        
        return node.onUpdate(ctx);
    }
    
    @Override
    public Optional<Entity> checkEntityCollision(SpellContext ctx) {
        if (!alive) return Optional.empty();
        AABB box = new AABB(position, position).inflate(radius);
        return ctx.level().getEntitiesOfClass(Entity.class, box, 
            e -> e != ctx.caster() && e.isAlive()).stream().findFirst();
    }
    
    @Override
    public ExecutionResult onCollideEntity(Entity entity, SpellContext ctx) {
        if (!alive) return ExecutionResult.terminate();
        ExecutionResult result = node.onCollideEntity(entity, ctx);
        if (result.shouldRemove()) {
            alive = false;
        }
        return result;
    }
    
    @Override
    public Optional<BlockPos> checkBlockCollision(SpellContext ctx) {
        if (!alive) return Optional.empty();
        BlockPos pos = BlockPos.containing(position);
        if (!ctx.level().getBlockState(pos).isAir()) {
            return Optional.of(pos);
        }
        return Optional.empty();
    }
    
    @Override
    public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
        if (!alive) return ExecutionResult.terminate();
        ExecutionResult result = node.onCollideBlock(pos, ctx);
        if (result.shouldRemove()) {
            alive = false;
        }
        return result;
    }
    
    @Override
    public SpellNode tryModify(SpellNode target, SpellContext ctx) {
        return node.tryModify(target, ctx);
    }
    
    @Override
    public ActiveNode clone() {
        return new ActiveProjectile(node, position, velocity, alive);
    }
}