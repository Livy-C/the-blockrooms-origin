package name.blockrooms.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import name.blockrooms.Blockrooms;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class Bl3TemplateStructure extends TemplateScatterStructure {

    private final String template;
    private final int sx;
    private final int sy;
    private final int sz;
    private final int yOffset;
    private final long salt;

    public Bl3TemplateStructure(StructureSettings settings, String template,
                                int sx, int sy, int sz, int yOffset, long salt) {
        super(settings);
        this.template = template;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.yOffset = yOffset;
        this.salt = salt;
    }

    public static MapCodec<Bl3TemplateStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Codec.STRING.fieldOf("template").forGetter(s -> s.template),
            Codec.INT.fieldOf("sx").forGetter(s -> s.sx),
            Codec.INT.fieldOf("sy").forGetter(s -> s.sy),
            Codec.INT.fieldOf("sz").forGetter(s -> s.sz),
            Codec.INT.optionalFieldOf("y_offset", 0).forGetter(s -> s.yOffset),
            Codec.LONG.optionalFieldOf("salt", 0x1BADC0DE00000021L).forGetter(s -> s.salt)
    ).apply(instance, Bl3TemplateStructure::new));

    @Override
    protected Identifier templateId() {
        return Identifier.fromNamespaceAndPath(Blockrooms.MODID, template);
    }

    @Override
    protected int sizeX() {
        return sx;
    }

    @Override
    protected int sizeY() {
        return sy;
    }

    @Override
    protected int sizeZ() {
        return sz;
    }

    @Override
    protected int anchorYOffset() {
        return yOffset;
    }

    @Override
    protected long salt() {
        return salt;
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.BL3_TEMPLATE_TYPE.get();
    }
}
