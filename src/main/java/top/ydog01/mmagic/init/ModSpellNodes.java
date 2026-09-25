package top.ydog01.mmagic.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.spell.NodeParameter;
import top.ydog01.mmagic.spell.SpellRegistry;
import top.ydog01.mmagic.spell.node_api.SpellNode;
import top.ydog01.mmagic.spell.node_implementations.*;
import java.util.List;
import java.util.function.Supplier;

public final class ModSpellNodes {
    private ModSpellNodes() {}

    public static void register() {
        System.out.println("[ModernMagic] 开始注册法术节点...");

        SpellRegistry.register(SpellRegistry.START_ID, 0, 1, 0, 0,
            StartNode::new, () -> new ItemStack(Items.NETHER_STAR));

        System.out.println("[ModernMagic] 已注册: " + SpellRegistry.ids().size() + " 个节点");

        missile("magic_missile", 2, 4.0f, 1.5f, ModItems.SPELL_NODE_MAGIC_MISSILE, MagicMissileNode::new);

        // Delayed and trigger missiles store their downstream connection and
        // release it later, so they must expose one output port.
        SpellRegistry.register(id("delayed_magic_missile"), 1, 1, 2, 0,
            DelayedMagicMissileNode::new, icon(ModItems.SPELL_NODE_DELAYED_MAGIC_MISSILE),
            List.of(
                new NodeParameter("damage", NodeParameter.Kind.FLOAT, 1f, 15f, 1f, 2.0f, "param.modern_magic.damage", 1f),
                new NodeParameter("speed", NodeParameter.Kind.FLOAT, 0.5f, 3f, 0.1f, 1.2f, "param.modern_magic.speed", 0.5f),
                new NodeParameter("flight_ticks", NodeParameter.Kind.INT, 5f, 200f, 5f, 40f, "param.modern_magic.flight_ticks", 0f)
            ),
            List.of("control", "trigger"));

        SpellRegistry.register(id("trigger_missile"), 1, 1, 2, 0,
            TriggerMissileNode::new, icon(ModItems.SPELL_NODE_TRIGGER_MISSILE),
            List.of(
                new NodeParameter("damage", NodeParameter.Kind.FLOAT, 1f, 15f, 1f, 3.0f, "param.modern_magic.damage", 1f),
                new NodeParameter("speed", NodeParameter.Kind.FLOAT, 0.5f, 3f, 0.1f, 1.5f, "param.modern_magic.speed", 0.5f)
            ),
            List.of("control", "trigger"));

        SpellRegistry.register(id("explosion"), 1, 0, 5, 0,
            ExplosionNode::new, icon(ModItems.SPELL_NODE_EXPLOSION),
            List.of(
                new NodeParameter("radius", NodeParameter.Kind.FLOAT, 1f, 8f, 0.5f, 2f, "param.modern_magic.radius", 2f),
                new NodeParameter("damage", NodeParameter.Kind.FLOAT, 0f, 40f, 1f, 12f, "param.modern_magic.damage", 1f),
                new NodeParameter("destroy_terrain", NodeParameter.Kind.BOOL, 0f, 1f, 1f, 1f, "param.modern_magic.destroy_terrain", 2f)
            ),
            List.of("attack", "area"));

        SpellRegistry.register(id("lightning"), 1, 0, 10, 0,
            UtilityNodes.LightningNode::new, icon(ModItems.EXTRA_NODES.get("lightning")),
            List.of(), List.of("attack", "special"));

        SpellRegistry.register(id("teleport"), 1, 0, 4, 0,
            UtilityNodes.TeleportNode::new, icon(ModItems.EXTRA_NODES.get("teleport")),
            List.of(), List.of("support", "mobility"));

        SpellRegistry.register(id("launch"), 1, 0, 2, 0,
            UtilityNodes.LaunchNode::new, icon(ModItems.EXTRA_NODES.get("launch")),
            List.of(), List.of("support", "mobility"));

        SpellRegistry.register(id("knockback_pulse"), 1, 0, 3, 0,
            UtilityNodes.KnockbackPulseNode::new, icon(ModItems.EXTRA_NODES.get("knockback_pulse")),
            List.of(), List.of("attack", "area"));

        SpellRegistry.register(id("heal"), 1, 0, 3, 0,
            EffectNodes.HealNode::new, icon(ModItems.EXTRA_NODES.get("heal")),
            List.of(new NodeParameter("amount", NodeParameter.Kind.INT, 2f, 20f, 1f, 6f, "param.modern_magic.amount", 0.5f)),
            List.of("support", "heal"));

        SpellRegistry.register(id("speed"), 1, 0, 3, 0,
            EffectNodes.SpeedNode::new, icon(ModItems.EXTRA_NODES.get("speed")),
            List.of(
                new NodeParameter("duration", NodeParameter.Kind.INT, 40f, 600f, 40f, 120f, "param.modern_magic.duration", 0.05f),
                new NodeParameter("level", NodeParameter.Kind.INT, 0f, 3f, 1f, 1f, "param.modern_magic.level", 3f)
            ),
            List.of("support", "buff"));

        SpellRegistry.register(id("strength"), 1, 0, 3, 0,
            EffectNodes.StrengthNode::new, icon(ModItems.EXTRA_NODES.get("strength")),
            List.of(
                new NodeParameter("duration", NodeParameter.Kind.INT, 40f, 600f, 40f, 120f, "param.modern_magic.duration", 0.05f),
                new NodeParameter("level", NodeParameter.Kind.INT, 0f, 3f, 1f, 1f, "param.modern_magic.level", 3f)
            ),
            List.of("support", "buff"));

        SpellRegistry.register(id("absorption"), 1, 0, 4, 0,
            EffectNodes.AbsorptionNode::new, icon(ModItems.EXTRA_NODES.get("absorption")),
            List.of(
                new NodeParameter("duration", NodeParameter.Kind.INT, 40f, 600f, 40f, 120f, "param.modern_magic.duration", 0.05f),
                new NodeParameter("level", NodeParameter.Kind.INT, 0f, 3f, 1f, 1f, "param.modern_magic.level", 3f)
            ),
            List.of("support", "buff"));


        SpellRegistry.register(id("multi_cast"), 1, 5, 1, 0,
            ControlNodes.MultiCastNode::new, icon(ModItems.EXTRA_NODES.get("multi_cast")),
            List.of(new NodeParameter("outputs", NodeParameter.Kind.INT, 2f, 5f, 1f, 2f, "param.modern_magic.outputs", 1f)),
            List.of("control", "flow"));

        SpellRegistry.register(id("random_cast"), 1, 2, 2, 0,
            ControlNodes.RandomCastNode::new, icon(ModItems.EXTRA_NODES.get("random_cast")),
            List.of(), List.of("control", "flow"));

        SpellRegistry.register(id("echo"), 1, 1, 1, 0,
            ControlNodes.EchoNode::new, icon(ModItems.EXTRA_NODES.get("echo")),
            List.of(new NodeParameter("times", NodeParameter.Kind.INT, 2f, 4f, 1f, 2f, "param.modern_magic.times", 1f)),
            List.of("control", "flow"));

        SpellRegistry.register(id("terminate"), 1, 0, 0, 0,
            ControlNodes.TerminateNode::new, icon(ModItems.EXTRA_NODES.get("terminate")),
            List.of(), List.of("control", "flow"));

        SpellRegistry.register(id("wait"), 1, 1, 2, 0,
            ControlNodes.WaitNode::new, icon(ModItems.EXTRA_NODES.get("wait")),
            List.of(new NodeParameter("time", NodeParameter.Kind.INT, 1f, 200f, 5f, 20f, "param.modern_magic.time", 0f)),
            List.of("control", "flow"));

        SpellRegistry.register(id("condition"), 1, 2, 1, 0,
            ControlNodes.ConditionNode::new, icon(ModItems.EXTRA_NODES.get("condition")),
            List.of(
                new NodeParameter("min", NodeParameter.Kind.INT, 1f, 99f, 1f, 1f, "param.modern_magic.min", 0f),
                new NodeParameter("max", NodeParameter.Kind.INT, 1f, 99f, 1f, 1f, "param.modern_magic.max", 0f)
            ),
            List.of("control", "flow"));

        SpellRegistry.register(id("amplifier"), 1, 1, 2, 0,
            MotionNodes.AmplifierNode::new, icon(ModItems.EXTRA_NODES.get("amplifier")),
            List.of(new NodeParameter("mult", NodeParameter.Kind.FLOAT, 1f, 3f, 0.25f, 1.5f, "param.modern_magic.mult", 2f)),
            List.of("control", "modifier"));

        SpellRegistry.register(id("accelerator"), 1, 1, 1, 0,
            MotionNodes.AcceleratorNode::new, icon(ModItems.EXTRA_NODES.get("accelerator")),
            List.of(new NodeParameter("mult", NodeParameter.Kind.FLOAT, 1f, 3f, 0.25f, 1.8f, "param.modern_magic.mult", 1.5f)),
            List.of("control", "motion"));

        SpellRegistry.register(id("rotate"), 1, 1, 1, 0,
            MotionNodes.RotateNode::new, icon(ModItems.EXTRA_NODES.get("rotate")),
            List.of(new NodeParameter("angle", NodeParameter.Kind.INT, -180f, 180f, 15f, 90f, "param.modern_magic.angle", 0f)),
            List.of("control", "motion"));

        SpellRegistry.register(id("direction"), 1, 1, 2, 0,
            MotionNodes.DirectionNode::new, icon(ModItems.EXTRA_NODES.get("direction")),
            List.of(
                new NodeParameter("yaw", NodeParameter.Kind.INT, -180f, 180f, 15f, 0f, "param.modern_magic.yaw", 0f),
                new NodeParameter("pitch", NodeParameter.Kind.INT, -180f, 180f, 15f, 0f, "param.modern_magic.pitch", 0f)
            ),
            List.of("control", "motion"));

        SpellRegistry.register(id("set_speed"), 1, 1, 2, 0,
            MotionNodes.SetSpeedNode::new, icon(ModItems.EXTRA_NODES.get("set_speed")),
            List.of(new NodeParameter("speed", NodeParameter.Kind.FLOAT, 0.5f, 3f, 0.25f, 1f, "param.modern_magic.speed", 1f)),
            List.of("control", "motion"));

        SpellRegistry.register(id("decelerate"), 1, 1, 1, 0,
            MotionNodes.DecelerateNode::new, icon(ModItems.EXTRA_NODES.get("decelerate")),
            List.of(new NodeParameter("mult", NodeParameter.Kind.FLOAT, 0.2f, 1f, 0.1f, 0.5f, "param.modern_magic.mult", 0f)),
            List.of("control", "motion"));

        SpellRegistry.register(id("offset_up"), 1, 1, 1, 0,
            MotionNodes.OffsetUpNode::new, icon(ModItems.EXTRA_NODES.get("offset_up")),
            List.of(new NodeParameter("distance", NodeParameter.Kind.INT, 1f, 10f, 1f, 3f, "param.modern_magic.distance", 0.1f)),
            List.of("control", "motion"));

        SpellRegistry.register(id("offset_forward"), 1, 1, 1, 0,
            MotionNodes.OffsetForwardNode::new, icon(ModItems.EXTRA_NODES.get("offset_forward")),
            List.of(new NodeParameter("distance", NodeParameter.Kind.INT, 1f, 20f, 1f, 8f, "param.modern_magic.distance", 0.1f)),
            List.of("control", "motion"));

        // 修饰节点
        modifier("fire", 1, ModItems.EXTRA_NODES.get("fire"), ModifierNodes.FireModifierNode::new);
        modifier("ice", 1, ModItems.EXTRA_NODES.get("ice"), ModifierNodes.IceModifierNode::new);
        modifier("poison", 1, ModItems.EXTRA_NODES.get("poison"), ModifierNodes.PoisonModifierNode::new);
        modifier("wither", 1, ModItems.EXTRA_NODES.get("wither"), ModifierNodes.WitherModifierNode::new);
        modifier("levitate", 1, ModItems.EXTRA_NODES.get("levitate"), ModifierNodes.LevitateModifierNode::new);
        modifier("water", 1, ModItems.EXTRA_NODES.get("water"), ModifierNodes.WaterModifierNode::new);
        modifier("homing", 2, ModItems.EXTRA_NODES.get("homing"), ModifierNodes.HomingModifierNode::new);
        modifier("burst", 3, ModItems.EXTRA_NODES.get("burst"), ModifierNodes.BurstModifierNode::new,
            List.of(new NodeParameter("radius", NodeParameter.Kind.FLOAT, 1f, 5f, 0.5f, 2f, "param.modern_magic.radius", 1f)));
        modifier("bounce", 1, ModItems.EXTRA_NODES.get("bounce"), ModifierNodes.BounceModifierNode::new,
            List.of(new NodeParameter("times", NodeParameter.Kind.INT, 1f, 5f, 1f, 2f, "param.modern_magic.times", 0.5f)));
        modifier("pierce", 2, ModItems.EXTRA_NODES.get("pierce"), ModifierNodes.PierceModifierNode::new,
            List.of(new NodeParameter("times", NodeParameter.Kind.INT, 1f, 5f, 1f, 1f, "param.modern_magic.times", 1f)));
        modifier("gravity", 1, ModItems.EXTRA_NODES.get("gravity"), ModifierNodes.GravityModifierNode::new);
        modifier("pierce_wall", 3, ModItems.EXTRA_NODES.get("pierce_wall"), ModifierNodes.PierceWallModifierNode::new);
        modifier("pickup", 2, ModItems.EXTRA_NODES.get("pickup"), ModifierNodes.PickupModifierNode::new,
            List.of(new NodeParameter("radius", NodeParameter.Kind.FLOAT, 1f, 16f, 0.5f, 4f, "param.modern_magic.pickup_radius", 0.5f)));
        modifier("silk_touch", 2, ModItems.EXTRA_NODES.get("silk_touch"), ModifierNodes.SilkTouchModifierNode::new);
        modifier("looting", 2, ModItems.EXTRA_NODES.get("looting"), ModifierNodes.LootingModifierNode::new,
            List.of(new NodeParameter("level", NodeParameter.Kind.INT, 0f, 3f, 1f, 1f, "param.modern_magic.level", 1f)));

        // 挖掘节点
        modifier("dig", 4, ModItems.EXTRA_NODES.get("dig"), ModifierNodes.DigModifierNode::new,
            List.of(
                new NodeParameter("radius", NodeParameter.Kind.FLOAT, 1f, 5f, 0.5f, 2f, "param.modern_magic.radius", 0f),
                new NodeParameter("level", NodeParameter.Kind.INT, 0f, 4f, 1f, 1f, "param.modern_magic.level", 1f),
                new NodeParameter("drop", NodeParameter.Kind.BOOL, 0f, 1f, 1f, 1f, "param.modern_magic.drop", 0f)
            ));
        modifier("chain_dig", 4, ModItems.EXTRA_NODES.get("chain_dig"), ModifierNodes.ChainDigModifierNode::new,
            List.of(
                new NodeParameter("radius", NodeParameter.Kind.INT, 1f, 16f, 1f, 8f, "param.modern_magic.chain_radius", 0f),
                new NodeParameter("level", NodeParameter.Kind.INT, 0f, 4f, 1f, 1f, "param.modern_magic.level", 1f),
                new NodeParameter("drop", NodeParameter.Kind.BOOL, 0f, 1f, 1f, 1f, "param.modern_magic.drop", 0f)
            ));

        // 增益
        modifier("heal_mod", 2, ModItems.EXTRA_NODES.get("heal_mod"), ModifierNodes.HealModifierNode::new,
            List.of(new NodeParameter("amount", NodeParameter.Kind.INT, 2f, 20f, 1f, 4f, "param.modern_magic.amount", 0.5f)));

        System.out.println("[ModernMagic] 注册完成，共 " + SpellRegistry.ids().size() + " 个节点");
    }

    private static void missile(String name, int mana, float defDamage, float defSpeed,
                                DeferredItem<Item> item, Supplier<SpellNode> factory) {
        SpellRegistry.register(id(name), 1, 0, mana, 0, factory, icon(item),
            List.of(
                new NodeParameter("damage", NodeParameter.Kind.FLOAT, 1f, 15f, 1f, defDamage, "param.modern_magic.damage", 1f),
                new NodeParameter("speed", NodeParameter.Kind.FLOAT, 0.5f, 3f, 0.1f, defSpeed, "param.modern_magic.speed", 0.5f)
            ),
            List.of("attack", "projectile"));
    }

    private static void modifier(String name, int mana, DeferredItem<Item> item, Supplier<SpellNode> factory) {
        SpellRegistry.register(id(name), 1, 1, mana, 0, factory, icon(item), List.of(), List.of("control", "modifier"));
    }

    private static void modifier(String name, int mana, DeferredItem<Item> item,
                                 Supplier<SpellNode> factory, List<NodeParameter> params) {
        SpellRegistry.register(id(name), 1, 1, mana, 0, factory, icon(item), params, List.of("control", "modifier"));
    }

    private static Supplier<ItemStack> icon(DeferredItem<Item> item) {
        return () -> new ItemStack(item.get());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, path);
    }
}
