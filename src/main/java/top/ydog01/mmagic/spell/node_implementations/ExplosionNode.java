package top.ydog01.mmagic.spell.node_implementations;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.SpellRegistry;

public class ExplosionNode extends SpellNode {
    public static final String ID = "explosion";
    
    private boolean executed = false;
    
    public ExplosionNode() {
        super(ID, SpellRegistry.get(ID));
    }
    
    @Override
    public ExecutionResult execute(SpellContext ctx) {
        return ExecutionResult.empty();
    }
    
    @Override
    public ExecutionResult tick(SpellContext ctx) {
        if (executed) {
            return ExecutionResult.continueTo(0);
        }
        
        if (!(ctx.level() instanceof ServerLevel level)) {
            return ExecutionResult.empty();
        }
        
        float radius = paramFloat("radius");
        float damage = paramFloat("damage");
        boolean destroyTerrain = paramBool("destroy_terrain");
        
        Vec3 pos = ctx.getCurrentPosition();
        
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, 
                new AABB(pos, pos).inflate(radius))) {
            if (entity != ctx.caster()) {
                double distance = entity.distanceToSqr(pos);
                if (distance < radius * radius) {
                    float damageAmount = damage * (1 - (float)Math.sqrt(distance) / radius);
                    entity.hurt(entity.damageSources().explosion(null, ctx.caster()), damageAmount);
                }
            }
        }
        
        Explosion explosion = new Explosion(
            level, 
            ctx.caster(), 
            pos.x, pos.y, pos.z, 
            radius, 
            false,
            destroyTerrain ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP
        );
        explosion.explode();
        explosion.finalizeExplosion(false);
        
        executed = true;
        return ExecutionResult.continueTo(0);
    }
    
    @Override
    public int getManaCost() {
        int base = super.getManaCost();
        float radius = paramFloat("radius");
        float damage = paramFloat("damage");
        boolean destroyTerrain = paramBool("destroy_terrain");
        
        int extra = 0;
        extra += Math.round(radius * 0.5f);
        extra += Math.round(damage * 0.2f);
        if (destroyTerrain) {
            extra += 2;
        }
        
        return base + extra;
    }
}