package top.ydog01.mmagic.spell.node_implementations;

import net.minecraft.server.level.ServerLevel;
import top.ydog01.mmagic.entity.MagicMissileEntity;
import top.ydog01.mmagic.init.ModEntityTypes;
import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.SpellRegistry;

public class MagicMissileNode extends SpellNode {
    public static final String ID = "magic_missile";
    
    private MagicMissileEntity missile;
    private boolean launched = false;
    
    public MagicMissileNode() {
        super(ID, SpellRegistry.get(ID));
    }
    
    @Override
    public ExecutionResult execute(SpellContext ctx) {
        return ExecutionResult.empty();
    }
    
    @Override
    public ExecutionResult tick(SpellContext ctx) {
        if (!launched) {
            if (!(ctx.level() instanceof ServerLevel level)) {
                return ExecutionResult.empty();
            }
            
            missile = new MagicMissileEntity(
                ModEntityTypes.MAGIC_MISSILE.get(),
                ctx.caster(),
                level
            );
            
            float damage = paramFloat("damage");
            float speed = paramFloat("speed");
            
            missile.setDamage(damage);
            missile.applyHarvestModifiers(ctx);
            missile.setPos(ctx.getCurrentPosition());
            missile.setDeltaMovement(ctx.getCurrentVelocity().scale(speed));
            
            level.addFreshEntity(missile);
            launched = true;
            return null;
        }
        
        if (missile != null && missile.isRemoved()) {
            return ExecutionResult.continueTo(0);
        }
        
        return null;
    }
    
    @Override
    public int getManaCost() {
        int base = super.getManaCost();
        float damage = paramFloat("damage");
        float speed = paramFloat("speed");
        
        int extra = 0;
        extra += Math.round(damage * 0.5f);
        extra += Math.round((speed - 0.5f) * 2f);
        if (extra < 0) extra = 0;
        
        return base + extra;
    }
}