package top.ydog01.mmagic.init;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.block.AltarBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModernMagic.MODID);

    public static final DeferredBlock<Block> ALTAR = BLOCKS.register("altar",
            () -> new AltarBlock(Block.Properties.of().strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()));

    private ModBlocks() {
    }
}
