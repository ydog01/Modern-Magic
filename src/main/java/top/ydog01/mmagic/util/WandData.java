package top.ydog01.mmagic.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import top.ydog01.mmagic.spell.SpellGraph;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class WandData {
    public static final String KEY_MANA = "mana";
    public static final String KEY_MAX_MANA = "maxMana";
    public static final String KEY_REGEN = "manaRegen";
    public static final String KEY_COOLDOWN = "cooldown";
    public static final String KEY_LAST_UPDATE = "lastUpdateTick";
    public static final String KEY_LAST_CAST = "lastCastTick";
    public static final String KEY_WAND_ID = "wandId";

    public static final int DEFAULT_MAX_MANA = 20;
    public static final double DEFAULT_REGEN = 1.0;
    public static final int DEFAULT_COOLDOWN = 40;

    private WandData() {
    }

    public static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static void updateTag(ItemStack stack, Consumer<CompoundTag> updater) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, updater);
    }

    public static int getMaxMana(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (tag.contains(KEY_MAX_MANA)) {
            return tag.getInt(KEY_MAX_MANA);
        }
        return DEFAULT_MAX_MANA;
    }

    public static double getRegen(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (tag.contains(KEY_REGEN)) {
            return tag.getDouble(KEY_REGEN);
        }
        return DEFAULT_REGEN;
    }

    public static int getCooldown(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (tag.contains(KEY_COOLDOWN)) {
            return tag.getInt(KEY_COOLDOWN);
        }
        return DEFAULT_COOLDOWN;
    }

    public static double getStoredMana(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (tag.contains(KEY_MANA)) {
            return tag.getDouble(KEY_MANA);
        }
        return getMaxMana(stack);
    }

    public static double getMana(ItemStack stack, Level level) {
        double mana = getManaDisplay(stack, level);
        updateTag(stack, t -> {
            t.putLong(KEY_LAST_UPDATE, level.getGameTime());
            t.putDouble(KEY_MANA, mana);
        });
        return mana;
    }

    public static double getManaDisplay(ItemStack stack, Level level) {
        CompoundTag tag = tag(stack);
        long now = level.getGameTime();
        double mana;
        if (tag.contains(KEY_MANA)) {
            mana = tag.getDouble(KEY_MANA);
            long last = tag.getLong(KEY_LAST_UPDATE);
            if (now > last) {
                mana += getRegen(stack) * (now - last) / 20.0;
                int max = getMaxMana(stack);
                if (mana > max) {
                    mana = max;
                }
            }
        } else {
            mana = getMaxMana(stack);
        }
        return mana;
    }

    public static boolean consumeMana(ItemStack stack, int cost, Level level) {
        if (cost <= 0) {
            return true;
        }
        double mana = getMana(stack, level);
        if (mana < cost) {
            return false;
        }
        updateTag(stack, t -> t.putDouble(KEY_MANA, mana - cost));
        return true;
    }

    public static boolean isReady(ItemStack stack, Level level) {
        long last = tag(stack).getLong(KEY_LAST_CAST);
        return (level.getGameTime() - last) >= getCooldown(stack);
    }

    public static void markCast(ItemStack stack, Level level) {
        updateTag(stack, t -> t.putLong(KEY_LAST_CAST, level.getGameTime()));
    }

    public static String getName(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name == null ? "" : name.getString();
    }

    public static void setName(ItemStack stack, String name) {
        if (name == null || name.isBlank()) {
            stack.remove(DataComponents.CUSTOM_NAME);
        } else {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
    }

    public static void setAttributes(ItemStack stack, int maxMana, double regen, int cooldown) {
        final int clampedMana = Math.max(1, maxMana);
        final double clampedRegen = Math.max(0.0, regen);
        final int clampedCooldown = Math.max(1, cooldown);
        updateTag(stack, t -> {
            t.putInt(KEY_MAX_MANA, clampedMana);
            t.putDouble(KEY_REGEN, clampedRegen);
            t.putInt(KEY_COOLDOWN, clampedCooldown);
            if (t.contains(KEY_MANA) && t.getDouble(KEY_MANA) > clampedMana) {
                t.putDouble(KEY_MANA, clampedMana);
            }
        });
    }

    public static SpellGraph getGraph(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (tag.contains(SpellGraph.KEY)) {
            return SpellGraph.fromTag(tag.getCompound(SpellGraph.KEY));
        }
        return SpellGraph.createDefault();
    }

    public static void setGraph(ItemStack stack, SpellGraph graph) {
        updateTag(stack, t -> t.put(SpellGraph.KEY, graph.toTag()));
    }

    public static java.util.UUID getWandId(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (tag.hasUUID(KEY_WAND_ID)) {
            return tag.getUUID(KEY_WAND_ID);
        }
        java.util.UUID id = java.util.UUID.randomUUID();
        updateTag(stack, t -> t.putUUID(KEY_WAND_ID, id));
        return id;
    }

    /**
     * True when the stack is the wand carrying the given id. Unlike
     * {@link #getWandId(ItemStack)} this never generates a new id.
     */
    public static boolean hasWandId(ItemStack stack, UUID wandId) {
        if (stack.isEmpty() || wandId == null) {
            return false;
        }
        CompoundTag tag = tag(stack);
        return tag.hasUUID(KEY_WAND_ID) && wandId.equals(tag.getUUID(KEY_WAND_ID));
    }

    /**
     * Locate a wand by id in a living entity's hands and, for players, their
     * whole inventory. The ItemStack captured when a spell starts becomes stale
     * as soon as the player moves the wand, so mana operations must resolve the
     * current stack instead of holding onto that old reference.
     */
    public static ItemStack findWandById(LivingEntity living, UUID wandId) {
        if (living == null || wandId == null) {
            return ItemStack.EMPTY;
        }
        ItemStack main = living.getMainHandItem();
        if (hasWandId(main, wandId)) {
            return main;
        }
        ItemStack off = living.getOffhandItem();
        if (hasWandId(off, wandId)) {
            return off;
        }
        if (living instanceof Player player) {
            ItemStack carried = player.containerMenu.getCarried();
            if (hasWandId(carried, wandId)) {
                return carried;
            }
            for (ItemStack stack : player.getInventory().items) {
                if (hasWandId(stack, wandId)) {
                    return stack;
                }
            }
            for (ItemStack stack : player.getInventory().armor) {
                if (hasWandId(stack, wandId)) {
                    return stack;
                }
            }
            if (player.containerMenu instanceof InventoryMenu menu) {
                for (Slot slot : menu.slots) {
                    ItemStack stack = slot.getItem();
                    if (hasWandId(stack, wandId)) {
                        return stack;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }
    /**
     * Collect every wand id currently present in a living entity's hands,
     * cursor and (for players) inventory/crafting slots. Used once per tick to
     * cheaply check whether the wand backing each active spell is still held.
     */
    public static Set<UUID> collectWandIds(LivingEntity living) {
        Set<UUID> ids = new HashSet<>();
        if (living == null) {
            return ids;
        }
        collectWandId(ids, living.getMainHandItem());
        collectWandId(ids, living.getOffhandItem());
        if (living instanceof Player player) {
            collectWandId(ids, player.containerMenu.getCarried());
            for (ItemStack stack : player.getInventory().items) {
                collectWandId(ids, stack);
            }
            for (ItemStack stack : player.getInventory().armor) {
                collectWandId(ids, stack);
            }
            if (player.containerMenu instanceof InventoryMenu menu) {
                for (Slot slot : menu.slots) {
                    collectWandId(ids, slot.getItem());
                }
            }
        }
        return ids;
    }

    private static void collectWandId(Set<UUID> ids, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = tag(stack);
        if (tag.hasUUID(KEY_WAND_ID)) {
            ids.add(tag.getUUID(KEY_WAND_ID));
        }
    }
}
