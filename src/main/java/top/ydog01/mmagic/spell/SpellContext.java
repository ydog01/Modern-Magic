package top.ydog01.mmagic.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.network.ModNetwork;
import top.ydog01.mmagic.util.WandData;

import java.util.HashSet;
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
    
    public SpellContext(ServerLevel level, LivingEntity caster, ItemStack wand) {
        this.level = level;
        this.caster = caster;
        this.wand = wand;
        this.graph = WandData.getGraph(wand);
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
        SpellContext copy = new SpellContext(level, caster, wand, graph, origin, direction, wandId,
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
    
    public boolean consumeMana(int cost) {
        if (cost <= 0) return true;

        // Use the wand's stored mana directly so every active branch of the same
        // wand consumes from one shared pool.
        if (!WandData.consumeMana(wand, cost, level)) {
            return false;
        }

        if (caster instanceof ServerPlayer player) {
            ModNetwork.sendManaSync(player, wand);
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
    public ItemStack wand() { return wand; }
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