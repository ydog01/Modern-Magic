package top.ydog01.mmagic.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.ydog01.mmagic.spell.SpellNodeType;
import top.ydog01.mmagic.spell.SpellRegistry;

import java.util.ArrayList;
import java.util.List;

public class UnknownNodeItem extends Item {
    public UnknownNodeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            stack.shrink(1);

            RandomSource random = level.random;
            ItemStack reward;
            if (random.nextFloat() < 0.10f) {
                List<ResourceLocation> ids = new ArrayList<>(SpellRegistry.ids());
                ids.remove(SpellRegistry.START_ID);
                if (!ids.isEmpty()) {
                    SpellNodeType type = SpellRegistry.get(ids.get(random.nextInt(ids.size())));
                    reward = type == null ? new ItemStack(Items.SNOWBALL) : new ItemStack(type.icon().getItem());
                } else {
                    reward = new ItemStack(Items.SNOWBALL);
                }
            } else {
                reward = new ItemStack(Items.SNOWBALL);
            }

            if (!player.getInventory().add(reward)) {
                player.drop(reward, false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modern_magic.unknown_node"));
    }
}
