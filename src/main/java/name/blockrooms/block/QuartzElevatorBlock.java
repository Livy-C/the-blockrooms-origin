package name.blockrooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class QuartzElevatorBlock extends Block {
    public QuartzElevatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; ++i) {
            double d = (double) pos.getX() + 0.5D + (0.5D - random.nextDouble());
            double e = (double) pos.getY() + 0.5D + random.nextDouble();
            double f = (double) pos.getZ() + 0.5D + (0.5D - random.nextDouble());
            double g = 0.0D;
            double h = random.nextDouble() * 0.0625D;
            double l = 0.0D;
            level.addParticle(ParticleTypes.REVERSE_PORTAL, d, e, f, g, h, l);
        }
    }
}
