package top.ydog01.mmagic.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ydog01.mmagic.network.ModNetwork;
import top.ydog01.mmagic.util.WandData;

public final class SpellContext {
    private final ServerLevel level;
    private final LivingEntity caster;
    private final ItemStack wand;
    private final SpellGraph graph;
    private final Vec3 origin;
    private final Vec3 direction;
    private final java.util.UUID wandId;

    public SpellContext(ServerLevel level, LivingEntity caster, ItemStack wand, SpellGraph graph) {
        this.level = level;
        this.caster = caster;
        this.wand = wand;
        this.graph = graph;
        this.origin = caster.getEyePosition();
        this.direction = caster.getLookAngle();
        this.wandId = WandData.getWandId(wand);
    }

    public ServerLevel level() {
        return level;
    }

    public LivingEntity caster() {
        return caster;
    }

    public ItemStack wand() {
        return wand;
    }

    public SpellGraph graph() {
        return graph;
    }

    public Vec3 origin() {
        return origin;
    }

    public Vec3 direction() {
        return direction;
    }

    public boolean terminated() {
        return SpellRunner.isCastTerminated(wandId);
    }

    public void markTerminated() {
        SpellRunner.markCastTerminated(wand);
    }

    public boolean consumeMana(int cost) {
        if (cost <= 0) {
            return true;
        }
        java.util.UUID wandId = WandData.getWandId(wand);
        if (SpellRunner.hasActiveSpell(wandId)) {
            return SpellRunner.consumeActiveMana(caster, wandId, cost);
        }
        if (SpellRunner.findWandById(caster, wandId).isEmpty()) {
            return false;
        }
        if (!WandData.consumeMana(wand, cost, level)) {
            return false;
        }
        if (caster instanceof ServerPlayer sp) {
            ModNetwork.sendManaSync(sp, wand);
        }
        return true;
    }
}
