package top.ydog01.mmagic.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.network.ModNetwork;
import top.ydog01.mmagic.util.WandData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SpellContext {
    
    private final ServerLevel level;
    private final LivingEntity caster;
    private final ItemStack wand;
    private final SpellGraph graph;
    private final Vec3 origin;
    private final Vec3 direction;
    private final UUID wandId;
    private Vec3 currentPosition;
    private Vec3 currentVelocity;
    private Set<UUID> removedNodes = new HashSet<>();
    private boolean terminated = false;
    private double manaPool = 0;
    private long lastUpdateTick = 0;

    private boolean pickupEnabled = false;
    private float pickupRadius = 4.0f;
    private boolean digEnabled = false;
    private float digRadius = 2.0f;
    private int digLevel = 1;
    private boolean digDrops = true;
    private boolean chainDigEnabled = false;
    private float chainDigRadius = 8.0f;
    private int chainDigLevel = 1;
    private boolean chainDigDrops = true;

    private float damageMultiplier = 1.0f;
    private float speedMultiplier = 1.0f;
    private final List<SpellTrail> trails = new ArrayList<>();
    
    public SpellContext(ServerLevel level, LivingEntity caster, ItemStack wand) {
        this(level, caster, wand, WandData.getGraph(wand));
    }

    public SpellContext(ServerLevel level, LivingEntity caster, ItemStack wand, SpellGraph graph) {
        this.level = level;
        this.caster = caster;
        this.wand = wand;
        this.graph = graph;
        this.origin = caster.getEyePosition();
        this.direction = caster.getLookAngle();
        this.wandId = WandData.getWandId(wand);
        this.currentPosition = origin;
        this.currentVelocity = direction;
    }
    
    private SpellContext(ServerLevel level, LivingEntity caster, ItemStack wand, SpellGraph graph,
                         Vec3 origin, Vec3 direction, UUID wandId, Vec3 currentPosition,
                         Vec3 currentVelocity, Set<UUID> removedNodes, boolean terminated,
                         double manaPool, long lastUpdateTick) {
        this.level = level;
        this.caster = caster;
        this.wand = wand;
        this.graph = graph;
        this.origin = origin;
        this.direction = direction;
        this.wandId = wandId;
        this.currentPosition = currentPosition;
        this.currentVelocity = currentVelocity;
        this.removedNodes = new HashSet<>(removedNodes);
        this.terminated = terminated;
        this.manaPool = manaPool;
        this.lastUpdateTick = lastUpdateTick;
    }
    
    public SpellContext clone() {
        return cloneWithGraph(graph);
    }

    /**
     * Copy this context but bind it to the given graph instance. Used by branches
     * produced from multi-cast / echo / projectile continuation so that every
     * branch owns its own node instances.
     */
    public SpellContext cloneWithGraph(SpellGraph newGraph) {
        SpellContext copy = new SpellContext(level, caster, wand, newGraph, origin, direction, wandId,
                currentPosition, currentVelocity, removedNodes, terminated, manaPool, lastUpdateTick);
        copy.pickupEnabled = pickupEnabled;
        copy.pickupRadius = pickupRadius;
        copy.digEnabled = digEnabled;
        copy.digRadius = digRadius;
        copy.digLevel = digLevel;
        copy.digDrops = digDrops;
        copy.chainDigEnabled = chainDigEnabled;
        copy.chainDigRadius = chainDigRadius;
        copy.chainDigLevel = chainDigLevel;
        copy.chainDigDrops = chainDigDrops;
        copy.damageMultiplier = damageMultiplier;
        copy.speedMultiplier = speedMultiplier;
        copy.trails.addAll(trails);
        return copy;
    }

    public void enablePickup(float radius) {
        this.pickupEnabled = true;
        this.pickupRadius = Math.max(0.0f, radius);
    }

    public void enableDig(float radius, int level, boolean drops) {
        this.digEnabled = true;
        this.digRadius = Math.max(0.0f, radius);
        this.digLevel = level;
        this.digDrops = drops;
    }

    public void enableChainDig(float radius, int level, boolean drops) {
        this.chainDigEnabled = true;
        this.chainDigRadius = Math.max(0.0f, radius);
        this.chainDigLevel = level;
        this.chainDigDrops = drops;
    }

    public boolean isPickupEnabled() { return pickupEnabled; }
    public float getPickupRadius() { return pickupRadius; }
    public boolean isDigEnabled() { return digEnabled; }
    public float getDigRadius() { return digRadius; }
    public int getDigLevel() { return digLevel; }
    public boolean isDigDrops() { return digDrops; }
    public boolean isChainDigEnabled() { return chainDigEnabled; }
    public float getChainDigRadius() { return chainDigRadius; }
    public int getChainDigLevel() { return chainDigLevel; }
    public boolean isChainDigDrops() { return chainDigDrops; }

    public float getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(float damageMultiplier) { this.damageMultiplier = damageMultiplier; }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(float speedMultiplier) { this.speedMultiplier = speedMultiplier; }

    /** Add a particle trail to this cast; duplicates are ignored. */
    public void addTrail(SpellTrail trail) {
        if (trail != null && !trails.contains(trail)) {
            trails.add(trail);
        }
    }

    public List<SpellTrail> getTrails() { return List.copyOf(trails); }
    
    public boolean consumeMana(int cost) {
        if (cost <= 0) return true;

        // The ItemStack captured at cast time goes stale as soon as the player
        // moves the wand or rearranges the inventory. Resolve the wand by id from
        // the caster every time so the real stack is charged instead of a detached
        // copy (which previously granted effectively infinite mana).
        ItemStack current = WandData.findWandById(caster, wandId);
        if (current.isEmpty()) {
            // Wand dropped / stored away: never charge a detached copy. The
            // ActiveSpellManager also stops this branch on its next tick.
            return false;
        }

        if (!WandData.consumeMana(current, cost, level)) {
            return false;
        }

        if (caster instanceof ServerPlayer player) {
            ModNetwork.sendManaSync(player, current);
        }
        return true;
    }

    public void markNodeRemoved(UUID nodeId) {
        removedNodes.add(nodeId);
    }
    
    public boolean isNodeRemoved(UUID nodeId) {
        return removedNodes.contains(nodeId);
    }
    
    public void markTerminated() { this.terminated = true; }
    public boolean isTerminated() { return terminated; }

    public ServerLevel level() { return level; }
    public LivingEntity caster() { return caster; }
    public ItemStack wand() {
        ItemStack current = WandData.findWandById(caster, wandId);
        return current.isEmpty() ? wand : current;
    }
    public SpellGraph graph() { return graph; }
    public Vec3 origin() { return origin; }
    public Vec3 direction() { return direction; }
    public UUID wandId() { return wandId; }
    public Vec3 getCurrentPosition() { return currentPosition; }
    public Vec3 getCurrentVelocity() { return currentVelocity; }
    public void setCurrentPosition(Vec3 pos) { this.currentPosition = pos; }
    public void setCurrentVelocity(Vec3 vel) { this.currentVelocity = vel; }
    public Set<UUID> getRemovedNodes() { return java.util.Collections.unmodifiableSet(removedNodes); }
    public double getManaPool() { return manaPool; }
}