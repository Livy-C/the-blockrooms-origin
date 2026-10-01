package name.blockrooms.util;

import name.blockrooms.block.ModBlocks;
import name.blockrooms.block.TeleporterBlock;
import name.blockrooms.block.entity.TeleporterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Convenience API for placing {@link TeleporterBlock}s in code (structure
 * generation, event handlers, commands, ...). Every method places the block
 * with its default state and wires the {@link TeleporterBlockEntity} data in
 * the {@link TeleportUtils#STANDARD_TARGET} format: a target dimension and an
 * optional absolute destination.
 *
 * <p>When the destination is omitted, the teleporter falls back to the global
 * {@link TeleportUtils#STANDARD_TARGET} transform of the target dimension
 * (and finally to the entity's own position).</p>
 *
 * <p>Usage examples:</p>
 * <pre>
 * TeleporterApi.placeTeleporter(level, pos, ModLevels.BLOCKLEVEL_4);
 * TeleporterApi.placeTeleporter(level, x, y, z, ModLevels.GALLERY, new Vec3(0, 1, 0));
 * TeleporterApi.placeTeleporter(level, 0, 64, 0, Level.OVERWORLD, 0.5, 65, 0.5);
 * </pre>
 */
public final class TeleporterApi {
    private TeleporterApi() {
    }

    /** Places a teleporter targeting {@code targetDimension} (no explicit destination). */
    @Nullable
    public static TeleporterBlockEntity placeTeleporter(Level level, BlockPos pos, ResourceKey<Level> targetDimension) {
        return placeTeleporter(level, pos, targetDimension, null);
    }

    /** Places a teleporter targeting {@code targetDimension} (no explicit destination). */
    @Nullable
    public static TeleporterBlockEntity placeTeleporter(Level level, int x, int y, int z, ResourceKey<Level> targetDimension) {
        return placeTeleporter(level, new BlockPos(x, y, z), targetDimension, null);
    }

    /** Places a teleporter targeting {@code targetDimension} at the given absolute destination. */
    @Nullable
    public static TeleporterBlockEntity placeTeleporter(Level level, BlockPos pos, ResourceKey<Level> targetDimension, @Nullable Vec3 destination) {
        if (level == null || pos == null || targetDimension == null) {
            return null;
        }
        level.setBlock(pos, ModBlocks.TELEPORTER_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        return setTarget(level, pos, new TeleporterBlockEntity.Target(targetDimension, destination));
    }

    /** Places a teleporter targeting {@code targetDimension} at the given absolute destination. */
    @Nullable
    public static TeleporterBlockEntity placeTeleporter(Level level, int x, int y, int z, ResourceKey<Level> targetDimension, @Nullable Vec3 destination) {
        return placeTeleporter(level, new BlockPos(x, y, z), targetDimension, destination);
    }

    /** Places a teleporter targeting {@code targetDimension} at the given absolute destination. */
    @Nullable
    public static TeleporterBlockEntity placeTeleporter(Level level, int x, int y, int z, ResourceKey<Level> targetDimension,
                                                        double destX, double destY, double destZ) {
        return placeTeleporter(level, new BlockPos(x, y, z), targetDimension, new Vec3(destX, destY, destZ));
    }

    /** Replaces the data of an existing teleporter block entity with a single target. */
    @Nullable
    public static TeleporterBlockEntity setTarget(Level level, BlockPos pos, TeleporterBlockEntity.Target target) {
        return setTargets(level, pos, List.of(target));
    }

    /** Replaces the data of an existing teleporter block entity; first entry wins at teleport time. */
    @Nullable
    public static TeleporterBlockEntity setTargets(Level level, BlockPos pos, List<TeleporterBlockEntity.Target> targets) {
        if (level == null || pos == null || targets == null) {
            return null;
        }
        if (!(level.getBlockEntity(pos) instanceof TeleporterBlockEntity blockEntity)) {
            return null;
        }
        blockEntity.setTargets(targets);
        blockEntity.setChanged();
        return blockEntity;
    }
}
