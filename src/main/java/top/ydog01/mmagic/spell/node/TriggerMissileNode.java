package top.ydog01.mmagic.spell.node;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.entity.MagicMissileEntity;
import top.ydog01.mmagic.init.ModEntityTypes;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellModifiers;
import top.ydog01.mmagic.spell.SpellNode;

import java.util.ArrayList;
import java.util.List;

public class TriggerMissileNode extends SpellNode {
    public static final int MAX_FLIGHT_TICKS = 200;

    @Override
    public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
        ServerLevel level = ctx.level();
        MagicMissileEntity missile = new MagicMissileEntity(ModEntityTypes.MAGIC_MISSILE.get(), ctx.caster(), level);
        missile.setPos(at);
        missile.setMultipliers(damageMult, speedMult);
        missile.setModifiers(mods);
        missile.shoot(vel.x, vel.y, vel.z, 1.5f * speedMult, 0.5f);
        missile.setDamage(3.0f);
        missile.setTrigger(true, MAX_FLIGHT_TICKS, ctx.caster().getUUID(), top.ydog01.mmagic.util.WandData.getWandId(ctx.wand()),
                new ArrayList<>(this.outputs(0)));
        level.addFreshEntity(missile);
        return null;
    }
}
