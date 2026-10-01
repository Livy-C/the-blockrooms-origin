package name.blockrooms.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.level.Level;

import java.util.List;

public class FakePainting extends Painting {
    public FakePainting(EntityType<? extends Painting> p_477920_, Level p_480591_) {
        super(p_477920_, p_480591_);
    }

    @Override
    public void tick() {
        super.tick();
        List<ServerPlayer> players = level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox());
        players.forEach(p -> {
            
        });
    }
}
