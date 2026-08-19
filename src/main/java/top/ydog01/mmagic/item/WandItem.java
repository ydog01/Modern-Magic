package top.ydog01.mmagic.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.ydog01.mmagic.spell.casting.SpellRunner;
import top.ydog01.mmagic.util.WandData;

import java.util.List;

public class WandItem extends Item {
    public WandItem(Properties properties) {

        super(properties.stacksTo(1).attributes(SwordItem.createAttributes(Tiers.WOOD, 2.0F, -2.4F)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.MAIN_HAND && level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            SpellRunner.cast(serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.modern_magic.wand.max_mana", WandData.getMaxMana(stack)));
        tooltip.add(Component.translatable("tooltip.modern_magic.wand.regen", String.format("%.1f", WandData.getRegen(stack))));
        tooltip.add(Component.translatable("tooltip.modern_magic.wand.cooldown", String.format("%.2f", WandData.getCooldown(stack) / 20.0)));
        Level level = context.level();
        double mana = level != null ? WandData.getManaDisplay(stack, level) : WandData.getStoredMana(stack);
        tooltip.add(Component.translatable("tooltip.modern_magic.wand.mana", String.format("%.1f", mana)));
    }
}
