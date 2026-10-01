package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.util.Util;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;

/**
 * 画廊生成器：一条沿 X 轴无限延伸的廊道（无封墙），在 Z 轴方向无限多条平行排列，
 * 相邻廊道之间用「可穿过画」链接（走进 4×3 矮画 → 传送到相邻廊道，见 GalleryPassageHandler）。
 *
 * <p>Z 方向布局（每 CORRIDOR_SPACING 格重复一条廊道）：
 * <pre>
 *   relZ  0       1 2 3 4      5       6 .. 23
 *        墙  |  廊道内部(空气) |  墙  |  实心间隔区（下一周期的墙）
 * </pre>
 * 廊道内部宽 4 格（relZ 1..4），y=0 地板、y=5 天花板（橡木木板），y=6 基岩封顶；
 * 墙与间隔区为实心木板，X 方向没有任何封墙——廊道真正无限延伸。
 *
 * <p>传送画（4×3 矮画）：挂在廊道两侧墙的内侧，每 LINK_INTERVAL 格一幅
 * （锚点 x%LINK_INTERVAL==PASSAGE_X_OFFSET），走进它会被传送到 Z 轴相邻的廊道
 * （x/y 不变，z ± CORRIDOR_SPACING）。其余墙面随机挂 4×4 大画装饰（不可穿过）。
 */
public class TheGalleryGenerator extends SimpleGenerator {

    /** Z 方向廊道间距：廊道 k 位于 z ∈ [k*S, k*S+5]（含墙），相邻廊道中心距 S。 */
    public static final int CORRIDOR_SPACING = 24;

    /** 廊道内部相对 z（空气区）：relZ ∈ [1, 4]，宽 4。 */
    public static final int INNER_Z_MIN = 1;
    public static final int INNER_Z_MAX = 4;

    /** 廊道侧墙的 relZ：西墙 0、东墙 5；relZ ∈ [6, S-1] 为实心间隔区。 */
    public static final int WALL_REL_Z_NEG = 0;
    public static final int WALL_REL_Z_POS = 5;

    public static final int FLOOR_Y = 0;
    public static final int CEILING_Y = 5;
    public static final int BEDROCK_Y = 6;

    /** 廊道内部相对 z（TeleportUtils 出生点沿用）：内部 [1,4] 的中线 2.5。 */
    public static final int SPAWN_Z = 2;

    /** 传送画沿 X 的间隔与锚点偏移（世界 x%LINK_INTERVAL==PASSAGE_X_OFFSET 处挂画）。 */
    public static final int LINK_INTERVAL = 24;
    public static final int PASSAGE_X_OFFSET = 6;

    /** 画锚点 y：4×3 矮画覆盖 y=2..4，4×4 大画覆盖 y=2..5（顶部贴天花板下缘）。 */
    private static final int PAINT_ANCHOR_Y = 2;

    private static final int LAMP_INTERVAL = 8;
    private static final int LAMP_REL_Z = 2;

    /** 装饰画概率；装饰画锚点 x（chunk 内相对）候选，天然避开传送画列 [6,10)。 */
    private static final double DECOR_CHANCE = 0.5;
    private static final double EXIT_FRAME_CHANCE = 0.04;
    private static final int[] DECOR_X = {2, 3, 4, 5, 10, 11, 12};

    public static final MapCodec<TheGalleryGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(TheGalleryGenerator::getBiomeSource)
            ).apply(instance, TheGalleryGenerator::new)
    );

    public TheGalleryGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            int worldX = minBlockX + x;
            boolean lampColumn = Math.floorMod(worldX, LAMP_INTERVAL) == 0;
            for (int lz = 0; lz < 16; lz++) {
                int wz = minBlockZ + lz;
                int relZ = Math.floorMod(wz, CORRIDOR_SPACING);
                boolean interior = relZ >= INNER_Z_MIN && relZ <= INNER_Z_MAX;
                for (int y = this.getMinY(); y <= BEDROCK_Y; y++) {
                    BlockState state;
                    if (y == BEDROCK_Y) {
                        // 基岩封顶：玩家挖不开，够不到上方的虚空
                        state = Blocks.BEDROCK.defaultBlockState();
                    } else if (interior) {
                        if (y == FLOOR_Y || y == CEILING_Y) {
                            state = Blocks.OAK_PLANKS.defaultBlockState();
                        } else if (lampColumn && relZ == LAMP_REL_Z && y == CEILING_Y - 1) {
                            state = Blocks.IRON_CHAIN.defaultBlockState();
                        } else if (lampColumn && relZ == LAMP_REL_Z && y == CEILING_Y - 2) {
                            state = Blocks.LANTERN.defaultBlockState();
                        } else {
                            state = Blocks.CAVE_AIR.defaultBlockState();
                        }
                    } else {
                        // 墙与间隔区：实心木板
                        state = Blocks.OAK_PLANKS.defaultBlockState();
                    }
                    chunk.setBlockState(new BlockPos(x, y, lz), state, Block.UPDATE_NONE);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        ChunkPos chunkPos = worldGenRegion.getCenter();

        long seed = worldGenRegion.getSeed();
        Random random = new Random(seed ^ (chunkPos.x() * 0x9e3779b97f4a7c15L) ^ (chunkPos.z() * 0xdefacedddeedbeefL));

        List<Holder<PaintingVariant>> largeVariants = variants(worldGenRegion, 4, 4);
        List<Holder<PaintingVariant>> passageVariants = variants(worldGenRegion, 4, 3);
        if (passageVariants.isEmpty()) return;

        int minBlockX = chunkPos.getMinBlockX();
        int minBlockZ = chunkPos.getMinBlockZ();

        for (int lz = 0; lz < 16; lz++) {
            int wz = minBlockZ + lz;
            int relZ = Math.floorMod(wz, CORRIDOR_SPACING);
            final boolean westWall;
            if (relZ == WALL_REL_Z_NEG) {
                westWall = true;
            } else if (relZ == WALL_REL_Z_POS) {
                westWall = false;
            } else {
                continue;
            }

            int k = Math.floorDiv(wz, CORRIDOR_SPACING);
            int wallWorldZ = k * CORRIDOR_SPACING + relZ;
            // 画挂在墙的内侧面：锚点 z 向廊道内部偏移 1 格，facing 朝廊道内
            int anchorZ = westWall ? wallWorldZ + 1 : wallWorldZ - 1;
            Direction facing = westWall ? Direction.SOUTH : Direction.NORTH;

            // 传送画列：锚点 x%LINK_INTERVAL==PASSAGE_X_OFFSET（每 24 格一幅，4x3 矮画可穿过）
            for (int lx = 0; lx < 16; lx++) {
                int wx = minBlockX + lx;
                if (Math.floorMod(wx, LINK_INTERVAL) == PASSAGE_X_OFFSET) {
                    placePainting(worldGenRegion, passageVariants,
                            new BlockPos(wx, PAINT_ANCHOR_Y, anchorZ), facing);
                }
            }

            // 装饰画：每面墙每 chunk 至多一幅（4x4 大画，或 4% 概率物品展示框+火把）
            if (random.nextDouble() < DECOR_CHANCE) {
                int decorLx = DECOR_X[random.nextInt(DECOR_X.length)];
                int decorWx = minBlockX + decorLx;
                if (Math.floorMod(decorWx, LINK_INTERVAL) != PASSAGE_X_OFFSET) {
                    BlockPos anchor = new BlockPos(decorWx, PAINT_ANCHOR_Y, anchorZ);
                    if (random.nextDouble() < EXIT_FRAME_CHANCE) {
                        ItemFrame frame = new ItemFrame(worldGenRegion.getLevel(), anchor, facing);
                        worldGenRegion.addFreshEntity(frame);
                        worldGenRegion.getLevel().getServer().execute(() -> {
                            if (frame.isAlive()) {
                                frame.setItem(new ItemStack(Items.PAINTING));
                            }
                        });
                        worldGenRegion.getChunk(anchor.below()).setBlockState(anchor.below(),
                                Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing),
                                Block.UPDATE_NONE);
                    } else {
                        placePainting(worldGenRegion, largeVariants, anchor, facing);
                    }
                }
            }
        }
    }

    private static List<Holder<PaintingVariant>> variants(WorldGenRegion region, int width, int height) {
        return StreamSupport.stream(region.registryAccess()
                        .lookupOrThrow(Registries.PAINTING_VARIANT)
                        .getTagOrEmpty(PaintingVariantTags.PLACEABLE)
                        .spliterator(), false)
                .filter(variant -> variant.value().width() == width && variant.value().height() == height)
                .toList();
    }

    private static void placePainting(WorldGenRegion region,
                                      List<Holder<PaintingVariant>> variants, BlockPos anchor, Direction facing) {
        Optional<Holder<PaintingVariant>> variant = Util.getRandomSafe(variants, region.getRandom());
        if (variant.isEmpty()) return;
        region.addFreshEntity(new Painting(region.getLevel(), anchor, facing, variant.get()));
    }

    @Override
    public int getBaseHeight(int i, int i1, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return 0;
    }
}
