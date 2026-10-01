package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 虚空之船（黑石大船，含梯子、末地烛与酿造台，跨区块 13×23×28）。
 * 模板：{@code data/blockrooms/structure/void_boat.nbt}。
 * 只注册、暂不自然生成——预留给 The Void 维度；维度实装后为它添加
 * structure_set 并改为间距门控即可自然生成。
 */
public class VoidBoatStructure extends TemplateScatterStructure {
    public static final MapCodec<VoidBoatStructure> CODEC = simpleCodec(VoidBoatStructure::new);
    private static final long SALT = 0x1BADC0DE00000005L;

    public VoidBoatStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "void_boat");
    }

    @Override
    protected int sizeX() {
        return 13;
    }

    @Override
    protected int sizeY() {
        return 23;
    }

    @Override
    protected int sizeZ() {
        return 28;
    }

    @Override
    protected int anchorYOffset() {
        return 0;
    }

    @Override
    protected long salt() {
        return SALT;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.VOID_BOAT_TYPE.get();
    }
}
