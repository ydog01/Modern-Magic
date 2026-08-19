package top.ydog01.mmagic.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.spell.SpellHarvest;
import top.ydog01.mmagic.spell.SpellModifiers;
import top.ydog01.mmagic.spell.SpellNode;
import top.ydog01.mmagic.spell.SpellRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MagicMissileEntity extends ThrowableItemProjectile {

    public static final int MAX_LIFETIME_TICKS = 200;
    public static final double HOMING_RANGE = 48.0;

    private float damage = 4.0f;
    private int mods;
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
    private LivingEntity lockedTarget;
    private int searchCooldown = 0;
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

    public void setModifiers(SpellModifiers modifiers) {
        this.mods = modifiers.bits();
        this.healAmount = modifiers.healAmount();
        this.burstRadius = modifiers.burstRadius();
        this.bounces = modifiers.bounces();
        this.pierces = modifiers.pierces();
        this.pickupRadius = modifiers.pickupRadius();
        this.digRadius = modifiers.digRadius();
        this.digLevel = modifiers.digLevel();
        this.digDrops = modifiers.digDrops();
        this.chainRadius = modifiers.chainRadius();
        this.chainLevel = modifiers.chainLevel();
        this.chainDrops = modifiers.chainDrops();
        this.lootingLevel = modifiers.lootingLevel();
        this.setNoGravity((this.mods & SpellModifiers.GRAVITY) == 0);
    }

    public void setMultipliers(float damageMult, float speedMult) {
        this.damageMult = damageMult;
        this.speedMult = speedMult;
    }

    public void setEffectDuration(int ticks) {
        this.effectDuration = ticks;
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
        if ((this.mods & SpellModifiers.HOMING) != 0) {
            steerTowardTarget();
        }
        if (this.delayed) {
            this.flightTicks--;
            if (this.flightTicks <= 0) {
                continueSpell(this.position(), this.getDeltaMovement());
                this.discard();
            }
        }
    }

    private void steerTowardTarget() {
        if (this.lockedTarget == null) {
            if (this.searchCooldown > 0) {
                this.searchCooldown--;
                return;
            }
            this.lockedTarget = findTarget();
            if (this.lockedTarget == null) {
                this.searchCooldown = 5;
                return;
            }
        }
        if (!this.lockedTarget.isAlive() || this.lockedTarget.isRemoved()) {
            this.lockedTarget = null;
            this.searchCooldown = 8;
            return;
        }
        Vec3 pos = this.position();
        Vec3 aim = this.lockedTarget.getEyePosition().subtract(pos);
        if (aim.lengthSqr() < 0.0001) {
            return;
        }
        Vec3 to = aim.normalize();
        Vec3 vel = this.getDeltaMovement();
        double speed = vel.length();
        if (speed <= 0.001) {
            return;
        }
        Vec3 velDir = vel.normalize();
        if (velDir.dot(to) < 0.0 && aim.lengthSqr() > 36.0) {
            return;
        }
        double homingSpeed = Math.min(speed, 1.2);
        Vec3 steered = velDir.scale(0.3).add(to.scale(0.7)).normalize().scale(homingSpeed);
        this.setDeltaMovement(steered);
    }

    private LivingEntity findTarget() {
        Entity owner = getOwner();
        java.util.function.Predicate<LivingEntity> valid = e -> e.isAlive() && e != owner;
        if (owner != null) {
            Vec3 eye = owner.getEyePosition();
            Vec3 end = eye.add(owner.getLookAngle().scale(HOMING_RANGE));
            LivingEntity aimed = null;
            double aimedDist = Double.MAX_VALUE;
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(2.0), valid)) {
                java.util.Optional<Vec3> hit = e.getBoundingBox().inflate(0.5).clip(eye, end);
                if (hit.isPresent()) {
                    double d = eye.distanceToSqr(hit.get());
                    if (d < aimedDist) {
                        aimedDist = d;
                        aimed = e;
                    }
                }
            }
            if (aimed != null) {
                return aimed;
            }
        }
        Vec3 pos = this.position();
        AABB box = new AABB(pos, pos).inflate(HOMING_RANGE);
        LivingEntity target = null;
        double best = HOMING_RANGE * HOMING_RANGE;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, valid)) {
            double d = e.distanceToSqr(this);
            if (d < best) {
                best = d;
                target = e;
            }
        }
        return target;
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
        if ((this.mods & SpellModifiers.HEAL) != 0 && target instanceof LivingEntity living) {
            living.heal(this.healAmount * this.damageMult);
        } else if ((this.mods & SpellModifiers.WATER) != 0) {
            target.clearFire();
            hurtWithLooting(target, this.damage * this.damageMult);
        } else {
            hurtWithLooting(target, this.damage * this.damageMult);
        }
        if (target instanceof LivingEntity living) {
            if ((this.mods & SpellModifiers.FIRE) != 0) {
                living.setRemainingFireTicks(this.effectDuration);
            }
            if ((this.mods & SpellModifiers.ICE) != 0) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.effectDuration, 1));
            }
            if ((this.mods & SpellModifiers.POISON) != 0) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, this.effectDuration, 0));
            }
            if ((this.mods & SpellModifiers.WITHER) != 0) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, this.effectDuration, 1));
            }
            if ((this.mods & SpellModifiers.LEVITATE) != 0) {
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, this.effectDuration, 0));
            }
        }
        if ((this.mods & SpellModifiers.BURST) != 0) {
            burst(result.getLocation());
        }
        if ((this.mods & SpellModifiers.PICKUP) != 0 && level() instanceof ServerLevel serverLevel
                && getOwner() instanceof LivingEntity owner) {
            SpellHarvest.pickup(serverLevel, owner, result.getLocation(), this.pickupRadius);
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

    private void hurtWithLooting(Entity target, float amount) {
        if ((this.mods & SpellModifiers.LOOTING) != 0 && this.lootingLevel > 0) {
            SpellHarvest.setPendingLooting(target.getId(), this.lootingLevel);
            try {
                target.hurt(target.damageSources().indirectMagic(this, getOwner()), amount);
            } finally {
                SpellHarvest.clearPendingLooting(target.getId());
            }
        } else {
            target.hurt(target.damageSources().indirectMagic(this, getOwner()), amount);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide()) {
            return;
        }
        if ((this.mods & SpellModifiers.PIERCE_BLOCK) != 0) {
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
        if ((this.mods & SpellModifiers.BURST) != 0) {
            burst(result.getLocation());
        }
        if (level() instanceof ServerLevel serverLevel && getOwner() instanceof LivingEntity owner) {
            boolean silk = (this.mods & SpellModifiers.SILK_TOUCH) != 0;
            int fortune = (this.mods & SpellModifiers.LOOTING) != 0 ? this.lootingLevel : 0;
            if ((this.mods & SpellModifiers.DIG) != 0) {
                SpellHarvest.digArea(serverLevel, owner, result.getBlockPos(), this.digRadius,
                        this.digLevel, this.digDrops, silk, fortune);
            }
            if ((this.mods & SpellModifiers.CHAIN_DIG) != 0) {
                SpellHarvest.chainDig(serverLevel, owner, result.getBlockPos(), this.chainRadius,
                        this.chainLevel, this.chainDrops, silk, fortune);
            }
            if ((this.mods & SpellModifiers.PICKUP) != 0) {
                SpellHarvest.pickup(serverLevel, owner, result.getLocation(), this.pickupRadius);
            }
        }
        if (this.trigger) {
            continueSpell(result.getLocation().add(0.0, 2.0, 0.0), this.getDeltaMovement());
        }
        this.discard();
    }

    private void burst(Vec3 at) {
        this.level().explode(getOwner(), at.x, at.y, at.z, this.burstRadius, false, Level.ExplosionInteraction.NONE);
    }

    private void continueSpell(Vec3 at, Vec3 vel) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (ownerId == null || wandId == null || continuation.isEmpty()) {
            return;
        }

        SpellRunner.continueFrom(serverLevel, ownerId, wandId, continuation, at, vel, this.damageMult, this.speedMult,
                new SpellModifiers(this.mods, this.healAmount, this.burstRadius, this.bounces, this.pierces,
                        this.pickupRadius, this.digRadius, this.digLevel, this.digDrops,
                        this.chainRadius, this.chainLevel, this.chainDrops, this.lootingLevel));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", this.damage);
        tag.putInt("mods", this.mods);
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
        this.mods = tag.getInt("mods");
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