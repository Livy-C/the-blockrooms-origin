package name.blockrooms.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * 通用 NBT 模板结构部件：按 {@link #templateId} 从模板管理器加载
 * {@code data/blockrooms/structure/<id>.nbt} 并放置到锚点位置。
 *
 * <p>供所有"模板即结构"的类型共用（废弃营地、村民小屋、木筏、海床洞穴、
 * 虚空之船等），模板 ID 与锚点/朝向随 NBT 序列化，跨区块生成与重载保持一致。
 * 模板缺失时 {@link StructureTemplateManager#getOrCreate} 返回空模板，
 * 部件什么都不生成、不会崩溃。</p>
 *
 * <p>支持跨区块模板：{@code StructureStart} 会对每个与部件包围盒相交的区块
 * 调用 {@link #postProcess}，模板放置按传入的区块 box 裁剪。</p>
 */
public class NbtTemplatePiece extends StructurePiece {
    private static final String TEMPLATE_TAG = "TID";
    private static final String ANCHOR_X_TAG = "AX";
    private static final String ANCHOR_Y_TAG = "AY";
    private static final String ANCHOR_Z_TAG = "AZ";
    private static final String ROTATION_TAG = "ROT";

    private final Identifier templateId;
    private final BlockPos anchor;
    private final Rotation rotation;
    private StructureTemplate template; // 懒加载（首次 postProcess 时从模板管理器读取）

    public NbtTemplatePiece(Identifier templateId, BlockPos anchor, Rotation rotation,
                            int sizeX, int sizeY, int sizeZ) {
        super(ModStructures.NBT_TEMPLATE_PIECE_TYPE.get(), 0,
                BoundingBox.fromCorners(anchor, anchor.offset(sizeX - 1, sizeY - 1, sizeZ - 1)));
        this.templateId = templateId;
        this.anchor = anchor;
        this.rotation = rotation;
    }

    /** 反序列化（经 {@link StructurePieceType.StructureTemplateType} 工厂调用）。 */
    public NbtTemplatePiece(StructureTemplateManager templates, CompoundTag tag) {
        super(ModStructures.NBT_TEMPLATE_PIECE_TYPE.get(), tag);
        this.templateId = Identifier.parse(tag.getStringOr(TEMPLATE_TAG, ""));
        this.anchor = new BlockPos(tag.getIntOr(ANCHOR_X_TAG, 0), tag.getIntOr(ANCHOR_Y_TAG, 0), tag.getIntOr(ANCHOR_Z_TAG, 0));
        this.rotation = Rotation.values()[Math.floorMod(tag.getIntOr(ROTATION_TAG, 0), Rotation.values().length)];
        this.template = templates.getOrCreate(templateId);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString(TEMPLATE_TAG, templateId.toString());
        tag.putInt(ANCHOR_X_TAG, anchor.getX());
        tag.putInt(ANCHOR_Y_TAG, anchor.getY());
        tag.putInt(ANCHOR_Z_TAG, anchor.getZ());
        tag.putInt(ROTATION_TAG, rotation.ordinal());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        StructureTemplate template = this.template;
        if (template == null) {
            StructureTemplateManager manager = ((ServerLevel) level.getLevel()).getStructureManager();
            template = manager.getOrCreate(templateId);
            this.template = template;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setBoundingBox(box);
        template.placeInWorld(level, anchor, anchor, settings, random, Block.UPDATE_CLIENTS);
    }
}
