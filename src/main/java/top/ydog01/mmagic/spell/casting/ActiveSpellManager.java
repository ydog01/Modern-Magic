package top.ydog01.mmagic.spell.casting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellGraph;
import top.ydog01.mmagic.spell.SpellTrail;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.util.WandData;

import java.util.*;

public final class ActiveSpellManager {
    
    private final List<ActiveSpell> activeSpells = new ArrayList<>();
    
    public void castSpell(ServerPlayer player, ItemStack wand) {
        SpellGraph graph = WandData.getGraph(wand);
        SpellContext context = new SpellContext((ServerLevel) player.level(), player, wand, graph);
        ActiveSpell spell = new ActiveSpell(this, context, graph);
        activeSpells.add(spell);
    }
    
    public void castSpell(LivingEntity caster, ItemStack wand) {
        SpellGraph graph = WandData.getGraph(wand);
        SpellContext context = new SpellContext((ServerLevel) caster.level(), caster, wand, graph);
        ActiveSpell spell = new ActiveSpell(this, context, graph);
        activeSpells.add(spell);
    }
    
    public void continueFrom(ServerLevel level, UUID casterId, UUID wandId,
                             List<SpellNode.Connection> connections,
                             Vec3 at, Vec3 vel, List<SpellTrail> trails) {
        if (connections.isEmpty()) return;
        
        LivingEntity caster = (LivingEntity) level.getEntity(casterId);
        if (caster == null) return;
        
        ItemStack wand = findWandById(caster, wandId);
        if (wand.isEmpty()) return;
        
        SpellGraph graph = WandData.getGraph(wand);

        for (SpellNode.Connection connection : connections) {
            // Each released branch gets a private copy of the graph. Node state such as
            // ExplosionNode.executed / WaitNode.ticks must not leak between branches.
            SpellGraph branchGraph = SpellGraph.fromTag(graph.toTag());
            SpellNode target = branchGraph.getNode(connection.targetId);
            if (target == null) continue;

            SpellContext context = new SpellContext(level, caster, wand, branchGraph);
            context.setCurrentPosition(at);
            context.setCurrentVelocity(vel);
            if (trails != null) {
                for (SpellTrail trail : trails) {
                    context.addTrail(trail);
                }
            }

            ActiveSpell spell = new ActiveSpell(this, context, branchGraph);
            spell.setCurrentNode(target);
            activeSpells.add(spell);
        }
    }
    
    public void tick() {
        // Snapshot to avoid ConcurrentModificationException when clone/multi-cast
        // spells are appended by ActiveSpell while we are iterating.
        List<ActiveSpell> snapshot = new ArrayList<>(activeSpells);

        // Cache the wand ids each caster still possesses for this tick. Every
        // branch of the same cast shares the caster + wand id, so this scans the
        // inventory once per caster instead of once per active branch.
        Map<UUID, Set<UUID>> presentWands = new HashMap<>();

        for (ActiveSpell spell : snapshot) {
            SpellContext context = spell.getContext();
            if (context != null && context.caster() != null && context.wandId() != null) {
                if (context.caster().isRemoved()) {
                    spell.stop();
                    continue;
                }
                UUID casterId = context.caster().getUUID();
                Set<UUID> wands = presentWands.get(casterId);
                if (wands == null) {
                    wands = WandData.collectWandIds(context.caster());
                    presentWands.put(casterId, wands);
                }
                if (!wands.contains(context.wandId())) {
                    // The wand was dropped / moved out of the caster's possession:
                    // stop this branch immediately instead of waiting for its next
                    // mana-consuming node.
                    spell.stop();
                    continue;
                }
            }
            spell.update(spell.getGraph());
        }
        activeSpells.removeIf(ActiveSpell::isStopped);
    }
    
    
    private ItemStack findWandById(LivingEntity living, UUID wandId) {
        return WandData.findWandById(living, wandId);
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