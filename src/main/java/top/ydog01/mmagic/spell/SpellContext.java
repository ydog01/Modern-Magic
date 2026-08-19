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
        return new SpellContext(level, caster, wand, graph, origin, direction, wandId,
                currentPosition, currentVelocity, removedNodes, terminated, manaPool, lastUpdateTick);
    }
    
    public boolean consumeMana(int cost) {
        if (cost <= 0) return true;
        if (caster instanceof ServerPlayer player && player.getAbilities().instabuild) return true;
        
        long now = level.getGameTime();
        if (lastUpdateTick != now) {
            double regen = WandData.getRegen(wand);
            int maxMana = WandData.getMaxMana(wand);
            manaPool = Math.min(maxMana, manaPool + regen * (now - lastUpdateTick) / 20.0);
            lastUpdateTick = now;
        }
        
        if (manaPool < cost) return false;
        manaPool -= cost;
        
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