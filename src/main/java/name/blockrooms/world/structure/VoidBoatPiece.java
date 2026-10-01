package name.blockrooms.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 虚空之船的部件骨架（当前未使用：VoidBoatStructure 走 NbtTemplatePiece 放置模板）。 */
public class VoidBoatPiece extends StructurePiece {

    public VoidBoatPiece(BlockPos anchor, int sizeX, int sizeY, int sizeZ) {
        super(ModStructures.NBT_TEMPLATE_PIECE_TYPE.get(), 0,
                BoundingBox.fromCorners(anchor, anchor.offset(sizeX - 1, sizeY - 1, sizeZ - 1)));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag) {

    }

    @Override
    public void postProcess(WorldGenLevel worldGenLevel, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource randomSource, BoundingBox boundingBox, ChunkPos chunkPos, BlockPos blockPos) {

    }
}
