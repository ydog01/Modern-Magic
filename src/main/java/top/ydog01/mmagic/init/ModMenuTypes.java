package top.ydog01.mmagic.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.menu.AltarMenu;

import java.util.function.Supplier;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ModernMagic.MODID);

    public static final Supplier<MenuType<AltarMenu>> ALTAR = MENUS.register("altar",
            () -> new MenuType<>(AltarMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenuTypes() {
    }
}
