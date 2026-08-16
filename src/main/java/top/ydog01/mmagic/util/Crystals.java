package top.ydog01.mmagic.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.ydog01.mmagic.init.ModItems;

public final class Crystals {
    private Crystals() {
    }

    public static int count(Player player) {
        return countOf(player, ModItems.MAGIC_CRYSTAL.get());
    }

    public static boolean consume(Player player, int amount) {
        return consumeItem(player, ModItems.MAGIC_CRYSTAL.get(), amount);
    }

    public static int countOf(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean consumeItem(Player player, Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (countOf(player, item) < amount) {
            return false;
        }
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) {
                    break;
                }
            }
        }
        return remaining <= 0;
    }
}
