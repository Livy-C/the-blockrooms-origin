package name.blockrooms.world.generator;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import name.blockrooms.entity.ModEntities;
import name.blockrooms.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.concurrent.CompletableFuture;

public class BlockLevelNullGenerator extends SimpleGenerator {

    public static final int PLATFORM_HALF = 16;
    /** 平台石头层 y */
    public static final int PLATFORM_Y = 0;
    /** 中心木桶位置（平台正中心上方） */
    public static final BlockPos BARREL_POS = new BlockPos(0, 1, 0);

    /** 虚无之船锚点（平台南方虚空上方，悬浮） */
    public static final BlockPos VOID_BOAT_ORIGIN = new BlockPos(0, 24, 96);
    /** 虚无之船模板 */
    private static final Identifier VOID_BOAT_TEMPLATE = Identifier.fromNamespaceAndPath(Blockrooms.MODID, "void_boat");
    /** 船上黑石潜伏者数量与位置（相对锚点的近似位置，模板 13×23×28） */
    private static final BlockPos[] SHULKER_SPOTS = {
            new BlockPos(6, 6, 8), new BlockPos(2, 6, 18), new BlockPos(10, 6, 18)
    };

    public static final MapCodec<BlockLevelNullGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(BlockLevelNullGenerator::getBiomeSource)
            ).apply(instance, BlockLevelNullGenerator::new)
    );

    public BlockLevelNullGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        for (int x = 0; x < 16; x++) {
            int wx = cp.getMinBlockX() + x;
            if (Math.abs(wx) > PLATFORM_HALF) continue;
            for (int z = 0; z < 16; z++) {
                int wz = cp.getMinBlockZ() + z;
                if (Math.abs(wz) > PLATFORM_HALF) continue;
                chunk.setBlockState(new BlockPos(x, PLATFORM_Y, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_NONE);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // 平台中心放置木桶（SPAWN 阶段禁止 WorldGenRegion.setBlock，直接写 ChunkAccess）
        ChunkPos cp = level.getCenter();
        if (cp.x() == 0 && cp.z() == 0) {
            ChunkAccess chunk = level.getChunk(0, 0);
            BlockPos local = new BlockPos(BARREL_POS.getX(), BARREL_POS.getY(), BARREL_POS.getZ());
            chunk.setBlockState(local, Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (chunk.getBlockEntity(local) instanceof BarrelBlockEntity barrel) {
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    barrel.setItem(i, new ItemStack(ModItems.ALMOND_MILK_BUCKET.get()));
                }
            }
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos cp = chunk.getPos();
        if (cp.x() == VOID_BOAT_ORIGIN.getX() >> 4 && cp.z() == VOID_BOAT_ORIGIN.getZ() >> 4) {
            placeVoidBoat(level, chunk, cp);
        }
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    private void placeVoidBoat(WorldGenLevel level, ChunkAccess chunk, ChunkPos cp) {
        StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();
        StructureTemplate template = manager.get(VOID_BOAT_TEMPLATE).orElse(null);
        if (template == null) {
            Blockrooms.LOGGER.warn("NULL-DIAG: void_boat template missing");
            return;
        }
        BlockPos origin = VOID_BOAT_ORIGIN;
        // 忽略结构方块（数据标记方块不放置，只作定位点）——原版末地船同款 processor
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
        template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_NONE);

        // 拼图方块标记（原版结构方块 DATA 模式已移除，用拼图方块 name 代替）：
        // name 含 "elytra" → 鞘翅展示框；name 含 "sentry" → 黑石潜伏者。
        // 拼图方块本身替换为黑石（船身材质），作为展示框挂点。
        boolean hasMarkers = false;
        for (StructureTemplate.JigsawBlockInfo jigsaw : template.getJigsaws(origin, Rotation.NONE)) {
            BlockPos p = jigsaw.info().pos();
            String name = jigsaw.name().getPath();
            level.setBlock(p, Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_NONE);
            if (name.contains("elytra")) {
                hasMarkers = true;
                ItemFrame frame = new ItemFrame(level.getLevel(), p, Direction.SOUTH);
                frame.setItem(new ItemStack(Items.ELYTRA), false);
                level.addFreshEntity(frame);
            } else if (name.contains("sentry")) {
                hasMarkers = true;
                var shulker = ModEntities.BLACKSTONE_SHULKER.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
                if (shulker != null) {
                    shulker.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                    shulker.setYRot(0);
                    shulker.setXRot(0);
                    level.addFreshEntity(shulker);
                }
            }
        }

        // 模板没有标记时的兜底：固定位置潜伏者 + 扫描鞘翅挂点
        if (!hasMarkers) {
            for (BlockPos spot : SHULKER_SPOTS) {
                BlockPos p = origin.offset(spot);
                var shulker = ModEntities.BLACKSTONE_SHULKER.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
                if (shulker != null) {
                    shulker.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                    shulker.setYRot(0);
                    shulker.setXRot(0);
                    level.addFreshEntity(shulker);
                }
            }
            BlockPos framePos = findFrameSpot(level, origin);
            if (framePos != null) {
                ItemFrame frame = new ItemFrame(level.getLevel(), framePos, Direction.NORTH);
                frame.setItem(new ItemStack(Items.ELYTRA), false);
                level.addFreshEntity(frame);
            }
        }
    }

    private static BlockPos findFrameSpot(WorldGenLevel level, BlockPos origin) {
        for (int z = 27; z >= 10; z--) {
            for (int y = 14; y >= 4; y--) {
                for (int x = 0; x < 13; x++) {
                    BlockPos p = origin.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()
                            && level.getBlockState(p.relative(Direction.NORTH)).isAir()) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }
}
