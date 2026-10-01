package name.blockrooms.event.level;

import name.blockrooms.util.ModLevels;
import name.blockrooms.util.TeleportUtils;
import name.blockrooms.world.generator.TheGalleryGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Set;

@EventBusSubscriber
public class GalleryPassageHandler {
    private static final String LAST_PASSAGE_TAG = "blockrooms.last_gallery_passage";
    private static final int COOLDOWN_TICKS = 20;

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Painting painting)) return;
        Level level = painting.level();
        if (level.isClientSide() || !level.dimension().equals(ModLevels.GALLERY)) return;
        var variant = painting.getVariant().value();
        if (variant.width() != 4 || variant.height() != 3) return;

        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, painting.getBoundingBox().inflate(0.25));
        if (players.isEmpty()) return;
        ServerPlayer player = players.getFirst();

        long now = level.getGameTime();
        if (now - player.getPersistentData().getLongOr(LAST_PASSAGE_TAG, 0L) < COOLDOWN_TICKS) return;
        player.getPersistentData().putLong(LAST_PASSAGE_TAG, now);

        // 传送画挂在廊道侧墙内侧：西墙画 facing=SOUTH、东墙画 facing=NORTH
        // 传送到相邻廊道（保持 x/y，z ± CORRIDOR_SPACING，落在相邻廊道内部中线）
        boolean westWall = painting.getDirection() == Direction.SOUTH;
        int k = Math.floorDiv(painting.getBlockZ(), TheGalleryGenerator.CORRIDOR_SPACING);
        int targetK = k + (westWall ? -1 : 1);
        Vec3 destination = new Vec3(
                painting.getBlockX() + 0.5,
                1,
                targetK * TheGalleryGenerator.CORRIDOR_SPACING + TheGalleryGenerator.SPAWN_Z + 0.5);
        BlockPos safe = TeleportUtils.findSafeSpot(level, BlockPos.containing(destination));
        if (safe != null) {
            destination = Vec3.atBottomCenterOf(safe);
        }
        player.teleportTo((ServerLevel) level, destination.x, destination.y, destination.z,
                Set.of(), player.getYRot(), player.getXRot(), true);
    }
}
