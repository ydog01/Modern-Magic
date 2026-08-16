package top.ydog01.mmagic.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import top.ydog01.mmagic.init.ModMenuTypes;

public class AltarMenu extends AbstractContainerMenu {
    public AltarMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.ALTAR.get(), containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
