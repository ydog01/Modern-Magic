package top.ydog01.mmagic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.init.ModItems;
import top.ydog01.mmagic.spell.SpellGraph;
import top.ydog01.mmagic.spell.SpellNode;
import top.ydog01.mmagic.spell.SpellNodeType;
import top.ydog01.mmagic.spell.SpellRegistry;
import top.ydog01.mmagic.spell.SpellRunner;
import top.ydog01.mmagic.util.WandData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WizardEntity extends Skeleton {
    private static final double LEGENDARY_CHANCE = 0.15;

    private final ServerBossEvent bossEvent;
    private final List<ItemStack> wands = new ArrayList<>();
    private final List<SkillCategory> wandCategory = new ArrayList<>();
    private final List<Boolean> wandHoming = new ArrayList<>();
    private final List<Boolean> wandKnockback = new ArrayList<>();
    private boolean legendary;
    private int castCooldown;
    private int activeWand = -1;

    public WizardEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(
                Component.translatable("entity.modern_magic.wizard"),
                BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public boolean isLegendary() {
        return legendary;
    }

    private void applyLegendary() {
        this.legendary = true;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
        this.setHealth(400.0f);
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.bossEvent.setName(Component.translatable("entity.modern_magic.wizard.legendary"));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        SpawnGroupData spawnGroupData) {
        if (level.getRandom().nextFloat() < LEGENDARY_CHANCE) {
            applyLegendary();
        }
        setupWands(level.getRandom());
        this.setPersistenceRequired();
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
    }

    private enum SkillCategory {
        ATTACK, BUFF, MOBILITY
    }

    private void setupWands(RandomSource random) {
        wands.clear();
        wandCategory.clear();
        wandHoming.clear();
        wandKnockback.clear();
        activeWand = -1;
        int count = legendary ? 4 + random.nextInt(3) : 1 + random.nextInt(2);
        List<SkillTemplate> pool = new ArrayList<>();
        pool.add(new SkillTemplate(SkillCategory.ATTACK, "§7飞弹法杖", types("magic_missile")));
        pool.add(new SkillTemplate(SkillCategory.ATTACK, "§c火焰法杖", types("fire", "magic_missile")));
        pool.add(new SkillTemplate(SkillCategory.ATTACK, "§9追踪法杖", types("homing", "magic_missile")));
        pool.add(new SkillTemplate(SkillCategory.ATTACK, "§6冲击法杖", types("knockback_pulse")));
        pool.add(new SkillTemplate(SkillCategory.BUFF, "§b疾风法杖", types("speed")));
        pool.add(new SkillTemplate(SkillCategory.BUFF, "§4力量法杖", types("strength")));
        pool.add(new SkillTemplate(SkillCategory.BUFF, "§a护盾法杖", types("absorption")));
        pool.add(new SkillTemplate(SkillCategory.MOBILITY, "§b跃迁法杖", types("launch")));
        pool.add(new SkillTemplate(SkillCategory.MOBILITY, "§d闪现法杖", List.of()));
        if (legendary) {
            pool.add(new SkillTemplate(SkillCategory.ATTACK, "§4爆裂法杖", types("offset_forward", "explosion")));
            pool.add(new SkillTemplate(SkillCategory.ATTACK, "§e雷霆法杖", types("offset_forward", "lightning")));
            pool.add(new SkillTemplate(SkillCategory.BUFF, "§a治疗法杖", types("heal")));
        }
        while (wands.size() < count && !pool.isEmpty()) {
            SkillTemplate template = pool.remove(random.nextInt(pool.size()));
            ItemStack wand = new ItemStack(ModItems.WAND.get());
            WandData.setAttributes(wand,
                    legendary ? 250 : 60,
                    legendary ? 12.0 : 4.0,
                    legendary ? 15 : 30);
            WandData.setName(wand, template.name);
            WandData.setGraph(wand, buildGraph(template.chain));
            wands.add(wand);
            wandCategory.add(template.category);
            SpellNodeType homing = SpellRegistry.get("homing");
            SpellNodeType knockback = SpellRegistry.get("knockback_pulse");
            wandHoming.add(homing != null && template.chain.contains(homing));
            wandKnockback.add(knockback != null && template.chain.contains(knockback));
        }
        if (!wands.isEmpty()) {
            activeWand = 0;
            this.setItemSlot(EquipmentSlot.MAINHAND, wands.get(0));
        }
    }

    private static List<SpellNodeType> types(String... ids) {
        List<SpellNodeType> list = new ArrayList<>();
        for (String id : ids) {
            SpellNodeType type = SpellRegistry.get(id);
            if (type != null) {
                list.add(type);
            }
        }
        return list;
    }

    private static SpellGraph buildGraph(List<SpellNodeType> chain) {
        SpellGraph graph = SpellGraph.createDefault();
        SpellNode prev = graph.start();
        float x = 0;
        for (SpellNodeType type : chain) {
            SpellNode node = graph.addNode(type, UUID.randomUUID(), x, 0);
            graph.connect(prev.getUuid(), 0, node.getUuid(), 0);
            prev = node;
            x += 120;
        }
        return graph;
    }

    private record SkillTemplate(SkillCategory category, String name, List<SpellNodeType> chain) {
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CastSpellGoal());
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    private class CastSpellGoal extends Goal {
        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (castCooldown > 0) {
                castCooldown--;
                return;
            }
            double distSqr = distanceToSqr(target);
            if (distSqr > 40.0 * 40.0) {
                return;
            }
            SkillCategory want = chooseCategory(distSqr);
            int index = selectWand(want, target, distSqr);
            if (index < 0) {
                castCooldown = 5;
                return;
            }
            if (index != activeWand) {
                activeWand = index;
                setItemSlot(EquipmentSlot.MAINHAND, wands.get(index));
            }
            castCurrent(target);
            castCooldown = legendary ? 3 + random.nextInt(4) : 5 + random.nextInt(5);
        }
    }

    private SkillCategory chooseCategory(double distSqr) {
        if (distSqr < 16.0 && anyReady(SkillCategory.MOBILITY)) {
            return SkillCategory.MOBILITY;
        }
        if (getHealth() < getMaxHealth() * 0.6 && anyReady(SkillCategory.BUFF)) {
            return SkillCategory.BUFF;
        }
        return SkillCategory.ATTACK;
    }

    private boolean anyReady(SkillCategory category) {
        for (int i = 0; i < wands.size(); i++) {
            if (wandCategory.get(i) == category && readyAt(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean readyAt(int index) {
        if (index < 0 || index >= wands.size()) {
            return false;
        }
        ItemStack wand = wands.get(index);
        return WandData.isReady(wand, level()) && WandData.getMana(wand, level()) >= 1.0;
    }

    private int selectWand(SkillCategory want, LivingEntity target, double distSqr) {
        boolean los = hasLineOfSight(target);
        boolean close = distSqr < 16.0;
        int best = -1;
        int bestScore = 0;
        for (int i = 0; i < wands.size(); i++) {
            if (!readyAt(i)) {
                continue;
            }
            int score = 0;
            if (wandCategory.get(i) == want) {
                score += 10;
            }
            if (want == SkillCategory.ATTACK) {
                if (!los && wandHoming.get(i)) {
                    score += 4;
                }
                if (close && wandKnockback.get(i)) {
                    score += 3;
                }
            }
            if (i == activeWand) {
                score += 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private void castCurrent(LivingEntity target) {
        ItemStack wand = getMainHandItem();
        if (!WandData.isReady(wand, level())) {
            return;
        }
        WandData.markCast(wand, level());
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        SpellGraph graph = WandData.getGraph(wand);
        if (graph.nodes().size() <= 1) {
            teleportAway(target);
            return;
        }
        SpellRunner.cast(this, wand);
        swing(InteractionHand.MAIN_HAND);
    }

    private void teleportAway(LivingEntity target) {
        Vec3 away = new Vec3(getX() - target.getX(), 0.0, getZ() - target.getZ());
        if (away.lengthSqr() < 0.01) {
            away = new Vec3(getLookAngle().x, 0.0, getLookAngle().z);
        }
        away = away.normalize();
        for (double d : new double[]{8.0, 5.0, 3.0}) {
            double tx = getX() + away.x * d;
            double tz = getZ() + away.z * d;
            int ty = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(tx), Mth.floor(tz)) + 1;
            BlockPos feet = BlockPos.containing(tx, ty, tz);
            if (level().getBlockState(feet).isAir() && level().getBlockState(feet.above()).isAir()) {
                teleportTo(tx, ty + 0.5, tz);
                return;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            this.bossEvent.setVisible(!this.isDeadOrDying());
            for (net.minecraft.server.level.ServerPlayer p : serverLevel.players()) {
                if (p.distanceToSqr(this) < 64.0 * 64.0) {
                    this.bossEvent.addPlayer(p);
                } else {
                    this.bossEvent.removePlayer(p);
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        RandomSource random = level.random;
        List<net.minecraft.world.item.Item> nodeItems = new ArrayList<>();
        for (net.minecraft.resources.ResourceLocation id : SpellRegistry.ids()) {
            if (id.equals(SpellRegistry.START_ID)) {
                continue;
            }
            SpellNodeType type = SpellRegistry.get(id);
            if (type != null) {
                nodeItems.add(type.icon().getItem());
            }
        }
        int nodeCount = legendary ? 4 + random.nextInt(5) : 2 + random.nextInt(3);
        for (int i = 0; i < nodeCount && !nodeItems.isEmpty(); i++) {
            ItemStack drop = new ItemStack(nodeItems.get(random.nextInt(nodeItems.size())), 1 + random.nextInt(2));
            spawnAtLocation(drop);
        }
        float wandChance = legendary ? 0.25f : 0.08f;
        for (ItemStack wand : wands) {
            if (random.nextFloat() < wandChance) {
                spawnAtLocation(wand.copy());
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Legendary", this.legendary);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Legendary")) {
            applyLegendary();
        }
    }
}