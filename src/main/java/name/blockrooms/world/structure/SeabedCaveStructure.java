package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 海床洞穴（圆石/苔石洞穴，含岩浆块与气泡柱）。
 * 模板：{@code data/blockrooms/structure/seabed_cave.nbt}（12×13×12）。
 * 只注册、暂不自然生成；锚点低于 OCEAN_FLOOR 高度图 8 格，使洞穴主体埋在
 * 河床/海床之下，顶部露出。
 */
public class SeabedCaveStructure extends TemplateScatterStructure {
    public static final MapCodec<SeabedCaveStructure> CODEC = simpleCodec(SeabedCaveStructure::new);
    private static final long SALT = 0x1BADC0DE00000004L;

    public SeabedCaveStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "seabed_cave");
    }

    @Override
    protected int sizeX() {
        return 12;
    }

    @Override
    protected int sizeY() {
        return 13;
    }

    @Override
    protected int sizeZ() {
        return 12;
    }

    @Override
    protected int anchorYOffset() {
        return -8; // 埋入河床/海床，顶部露出
    }

    @Override
    protected long salt() {
        return SALT;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SEABED_CAVE_TYPE.get();
    }
}
