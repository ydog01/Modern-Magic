package top.ydog01.mmagic.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredItem;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.spell.NodeParameter;
import top.ydog01.mmagic.spell.SpellNode;
import top.ydog01.mmagic.spell.SpellRegistry;
import top.ydog01.mmagic.spell.node.ControlNodes;
import top.ydog01.mmagic.spell.node.DelayedMagicMissileNode;
import top.ydog01.mmagic.spell.node.EffectNodes;
import top.ydog01.mmagic.spell.node.ExplosionNode;
import top.ydog01.mmagic.spell.node.MagicMissileNode;
import top.ydog01.mmagic.spell.node.ModifierNodes;
import top.ydog01.mmagic.spell.node.StartNode;
import top.ydog01.mmagic.spell.node.TriggerMissileNode;
import top.ydog01.mmagic.spell.node.UtilityNodes;

import java.util.List;
import java.util.function.Supplier;

public final class ModSpellNodes {
    private ModSpellNodes() {
    }

    public static void register() {

        SpellRegistry.register(SpellRegistry.START_ID, 0, 1, 0, 0,
                StartNode::new, () -> new ItemStack(Items.NETHER_STAR));

        missile("magic_missile", 2, 4.0f, 1.5f, ModItems.SPELL_NODE_MAGIC_MISSILE, MagicMissileNode::new,
                "attack", "projectile");

        SpellRegistry.register(id("delayed_magic_missile"), 1, 1, 2, 0,
                DelayedMagicMissileNode::new, icon(ModItems.SPELL_NODE_DELAYED_MAGIC_MISSILE),
                List.of(new NodeParameter("flight_ticks", NodeParameter.Kind.INT, 5f, 200f, 5f, 40f,
                        "param.modern_magic.flight_ticks", 0f)),
                List.of("control", "trigger"));
        SpellRegistry.register(id("trigger_missile"), 1, 1, 2, 0,
                TriggerMissileNode::new, icon(ModItems.SPELL_NODE_TRIGGER_MISSILE),
                List.of(), List.of("control", "trigger"));

        SpellRegistry.register(id("explosion"), 1, 0, 5, 0,
                ExplosionNode::new, icon(ModItems.SPELL_NODE_EXPLOSION),
                List.of(
                        new NodeParameter("radius", NodeParameter.Kind.FLOAT, 1f, 8f, 0.5f, 2f,
                                "param.modern_magic.radius", 2f),
                        new NodeParameter("damage", NodeParameter.Kind.FLOAT, 0f, 40f, 1f, 12f,
                                "param.modern_magic.damage", 1f),
                        new NodeParameter("destroy_terrain", NodeParameter.Kind.BOOL, 0f, 1f, 1f, 1f,
                                "param.modern_magic.destroy_terrain", 2f)),
                List.of("attack", "area"));
        utility("knockback_pulse", 3, extra("knockback_pulse"), UtilityNodes.KnockbackPulseNode::new, "attack", "area");
        utility("pull_pulse", 3, extra("pull_pulse"), UtilityNodes.PullPulseNode::new, "attack", "area");
        utility("freeze_pulse", 4, extra("freeze_pulse"), UtilityNodes.FreezePulseNode::new, "attack", "area");
        utility("area_damage", 4, extra("area_damage"), UtilityNodes.AreaDamageNode::new, "attack", "area");
        utility("fire_nova", 4, extra("fire_nova"), UtilityNodes.FireNovaNode::new, "attack", "area");

        utility("lightning", 10, extra("lightning"), UtilityNodes.LightningNode::new, "attack", "special");

        effect("heal", 3, 6, extra("heal"), EffectNodes.HealNode::new, "support", "heal");

        effect("speed", 3, 120, 1, extra("speed"), EffectNodes.SpeedNode::new, "support", "buff");
        effect("strength", 3, 120, 1, extra("strength"), EffectNodes.StrengthNode::new, "support", "buff");
        effect("invisibility", 4, 120, 0, extra("invisibility"), EffectNodes.InvisibilityNode::new, "support", "buff");
        effect("fire_resist", 3, 160, 0, extra("fire_resist"), EffectNodes.FireResistNode::new, "support", "buff");
        effect("regen", 4, 100, 0, extra("regen"), EffectNodes.RegenNode::new, "support", "buff");
        effect("night_vision", 2, 200, 0, extra("night_vision"), EffectNodes.NightVisionNode::new, "support", "buff");
        effect("jump_boost", 2, 120, 1, extra("jump_boost"), EffectNodes.JumpBoostNode::new, "support", "buff");
        effect("slow_fall", 2, 160, 0, extra("slow_fall"), EffectNodes.SlowFallNode::new, "support", "buff");
        effect("water_breath", 2, 160, 0, extra("water_breath"), EffectNodes.WaterBreathNode::new, "support", "buff");
        effect("absorption", 4, 120, 1, extra("absorption"), EffectNodes.AbsorptionNode::new, "support", "buff");

        utility("teleport", 4, extra("teleport"), UtilityNodes.TeleportNode::new, "support", "mobility");
        utility("launch", 2, extra("launch"), UtilityNodes.LaunchNode::new, "support", "mobility");

        SpellRegistry.register(id("multi_cast"), 1, 2, 1, 0,
                ControlNodes.MultiCastNode::new, icon(extra("multi_cast")),
                List.of(new NodeParameter("outputs", NodeParameter.Kind.INT, 2f, 5f, 1f, 2f,
                        "param.modern_magic.outputs", 1f)),
                List.of("control", "flow"));
        SpellRegistry.register(id("random_cast"), 1, 2, 2, 0,
                ControlNodes.RandomCastNode::new, icon(extra("random_cast")), List.of(), List.of("control", "flow"));
        SpellRegistry.register(id("echo"), 1, 1, 1, 0,
                ControlNodes.EchoNode::new, icon(extra("echo")),
                List.of(new NodeParameter("times", NodeParameter.Kind.INT, 2f, 4f, 1f, 2f,
                        "param.modern_magic.times", 1f)),
                List.of("control", "flow"));
        SpellRegistry.register(id("loop"), 2, 1, 1, 0,
                ControlNodes.LoopNode::new, icon(extra("loop")),
                List.of(new NodeParameter("inputs", NodeParameter.Kind.INT, 2f, 4f, 1f, 2f,
                        "param.modern_magic.inputs", 1f)),
                List.of("control", "flow"));
        SpellRegistry.register(id("condition"), 1, 2, 1, 0,
                ControlNodes.ConditionNode::new, icon(extra("condition")),
                List.of(
                        new NodeParameter("min", NodeParameter.Kind.INT, 1f, 99f, 1f, 1f,
                                "param.modern_magic.min", 0f),
                        new NodeParameter("max", NodeParameter.Kind.INT, 1f, 99f, 1f, 1f,
                                "param.modern_magic.max", 0f)),
                List.of("control", "flow"));
        SpellRegistry.register(id("terminate"), 1, 0, 0, 0,
                ControlNodes.TerminateNode::new, icon(extra("terminate")),
                List.of(), List.of("control", "flow"));
        SpellRegistry.register(id("terminate_all"), 1, 0, 0, 0,
                ControlNodes.GlobalTerminateNode::new, icon(extra("terminate_all")),
                List.of(), List.of("control", "flow"));
        SpellRegistry.register(id("rotate"), 1, 1, 1, 0,
                ControlNodes.RotateNode::new, icon(extra("rotate")),
                List.of(new NodeParameter("angle", NodeParameter.Kind.INT, -180f, 180f, 15f, 90f,
                        "param.modern_magic.angle", 0f)),
                List.of("control", "motion"));
        SpellRegistry.register(id("direction"), 1, 1, 2, 0,
                ControlNodes.DirectionNode::new, icon(extra("direction")),
                List.of(
                        new NodeParameter("yaw", NodeParameter.Kind.INT, -180f, 180f, 15f, 0f,
                                "param.modern_magic.yaw", 0f),
                        new NodeParameter("pitch", NodeParameter.Kind.INT, -180f, 180f, 15f, 0f,
                                "param.modern_magic.pitch", 0f)),
                List.of("control", "motion"));
        SpellRegistry.register(id("speed_return"), 1, 1, 2, 0,
                ControlNodes.SpeedReturnNode::new, icon(extra("speed_return")),
                List.of(), List.of("control", "motion"));
        SpellRegistry.register(id("set_speed"), 1, 1, 2, 0,
                ControlNodes.SetSpeedNode::new, icon(extra("set_speed")),
                List.of(new NodeParameter("speed", NodeParameter.Kind.FLOAT, 0.5f, 3f, 0.25f, 1f,
                        "param.modern_magic.speed", 1f)),
                List.of("control", "motion"));
        SpellRegistry.register(id("decelerate"), 1, 1, 1, 0,
                ControlNodes.DecelerateNode::new, icon(extra("decelerate")),
                List.of(new NodeParameter("mult", NodeParameter.Kind.FLOAT, 0.2f, 1f, 0.1f, 0.5f,
                        "param.modern_magic.mult", 0f)),
                List.of("control", "motion"));
        SpellRegistry.register(id("wait"), 1, 1, 2, 0,
                ControlNodes.WaitNode::new, icon(extra("wait")),
                List.of(new NodeParameter("time", NodeParameter.Kind.INT, 1f, 200f, 5f, 20f,
                        "param.modern_magic.time", 0f)),
                List.of("control", "flow"));
        SpellRegistry.register(id("stabilize"), 1, 0, 3, 0,
                EffectNodes.StabilizeNode::new, icon(extra("stabilize")),
                List.of(new NodeParameter("duration", NodeParameter.Kind.INT, 20f, 1200f, 20f, 200f,
                        "param.modern_magic.duration", 0.05f)),
                List.of("support", "buff"));
        SpellRegistry.register(id("offset_up"), 1, 1, 1, 0,
                ControlNodes.OffsetUpNode::new, icon(extra("offset_up")),
                List.of(new NodeParameter("distance", NodeParameter.Kind.INT, 1f, 10f, 1f, 3f,
                        "param.modern_magic.distance", 0.1f)),
                List.of("control", "motion"));
        SpellRegistry.register(id("offset_forward"), 1, 1, 1, 0,
                ControlNodes.OffsetForwardNode::new, icon(extra("offset_forward")),
                List.of(new NodeParameter("distance", NodeParameter.Kind.INT, 1f, 20f, 1f, 8f,
                        "param.modern_magic.distance", 0.1f)),
                List.of("control", "motion"));

        SpellRegistry.register(id("amplifier"), 1, 1, 2, 0,
                ControlNodes.AmplifierNode::new, icon(extra("amplifier")),
                List.of(new NodeParameter("mult", NodeParameter.Kind.FLOAT, 1f, 3f, 0.25f, 1.5f,
                        "param.modern_magic.mult", 2f)),
                List.of("control", "modifier"));
        SpellRegistry.register(id("accelerator"), 1, 1, 1, 0,
                ControlNodes.AcceleratorNode::new, icon(extra("accelerator")),
                List.of(new NodeParameter("mult", NodeParameter.Kind.FLOAT, 1f, 3f, 0.25f, 1.8f,
                        "param.modern_magic.mult", 1.5f)),
                List.of("control", "motion"));

        modifier("fire", 1, extra("fire"), ModifierNodes.FireModifierNode::new);
        modifier("ice", 1, extra("ice"), ModifierNodes.IceModifierNode::new);
        modifier("poison", 1, extra("poison"), ModifierNodes.PoisonModifierNode::new);
        modifier("wither", 1, extra("wither"), ModifierNodes.WitherModifierNode::new);
        modifier("levitate", 1, extra("levitate"), ModifierNodes.LevitateModifierNode::new);
        modifier("water", 1, extra("water"), ModifierNodes.WaterModifierNode::new);
        modifier("homing", 2, extra("homing"), ModifierNodes.HomingModifierNode::new);
        modifier("heal_mod", 2, extra("heal_mod"), ModifierNodes.HealModifierNode::new,
                List.of(new NodeParameter("amount", NodeParameter.Kind.INT, 2f, 20f, 1f, 4f,
                        "param.modern_magic.amount", 0.5f)));
        modifier("burst", 3, extra("burst"), ModifierNodes.BurstModifierNode::new,
                List.of(new NodeParameter("radius", NodeParameter.Kind.FLOAT, 1f, 5f, 0.5f, 2f,
                        "param.modern_magic.radius", 1f)));
        modifier("bounce", 1, extra("bounce"), ModifierNodes.BounceModifierNode::new,
                List.of(new NodeParameter("times", NodeParameter.Kind.INT, 1f, 5f, 1f, 2f,
                        "param.modern_magic.times", 0.5f)));
        modifier("pierce", 2, extra("pierce"), ModifierNodes.PierceModifierNode::new,
                List.of(new NodeParameter("times", NodeParameter.Kind.INT, 1f, 5f, 1f, 1f,
                        "param.modern_magic.times", 1f)));
        modifier("gravity", 1, extra("gravity"), ModifierNodes.GravityModifierNode::new);
        modifier("pierce_wall", 3, extra("pierce_wall"), ModifierNodes.PierceWallModifierNode::new);
    }

    private static void missile(String name, int mana, float defDamage, float defSpeed,
                                DeferredItem<Item> item, Supplier<SpellNode> factory, String... categories) {
        SpellRegistry.register(id(name), 1, 0, mana, 0, factory, icon(item),
                List.of(
                        new NodeParameter("damage", NodeParameter.Kind.FLOAT, 1f, 15f, 1f, defDamage,
                                "param.modern_magic.damage", 1f),
                        new NodeParameter("speed", NodeParameter.Kind.FLOAT, 0.5f, 3f, 0.1f, defSpeed,
                                "param.modern_magic.speed", 0.5f)),
                List.of(categories));
    }

    private static void modifier(String name, int mana, DeferredItem<Item> item, Supplier<SpellNode> factory) {
        SpellRegistry.register(id(name), 1, 1, mana, 0, factory, icon(item), List.of(),
                List.of("control", "modifier"));
    }

    private static void modifier(String name, int mana, DeferredItem<Item> item, Supplier<SpellNode> factory,
                                 List<NodeParameter> params) {
        SpellRegistry.register(id(name), 1, 1, mana, 0, factory, icon(item), params,
                List.of("control", "modifier"));
    }

    private static void effect(String name, int mana, int defaultDuration, int defaultLevel,
                               DeferredItem<Item> item, Supplier<SpellNode> factory, String... categories) {
        SpellRegistry.register(id(name), 1, 0, mana, 0, factory, icon(item),
                List.of(
                        new NodeParameter("duration", NodeParameter.Kind.INT, 40f, 600f, 40f, defaultDuration,
                                "param.modern_magic.duration", 0.05f),
                        new NodeParameter("level", NodeParameter.Kind.INT, 0f, 3f, 1f, defaultLevel,
                                "param.modern_magic.level", 3f)),
                List.of(categories));
    }

    private static void effect(String name, int mana, int defaultAmount,
                               DeferredItem<Item> item, Supplier<SpellNode> factory, String... categories) {
        SpellRegistry.register(id(name), 1, 0, mana, 0, factory, icon(item),
                List.of(new NodeParameter("amount", NodeParameter.Kind.INT, 2f, 20f, 1f, defaultAmount,
                        "param.modern_magic.amount", 0.5f)),
                List.of(categories));
    }

    private static void utility(String name, int mana, DeferredItem<Item> item, Supplier<SpellNode> factory,
                                String... categories) {
        SpellRegistry.register(id(name), 1, 0, mana, 0, factory, icon(item), List.of(), List.of(categories));
    }

    private static DeferredItem<Item> extra(String name) {
        return ModItems.EXTRA_NODES.get(name);
    }

    private static Supplier<ItemStack> icon(DeferredItem<Item> item) {
        return () -> new ItemStack(item.get());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, path);
    }
}
