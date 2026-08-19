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
    private boolean delayed = false;
    private int flightTicks = 0;
    private boolean trigger = false;
    private int maxTicks = MAX_LIFETIME_TICKS;
    private UUID ownerId = null;
    private UUID wandId = null;
    private List<SpellNode.Connection> continuation = List.of();
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
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide()) {
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

        SpellRunner.continueFrom(serverLevel, ownerId, wandId, continuation, at, vel);
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