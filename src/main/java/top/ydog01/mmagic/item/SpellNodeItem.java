package top.ydog01.mmagic.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SpellNodeItem extends Item {
    public SpellNodeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
