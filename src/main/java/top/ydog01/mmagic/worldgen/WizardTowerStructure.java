package top.ydog01.mmagic.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import top.ydog01.mmagic.init.ModStructures;

import java.util.Optional;

public class WizardTowerStructure extends Structure {
    public static final MapCodec<WizardTowerStructure> CODEC = simpleCodec(WizardTowerStructure::new);

    public WizardTowerStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder ->
                builder.addPiece(new WizardTowerPiece(context.random(),
                        context.chunkPos().getMinBlockX() + 8, context.chunkPos().getMinBlockZ() + 8)));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.WIZARD_TOWER_TYPE.get();
    }
}
