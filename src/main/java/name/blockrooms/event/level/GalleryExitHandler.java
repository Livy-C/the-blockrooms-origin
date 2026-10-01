package name.blockrooms.event.level;

import name.blockrooms.util.ModLevels;
import name.blockrooms.util.TeleportUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class GalleryExitHandler {
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ItemFrame frame && frame.getItem().is(Items.PAINTING))) return;
        Level level = frame.level();
        if (level.isClientSide() || !level.dimension().equals(ModLevels.GALLERY)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        event.setCanceled(true);

        TeleportUtils.teleportPlayer(player, ModLevels.BLOCKLEVEL_0, new Vec3(player.getX(), 1, player.getZ()));
    }
}
