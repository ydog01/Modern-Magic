package top.ydog01.mmagic.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.item.MagicCrystalItem;
import top.ydog01.mmagic.item.QuestBookItem;
import top.ydog01.mmagic.item.SpellNodeItem;
import top.ydog01.mmagic.item.WandItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModernMagic.MODID);

    public static final DeferredItem<Item> ALTAR = ITEMS.register("altar",
            () -> new BlockItem(ModBlocks.ALTAR.get(), new Item.Properties()));

    public static final DeferredItem<Item> WAND = ITEMS.register("wand",
            () -> new WandItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> MAGIC_CRYSTAL = ITEMS.register("magic_crystal",
            () -> new MagicCrystalItem(new Item.Properties().stacksTo(99)));

    public static final DeferredItem<Item> QUEST_BOOK = ITEMS.register("quest_book",
            () -> new QuestBookItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SPELL_NODE_EXPLOSION = nodeItem("explosion");
    public static final DeferredItem<Item> SPELL_NODE_MAGIC_MISSILE = nodeItem("magic_missile");
    public static final DeferredItem<Item> SPELL_NODE_DELAYED_MAGIC_MISSILE = nodeItem("delayed_magic_missile");
    public static final DeferredItem<Item> SPELL_NODE_TRIGGER_MISSILE = nodeItem("trigger_missile");

    public static final List<String> EXTRA_NODE_IDS = List.of(
            "fire", "ice", "poison", "wither", "levitate",
            "water", "heal_mod", "homing", "burst",
            "bounce", "pierce", "gravity", "pierce_wall",
            "heal", "speed", "strength", "invisibility", "fire_resist",
            "regen", "night_vision", "jump_boost", "slow_fall", "water_breath", "absorption",
            "lightning", "teleport", "knockback_pulse", "pull_pulse", "freeze_pulse",
            "area_damage", "fire_nova", "launch", "multi_cast", "random_cast",
            "loop", "condition", "terminate", "terminate_all", "rotate", "direction", "speed_return", "set_speed", "decelerate", "wait",
            "stabilize", "amplifier", "accelerator", "echo", "offset_up", "offset_forward"
    );

    public static final Map<String, DeferredItem<Item>> EXTRA_NODES = new LinkedHashMap<>();

    static {
        for (String id : EXTRA_NODE_IDS) {
            EXTRA_NODES.put(id, nodeItem(id));
        }
    }

    private static DeferredItem<Item> nodeItem(String id) {
        return ITEMS.register("spell_node_" + id,
                () -> new SpellNodeItem(new Item.Properties().stacksTo(99)));
    }

    private ModItems() {
    }
}
