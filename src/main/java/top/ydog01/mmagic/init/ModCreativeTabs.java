package top.ydog01.mmagic.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.ydog01.mmagic.ModernMagic;

import java.util.function.Supplier;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModernMagic.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.modern_magic"))
            .icon(() -> new ItemStack(ModItems.WAND.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.ALTAR.get());
                output.accept(ModItems.WAND.get());
                output.accept(ModItems.MAGIC_CRYSTAL.get());
                output.accept(ModItems.QUEST_BOOK.get());
            })
            .build());

    public static final Supplier<CreativeModeTab> SPELL_NODES = TABS.register("spell_nodes", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.modern_magic.spell_nodes"))
            .icon(() -> new ItemStack(ModItems.SPELL_NODE_EXPLOSION.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.SPELL_NODE_EXPLOSION.get());
                output.accept(ModItems.SPELL_NODE_MAGIC_MISSILE.get());
                output.accept(ModItems.SPELL_NODE_DELAYED_MAGIC_MISSILE.get());
                output.accept(ModItems.SPELL_NODE_TRIGGER_MISSILE.get());
                for (var entry : ModItems.EXTRA_NODES.values()) {
                    output.accept(entry.get());
                }
            })
            .build());

    private ModCreativeTabs() {
    }
}
