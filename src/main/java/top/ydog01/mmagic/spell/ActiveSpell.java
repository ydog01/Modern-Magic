package top.ydog01.mmagic.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.stream.Collectors;

public class ActiveSpell {
    
    private final ActiveSpellManager manager;
    private final SpellContext context;
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
        this.currentNode = graph.start();
        this.activeNodes = new ArrayList<>();
    }
    
    private ActiveSpell(ActiveSpellManager manager, SpellContext context, SpellNode currentNode, 
                        List<ActiveNode> activeNodes, UUID spellId) {
        this.manager = manager;
        this.context = context.clone();
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
        int count = result.getCloneCount();
        for (int i = 0; i < count; i++) {
            ActiveSpell clone = this.clone();
            for (int port : result.getOutputPorts()) {
                List<SpellNode.Connection> conns = currentNode.getConnections(port);
                for (SpellNode.Connection conn : conns) {
                    SpellNode target = graph.getNode(conn.targetId);
                    if (target != null) {
                        clone.currentNode = target;
                        break;
                    }
                }
            }
            manager.addSpell(clone);
        }
        currentNode = null;
    }
    
    public ActiveSpell clone() {
        return new ActiveSpell(manager, context, currentNode, activeNodes, UUID.randomUUID());
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
    public SpellNode getCurrentNode() { return currentNode; }
    public UUID getSpellId() { return spellId; }
}