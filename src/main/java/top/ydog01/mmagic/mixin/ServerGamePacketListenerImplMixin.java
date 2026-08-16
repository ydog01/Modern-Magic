package top.ydog01.mmagic.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ydog01.mmagic.item.WandItem;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void modernMagic$protectWandData(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        int slot = packet.slotNum();
        if (slot < 0) {
            return; 
        }
        ItemStack incoming = packet.itemStack();
        if (!(incoming.getItem() instanceof WandItem)) {
            return;
        }
        ItemStack existing = this.player.getInventory().getItem(slot);
        if (!(existing.getItem() instanceof WandItem)) {
            return;
        }
        if (incoming.get(DataComponents.CUSTOM_DATA) == null && existing.get(DataComponents.CUSTOM_DATA) != null) {
            ci.cancel();
        }
    }
}
