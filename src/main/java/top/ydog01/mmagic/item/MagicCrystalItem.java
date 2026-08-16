package top.ydog01.mmagic.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MagicCrystalItem extends Item {
    public MagicCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
