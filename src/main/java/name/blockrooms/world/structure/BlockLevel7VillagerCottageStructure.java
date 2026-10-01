package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 村民小屋（草地上的橡木小屋，预留给 BlockLevel 7）。
 * 模板：{@code data/blockrooms/structure/blocklevel7_villager_cottage.nbt}（9×8×9）。
 * 只注册、暂不自然生成——BlockLevel 7 维度实装后，为它添加 structure_set 并
 * 把 {@code findGenerationPoint} 改为间距门控即可自然生成。
 */
public class BlockLevel7VillagerCottageStructure extends TemplateScatterStructure {
    public static final MapCodec<BlockLevel7VillagerCottageStructure> CODEC = simpleCodec(BlockLevel7VillagerCottageStructure::new);
    private static final long SALT = 0x1BADC0DE00000002L;

    public BlockLevel7VillagerCottageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "blocklevel7_villager_cottage");
    }

    @Override
    protected int sizeX() {
        return 9;
    }

    @Override
    protected int sizeY() {
        return 8;
    }

    @Override
    protected int sizeZ() {
        return 9;
    }

    @Override
    protected int anchorYOffset() {
        return 0; // 模板 y=0 是草方块，锚点贴地面
    }

    @Override
    protected long salt() {
        return SALT;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BL7_COTTAGE_TYPE.get();
    }
}
