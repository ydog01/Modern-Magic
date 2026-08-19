package top.ydog01.mmagic.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.ydog01.mmagic.spell.node_api.SpellNode;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class SpellNodeType {
    
    private final ResourceLocation id;
    private final int inputCount;
    private final int outputCount;
    private final int manaCost;
    private final int delayTicks;
    private final Supplier<SpellNode> factory;
    private final Supplier<ItemStack> icon;
    private final List<NodeParameter> parameters;
    private final List<String> categories;
    
    public SpellNodeType(ResourceLocation id, int inputCount, int outputCount, int manaCost, int delayTicks,
                         Supplier<SpellNode> factory, Supplier<ItemStack> icon) {
        this(id, inputCount, outputCount, manaCost, delayTicks, factory, icon, List.of(), List.of());
    }
    
    public SpellNodeType(ResourceLocation id, int inputCount, int outputCount, int manaCost, int delayTicks,
                         Supplier<SpellNode> factory, Supplier<ItemStack> icon, List<NodeParameter> parameters,
                         List<String> categories) {
        this.id = id;
        this.inputCount = inputCount;
        this.outputCount = outputCount;
        this.manaCost = manaCost;
        this.delayTicks = delayTicks;
        this.factory = factory;
        this.icon = icon;
        this.parameters = List.copyOf(parameters);
        this.categories = List.copyOf(categories);
    }
    
    public ResourceLocation id() { return id; }
    public int inputCount() { return inputCount; }
    public int outputCount() { return outputCount; }
    public int manaCost() { return manaCost; }
    public int delayTicks() { return delayTicks; }
    public ItemStack icon() { return icon.get(); }
    public List<NodeParameter> parameters() { return parameters; }
    public List<String> categories() { return categories; }
    
    public Component displayName() {
        return Component.translatable("spell_node." + id.getNamespace() + "." + id.getPath());
    }
    
    public Component description() {
        return Component.translatable("spell_node." + id.getNamespace() + "." + id.getPath() + ".desc");
    }
    
    public SpellNode create(UUID nodeId) {
        SpellNode node = factory.get();
        node.setUuid(nodeId);
        return node;
    }
}