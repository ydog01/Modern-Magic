package top.ydog01.mmagic.spell.casting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellGraph;
import top.ydog01.mmagic.spell.SpellTrail;
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
        this.context = context.cloneWithGraph(graph);
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
        
        applyOutputs(modified);

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
        
        applyOutputs(currentModifiedNode);

        if (result.shouldClone()) {
            cloneAndBind(result, graph);
        } else {
            advanceToOutputs(result, graph);
        }
        
        currentNodeExecuted = false;
        currentModifiedNode = null;
    }
    
    /**
     * Push this node's position / velocity / damage / speed outputs into the
     * context before the spell walks to the next node. Without this, motion
     * nodes (offset_up, offset_forward, rotate, direction, amplifier,
     * accelerator, set_speed, decelerate) were dead code and every downstream
     * node kept using the original cast position and velocity.
     */
    private void applyOutputs(SpellNode node) {
        Vec3 pos = node.outputPosition(context.getCurrentPosition(), context);
        if (pos != null) {
            context.setCurrentPosition(pos);
        }
        Vec3 vel = node.outputVelocity(context.getCurrentVelocity(), context);
        if (vel != null) {
            context.setCurrentVelocity(vel);
        }
        context.setDamageMultiplier(node.outputDamageMult(context.getDamageMultiplier(), context));
        context.setSpeedMultiplier(node.outputSpeedMult(context.getSpeedMultiplier(), context));
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

        // Each branch gets its own copy of the spell graph. Node execution state
        // such as "already launched", wait ticks and executed flags must not be
        // shared between branches produced by multi-cast / echo.
        for (int port : result.getOutputPorts()) {
            SpellNode.Connection connection = firstConnection(port);
            if (connection == null) continue;

            SpellGraph branchGraph = SpellGraph.fromTag(graph.toTag());
            SpellNode target = branchGraph.getNode(connection.targetId);
            if (target == null) continue;

            ActiveSpell clone = new ActiveSpell(manager, context, branchGraph, target,
                    activeNodes, UUID.randomUUID());
            manager.addSpell(clone);
        }
        currentNode = null;
    }

    private SpellNode.Connection firstConnection(int port) {
        for (SpellNode.Connection connection : currentNode.getConnections(port)) {
            return connection;
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

        SpellTrail trail = node.trailEffect();
        if (trail != null) {
            context.addTrail(trail);
        }
    }

    
    public void addActiveNode(ActiveNode node) {
        queueAdd(node);
    }
    
    public void setCurrentNode(SpellNode node) {
        this.currentNode = node;
        this.currentNodeExecuted = false;
        this.currentModifiedNode = null;
    }
    
    /** Immediately stop this branch (e.g. when its wand is dropped). */
    public void stop() {
        this.stopped = true;
        this.context.markTerminated();
    }

    public boolean isStopped() { return stopped; }
    public List<ActiveNode> getActiveNodes() { return Collections.unmodifiableList(activeNodes); }
    public SpellContext getContext() { return context; }
    public SpellGraph getGraph() { return graph; }
    public SpellNode getCurrentNode() { return currentNode; }
    public UUID getSpellId() { return spellId; }
}