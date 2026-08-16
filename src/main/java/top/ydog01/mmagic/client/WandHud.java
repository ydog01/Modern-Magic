package top.ydog01.mmagic.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.item.WandItem;
import top.ydog01.mmagic.network.ModNetwork;
import top.ydog01.mmagic.util.WandData;

@EventBusSubscriber(modid = ModernMagic.MODID, value = Dist.CLIENT)
public final class WandHud {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(ModernMagic.MODID, "wand_hud");

    private WandHud() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, WandHud::renderHud);
    }

    public static void receiveManaSync(ModNetwork.ManaSyncPacket packet) {
    }

    private static void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof WandItem)) {
            return;
        }
        Component name = stack.getHoverName();

        long now = mc.level.getGameTime();
        double mana = WandData.getManaDisplay(stack, mc.level);
        int max = WandData.getMaxMana(stack);
        int cooldown = WandData.getCooldown(stack);
        long lastCast = WandData.tag(stack).getLong(WandData.KEY_LAST_CAST);

        Component manaLine = Component.literal(Math.round(mana) + " / " + max);
        long cdRemaining = cooldown - (now - lastCast);
        Component cdLine = null;
        if (cdRemaining > 0) {
            cdLine = Component.translatable("hud.modern_magic.cooldown",
                    String.format("%.1f", cdRemaining / 20.0));
        }

        int w = Math.max(mc.font.width(name), mc.font.width(manaLine));
        if (cdLine != null) {
            w = Math.max(w, mc.font.width(cdLine));
        }
        int x = g.guiWidth() - 10 - w;
        int y = 34;
        int height = cdLine != null ? 38 : 26;
        g.fill(x - 5, y - 4, x + w + 5, y - 4 + height, 0x80000000);
        g.drawString(mc.font, name, x, y, 0xFFFFFFFF);
        g.drawString(mc.font, manaLine, x, y + 12, 0xFF55FFFF);
        if (cdLine != null) {
            g.drawString(mc.font, cdLine, x, y + 24, 0xFFFF5555);
        }
    }
}
