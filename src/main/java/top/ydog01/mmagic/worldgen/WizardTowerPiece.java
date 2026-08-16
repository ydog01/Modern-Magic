package top.ydog01.mmagic.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import top.ydog01.mmagic.entity.WizardEntity;
import top.ydog01.mmagic.init.ModEntityTypes;
import top.ydog01.mmagic.init.ModStructures;

public class WizardTowerPiece extends StructurePiece {
    private boolean hasPlacedWizard;

    public WizardTowerPiece(RandomSource random, int centerX, int centerZ) {
        super(ModStructures.WIZARD_TOWER_PIECE.get(), 0, new BoundingBox(centerX - 5, -64, centerZ - 5, centerX + 5, 128, centerZ + 5));
    }

    public WizardTowerPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.WIZARD_TOWER_PIECE.get(), tag);
        this.hasPlacedWizard = tag.getBoolean("Wizard");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Wizard", this.hasPlacedWizard);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int cx = this.boundingBox.minX() + 5;
        int cz = this.boundingBox.minZ() + 5;
        int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz);
        if (ground <= level.getMinBuildHeight() + 1) {
            return;
        }
        buildTower(level, cx, ground, cz, box, random);
        spawnWizard(level, cx, ground, cz, random);
    }

    private void buildTower(WorldGenLevel level, int cx, int ground, int cz, BoundingBox box, RandomSource random) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState base = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState pane = Blocks.GLASS_PANE.defaultBlockState();
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                place(level, cx + dx, ground - 1, cz + dz, base, box);
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    for (int y = 0; y <= 11; y++) {
                        place(level, cx + dx, ground + y, cz + dz, random.nextInt(100) < 15 ? mossy : wall, box);
                    }
                } else {
                    place(level, cx + dx, ground, cz + dz, base, box);
                }
            }
        }
        for (int y = 4; y <= 8; y += 4) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx != -2 || dz != -1) {
                        place(level, cx + dx, ground + y, cz + dz, plank, box);
                    }
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx != -2 || dz != -1) {
                    place(level, cx + dx, ground + 11, cz + dz, wall, box);
                }
            }
        }
        for (int dx = -3; dx <= 1; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == -3 || dx == 1 || dz == -2 || dz == 2) {
                    for (int y = 12; y <= 14; y++) {
                        place(level, cx + dx, ground + y, cz + dz, random.nextInt(100) < 15 ? mossy : wall, box);
                    }
                }
            }
        }
        for (int dx = -3; dx <= 1; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                place(level, cx + dx, ground + 15, cz + dz, wall, box);
            }
        }
        for (int y = 1; y <= 14; y++) {
            place(level, cx - 2, ground + y, cz - 1, ladder, box);
        }
        int[] windowY = {2, 6, 10};
        for (int y : windowY) {
            place(level, cx - 3, ground + y, cz, pane, box);
            place(level, cx + 3, ground + y, cz, pane, box);
            place(level, cx, ground + y, cz - 3, pane, box);
            place(level, cx, ground + y, cz + 3, pane, box);
        }
        place(level, cx - 3, ground + 13, cz, pane, box);
        place(level, cx + 1, ground + 13, cz, pane, box);
        place(level, cx - 1, ground + 13, cz - 2, pane, box);
        place(level, cx - 1, ground + 13, cz + 2, pane, box);
        BlockState doorLower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        place(level, cx, ground, cz + 3, doorLower, box);
        place(level, cx, ground + 1, cz + 3, doorLower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), box);
        BlockState floorTorch = Blocks.TORCH.defaultBlockState();
        BlockState wallTorch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH);
        place(level, cx + 2, ground + 1, cz - 1, floorTorch, box);
        place(level, cx - 1, ground + 5, cz + 1, floorTorch, box);
        place(level, cx - 1, ground + 9, cz + 1, floorTorch, box);
        place(level, cx - 1, ground + 12, cz + 1, floorTorch, box);
        place(level, cx - 1, ground + 1, cz + 4, wallTorch, box);
        place(level, cx + 1, ground + 1, cz + 4, wallTorch, box);
        int roofY = ground + 16;
        coneLayer(level, cx - 3, cz - 2, 5, roofY, box);
        for (int dx = -2; dx <= 0; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                place(level, cx + dx, roofY, cz + dz, plank, box);
            }
        }
        coneLayer(level, cx - 2, cz - 1, 3, roofY + 1, box);
        place(level, cx - 1, roofY + 1, cz, plank, box);
        place(level, cx - 1, roofY + 2, cz, plank, box);
        place(level, cx - 1, roofY + 3, cz, Blocks.LANTERN.defaultBlockState(), box);
    }

    private void coneLayer(WorldGenLevel level, int minX, int minZ, int size, int y, BoundingBox box) {
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                if (dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1) {
                    Direction facing;
                    if (dx == 0) {
                        facing = Direction.WEST;
                    } else if (dx == size - 1) {
                        facing = Direction.EAST;
                    } else if (dz == 0) {
                        facing = Direction.NORTH;
                    } else {
                        facing = Direction.SOUTH;
                    }
                    place(level, minX + dx, y, minZ + dz,
                            Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing), box);
                }
            }
        }
    }

    private void spawnWizard(WorldGenLevel level, int cx, int ground, int cz, RandomSource random) {
        if (this.hasPlacedWizard) {
            return;
        }
        WizardEntity wizard = ModEntityTypes.WIZARD.get().create(level.getLevel());
        if (wizard == null) {
            return;
        }
        this.hasPlacedWizard = true;
        wizard.setPersistenceRequired();
        wizard.moveTo(cx - 1 + 0.5, ground + 12.0, cz + 0.5, random.nextFloat() * 360.0F, 0.0F);
        wizard.finalizeSpawn(level, level.getCurrentDifficultyAt(wizard.blockPosition()), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(wizard);
    }

    private void place(WorldGenLevel level, int x, int y, int z, BlockState state, BoundingBox box) {
        BlockPos pos = new BlockPos(x, y, z);
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }
}
