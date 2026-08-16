package top.ydog01.mmagic.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.ydog01.mmagic.item.WandItem;
import top.ydog01.mmagic.network.ModNetwork;
import top.ydog01.mmagic.util.WandData;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpellRunner {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final ConcurrentHashMap<UUID, ActiveSpell> ACTIVE_SPELLS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, LivingEntity> CASTERS = new ConcurrentHashMap<>();
    private static final ActiveSpell STOPPED = new ActiveSpell(null, -1.0, 0, 0, 0.0, null);

    public static final class ActiveSpell {
        final UUID ownerId;
        final int maxMana;
        final double regen;
        final SpellGraph graph;
        double mana;
        long tick;
        long lastPosCheck;
        boolean terminated;

        ActiveSpell(UUID ownerId, double mana, long tick, int maxMana, double regen, SpellGraph graph) {
            this.ownerId = ownerId;
            this.mana = mana;
            this.tick = tick;
            this.maxMana = maxMana;
            this.regen = regen;
            this.graph = graph;
        }
    }

    private SpellRunner() {
    }

    public static void cast(Player player, ItemStack wand) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WandData.isReady(wand, level)) {
            return;
        }
        SpellGraph graph = WandData.getGraph(wand);
        if (graph.start() == null) {
            return;
        }
        WandData.markCast(wand, level);
        trackWand(wand, player, graph);

        if (player instanceof ServerPlayer sp) {
            ModNetwork.sendManaSync(sp, wand);
        }
        SpellContext ctx = new SpellContext(level, player, wand, graph);
        activate(ctx, graph.start(), ctx.origin(), ctx.direction(), 1.0f, 1.0f, new SpellModifiers());
    }

    public static void trackWand(ItemStack wand, LivingEntity caster) {
        trackWand(wand, caster, WandData.getGraph(wand));
    }

    public static void trackWand(ItemStack wand, LivingEntity caster, SpellGraph graph) {
        UUID id = WandData.getWandId(wand);
        net.minecraft.world.level.Level level = caster.level();
        double mana = WandData.getMana(wand, level);
        ACTIVE_SPELLS.put(id, new ActiveSpell(caster.getUUID(), mana, level.getGameTime(),
                WandData.getMaxMana(wand), WandData.getRegen(wand), graph));
        CASTERS.put(caster.getUUID(), caster);
    }

    public static boolean hasActiveSpell(UUID wandId) {
        return ACTIVE_SPELLS.get(wandId) != null;
    }

    public static boolean isCastTerminated(UUID wandId) {
        ActiveSpell spell = ACTIVE_SPELLS.get(wandId);
        return spell != null && spell != STOPPED && spell.terminated;
    }

    public static void markCastTerminated(ItemStack wand) {
        UUID id = WandData.getWandId(wand);
        ActiveSpell spell = ACTIVE_SPELLS.get(id);
        if (spell != null && spell != STOPPED) {
            spell.terminated = true;
        }
    }

    public static boolean consumeActiveMana(LivingEntity owner, UUID wandId, double cost) {
        ActiveSpell spell = ACTIVE_SPELLS.get(wandId);
        if (spell == null) {
            return true;
        }
        if (spell == STOPPED) {
            return false;
        }
        long now = owner.level().getGameTime();
        if (spell.lastPosCheck != now) {
            spell.lastPosCheck = now;
            if (findWandById(owner, wandId).isEmpty()) {
                LOGGER.warn("spell stopped: wand {} left the player's possession", wandId);
                ACTIVE_SPELLS.put(wandId, STOPPED);
                return false;
            }
        }
        double current = Math.min(spell.maxMana, spell.mana + spell.regen * (now - spell.tick) / 20.0);
        if (current < cost) {
            LOGGER.warn("spell stopped: wand {} out of mana ({} < {})", wandId, current, cost);
            return false;
        }
        spell.mana = current - cost;
        spell.tick = now;
        flushWand(owner, wandId, spell.mana, now);
        return true;
    }

    public static void flushWand(LivingEntity owner, UUID wandId, double mana, long now) {
        ItemStack found = findWandById(owner, wandId);
        if (found.isEmpty()) {
            return;
        }
        WandData.updateTag(found, t -> {
            t.putDouble(WandData.KEY_MANA, mana);
            t.putLong(WandData.KEY_LAST_UPDATE, now);
        });
        if (owner instanceof ServerPlayer sp) {
            ModNetwork.sendWandSlotSync(sp, found);
        }
    }

    public static void activate(SpellContext ctx, SpellNode node, Vec3 at, Vec3 vel, float damageMult, float speedMult,
                                SpellModifiers mods) {
        SpellScheduler.schedule(node.delayTicks(), () -> execute(ctx, node, at, vel, damageMult, speedMult, mods));
    }

    public static void execute(SpellContext ctx, SpellNode node, Vec3 at, Vec3 vel, float damageMult, float speedMult,
                               SpellModifiers mods) {
        if (ctx.terminated()) {
            return;
        }
        if (node.manaCost() > 0 && !ctx.consumeMana(node.manaCost())) {
            return;
        }
        if (vel == null) {
            vel = ctx.direction();
        }
        List<Integer> outputs = node.execute(ctx, at, vel, damageMult, speedMult, mods);
        if (outputs == null) {
            return;
        }
        Vec3 outAt = node.outputPosition(at);
        float outDm = node.outputDamageMult(damageMult);
        float outSm = node.outputSpeedMult(speedMult);
        SpellModifiers outMods = node.outputModifiers(mods);
        Vec3 outVel = node.keepVelocity() ? node.outputVelocity(vel) : node.outputVelocity(null);
        for (int out : outputs) {
            emit(ctx, node, out, outAt, outVel, outDm, outSm, outMods);
        }
    }

    public static void emit(SpellContext ctx, SpellNode node, int outPort, Vec3 at, Vec3 vel, float damageMult,
                            float speedMult, SpellModifiers mods) {
        if (outPort < 0 || outPort >= node.outputCount()) {
            return;
        }
        for (SpellNode.Connection c : node.outputs(outPort)) {
            SpellNode target = ctx.graph().node(c.targetId);
            if (target != null) {
                activate(ctx, target, at, vel, damageMult, speedMult, mods);
            }
        }
    }

    public static void continueFrom(ServerLevel level, UUID casterId, UUID wandId, List<SpellNode.Connection> connections,
                                    Vec3 at, Vec3 vel, float damageMult, float speedMult, SpellModifiers mods) {
        LivingEntity living = CASTERS.get(casterId);
        if (living == null && level.getEntity(casterId) instanceof LivingEntity l) {
            living = l;
        }
        if (living == null) {
            return;
        }
        ActiveSpell spell = ACTIVE_SPELLS.get(wandId);
        if (spell == STOPPED) {
            return;
        }
        ItemStack wand = findWandById(living, wandId);
        if (wand.isEmpty()) {
            if (spell != null) {
                LOGGER.warn("spell stopped: wand {} left the player's possession", wandId);
                ACTIVE_SPELLS.put(wandId, STOPPED);
            }
            return;
        }
        SpellGraph graph = spell != null ? spell.graph : WandData.getGraph(wand);
        SpellContext ctx = new SpellContext(level, living, wand, graph);
        for (SpellNode.Connection c : connections) {
            SpellNode target = graph.node(c.targetId);
            if (target != null) {
                activate(ctx, target, at, vel, damageMult, speedMult, mods);
            }
        }
    }

    public static ItemStack findWand(LivingEntity living) {
        ItemStack main = living.getMainHandItem();
        if (main.getItem() instanceof WandItem) {
            return main;
        }
        ItemStack off = living.getOffhandItem();
        if (off.getItem() instanceof WandItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack findWandById(LivingEntity living, UUID wandId) {
        if (living.getMainHandItem().getItem() instanceof WandItem
                && WandData.getWandId(living.getMainHandItem()).equals(wandId)) {
            return living.getMainHandItem();
        }
        if (living.getOffhandItem().getItem() instanceof WandItem
                && WandData.getWandId(living.getOffhandItem()).equals(wandId)) {
            return living.getOffhandItem();
        }
        if (living instanceof Player player) {
            ItemStack carried = player.containerMenu.getCarried();
            if (carried.getItem() instanceof WandItem && WandData.getWandId(carried).equals(wandId)) {
                return carried;
            }
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof WandItem && WandData.getWandId(stack).equals(wandId)) {
                    return stack;
                }
            }
            if (player.containerMenu instanceof net.minecraft.world.inventory.InventoryMenu menu) {
                for (net.minecraft.world.inventory.Slot slot : menu.slots) {
                    ItemStack s = slot.getItem();
                    if (s.getItem() instanceof WandItem && WandData.getWandId(s).equals(wandId)) {
                        return s;
                    }
                }
            }
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = living.getItemBySlot(slot);
                if (stack.getItem() instanceof WandItem && WandData.getWandId(stack).equals(wandId)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @EventBusSubscriber
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() != null) {
                UUID id = event.getEntity().getUUID();
                ACTIVE_SPELLS.entrySet().removeIf(e -> {
                    ActiveSpell s = e.getValue();
                    return s != null && id.equals(s.ownerId);
                });
            }
        }
    }
}
