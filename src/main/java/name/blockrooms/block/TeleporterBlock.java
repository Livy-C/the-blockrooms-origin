package name.blockrooms.block;

import name.blockrooms.block.entity.TeleporterBlockEntity;
import name.blockrooms.util.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Structure-void-like teleporter: no collision volume, pure black texture.
 * Any player or entity whose bounding box overlaps the block is teleported
 * according to the data stored in its {@link TeleporterBlockEntity}, which
 * mirrors the {@link TeleportUtils#STANDARD_TARGET} format (target dimension
 * -> destination). The first valid entry wins.
 */
public class TeleporterBlock extends Block implements EntityBlock {
    /** Persistent-data key holding the last teleport tick per entity (cooldown). */
    public static final String LAST_USE_TAG = "blockrooms.last_teleporter_use";
    /** Minimum ticks between two teleports of the same entity. */
    private static final int COOLDOWN_TICKS = 10;

    public TeleporterBlock(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeleporterBlockEntity(pos, state);
    }

    /**
     * The block has no physical collision, but must still register entity
     * overlaps: returning a full shape makes the entity-tracking system call
     * {@link #entityInside} while an entity is inside the cell.
     */
    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return Shapes.block();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean isInside) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof TeleporterBlockEntity blockEntity)) {
            return;
        }
        List<TeleporterBlockEntity.Target> targets = blockEntity.getTargets();
        if (targets.isEmpty()) {
            return;
        }
        TeleporterBlockEntity.Target target = targets.get(0);
        ServerLevel targetLevel = serverLevel.getServer().getLevel(target.dimension());
        if (targetLevel == null) {
            return;
        }

        // Per-entity cooldown: entityInside fires every tick while overlapping.
        CompoundTag data = entity.getPersistentData();
        long now = serverLevel.getGameTime();
        if (now - data.getLongOr(LAST_USE_TAG, 0L) < COOLDOWN_TICKS) {
            return;
        }
        data.putLong(LAST_USE_TAG, now);

        if (entity instanceof ServerPlayer player) {
            if (target.position() != null) {
                TeleportUtils.teleportPlayer(player, target.dimension(), target.position());
            } else {
                TeleportUtils.teleportPlayer(player, target.dimension());
            }
            return;
        }

        // Non-player entities: stored position wins, otherwise STANDARD_TARGET
        // transform, otherwise the entity's own position; always seek a safe spot.
        Vec3 destination = target.position() != null ? target.position() : entity.position();
        TeleportUtils.teleportPlayer(entity, target.dimension(), destination, true);
        /*
        if (TeleportUtils.STANDARD_TARGET.containsKey(target.dimension())) {
            destination = TeleportUtils.STANDARD_TARGET.get(target.dimension()).apply(destination);
        }
        BlockPos safe = TeleportUtils.findSafeSpot(targetLevel, BlockPos.containing(destination));
        if (safe != null) {
            destination = safe;
        }
        entity.teleportTo(targetLevel, destination.x, destination.y, destination.z,
                Set.of(), entity.getYRot(), entity.getXRot(), false);
         */
    }
}
