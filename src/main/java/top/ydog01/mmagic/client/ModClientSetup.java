package top.ydog01.mmagic.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.init.ModEntityTypes;
import top.ydog01.mmagic.init.ModMenuTypes;

@EventBusSubscriber(modid = ModernMagic.MODID, value = Dist.CLIENT)
public final class ModClientSetup {
    private ModClientSetup() {
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ALTAR.get(), AltarScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.MAGIC_MISSILE.get(), ctx -> new ThrownItemRenderer<>(ctx));
        event.registerEntityRenderer(ModEntityTypes.WIZARD.get(), WizardRenderer::new);
    }
}
