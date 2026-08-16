package top.ydog01.mmagic.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.ydog01.mmagic.ModernMagic;
import top.ydog01.mmagic.worldgen.WizardTowerPiece;
import top.ydog01.mmagic.worldgen.WizardTowerStructure;

import java.util.function.Supplier;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, ModernMagic.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, ModernMagic.MODID);

    public static final Supplier<StructurePieceType> WIZARD_TOWER_PIECE = PIECE_TYPES.register("wizard_tower_piece",
            () -> (context, tag) -> new WizardTowerPiece(context, tag));

    public static final Supplier<StructureType<WizardTowerStructure>> WIZARD_TOWER_TYPE =
            STRUCTURE_TYPES.register("wizard_tower", () -> () -> WizardTowerStructure.CODEC);

    private ModStructures() {
    }
}
