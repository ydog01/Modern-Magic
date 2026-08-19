package top.ydog01.mmagic.spell.node_implementations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import top.ydog01.mmagic.spell.casting.ExecutionResult;
import top.ydog01.mmagic.spell.node_api.ModifierNode;
import top.ydog01.mmagic.spell.node_api.ProjectileNode;
import top.ydog01.mmagic.spell.SpellContext;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.SpellNodeType;
import top.ydog01.mmagic.spell.SpellRegistry;
import java.util.*;

public final class ModifierNodes {
    private ModifierNodes() {}

    public abstract static class ElementModifierNode extends ModifierNode {
        public ElementModifierNode(String id, SpellNodeType type) { 
            super(id, type); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new WrappedProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        protected abstract void applyEffect(net.minecraft.world.entity.Entity target, SpellContext ctx);
        
        protected class WrappedProjectile extends ProjectileNode {
            protected final ProjectileNode wrapped;
            private boolean launched = false;
            private boolean done = false;
            
            public WrappedProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_wrapped", wrapped.getType());
                this.wrapped = wrapped;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                applyEffect(entity, ctx);
                done = true;
                return wrapped.onCollideEntity(entity, ctx);
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) { 
                return wrapped.onUpdate(ctx); 
            }
        }
    }

    public static class FireModifierNode extends ElementModifierNode {
        public static final String ID = "fire";
        private static final int DURATION = 80;
        
        public FireModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        protected void applyEffect(net.minecraft.world.entity.Entity target, SpellContext ctx) {
            if (target instanceof LivingEntity living) {
                living.setRemainingFireTicks(DURATION);
            }
        }
        
        @Override
        public int getManaCost() {
            return super.getManaCost() + DURATION / 20;
        }
    }

    public static class IceModifierNode extends ElementModifierNode {
        public static final String ID = "ice";
        private static final int DURATION = 100;
        
        public IceModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        protected void applyEffect(net.minecraft.world.entity.Entity target, SpellContext ctx) {
            if (target instanceof LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, DURATION, 1
                ));
            }
        }
        
        @Override
        public int getManaCost() {
            return super.getManaCost() + DURATION / 20;
        }
    }

    public static class PoisonModifierNode extends ElementModifierNode {
        public static final String ID = "poison";
        private static final int DURATION = 80;
        
        public PoisonModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        protected void applyEffect(net.minecraft.world.entity.Entity target, SpellContext ctx) {
            if (target instanceof LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, DURATION, 0
                ));
            }
        }
        
        @Override
        public int getManaCost() {
            return super.getManaCost() + DURATION / 20;
        }
    }

    public static class WitherModifierNode extends ElementModifierNode {
        public static final String ID = "wither";
        private static final int DURATION = 80;
        
        public WitherModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        protected void applyEffect(net.minecraft.world.entity.Entity target, SpellContext ctx) {
            if (target instanceof LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WITHER, DURATION, 1
                ));
            }
        }
        
        @Override
        public int getManaCost() {
            return super.getManaCost() + DURATION / 20;
        }
    }

    public static class LevitateModifierNode extends ElementModifierNode {
        public static final String ID = "levitate";
        private static final int DURATION = 80;
        
        public LevitateModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        protected void applyEffect(net.minecraft.world.entity.Entity target, SpellContext ctx) {
            if (target instanceof LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.LEVITATION, DURATION, 0
                ));
            }
        }
        
        @Override
        public int getManaCost() {
            return super.getManaCost() + DURATION / 20;
        }
    }

    public static class WaterModifierNode extends ElementModifierNode {
        public static final String ID = "water";
        
        public WaterModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        protected void applyEffect(net.minecraft.world.entity.Entity target, SpellContext ctx) {
            target.clearFire();
        }
    }

    public static class HealModifierNode extends ElementModifierNode {
        public static final String ID = "heal_mod";
        private static final float AMOUNT = 4.0f;
        
        public HealModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        protected void applyEffect(net.minecraft.world.entity.Entity target, SpellContext ctx) {
            if (target instanceof LivingEntity living) {
                living.heal(AMOUNT);
            }
        }
        
        @Override
        public int getManaCost() {
            return super.getManaCost() + (int) AMOUNT;
        }
    }

    public static class HomingModifierNode extends ModifierNode {
        public static final String ID = "homing";
        
        public HomingModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new HomingProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        private static class HomingProjectile extends ProjectileNode {
            private final ProjectileNode wrapped;
            private LivingEntity target;
            private int searchCooldown = 0;
            private boolean launched = false;
            private boolean done = false;
            
            public HomingProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_homing", wrapped.getType());
                this.wrapped = wrapped;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) {
                if (target == null || !target.isAlive()) {
                    if (searchCooldown > 0) {
                        searchCooldown--;
                    } else {
                        searchCooldown = 5;
                        AABB box = new AABB(ctx.getCurrentPosition(), ctx.getCurrentPosition()).inflate(48.0);
                        target = ctx.level().getEntitiesOfClass(LivingEntity.class, box, 
                            e -> e.isAlive() && e != ctx.caster()
                        ).stream().findFirst().orElse(null);
                    }
                }
                if (target != null) {
                    Vec3 to = target.getEyePosition().subtract(ctx.getCurrentPosition());
                    if (to.lengthSqr() > 0.0001) {
                        ctx.setCurrentVelocity(
                            ctx.getCurrentVelocity().add(to.normalize().scale(0.1))
                        );
                    }
                }
                return wrapped.onUpdate(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                done = true;
                return wrapped.onCollideEntity(entity, ctx);
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
        }
    }

    public static class BurstModifierNode extends ModifierNode {
        public static final String ID = "burst";
        private static final float RADIUS = 2.0f;
        
        public BurstModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new BurstProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        private class BurstProjectile extends ProjectileNode {
            private final ProjectileNode wrapped;
            private boolean exploded = false;
            private boolean launched = false;
            private boolean done = false;
            
            public BurstProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_burst", wrapped.getType());
                this.wrapped = wrapped;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                if (!exploded) {
                    exploded = true;
                    Vec3 pos = ctx.getCurrentPosition();
                    ctx.level().explode(ctx.caster(), pos.x, pos.y, pos.z, RADIUS, false, Level.ExplosionInteraction.NONE);
                }
                done = true;
                return wrapped.onCollideEntity(entity, ctx);
            }
            
            @Override 
            public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
                if (!exploded) {
                    exploded = true;
                    ctx.level().explode(ctx.caster(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RADIUS, false, Level.ExplosionInteraction.NONE);
                }
                done = true;
                return wrapped.onCollideBlock(pos, ctx);
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) { 
                return wrapped.onUpdate(ctx); 
            }
        }
    }

    public static class BounceModifierNode extends ModifierNode {
        public static final String ID = "bounce";
        private static final int MAX_BOUNCES = 2;
        
        public BounceModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new BounceProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        private class BounceProjectile extends ProjectileNode {
            private final ProjectileNode wrapped;
            private int bouncesRemaining;
            private boolean launched = false;
            private boolean done = false;
            
            public BounceProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_bounce", wrapped.getType());
                this.wrapped = wrapped;
                this.bouncesRemaining = MAX_BOUNCES;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
                if (bouncesRemaining > 0) {
                    bouncesRemaining--;
                    Vec3 vel = ctx.getCurrentVelocity();
                    BlockState state = ctx.level().getBlockState(pos);
                    VoxelShape shape = state.getCollisionShape(ctx.level(), pos);
                    Vec3 normal = Vec3.ZERO;
                    
                    if (!shape.isEmpty()) {
                        AABB box = shape.bounds();
                        double cx = box.getCenter().x;
                        double cy = box.getCenter().y;
                        double cz = box.getCenter().z;
                        double dx = pos.getX() + 0.5 - cx;
                        double dy = pos.getY() + 0.5 - cy;
                        double dz = pos.getZ() + 0.5 - cz;
                        double maxDist = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                        if (maxDist > 0.001) {
                            normal = new Vec3(dx, dy, dz).normalize();
                        }
                    }
                    
                    if (normal.lengthSqr() < 0.0001) {
                        normal = new Vec3(0, 1, 0);
                    }
                    
                    double dot = vel.dot(normal);
                    if (dot < 0) {
                        ctx.setCurrentVelocity(vel.subtract(normal.scale(2.0 * dot)).scale(0.75));
                    }
                    return ExecutionResult.empty();
                }
                done = true;
                return wrapped.onCollideBlock(pos, ctx);
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                done = true;
                return wrapped.onCollideEntity(entity, ctx);
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) { 
                return wrapped.onUpdate(ctx); 
            }
        }
    }

    public static class PierceModifierNode extends ModifierNode {
        public static final String ID = "pierce";
        private static final int MAX_PIERCES = 1;
        
        public PierceModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new PierceProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        private class PierceProjectile extends ProjectileNode {
            private final ProjectileNode wrapped;
            private int piercesRemaining;
            private final Set<Integer> hitEntities = new HashSet<>();
            private boolean launched = false;
            private boolean done = false;
            
            public PierceProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_pierce", wrapped.getType());
                this.wrapped = wrapped;
                this.piercesRemaining = MAX_PIERCES;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                if (!hitEntities.contains(entity.getId())) {
                    hitEntities.add(entity.getId());
                    ExecutionResult result = wrapped.onCollideEntity(entity, ctx);
                    if (piercesRemaining > 0) {
                        piercesRemaining--;
                        if (result == null || result.isKeepAlive()) {
                            return ExecutionResult.empty();
                        }
                        return result;
                    }
                    done = true;
                    return result;
                }
                return ExecutionResult.empty();
            }
            
            @Override 
            public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
                done = true;
                return wrapped.onCollideBlock(pos, ctx);
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) { 
                return wrapped.onUpdate(ctx); 
            }
        }
    }

    public static class GravityModifierNode extends ModifierNode {
        public static final String ID = "gravity";
        
        public GravityModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                ((ProjectileNode) target).setGravity(true);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
    }

    public static class PierceWallModifierNode extends ModifierNode {
        public static final String ID = "pierce_wall";
        
        public PierceWallModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new WallPierceProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        private static class WallPierceProjectile extends ProjectileNode {
            private final ProjectileNode wrapped;
            private boolean launched = false;
            private boolean done = false;
            
            public WallPierceProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_wallpierce", wrapped.getType());
                this.wrapped = wrapped;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) { 
                return ExecutionResult.empty();
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                done = true;
                return wrapped.onCollideEntity(entity, ctx);
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) { 
                return wrapped.onUpdate(ctx); 
            }
        }
    }

    public static class PickupModifierNode extends ModifierNode {
        public static final String ID = "pickup";
        private static final float RADIUS = 4.0f;
        
        public PickupModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new PickupProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        private class PickupProjectile extends ProjectileNode {
            private final ProjectileNode wrapped;
            private boolean pickupDone = false;
            private boolean launched = false;
            private boolean done = false;
            
            public PickupProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_pickup", wrapped.getType());
                this.wrapped = wrapped;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                doPickup(ctx);
                done = true;
                return wrapped.onCollideEntity(entity, ctx);
            }
            
            @Override 
            public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
                doPickup(ctx);
                done = true;
                return wrapped.onCollideBlock(pos, ctx);
            }
            
            private void doPickup(SpellContext ctx) {
                if (pickupDone) return;
                pickupDone = true;
                
                Vec3 pos = ctx.getCurrentPosition();
                for (ItemEntity item : ctx.level().getEntitiesOfClass(
                        ItemEntity.class, 
                        new AABB(pos, pos).inflate(RADIUS)
                )) {
                    ItemStack stack = item.getItem();
                    if (ctx.caster() instanceof net.minecraft.world.entity.player.Player player) {
                        if (player.getInventory().add(stack)) {
                            item.discard();
                        } else {
                            item.teleportTo(ctx.caster().getX(), ctx.caster().getY() + 0.2, ctx.caster().getZ());
                        }
                    }
                }
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) { 
                return wrapped.onUpdate(ctx); 
            }
        }
    }

    public static class DigModifierNode extends ModifierNode {
        public static final String ID = "dig";
        
        public DigModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new DigProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        private class DigProjectile extends ProjectileNode {
            private final ProjectileNode wrapped;
            private boolean dug = false;
            private boolean launched = false;
            private boolean done = false;
            
            public DigProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_dig", wrapped.getType());
                this.wrapped = wrapped;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
                if (!dug) {
                    dug = true;
                    float radius = paramFloat("radius");
                    int level = paramInt("level");
                    boolean drop = paramBool("drop");
                    int r = Math.max(0, Math.round(radius));
                    
                    for (BlockPos p : BlockPos.betweenClosed(
                            pos.offset(-r, -r, -r), 
                            pos.offset(r, r, r)
                    )) {
                        if (p.distSqr(pos) > radius * radius) continue;
                        tryDig(p, ctx, level, drop);
                    }
                }
                done = true;
                return wrapped.onCollideBlock(pos, ctx);
            }
            
            private void tryDig(BlockPos pos, SpellContext ctx, int digLevel, boolean drop) {
                BlockState state = ctx.level().getBlockState(pos);
                if (state.isAir()) return;
                if (state.getDestroySpeed(ctx.level(), pos) < 0) return;
                if (state.is(BlockTags.NEEDS_DIAMOND_TOOL) && digLevel < 3) return;
                if (state.is(BlockTags.NEEDS_IRON_TOOL) && digLevel < 2) return;
                if (state.is(BlockTags.NEEDS_STONE_TOOL) && digLevel < 1) return;
                
                if (drop) {
                    BlockEntity be = ctx.level().getBlockEntity(pos);
                    for (ItemStack stack : Block.getDrops(state, ctx.level(), pos, be, ctx.caster(), 
                            new ItemStack(Items.NETHERITE_PICKAXE))) {
                        Block.popResource(ctx.level(), pos, stack);
                    }
                }
                ctx.level().destroyBlock(pos, false);
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                done = true;
                return wrapped.onCollideEntity(entity, ctx);
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) { 
                return wrapped.onUpdate(ctx); 
            }
        }
        
        @Override
        public int getManaCost() {
            return super.getManaCost() + (int) paramFloat("radius") + paramInt("level");
        }
    }

    public static class ChainDigModifierNode extends ModifierNode {
        public static final String ID = "chain_dig";
        
        public ChainDigModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) {
            if (target instanceof ProjectileNode) {
                return new ChainDigProjectile((ProjectileNode) target);
            }
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
        
        private class ChainDigProjectile extends ProjectileNode {
            private final ProjectileNode wrapped;
            private boolean dug = false;
            private boolean launched = false;
            private boolean done = false;
            
            public ChainDigProjectile(ProjectileNode wrapped) {
                super(wrapped.getId() + "_chaindig", wrapped.getType());
                this.wrapped = wrapped;
                this.hasGravity = wrapped.hasGravity();
                this.speed = wrapped.getSpeed();
                this.damage = wrapped.getDamage();
            }
            
            @Override
            public ExecutionResult tick(SpellContext ctx) {
                if (done) return ExecutionResult.continueTo(0);
                if (!launched) {
                    launched = true;
                    return null;
                }
                return wrapped.tick(ctx);
            }
            
            @Override 
            public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
                if (!dug) {
                    dug = true;
                    chainDig(pos, ctx);
                }
                done = true;
                return wrapped.onCollideBlock(pos, ctx);
            }
            
            private void chainDig(BlockPos start, SpellContext ctx) {
                BlockState startState = ctx.level().getBlockState(start);
                if (startState.isAir()) return;
                if (startState.getDestroySpeed(ctx.level(), start) < 0) return;
                
                int maxRadius = paramInt("radius");
                int digLevel = paramInt("level");
                boolean drop = paramBool("drop");
                
                if (startState.is(BlockTags.NEEDS_DIAMOND_TOOL) && digLevel < 3) return;
                if (startState.is(BlockTags.NEEDS_IRON_TOOL) && digLevel < 2) return;
                if (startState.is(BlockTags.NEEDS_STONE_TOOL) && digLevel < 1) return;
                
                Set<BlockPos> visited = new HashSet<>();
                Deque<BlockPos> queue = new ArrayDeque<>();
                visited.add(start);
                queue.add(start);
                
                while (!queue.isEmpty()) {
                    BlockPos pos = queue.poll();
                    if (pos.distSqr(start) > maxRadius * maxRadius) continue;
                    
                    BlockState state = ctx.level().getBlockState(pos);
                    if (!state.is(startState.getBlock())) continue;
                    
                    tryDig(pos, ctx, digLevel, drop);
                    
                    for (Direction dir : Direction.values()) {
                        BlockPos next = pos.relative(dir);
                        if (!visited.contains(next) && ctx.level().getBlockState(next).is(startState.getBlock())) {
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
            
            private void tryDig(BlockPos pos, SpellContext ctx, int digLevel, boolean drop) {
                BlockState state = ctx.level().getBlockState(pos);
                if (state.isAir()) return;
                if (state.getDestroySpeed(ctx.level(), pos) < 0) return;
                
                if (drop) {
                    BlockEntity be = ctx.level().getBlockEntity(pos);
                    for (ItemStack stack : Block.getDrops(state, ctx.level(), pos, be, ctx.caster(), 
                            new ItemStack(Items.NETHERITE_PICKAXE))) {
                        Block.popResource(ctx.level(), pos, stack);
                    }
                }
                ctx.level().destroyBlock(pos, false);
            }
            
            @Override 
            public ExecutionResult onCollideEntity(net.minecraft.world.entity.Entity entity, SpellContext ctx) {
                done = true;
                return wrapped.onCollideEntity(entity, ctx);
            }
            
            @Override 
            public ExecutionResult execute(SpellContext ctx) { 
                return wrapped.execute(ctx); 
            }
            
            @Override 
            public ExecutionResult onUpdate(SpellContext ctx) { 
                return wrapped.onUpdate(ctx); 
            }
        }
        
        @Override
        public int getManaCost() {
            return super.getManaCost() + paramInt("radius") / 2 + paramInt("level");
        }
    }

    public static class SilkTouchModifierNode extends ModifierNode {
        public static final String ID = "silk_touch";
        
        public SilkTouchModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) { 
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
    }

    public static class LootingModifierNode extends ModifierNode {
        public static final String ID = "looting";
        
        public LootingModifierNode() { 
            super(ID, SpellRegistry.get(ID)); 
        }
        
        @Override 
        public SpellNode tryModify(SpellNode target, SpellContext ctx) { 
            return target;
        }
        
        @Override
        public ExecutionResult tick(SpellContext ctx) {
            return ExecutionResult.continueTo(0);
        }
    }
}