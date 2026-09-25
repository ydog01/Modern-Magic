package top.ydog01.mmagic.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class SpellHarvest {
    private SpellHarvest() {}

    public static void pickup(ServerLevel level, LivingEntity caster, Vec3 at, float radius) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(radius))) {
            ItemStack stack = item.getItem();
            if (caster instanceof Player player) {
                if (player.getInventory().add(stack)) {
                    item.discard();
                } else {
                    item.teleportTo(caster.getX(), caster.getY() + 0.2, caster.getZ());
                }
            }
        }
    }

    public static void digArea(ServerLevel level, LivingEntity caster, BlockPos center, float radius,
                               int digLevel, boolean drops, boolean silk, int fortune) {
        int r = Math.max(0, Math.round(radius));
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-r, -r, -r),
                center.offset(r, r, r))) {
            if (pos.distSqr(center) > radius * radius) continue;
            digBlock(level, caster, pos.immutable(), digLevel, drops, silk, fortune);
        }
    }

    public static void chainDig(ServerLevel level, LivingEntity caster, BlockPos start, float radius,
                                int digLevel, boolean drops, boolean silk, int fortune) {
        BlockState startState = level.getBlockState(start);
        if (startState.isAir() || startState.getDestroySpeed(level, start) < 0) {
            return;
        }
        if (!canDig(startState, digLevel)) {
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (pos.distSqr(start) > radius * radius) continue;

            BlockState state = level.getBlockState(pos);
            if (!state.is(startState.getBlock())) continue;

            digBlock(level, caster, pos, digLevel, drops, silk, fortune);

            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.add(next) && level.getBlockState(next).is(startState.getBlock())) {
                    queue.add(next);
                }
            }
        }
    }

    private static void digBlock(ServerLevel level, LivingEntity caster, BlockPos pos,
                                 int digLevel, boolean drops, boolean silk, int fortune) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        if (state.getDestroySpeed(level, pos) < 0) return;
        if (!canDig(state, digLevel)) return;

        if (drops) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
            for (ItemStack stack : Block.getDrops(state, level, pos, blockEntity, caster, tool)) {
                Block.popResource(level, pos, stack);
            }
        }
        level.destroyBlock(pos, false);
    }

    private static boolean canDig(BlockState state, int digLevel) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL) && digLevel < 3) return false;
        if (state.is(BlockTags.NEEDS_IRON_TOOL) && digLevel < 2) return false;
        if (state.is(BlockTags.NEEDS_STONE_TOOL) && digLevel < 1) return false;
        return true;
    }

    // Kept for API compatibility with entity loot hooks.
    public static void setPendingLooting(int entityId, int level) {}
    public static void clearPendingLooting(int entityId) {}
}
