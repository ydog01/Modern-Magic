package top.ydog01.mmagic.spell;

import net.minecraft.resources.ResourceLocation;

public abstract class ProjectileNode extends SpellNode {
    
    protected boolean hasGravity = false;
    protected ResourceLocation model;
    protected float speed = 1.5f;
    protected float damage = 4.0f;
    
    public ProjectileNode(String id, SpellNodeType type) {
        super(id, type);
    }
    
    public ProjectileNode setGravity(boolean hasGravity) {
        this.hasGravity = hasGravity;
        return this;
    }
    
    public ProjectileNode setModel(ResourceLocation model) {
        this.model = model;
        return this;
    }
    
    public ProjectileNode setSpeed(float speed) {
        this.speed = speed;
        return this;
    }
    
    public ProjectileNode setDamage(float damage) {
        this.damage = damage;
        return this;
    }
    
    public boolean hasGravity() { return hasGravity; }
    public ResourceLocation getModel() { return model; }
    public float getSpeed() { return speed; }
    public float getDamage() { return damage; }
    
    @Override
    public ExecutionResult execute(SpellContext ctx) {
        return ExecutionResult.continueTo(0);
    }
    
    @Override
    public SpellNode tryModify(SpellNode target, SpellContext ctx) {
        return target;
    }
}
