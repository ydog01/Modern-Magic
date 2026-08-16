package top.ydog01.mmagic.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.entity.MagicMissileEntity;
import top.ydog01.mmagic.entity.WizardEntity;

import java.util.function.Supplier;

@EventBusSubscriber(modid = ModernMagic.MODID)
public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ModernMagic.MODID);

    public static final Supplier<EntityType<MagicMissileEntity>> MAGIC_MISSILE = ENTITY_TYPES.register("magic_missile",
            () -> EntityType.Builder.<MagicMissileEntity>of(MagicMissileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("magic_missile"));

    public static final Supplier<EntityType<WizardEntity>> WIZARD = ENTITY_TYPES.register("wizard",
            () -> EntityType.Builder.of(WizardEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.99f)
                    .clientTrackingRange(8)
                    .build("wizard"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WIZARD.get(), WizardEntity.createAttributes().build());
    }

    private ModEntityTypes() {
    }
}
