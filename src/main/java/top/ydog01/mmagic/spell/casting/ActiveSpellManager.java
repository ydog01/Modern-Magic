package top.ydog01.mmagic.spell.casting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.item.WandItem;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellGraph;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.util.WandData;

import java.util.*;

public final class ActiveSpellManager {
    
    private final List<ActiveSpell> activeSpells = new ArrayList<>();
    
    public void castSpell(ServerPlayer player, ItemStack wand) {
        SpellGraph graph = WandData.getGraph(wand);
        SpellContext context = new SpellContext((ServerLevel) player.level(), player, wand);
        ActiveSpell spell = new ActiveSpell(this, context, graph);
        activeSpells.add(spell);
    }
    
    public void castSpell(LivingEntity caster, ItemStack wand) {
        SpellGraph graph = WandData.getGraph(wand);
        SpellContext context = new SpellContext((ServerLevel) caster.level(), caster, wand);
        ActiveSpell spell = new ActiveSpell(this, context, graph);
        activeSpells.add(spell);
    }
    
    public void continueFrom(ServerLevel level, UUID casterId, UUID wandId,
                             List<SpellNode.Connection> connections,
                             Vec3 at, Vec3 vel) {
        if (connections.isEmpty()) return;
        
        LivingEntity caster = (LivingEntity) level.getEntity(casterId);
        if (caster == null) return;
        
        ItemStack wand = findWandById(caster, wandId);
        if (wand.isEmpty()) return;
        
        SpellGraph graph = WandData.getGraph(wand);
        
        for (SpellNode.Connection connection : connections) {
            SpellNode target = graph.getNode(connection.targetId);
            if (target == null) continue;

            SpellContext context = new SpellContext(level, caster, wand);
            context.setCurrentPosition(at);
            context.setCurrentVelocity(vel);

            ActiveSpell spell = new ActiveSpell(this, context, graph);
            spell.setCurrentNode(target);
            activeSpells.add(spell);
        }
    }
    
    public void tick() {
        // Snapshot to avoid ConcurrentModificationException when clone/multi-cast
        // spells are appended by ActiveSpell while we are iterating.
        List<ActiveSpell> snapshot = new ArrayList<>(activeSpells);
        for (ActiveSpell spell : snapshot) {
            spell.update(spell.getGraph());
        }
        activeSpells.removeIf(ActiveSpell::isStopped);
    }
    
    
    private ItemStack findWandById(LivingEntity living, UUID wandId) {
        if (living.getMainHandItem().getItem() instanceof WandItem
                && WandData.getWandId(living.getMainHandItem()).equals(wandId)) {
            return living.getMainHandItem();
        }
        if (living.getOffhandItem().getItem() instanceof WandItem
                && WandData.getWandId(living.getOffhandItem()).equals(wandId)) {
            return living.getOffhandItem();
        }
        if (living instanceof Player player) {
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof WandItem 
                        && WandData.getWandId(stack).equals(wandId)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
    
    public void addSpell(ActiveSpell spell) {
        activeSpells.add(spell);
    }
    
    public void clear() {
        activeSpells.clear();
    }
    
    public List<ActiveSpell> getActiveSpells() {
        return Collections.unmodifiableList(activeSpells);
    }
}