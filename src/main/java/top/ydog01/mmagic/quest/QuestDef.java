package top.ydog01.mmagic.quest;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record QuestDef(ResourceLocation id, Kind kind, ItemStack icon, int target, int xp, List<ItemStack> rewards,
                       ResourceLocation recipeId) {
    public enum Kind {
        ALTAR, CRYSTALS, WAND, NODES, MISSILE, DUPLICATE, SPELL_EDIT, WIZARD_KILL, LEGENDARY_KILL
    }
}
