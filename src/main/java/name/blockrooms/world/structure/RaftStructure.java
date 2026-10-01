package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 木筏（橡木筏，含酿造台与红石灯）。
 * 模板：{@code data/blockrooms/structure/raft.nbt}（9×6×6）。
 * 只注册、暂不自然生成；锚点略高于 OCEAN_FLOOR 高度图，便于放在水面附近。
 */
public class RaftStructure extends TemplateScatterStructure {
    public static final MapCodec<RaftStructure> CODEC = simpleCodec(RaftStructure::new);
    private static final long SALT = 0x1BADC0DE00000003L;

    public RaftStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "raft");
    }

    @Override
    protected int sizeX() {
        return 9;
    }

    @Override
    protected int sizeY() {
        return 6;
    }

    @Override
    protected int sizeZ() {
        return 6;
    }

    @Override
    protected int anchorYOffset() {
        return 1; // 贴水底/地面之上
    }

    @Override
    protected long salt() {
        return SALT;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.RAFT_TYPE.get();
    }
}
