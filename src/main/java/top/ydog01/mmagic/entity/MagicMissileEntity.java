package top.ydog01.mmagic.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.SpellHarvest;
import top.ydog01.mmagic.spell.SpellTrail;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.casting.SpellRunner;
import top.ydog01.mmagic.util.WandData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MagicMissileEntity extends ThrowableItemProjectile {

    public static final int MAX_LIFETIME_TICKS = 200;

    private float damage = 4.0f;
    private boolean delayed = false;
    private int flightTicks = 0;
    private boolean trigger = false;
    private int maxTicks = MAX_LIFETIME_TICKS;
    private UUID ownerId = null;
    private UUID wandId = null;
    private List<SpellNode.Connection> continuation = List.of();
    private final List<SpellTrail> trails = new ArrayList<>();
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
    private long expireAt = -1;
    private final java.util.Set<Integer> hitEntities = new java.util.HashSet<>();

    public MagicMissileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public MagicMissileEntity(EntityType<? extends ThrowableItemProjectile> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
        this.setNoGravity(true);
    }

    public MagicMissileEntity(EntityType<? extends ThrowableItemProjectile> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.NETHER_STAR;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void applyHarvestModifiers(SpellContext ctx) {
        this.pickupEnabled = ctx.isPickupEnabled();
        this.pickupRadius = ctx.getPickupRadius();
        this.digEnabled = ctx.isDigEnabled();
        this.digRadius = ctx.getDigRadius();
        this.digLevel = ctx.getDigLevel();
        this.digDrops = ctx.isDigDrops();
        this.chainDigEnabled = ctx.isChainDigEnabled();
        this.chainDigRadius = ctx.getChainDigRadius();
        this.chainDigLevel = ctx.getChainDigLevel();
        this.chainDigDrops = ctx.isChainDigDrops();
    }

    public void setTrails(List<SpellTrail> trails) {
        this.trails.clear();
        this.trails.addAll(trails);
    }

    /** Remember which caster/wand this missile belongs to, so it can fizzle if the wand is lost. */
    public void setSpellOwner(UUID ownerId, UUID wandId) {
        this.ownerId = ownerId;
        this.wandId = wandId;
    }

    public void setDelayed(boolean delayed, int flightTicks, UUID ownerId, UUID wandId, List<SpellNode.Connection> continuation) {
        this.delayed = delayed;
        this.flightTicks = flightTicks;
        this.ownerId = ownerId;
        this.wandId = wandId;
        this.continuation = new ArrayList<>(continuation);
    }

    public void setTrigger(boolean trigger, int maxTicks, UUID ownerId, UUID wandId, List<SpellNode.Connection> continuation) {
        this.trigger = trigger;
        this.maxTicks = maxTicks;
        this.ownerId = ownerId;
        this.wandId = wandId;
        this.continuation = new ArrayList<>(continuation);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        // onHitBlock / onHitEntity may already have removed the missile this tick.
        if (this.isRemoved()) {
            return;
        }
        if (!isWandStillPresent()) {
            spawnBurstParticles();
            this.discard();
            return;
        }
        long gameTime = level().getGameTime();
        if (this.expireAt < 0) {
            this.expireAt = gameTime + Math.max(1, this.delayed ? this.flightTicks : this.maxTicks - this.tickCount);
        }
        if (gameTime >= this.expireAt) {
            spawnBurstParticles();
            this.discard();
            return;
        }
        if (this.delayed) {
            this.flightTicks--;
            if (this.flightTicks <= 0) {
                spawnBurstParticles();
                continueSpell(this.position(), this.getDeltaMovement());
                this.discard();
                return;
            }
        }
        spawnTrailParticles();
    }

    /** Returns false once the owning wand has left the caster's possession. */
    private boolean isWandStillPresent() {
        if (this.wandId == null) {
            return true;
        }
        if (this.ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!(serverLevel.getEntity(this.ownerId) instanceof LivingEntity owner)) {
            return false;
        }
        return !WandData.findWandById(owner, this.wandId).isEmpty();
    }

    /** Emit the modifier trails between the previous and current tick position. */
    private void spawnTrailParticles() {
        if (trails.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double dx = getX() - xo;
        double dy = getY() - yo;
        double dz = getZ() - zo;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(distance));
        for (SpellTrail trail : trails) {
            for (int i = 0; i < steps; i++) {
                double t = steps == 1 ? 0.0 : i / (double) (steps - 1);
                trail.spawn(serverLevel, xo + dx * t, yo + dy * t, zo + dz * t);
            }
        }
    }

    /** Emit a larger burst of every trail, used on impact / expiry. */
    private void spawnBurstParticles() {
        if (trails.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (SpellTrail trail : trails) {
            trail.burst(serverLevel, getX(), getY(), getZ());
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide()) {
            return;
        }
        Entity target = result.getEntity();
        if (target == getOwner() || !this.hitEntities.add(target.getId())) {
            return;
        }
        if (target instanceof LivingEntity livingTarget) {
            livingTarget.hurt(livingTarget.damageSources().indirectMagic(this, getOwner()), this.damage);
        }
        if (this.pickupEnabled && level() instanceof ServerLevel serverLevel && getOwner() instanceof LivingEntity owner) {
            SpellHarvest.pickup(serverLevel, owner, result.getLocation(), this.pickupRadius);
        }
        if (this.trigger) {
            continueSpell(result.getLocation().add(0.0, 2.0, 0.0), this.getDeltaMovement());
        } else if (this.delayed) {
            // A delayed missile that hits early must still release its payload at the
            // impact point, otherwise downstream nodes (delayed meteors, teleports, ...)
            // are silently skipped.
            continueSpell(result.getLocation(), this.getDeltaMovement());
        }
        spawnBurstParticles();
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide()) {
            return;
        }
        if (level() instanceof ServerLevel serverLevel && getOwner() instanceof LivingEntity owner) {
            if (this.digEnabled) {
                SpellHarvest.digArea(serverLevel, owner, result.getBlockPos(), this.digRadius,
                        this.digLevel, this.digDrops, false, 0);
            }
            if (this.chainDigEnabled) {
                SpellHarvest.chainDig(serverLevel, owner, result.getBlockPos(), this.chainDigRadius,
                        this.chainDigLevel, this.chainDigDrops, false, 0);
            }
            // Run after digging so drops spawned by the harvest can be collected.
            if (this.pickupEnabled) {
                SpellHarvest.pickup(serverLevel, owner, result.getLocation(), this.pickupRadius);
            }
        }
        if (this.trigger) {
            continueSpell(result.getLocation().add(0.0, 2.0, 0.0), this.getDeltaMovement());
        } else if (this.delayed) {
            // A delayed missile that hits early must still release its payload at the
            // impact point, otherwise downstream nodes (delayed meteors, teleports, ...)
            // are silently skipped.
            continueSpell(result.getLocation(), this.getDeltaMovement());
        }
        spawnBurstParticles();
        this.discard();
    }

    private void continueSpell(Vec3 at, Vec3 vel) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (ownerId == null || wandId == null || continuation.isEmpty()) {
            return;
        }

        SpellRunner.continueFrom(serverLevel, ownerId, wandId, continuation, at, vel, trails);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", this.damage);
        tag.putBoolean("delayed", this.delayed);
        tag.putInt("flightTicks", this.flightTicks);
        tag.putBoolean("trigger", this.trigger);
        tag.putInt("maxTicks", this.maxTicks);
        tag.putLong("expireAt", this.expireAt);
        if (ownerId != null) {
            tag.putUUID("ownerId", ownerId);
        }
        if (wandId != null) {
            tag.putUUID("wandId", wandId);
        }
        tag.putBoolean("pickupEnabled", this.pickupEnabled);
        tag.putFloat("pickupRadius", this.pickupRadius);
        tag.putBoolean("digEnabled", this.digEnabled);
        tag.putFloat("digRadius", this.digRadius);
        tag.putInt("digLevel", this.digLevel);
        tag.putBoolean("digDrops", this.digDrops);
        tag.putBoolean("chainDigEnabled", this.chainDigEnabled);
        tag.putFloat("chainDigRadius", this.chainDigRadius);
        tag.putInt("chainDigLevel", this.chainDigLevel);
        tag.putBoolean("chainDigDrops", this.chainDigDrops);
        if (!trails.isEmpty()) {
            ListTag trailList = new ListTag();
            for (SpellTrail trail : trails) {
                trailList.add(StringTag.valueOf(trail.name()));
            }
            tag.put("trails", trailList);
        }
        if (!continuation.isEmpty()) {
            ListTag list = new ListTag();
            for (SpellNode.Connection c : continuation) {
                CompoundTag ct = new CompoundTag();
                ct.putUUID("target", c.targetId);
                ct.putInt("port", c.targetPort);
                list.add(ct);
            }
            tag.put("continuation", list);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("damage");
        this.delayed = tag.getBoolean("delayed");
        this.flightTicks = tag.getInt("flightTicks");
        this.trigger = tag.getBoolean("trigger");
        this.maxTicks = tag.getInt("maxTicks");
        this.expireAt = tag.contains("expireAt") ? tag.getLong("expireAt") : -1;
        this.pickupEnabled = tag.getBoolean("pickupEnabled");
        this.pickupRadius = tag.contains("pickupRadius") ? tag.getFloat("pickupRadius") : 4.0f;
        this.digEnabled = tag.getBoolean("digEnabled");
        this.digRadius = tag.contains("digRadius") ? tag.getFloat("digRadius") : 2.0f;
        this.digLevel = tag.contains("digLevel") ? tag.getInt("digLevel") : 1;
        this.digDrops = !tag.contains("digDrops") || tag.getBoolean("digDrops");
        this.chainDigEnabled = tag.getBoolean("chainDigEnabled");
        this.chainDigRadius = tag.contains("chainDigRadius") ? tag.getFloat("chainDigRadius") : 8.0f;
        this.chainDigLevel = tag.contains("chainDigLevel") ? tag.getInt("chainDigLevel") : 1;
        this.chainDigDrops = !tag.contains("chainDigDrops") || tag.getBoolean("chainDigDrops");
        this.trails.clear();
        if (tag.contains("trails", Tag.TAG_LIST)) {
            ListTag trailList = tag.getList("trails", Tag.TAG_STRING);
            for (int i = 0; i < trailList.size(); i++) {
                try {
                    this.trails.add(SpellTrail.valueOf(trailList.getString(i)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown trails from a different mod version.
                }
            }
        }
        if (tag.hasUUID("ownerId")) {
            this.ownerId = tag.getUUID("ownerId");
        }
        if (tag.hasUUID("wandId")) {
            this.wandId = tag.getUUID("wandId");
        }
        this.continuation = new ArrayList<>();
        if (tag.contains("continuation", Tag.TAG_LIST)) {
            ListTag list = tag.getList("continuation", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag ct = list.getCompound(i);
                this.continuation.add(new SpellNode.Connection(ct.getUUID("target"), ct.getInt("port")));
            }
        }
    }
}