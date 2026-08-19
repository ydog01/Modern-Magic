package top.ydog01.mmagic.spell;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class SpellRegistry {
    
    public static final ResourceLocation START_ID = 
            ResourceLocation.fromNamespaceAndPath("modern_magic", "start");
    
    private static final Map<ResourceLocation, Supplier<SpellNodeType>> TYPES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, SpellNodeType> CACHE = new HashMap<>();
    
    public static void register(ResourceLocation id, int inputCount, int outputCount, int manaCost, int delayTicks,
                                Supplier<SpellNode> factory, Supplier<ItemStack> icon) {
        register(id, inputCount, outputCount, manaCost, delayTicks, factory, icon, List.of(), List.of());
    }
    
    public static void register(ResourceLocation id, int inputCount, int outputCount, int manaCost, int delayTicks,
                                Supplier<SpellNode> factory, Supplier<ItemStack> icon, List<NodeParameter> parameters,
                                List<String> categories) {
        TYPES.put(id, () -> new SpellNodeType(id, inputCount, outputCount, manaCost, delayTicks, factory, icon,
                parameters, categories));
    }
    
    public static Collection<ResourceLocation> ids() {
        return Collections.unmodifiableSet(TYPES.keySet());
    }
    
    public static SpellNodeType get(ResourceLocation id) {
        System.out.println("[ModernMagic] 查询节点: " + id);
        SpellNodeType cached = CACHE.get(id);
        if (cached == null) {
            Supplier<SpellNodeType> supplier = TYPES.get(id);
            if (supplier != null) {
                cached = supplier.get();
                CACHE.put(id, cached);
            }
        }
        return cached;
    }
    
    public static SpellNodeType get(String id) {
        ResourceLocation location = id.indexOf(':') >= 0
                ? ResourceLocation.parse(id)
                : ResourceLocation.fromNamespaceAndPath("modern_magic", id);
        return get(location);
    }
}