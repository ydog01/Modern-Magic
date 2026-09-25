package top.ydog01.mmagic.spell.casting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellGraph;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.node_implementations.ModifierNodes;

import java.util.*;
import java.util.stream.Collectors;

public class ActiveSpell {
    
    private final ActiveSpellManager manager;
    private final SpellContext context;
    private final SpellGraph graph;
    private SpellNode currentNode;
    private final List<ActiveNode> activeNodes;
    private boolean stopped = false;
    private UUID spellId = UUID.randomUUID();
    private boolean iterating = false;
    private final List<ActiveNode> pendingAdd = new ArrayList<>();
    private final List<ActiveNode> pendingRemove = new ArrayList<>();
    private boolean currentNodeExecuted = false;
    private SpellNode currentModifiedNode = null;
    
    public ActiveSpell(ActiveSpellManager manager, SpellContext context, SpellGraph graph) {
        this.manager = manager;
        this.context = context;
        this.graph = graph;
        this.currentNode = graph.start();
        this.activeNodes = new ArrayList<>();
    }
    
    private ActiveSpell(ActiveSpellManager manager, SpellContext context, SpellGraph graph,
                        SpellNode currentNode, List<ActiveNode> activeNodes, UUID spellId) {
        this.manager = manager;
        this.context = context.clone();
        this.graph = graph;
        this.currentNode = currentNode;
        this.activeNodes = activeNodes.stream()
                .map(ActiveNode::clone)
                .collect(Collectors.toList());
        this.spellId = spellId;
        this.currentNodeExecuted = false;
    }
    
    public void update(SpellGraph graph) {
        if (stopped) return;
        
        iterating = true;
        for (ActiveNode active : activeNodes) {
            ExecutionResult updateResult = active.onUpdate(context);
            if (updateResult == null || updateResult.shouldRemove()) {
                queueRemove(active);
                continue;
            }
            
            Optional<Entity> hitEntity = active.checkEntityCollision(context);
            if (hitEntity.isPresent()) {
                ExecutionResult hitResult = active.onCollideEntity(hitEntity.get(), context);
                if (hitResult == null || hitResult.shouldRemove()) {
                    queueRemove(active);
                    continue;
                }
            }
            
            Optional<BlockPos> hitBlock = active.checkBlockCollision(context);
            if (hitBlock.isPresent()) {
                ExecutionResult hitResult = active.onCollideBlock(hitBlock.get(), context);
                if (hitResult == null || hitResult.shouldRemove()) {
                    queueRemove(active);
                }
            }
        }
        iterating = false;
        
        if (!pendingRemove.isEmpty()) {
            activeNodes.removeAll(pendingRemove);
            pendingRemove.clear();
        }
        if (!pendingAdd.isEmpty()) {
            activeNodes.addAll(pendingAdd);
            pendingAdd.clear();
        }
        
        if (activeNodes.isEmpty() && currentNode == null) {
            stopped = true;
            return;
        }
        
        if (currentNode != null) {
            if (!currentNodeExecuted) {
                executeNode(currentNode, graph);
            } else {
                continueNode(graph);
            }
        }
    }
    
    private void queueRemove(ActiveNode node) {
        if (iterating) {
            pendingRemove.add(node);
        } else {
            activeNodes.remove(node);
        }
    }
    
    private void queueAdd(ActiveNode node) {
        if (iterating) {
            pendingAdd.add(node);
        } else {
            activeNodes.add(node);
        }
    }
    
    private void executeNode(SpellNode node, SpellGraph graph) {
        for (ActiveNode active : activeNodes) {
            if (!node.allowModification(active.getModifier())) {
                return;
            }
        }
        
        SpellNode modified = node;
        for (ActiveNode active : activeNodes) {
            modified = active.tryModify(modified, context);
            if (modified == null) {
                return;
            }
        }
        
        int manaCost = modified.getManaCost();
        if (manaCost > 0 && !context.consumeMana(manaCost)) {
            stopped = true;
            currentModifiedNode = null;
            return;
        }

        applyContextModifier(node);
        currentModifiedNode = modified;
        modified.execute(context);
        ExecutionResult result = modified.tick(context);
        
        if (result == null) {
            currentNodeExecuted = true;
            return;
        }
        
        if (result.isTerminate()) {
            stopped = true;
            currentModifiedNode = null;
            return;
        }
        
        if (result.shouldClone()) {
            cloneAndBind(result, graph);
        } else {
            advanceToOutputs(result, graph);
        }
        
        currentNodeExecuted = false;
        currentModifiedNode = null;
    }
    
    private void continueNode(SpellGraph graph) {
        if (currentModifiedNode == null) return;
        
        ExecutionResult result = currentModifiedNode.tick(context);
        
        if (result == null) {
            return;
        }
        
        if (result.isTerminate()) {
            stopped = true;
            currentModifiedNode = null;
            return;
        }
        
        if (result.shouldClone()) {
            cloneAndBind(result, graph);
        } else {
            advanceToOutputs(result, graph);
        }
        
        currentNodeExecuted = false;
        currentModifiedNode = null;
    }
    
    private void advanceToOutputs(ExecutionResult result, SpellGraph graph) {
        if (currentNode == null) return;
        for (int port : result.getOutputPorts()) {
            List<SpellNode.Connection> conns = currentNode.getConnections(port);
            for (SpellNode.Connection conn : conns) {
                SpellNode target = graph.getNode(conn.targetId);
                if (target != null) {
                    currentNode = target;
                    return;
                }
            }
        }
        currentNode = null;
    }
    
    private void cloneAndBind(ExecutionResult result, SpellGraph graph) {
        if (currentNode == null) return;

        // One clone per output port entry. Multi-cast supplies distinct ports,
        // echo supplies the same port repeatedly.
        for (int port : result.getOutputPorts()) {
            SpellNode target = firstTarget(graph, port);
            if (target == null) continue;

            ActiveSpell clone = this.clone();
            clone.currentNode = target;
            clone.currentNodeExecuted = false;
            clone.currentModifiedNode = null;
            manager.addSpell(clone);
        }
        currentNode = null;
    }

    private SpellNode firstTarget(SpellGraph graph, int port) {
        for (SpellNode.Connection connection : currentNode.getConnections(port)) {
            SpellNode target = graph.getNode(connection.targetId);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    private void applyContextModifier(SpellNode node) {
        if (node instanceof ModifierNodes.PickupModifierNode pickup) {
            context.enablePickup(pickup.paramFloat("radius"));
        } else if (node instanceof ModifierNodes.DigModifierNode dig) {
            context.enableDig(dig.paramFloat("radius"), dig.paramInt("level"), dig.paramBool("drop"));
        } else if (node instanceof ModifierNodes.ChainDigModifierNode chain) {
            context.enableChainDig(chain.paramFloat("radius"), chain.paramInt("level"), chain.paramBool("drop"));
        }
    }

    public ActiveSpell clone() {
        return new ActiveSpell(manager, context, graph, currentNode, activeNodes, UUID.randomUUID());
    }
    
    public void addActiveNode(ActiveNode node) {
        queueAdd(node);
    }
    
    public void setCurrentNode(SpellNode node) {
        this.currentNode = node;
        this.currentNodeExecuted = false;
        this.currentModifiedNode = null;
    }
    
    public boolean isStopped() { return stopped; }
    public List<ActiveNode> getActiveNodes() { return Collections.unmodifiableList(activeNodes); }
    public SpellContext getContext() { return context; }
    public SpellGraph getGraph() { return graph; }
    public SpellNode getCurrentNode() { return currentNode; }
    public UUID getSpellId() { return spellId; }
}