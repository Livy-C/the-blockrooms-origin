package name.blockrooms.block;

import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class DetectorWallTorchBlock extends WallTorchBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public DetectorWallTorchBlock(SimpleParticleType flameParticle, Properties properties) {
        super(flameParticle, properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            Direction direction = state.getValue(FACING);
            double d0 = (double)pos.getX() + 0.5;
            double d1 = (double)pos.getY() + 0.7;
            double d2 = (double)pos.getZ() + 0.5;
            Direction direction1 = direction.getOpposite();
            level.addParticle(ParticleTypes.SMOKE, d0 + 0.27 * (double)direction1.getStepX(), d1 + 0.22, d2 + 0.27 * (double)direction1.getStepZ(), 0.0, 0.0, 0.0);
            level.addParticle(this.flameParticle, d0 + 0.27 * (double)direction1.getStepX(), d1 + 0.22, d2 + 0.27 * (double)direction1.getStepZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !(level.dimension() == ModLevels.BLOCKLEVEL_1)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean isDay = context.getLevel().isBrightOutside();
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        return state.setValue(LIT, isDay);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        boolean isDay = level.isBrightOutside();
        if (state.getValue(LIT) != isDay) {
            level.setBlock(pos, state.setValue(LIT, isDay), UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, 20);
    }
}
