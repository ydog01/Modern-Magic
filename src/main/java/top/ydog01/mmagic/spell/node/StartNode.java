package top.ydog01.mmagic.spell.node;

import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellModifiers;
import top.ydog01.mmagic.spell.SpellNode;

import java.util.List;

public class StartNode extends SpellNode {
    @Override
    public List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
        return List.of(0);
    }
}
