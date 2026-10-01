package name.blockrooms.world.structure;

import com.mojang.serialization.MapCodec;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class AbandonedCampStructure extends TemplateScatterStructure {
    public static final MapCodec<AbandonedCampStructure> CODEC = simpleCodec(AbandonedCampStructure::new);
    private static final long SALT = 0x1BADC0DE00000001L;

    public AbandonedCampStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, "bl1_abandoned_camp");
    }

    @Override
    protected int sizeX() {
        return 16;
    }

    @Override
    protected int sizeY() {
        return 7;
    }

    @Override
    protected int sizeZ() {
        return 16;
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
    protected long spacing() {
        return 12;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ABANDONED_CAMP_TYPE.get();
    }
}
