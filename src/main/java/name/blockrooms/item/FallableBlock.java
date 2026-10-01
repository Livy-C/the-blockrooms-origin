package name.blockrooms.item;

import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class FallableBlock extends SandBlock {

    public FallableBlock(ColorRGBA dustColor, BlockBehaviour.Properties properties) {
        super(dustColor, properties);
    }
}
