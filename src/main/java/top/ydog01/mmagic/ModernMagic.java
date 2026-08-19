package top.ydog01.mmagic;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import top.ydog01.mmagic.init.ModAttachments;
import top.ydog01.mmagic.init.ModBlocks;
import top.ydog01.mmagic.init.ModCreativeTabs;
import top.ydog01.mmagic.init.ModEntityTypes;
import top.ydog01.mmagic.init.ModItems;
import top.ydog01.mmagic.init.ModLootModifiers;
import top.ydog01.mmagic.init.ModMenuTypes;
import top.ydog01.mmagic.init.ModSpellNodes;
import top.ydog01.mmagic.init.ModStructures;
import top.ydog01.mmagic.spell.casting.ActiveSpellManager;
import top.ydog01.mmagic.spell.casting.SpellRunner;
import top.ydog01.mmagic.spell.casting.SpellScheduler;

@Mod(ModernMagic.MODID)
public class ModernMagic {
    public static final String MODID = "modern_magic";

    public ModernMagic(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.PIECE_TYPES.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModLootModifiers.GLOBAL_LOOT_MODIFIERS.register(modEventBus);
        ModSpellNodes.register();
        ActiveSpellManager manager = new ActiveSpellManager();
        SpellRunner.setManager(manager);
        SpellScheduler.setManager(manager);
    }
}
