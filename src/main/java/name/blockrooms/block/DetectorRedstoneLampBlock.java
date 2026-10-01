package name.blockrooms.block;

import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class DetectorRedstoneLampBlock extends RedstoneLampBlock {
    public DetectorRedstoneLampBlock(Properties properties) {
        super(properties);
    }
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean shouldLit = calculateShouldLit(level, pos);
        return defaultBlockState().setValue(LIT, shouldLit);
    }
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !(level.dimension() == ModLevels.BLOCKLEVEL_1)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            boolean shouldLit = calculateShouldLit(level, pos);
            if (state.getValue(LIT) != shouldLit) {
                level.setBlock(pos, state.setValue(LIT, shouldLit), UPDATE_CLIENTS);
            }
            if (!level.isClientSide() && !(level.dimension() == ModLevels.BLOCKLEVEL_1)) {
                level.scheduleTick(pos, this, 20);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean shouldLit = calculateShouldLit(level, pos);
        if (state.getValue(LIT) != shouldLit) {
            level.setBlock(pos, state.setValue(LIT, shouldLit), UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, 20);
    }

    public boolean calculateShouldLit(Level level, BlockPos pos) {
        if (level.hasNeighborSignal(pos)) {
            return true;
        }
        return level.isBrightOutside();//level.getDayTime() % 24000 < 13000;
    }
}