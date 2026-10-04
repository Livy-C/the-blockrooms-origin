package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.StreamSupport;

public class BlockLevel0Generator extends SimpleGenerator {
    public static final MapCodec<BlockLevel0Generator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevel0Generator::getBiomeSource)
            ).apply(instance, BlockLevel0Generator::new)
    );

    private static final int STRUCTURE_RANGE = 128 * 16;

    private static final double LAMP_OFF_CHANCE = 0.10;
    private static final double CARPET_GAP_CHANCE = 0.10;
    private static final double CEILING_HOLE_CHANCE = 0.05;
    private static final double PAINTING_CHANCE = 0.02;

    private static final long OUTPOST_SEED_XOR = 0xB3E62F0A5D1C4E39L;

    public BlockLevel0Generator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;

                for (int y = this.getMinY(); y <= this.getMinY() + this.getGenDepth(); y++) {
                    if (y >= 64 && y <= 64 + 4) chunk.setBlockState(new BlockPos(x, y, z), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_NONE);
                    else chunk.setBlockState(new BlockPos(x, y, z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_NONE);
                }
                chunk.setBlockState(new BlockPos(x, 64, z), Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_NONE);
                chunk.setBlockState(new BlockPos(x, 64 + 1, z), Blocks.CARPET.brown().defaultBlockState(), Block.UPDATE_NONE);

                if ((worldX % 5 + 5) % 5 < 2 && (worldZ % 2 + 2) % 2 == 0) {
                    chunk.setBlockState(new BlockPos(x, 64 + 5, z), Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), Block.UPDATE_ALL);
                    chunk.setBlockState(new BlockPos(x, 64 + 6, z), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                }
                else {
                    chunk.setBlockState(new BlockPos(x, 64 + 5, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
                }

                Random random = new Random(worldGenRegion.getSeed() ^ (worldX * 0x9e3779b97f4a7c15L) ^ (worldZ * 0xdefacedddeedbeefL));
                if (random.nextDouble() <= 0.2) {
                    for (int y = 64 + 1; y <= 64 + 4; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.CHISELED_SANDSTONE.defaultBlockState(), Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
        }

        applyVariantRegions(worldGenRegion.getSeed(), chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
        for (int x = 1; x < 15; x++) {
            for (int z = 1; z < 15; z++) {
                int worldX = chunk.getPos().getMinBlockX() + x;
                int worldZ = chunk.getPos().getMinBlockZ() + z;

                RandomSource randomSource = RandomSource.create(worldGenRegion.getSeed() ^ (worldX * 0x9e3779b97f4a7c15L) ^ (worldZ * 0xdefacedddeedbeefL));
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(randomSource);
                if (randomSource.nextDouble() <= 0.8) {
                    placeOakExit(chunk, new BlockPos(x, 64 + 1, z), direction, worldGenRegion);
                }
            }
        }
    }

    private static void placeOakExit(ChunkAccess chunk, BlockPos pos, Direction direction, WorldGenRegion worldGenRegion) {
        for (int r = 64; r <= 64 + 4; r++) {
            if (!chunk.getBlockState(pos.relative(direction, r)).isAir()) {
                return;
            }
        }

        for (int xo = -1; xo <= 1; xo++) {
            for (int zo = -1; zo <= 1; zo++) {
                for (int yo = 0; yo < 4; yo++) {
                    chunk.setBlockState(pos.offset(xo, yo, zo), Blocks.CHISELED_SANDSTONE.defaultBlockState());
                }
            }
        }

        chunk.setBlockState(pos, ModBlocks.NOCLIP_POINT.get().defaultBlockState(), Block.UPDATE_NONE);
        chunk.setBlockState(pos.above(), ModBlocks.NOCLIP_POINT.get().defaultBlockState(), Block.UPDATE_NONE);
        chunk.setBlockState(pos.relative(direction),
                Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                        .setValue(DoorBlock.FACING, direction.getOpposite()), Block.UPDATE_NONE);
        chunk.setBlockState(pos.relative(direction).above(),
                Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                        .setValue(DoorBlock.FACING, direction.getOpposite()), Block.UPDATE_NONE);

        chunk.setBlockState(pos.relative(direction, 2).above(2),
                Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, direction),
                Block.UPDATE_NONE);

        /*((SignBlockEntity) Objects.requireNonNull(worldGenRegion.getBlockEntity(pos.relative(direction, 2).above(2)))).setText(
                new SignText(new Component[]{Component.literal("EXIT"), CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY},
                        new Component[]{CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY},
                        DyeColor.BLACK, false), true
        );*/
        SignBlockEntity signBlockEntity = new SignBlockEntity(pos.relative(direction, 2).above(2), Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, direction));
        signBlockEntity.setLevel(worldGenRegion.getLevel());
        signBlockEntity.setText(
                new SignText(new Component[]{Component.literal("EXIT"), CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY},
                        new Component[]{CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY},
                        DyeColor.BLACK, false), true
        );
        chunk.setBlockEntity(signBlockEntity);

        // DEBUG for finding exits
        chunk.setBlockState(pos.above(8), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_ALL);
        chunk.setBlockState(pos.above(9), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        placePaintings(worldGenRegion);
    }

    @Override
    public int getBaseHeight(int i, int i1, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return 0;
    }

    public static BlockPos outpostCenter(long seed) {
        Random random = new Random(seed ^ OUTPOST_SEED_XOR);
        int x = random.nextInt(STRUCTURE_RANGE * 2) - STRUCTURE_RANGE;
        int z = random.nextInt(STRUCTURE_RANGE * 2) - STRUCTURE_RANGE;
        return new BlockPos(x, 64 + 2, z);
    }

    private static void applyVariantRegions(long seed, ChunkAccess chunk) {
        int chunkX = chunk.getPos().x();
        int chunkZ = chunk.getPos().z();
        Random random = new Random(seed ^ (chunkX * 0x9e3779b97f4a7c15L) ^ (chunkZ * 0xdefacedddeedbeefL));

        boolean lampsOff = random.nextDouble() < LAMP_OFF_CHANCE;
        boolean carpetGaps = random.nextDouble() < CARPET_GAP_CHANCE;
        boolean ceilingHole = random.nextDouble() < CEILING_HOLE_CHANCE;
        if (!lampsOff && !carpetGaps && !ceilingHole) return;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (lampsOff) {
                    BlockPos lampPos = new BlockPos(x, 64 + 5, z);
                    BlockState lamp = chunk.getBlockState(lampPos);
                    if (lamp.is(Blocks.REDSTONE_LAMP)) {
                        chunk.setBlockState(lampPos, lamp.setValue(RedstoneLampBlock.LIT, false), Block.UPDATE_CLIENTS);
                        chunk.setBlockState(new BlockPos(x, 64 + 6, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                if (carpetGaps && random.nextDouble() < 0.25) {
                    BlockPos carpetPos = new BlockPos(x, 64 + 1, z);
                    if (chunk.getBlockState(carpetPos).is(Blocks.CARPET.brown())) {
                        chunk.setBlockState(carpetPos, Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        if (ceilingHole) {
            int holeX = random.nextInt(14);
            int holeZ = random.nextInt(14);
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    int bx = holeX + dx;
                    int bz = holeZ + dz;
                    if (chunk.getBlockState(new BlockPos(bx, 64 + 5, bz)).is(Blocks.REDSTONE_LAMP)) {
                        chunk.setBlockState(new BlockPos(bx, 64 + 6, bz), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    chunk.setBlockState(new BlockPos(bx, 64 + 5, bz), Blocks.CAVE_AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
    private static void placePaintings(WorldGenRegion region) {
        ChunkAccess chunk = region.getChunk(region.getCenter().x(), region.getCenter().z());
        List<Holder<PaintingVariant>> smallVariants = StreamSupport.stream(region.registryAccess()
                .lookupOrThrow(Registries.PAINTING_VARIANT)
                .getTagOrEmpty(PaintingVariantTags.PLACEABLE)
                                .spliterator()
                ,false)
                .filter(variant -> variant.value().width() == 1 && variant.value().height() == 1)
                .toList();
        if (smallVariants.isEmpty()) return;

        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();

        for (int y = 64 + 2; y <= 64 + 3; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!chunk.getBlockState(pos).isAir()) continue;

                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        int wx = x + dir.getStepX();
                        int wz = z + dir.getStepZ();
                        if (wx < 0 || wx > 15 || wz < 0 || wz > 15) continue;
                        BlockPos wallPos = new BlockPos(wx, y, wz);
                        if (!chunk.getBlockState(wallPos).is(Blocks.CHISELED_SANDSTONE)) continue;
                        int bx = wx + dir.getStepX();
                        int bz = wz + dir.getStepZ();
                        if (bx < 0 || bx > 15 || bz < 0 || bz > 15) continue;
                        if (!chunk.getBlockState(new BlockPos(bx, y, bz)).isAir()) continue;

                        if (region.getRandom().nextDouble() > PAINTING_CHANCE) continue;

                        Optional<Holder<PaintingVariant>> variant = Util.getRandomSafe(smallVariants, region.getRandom());
                        if (variant.isEmpty()) continue;
                        Painting painting = new Painting(region.getLevel(),
                                new BlockPos(minBlockX + x, y, minBlockZ + z), dir.getOpposite(), variant.get());
                        region.addFreshEntity(painting);
                        break;
                    }
                }
            }
        }
    }

}
