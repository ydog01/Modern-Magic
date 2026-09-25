package top.ydog01.mmagic.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.entity.MagicMissileEntity;

import java.util.function.Supplier;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ModernMagic.MODID);

    public static final Supplier<EntityType<MagicMissileEntity>> MAGIC_MISSILE = ENTITY_TYPES.register("magic_missile",
            () -> EntityType.Builder.<MagicMissileEntity>of(MagicMissileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("magic_missile"));

    private ModEntityTypes() {
    }
}
