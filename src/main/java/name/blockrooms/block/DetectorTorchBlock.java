package name.blockrooms.block;

import name.blockrooms.util.ModLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

public class DetectorTorchBlock extends TorchBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public DetectorTorchBlock(SimpleParticleType flameParticle, BlockBehaviour.Properties properties) {
        super(flameParticle, properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            double d0 = (double)pos.getX() + 0.5;
            double d1 = (double)pos.getY() + 0.7;
            double d2 = (double)pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
            level.addParticle(this.flameParticle, d0, d1, d2, 0.0, 0.0, 0.0);
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
        boolean isDay = context.getLevel().isBrightOutside();//context.getLevel().getDayTime() % 24000 < 13000;
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIT, isDay);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        boolean isDay = level.isBrightOutside();//level.getDayTime() % 24000 < 13000;
        if (state.getValue(LIT) != isDay) {
            level.setBlock(pos, state.setValue(LIT, isDay), UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, 20);
    }
}
