package top.ydog01.mmagic.init;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.loot.MobDropModifier;

import java.util.function.Supplier;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ModernMagic.MODID);

    public static final Supplier<MapCodec<MobDropModifier>> MOB_DROPS =
            GLOBAL_LOOT_MODIFIERS.register("mob_drops", () -> MobDropModifier.CODEC);

    private ModLootModifiers() {
    }
}
