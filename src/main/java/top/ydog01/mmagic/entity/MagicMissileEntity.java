package top.ydog01.mmagic.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import top.ydog01.mmagic.spell.SpellNode;
import top.ydog01.mmagic.spell.SpellRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MagicMissileEntity extends ThrowableItemProjectile {

    public static final int MAX_LIFETIME_TICKS = 200;

    private float damage = 4.0f;
    private float healAmount = 4.0f;
    private float damageMult = 1.0f;
    private float speedMult = 1.0f;
    private float burstRadius = 2.0f;
    private int effectDuration = 80;
    private boolean delayed = false;
    private int flightTicks = 0;
    private boolean trigger = false;
    private int maxTicks = MAX_LIFETIME_TICKS;
    private UUID ownerId = null;
    private UUID wandId = null;
    private List<SpellNode.Connection> continuation = List.of();
    private int bounces = 0;
    private int pierces = 0;
    private long expireAt = -1;
    private float pickupRadius = 4.0f;
    private float digRadius = 2.0f;
    private int digLevel = 1;
    private boolean digDrops = true;
    private float chainRadius = 8.0f;
    private int chainLevel = 1;
    private boolean chainDrops = true;
    private int lootingLevel = 0;
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
        long gameTime = level().getGameTime();
        if (this.expireAt < 0) {
            this.expireAt = gameTime + Math.max(1, this.delayed ? this.flightTicks : this.maxTicks - this.tickCount);
        }
        if (gameTime >= this.expireAt) {
            this.discard();
            return;
        }
        if (this.delayed) {
            this.flightTicks--;
            if (this.flightTicks <= 0) {
                continueSpell(this.position(), this.getDeltaMovement());
                this.discard();
            }
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
        if (this.trigger) {
            continueSpell(result.getLocation().add(0.0, 2.0, 0.0), this.getDeltaMovement());
        }
        if (this.pierces > 0) {
            this.pierces--;
        } else {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide()) {
            return;
        }
        if (this.bounces > 0) {
            this.bounces--;
            Vec3 normal = Vec3.atLowerCornerOf(result.getDirection().getNormal());
            Vec3 vel = this.getDeltaMovement();
            double dot = vel.dot(normal);
            if (dot < 0.0) {
                this.setDeltaMovement(vel.subtract(normal.scale(2.0 * dot)).scale(0.75));
                this.setPos(result.getLocation().add(normal.scale(0.05)));
            }
            return;
        }
        if (this.trigger) {
            continueSpell(result.getLocation().add(0.0, 2.0, 0.0), this.getDeltaMovement());
        }
        this.discard();
    }

    private void continueSpell(Vec3 at, Vec3 vel) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (ownerId == null || wandId == null || continuation.isEmpty()) {
            return;
        }

        SpellRunner.continueFrom(serverLevel, ownerId, wandId, continuation, at, vel, this.damageMult, this.speedMult);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", this.damage);
        tag.putFloat("healAmount", this.healAmount);
        tag.putFloat("damageMult", this.damageMult);
        tag.putFloat("speedMult", this.speedMult);
        tag.putFloat("burstRadius", this.burstRadius);
        tag.putInt("effectDuration", this.effectDuration);
        tag.putBoolean("delayed", this.delayed);
        tag.putInt("flightTicks", this.flightTicks);
        tag.putBoolean("trigger", this.trigger);
        tag.putInt("maxTicks", this.maxTicks);
        tag.putInt("bounces", this.bounces);
        tag.putInt("pierces", this.pierces);
        tag.putLong("expireAt", this.expireAt);
        tag.putFloat("pickupRadius", this.pickupRadius);
        tag.putFloat("digRadius", this.digRadius);
        tag.putInt("digLevel", this.digLevel);
        tag.putBoolean("digDrops", this.digDrops);
        tag.putFloat("chainRadius", this.chainRadius);
        tag.putInt("chainLevel", this.chainLevel);
        tag.putBoolean("chainDrops", this.chainDrops);
        tag.putInt("lootingLevel", this.lootingLevel);
        if (ownerId != null) {
            tag.putUUID("ownerId", ownerId);
        }
        if (wandId != null) {
            tag.putUUID("wandId", wandId);
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
        this.healAmount = tag.contains("healAmount") ? tag.getFloat("healAmount") : 4.0f;
        this.damageMult = tag.contains("damageMult") ? tag.getFloat("damageMult") : 1.0f;
        this.speedMult = tag.contains("speedMult") ? tag.getFloat("speedMult") : 1.0f;
        this.burstRadius = tag.contains("burstRadius") ? tag.getFloat("burstRadius") : 2.0f;
        this.effectDuration = tag.contains("effectDuration") ? tag.getInt("effectDuration") : 80;
        this.delayed = tag.getBoolean("delayed");
        this.flightTicks = tag.getInt("flightTicks");
        this.trigger = tag.getBoolean("trigger");
        this.maxTicks = tag.getInt("maxTicks");
        this.bounces = tag.contains("bounces") ? tag.getInt("bounces") : 0;
        this.pierces = tag.contains("pierces") ? tag.getInt("pierces") : 0;
        this.expireAt = tag.contains("expireAt") ? tag.getLong("expireAt") : -1;
        this.pickupRadius = tag.contains("pickupRadius") ? tag.getFloat("pickupRadius") : 4.0f;
        this.digRadius = tag.contains("digRadius") ? tag.getFloat("digRadius") : 2.0f;
        this.digLevel = tag.contains("digLevel") ? tag.getInt("digLevel") : 1;
        this.digDrops = !tag.contains("digDrops") || tag.getBoolean("digDrops");
        this.chainRadius = tag.contains("chainRadius") ? tag.getFloat("chainRadius") : 8.0f;
        this.chainLevel = tag.contains("chainLevel") ? tag.getInt("chainLevel") : 1;
        this.chainDrops = !tag.contains("chainDrops") || tag.getBoolean("chainDrops");
        this.lootingLevel = tag.contains("lootingLevel") ? tag.getInt("lootingLevel") : 0;
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