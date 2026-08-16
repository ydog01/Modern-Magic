package top.ydog01.mmagic.client;

import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import top.ydog01.mmagic.entity.WizardEntity;

public class WizardRenderer extends HumanoidMobRenderer<WizardEntity, SkeletonModel<WizardEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    public WizardRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(WizardEntity entity) {
        return TEXTURE;
    }
}
