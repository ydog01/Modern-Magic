package top.ydog01.mmagic.spell.casting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.item.WandItem;
import top.ydog01.mmagic.spell.SpellGraph;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.util.WandData;

import java.util.List;
import java.util.UUID;

public final class SpellRunner {
    
    private static ActiveSpellManager manager;
    
    public static void setManager(ActiveSpellManager m) {
        manager = m;
    }

    public static void cast(LivingEntity caster, ItemStack wand) {
        if (manager == null) return;
        if (!(caster.level() instanceof ServerLevel)) return;
        if (!WandData.isReady(wand, caster.level())) return;
        
        SpellGraph graph = WandData.getGraph(wand);
        if (graph.start() == null) return;
        
        WandData.markCast(wand, caster.level());
        manager.castSpell(caster, wand);
    }
    
    public static void continueFrom(ServerLevel level, UUID casterId, UUID wandId,
                                    List<SpellNode.Connection> connections,
                                    Vec3 at, Vec3 vel) {
        if (manager == null) return;
        manager.continueFrom(level, casterId, wandId, connections, at, vel);
    }
    
    public static ItemStack findWand(LivingEntity living) {
        ItemStack main = living.getMainHandItem();
        if (main.getItem() instanceof WandItem) {
            return main;
        }
        ItemStack off = living.getOffhandItem();
        if (off.getItem() instanceof WandItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }
}