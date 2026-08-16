package top.ydog01.mmagic.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import top.ydog01.mmagic.spell.SpellNodeType;
import top.ydog01.mmagic.spell.SpellRegistry;

import java.util.ArrayList;
import java.util.List;

public class MobDropModifier extends LootModifier {
    public static final MapCodec<MobDropModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, MobDropModifier::new));

    public MobDropModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof LivingEntity living)) {
            return generatedLoot;
        }
        if (living instanceof net.minecraft.world.entity.player.Player) {
            return generatedLoot;
        }
        RandomSource random = context.getRandom();
        if (random.nextFloat() < 0.05) {
            List<ResourceLocation> ids = new ArrayList<>(SpellRegistry.ids());
            ids.remove(SpellRegistry.START_ID);
            if (!ids.isEmpty()) {
                SpellNodeType type = SpellRegistry.get(ids.get(random.nextInt(ids.size())));
                if (type != null) {
                    generatedLoot.add(new ItemStack(type.icon().getItem(), 1));
                }
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
