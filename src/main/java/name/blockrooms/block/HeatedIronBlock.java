package name.blockrooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class HeatedIronBlock extends Block {
    public HeatedIronBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!player.fireImmune() && player.getRemainingFireTicks() >= 0) {
            player.igniteForSeconds(16.0F);
        }
        super.attack(state, level, pos, player);
    }
}
