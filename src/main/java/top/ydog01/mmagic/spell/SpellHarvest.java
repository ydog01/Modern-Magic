package top.ydog01.mmagic.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SpellHarvest {
    public static void pickup(ServerLevel level, LivingEntity caster, Vec3 at, float radius) {}
    public static void digArea(ServerLevel level, LivingEntity caster, BlockPos center, float radius, int digLevel, boolean drops, boolean silk, int fortune) {}
    public static void chainDig(ServerLevel level, LivingEntity caster, BlockPos start, float radius, int digLevel, boolean drops, boolean silk, int fortune) {}
    public static void setPendingLooting(int entityId, int level) {}
    public static void clearPendingLooting(int entityId) {}
}
