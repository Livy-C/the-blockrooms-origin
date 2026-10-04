package name.blockrooms.util;

import name.blockrooms.world.generator.TheGalleryGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

public class TeleportUtils {
    public static final Map<ResourceKey<Level>, UnaryOperator<Vec3>> STANDARD_TARGET = new HashMap<>();

    public static boolean teleportPlayer(Entity entity, ResourceKey<Level> level) {
        return teleportPlayer(entity, level, entity.position());
    }

    public static boolean teleportPlayer(Entity entity, ResourceKey<Level> level, Vec3 pos) {
        return teleportPlayer(entity, level, pos, true);
    }

    public static boolean teleportPlayer(Entity entity, ResourceKey<Level> level, Vec3 pos, boolean doFindSafeSpot) {
        if (entity instanceof ServerPlayer &&
                (ModLevels.isInBlockrooms(entity.level().dimension()) ^ ModLevels.isInBlockrooms(level))) {
            Minecraft.getInstance().reloadResourcePacks();
        }

        if (STANDARD_TARGET.containsKey(level)) {
            pos = STANDARD_TARGET.get(level).apply(pos);
        }
        return teleportTo(entity, level, pos, doFindSafeSpot);
    }

    private static boolean teleportTo(Entity entity, ResourceKey<Level> level, Vec3 pos, boolean doFindSafeSpot) {
        ServerLevel target = Objects.requireNonNull(entity.level().getServer().getLevel(level));
        if (doFindSafeSpot) {
            BlockPos safe = TeleportUtils.findSafeSpot(target, BlockPos.containing(pos));
            if (safe != null) return teleportTo(entity, target, Vec3.atBottomCenterOf(safe));
        }
        return teleportTo(entity, target, pos);
    }

    private static boolean teleportTo(Entity entity, ServerLevel level, Vec3 vec) {
        return entity.teleportTo(level, vec.x, vec.y, vec.z, Set.of(), entity.getYRot(), entity.getXRot(), true);
    }

    @Nullable
    public static BlockPos findSafeSpot(Level level, BlockPos target) {
        for (int dy = -2; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = target.offset(dx, dy, dz);
                    if (isSafe(level, pos)) return pos;
                }
            }
        }
        return null;
    }

    public static boolean isSafe(Level level, BlockPos pos) {
        return !level.getBlockState(pos).isSolid()
                && !level.getBlockState(pos.above()).isSolid()
                && level.getBlockState(pos.below()).isSolid();
    }

    static {
        STANDARD_TARGET.put(Level.OVERWORLD, pos -> new Vec3(pos.x(), 64, pos.z()));
        STANDARD_TARGET.put(Level.END, pos -> new Vec3(0, 4, 0));
        STANDARD_TARGET.put(ModLevels.BLOCKLEVEL_0, pos -> new Vec3(pos.x(), 64 + 1, pos.z()));
        STANDARD_TARGET.put(ModLevels.BLOCKLEVEL_1, pos -> new Vec3(pos.x(), 1, pos.z()));
        STANDARD_TARGET.put(ModLevels.BLOCKLEVEL_2, pos -> new Vec3(pos.x(), 1, pos.z()));
        STANDARD_TARGET.put(ModLevels.BLOCKLEVEL_3, pos -> new Vec3(pos.x(), 1, pos.z()));
        STANDARD_TARGET.put(ModLevels.BLOCKLEVEL_NULL, pos -> new Vec3(0, 1, 0));
        STANDARD_TARGET.put(ModLevels.BLOCKLEVEL_4, pos -> new Vec3(pos.x(), 64, pos.z()));
        STANDARD_TARGET.put(ModLevels.GALLERY, pos -> new Vec3(2, 1, TheGalleryGenerator.SPAWN_Z));

    }
}
