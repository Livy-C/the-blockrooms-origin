package name.blockrooms.block.entity;

import name.blockrooms.Blockrooms;
import name.blockrooms.util.TeleportUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Block entity of the {@link name.blockrooms.block.TeleporterBlock}. Stores the
 * teleport destination data in the same shape as
 * {@link TeleportUtils#STANDARD_TARGET} (target dimension -> destination).
 *
 * <p>NBT layout (compatible with structure NBT, so map builders can embed it):</p>
 * <pre>
 * {
 *   "targets": [
 *     { "dimension": "blockrooms:blocklevel4", "pos": { "x": 0.0, "y": 64.0, "z": 0.0 } },
 *     { "dimension": "minecraft:overworld" }
 *   ]
 * }
 * </pre>
 *
 * <p>Entries are tried in order; the first valid one wins. When {@code pos} is
 * absent the global {@link TeleportUtils#STANDARD_TARGET} transform for that
 * dimension is used (falling back to the entity's own position).</p>
 */
public class TeleporterBlockEntity extends BlockEntity {
    public static final String TARGETS_TAG = "targets";
    private static final String DIMENSION_TAG = "dimension";
    private static final String POS_TAG = "pos";

    private final List<Target> targets = new ArrayList<>();

    public TeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEPORTER.get(), pos, state);
    }

    /** Immutable teleport target, mirroring one entry of {@link TeleportUtils#STANDARD_TARGET}. */
    public record Target(ResourceKey<Level> dimension, @Nullable Vec3 position) {
    }

    public List<Target> getTargets() {
        return targets;
    }

    /** Convenience: single target without an explicit destination (uses STANDARD_TARGET transform). */
    public void setTarget(ResourceKey<Level> dimension) {
        setTargets(List.of(new Target(dimension, null)));
    }

    /** Convenience: single target with an absolute destination. */
    public void setTarget(ResourceKey<Level> dimension, @Nullable Vec3 position) {
        setTargets(List.of(new Target(dimension, position)));
    }

    public void setTargets(List<Target> newTargets) {
        targets.clear();
        targets.addAll(newTargets);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput.ValueOutputList list = output.childrenList(TARGETS_TAG);
        for (Target target : targets) {
            ValueOutput entry = list.addChild();
            entry.putString(DIMENSION_TAG, target.dimension().identifier().toString());
            if (target.position() != null) {
                ValueOutput pos = entry.child(POS_TAG);
                pos.putDouble("x", target.position().x());
                pos.putDouble("y", target.position().y());
                pos.putDouble("z", target.position().z());
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        targets.clear();
        for (ValueInput entry : input.childrenListOrEmpty(TARGETS_TAG)) {
            ResourceKey<Level> dimension = entry.getString(DIMENSION_TAG)
                    .map(TeleporterBlockEntity::parseDimension)
                    .orElse(null);
            if (dimension == null) {
                continue;
            }
            Vec3 position = entry.child(POS_TAG)
                    .map(pos -> new Vec3(pos.getDoubleOr("x", 0), pos.getDoubleOr("y", 0), pos.getDoubleOr("z", 0)))
                    .orElse(null);
            targets.add(new Target(dimension, position));
        }
    }

    private static ResourceKey<Level> parseDimension(String value) {
        try {
            return ResourceKey.create(Registries.DIMENSION, Identifier.parse(value));
        } catch (Exception e) {
            Blockrooms.LOGGER.warn("Teleporter block entity has invalid dimension '{}'", value);
            return null;
        }
    }
}
