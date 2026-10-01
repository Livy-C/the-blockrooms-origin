package name.blockrooms.event.client;

import name.blockrooms.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(value = Dist.CLIENT)
public class DynamicLightingHandler {
    private static Map<BlockPos, Integer> lightLevels = new HashMap<>();

    public static int getLightWithOcclusion(BlockAndLightGetter level, BlockPos pos) {
         if (!level.getBlockState(pos).canOcclude()) {
            return lightLevels.getOrDefault(pos, 0);
        }
        return 0;
    }

    private static void setLight(BlockAndLightGetter level, Set<BlockPos> sources, int i) {
        Map<BlockPos, Integer> newLightLevels = new HashMap<>();
        for (BlockPos pos : sources) newLightLevels.put(pos, i);
        for (int l = i - 1; l >= (Minecraft.getInstance().options.ambientOcclusion().get() ? 0 : 1); l--) {
            final int lightFilter = l + 1;
            for (BlockPos currentPos : newLightLevels.entrySet().stream().filter(e -> e.getValue() == lightFilter).map(Map.Entry::getKey).toArray(BlockPos[]::new)) {
                for (Direction dir : Direction.values()) {
                    BlockPos relativePos = currentPos.relative(dir);
                    if (!newLightLevels.containsKey(relativePos) && canLightPass(level, currentPos, relativePos, dir)) {
                        newLightLevels.put(relativePos, l);
                    }
                }
            }
        }
        if (!newLightLevels.equals(lightLevels)) {
            lightLevels = newLightLevels;
            for (BlockPos blockPos : lightLevels.keySet()) {
                setDirty(SectionPos.of(blockPos));
            }
        }
    }

    private static boolean canLightPass(BlockAndLightGetter level, BlockPos currentPos, BlockPos relativePos, Direction dir) {
        return !Shapes.faceShapeOccludes(getShape(level, level.getBlockState(currentPos), currentPos, dir), getShape(level, level.getBlockState(relativePos), relativePos, dir.getOpposite()));
    }

    private static VoxelShape getShape(BlockAndLightGetter level, BlockState state, BlockPos pos, Direction dir) {
        return (state.getLightEmission(level, pos) < 15 && !state.canOcclude()) || !state.isSolid() ? Shapes.empty() : state.getFaceOcclusionShape(dir);
    }

    public static void setDirty(SectionPos section) {
        Minecraft.getInstance().levelExtractor.setSectionDirty(section.getX(), section.getY(), section.getZ());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        if (player != null && level != null) {
            Set<BlockPos> sources = new HashSet<>();

            BlockPos pos = BlockPos.containing(player.getEyePosition());
            if (isPlayerLightSource(player)) {
                setLight(level, Set.of(pos), 10);
            } else if (!lightLevels.isEmpty()) {
                lightLevels = new HashMap<>();
                for (SectionPos sectionPos : SectionPos.cube(SectionPos.of(pos), 1).toArray(SectionPos[]::new)) {
                    setDirty(sectionPos);
                }
            }
        }
    }

    private static boolean isPlayerLightSource(Player player) {
        boolean flag = false;
        for (InteractionHand hand : InteractionHand.values()) {
            flag = flag || (player.getItemInHand(hand).is(ModItems.GLOWSTONE_LANTERN)
                    && !player.getItemInHand(hand).nextDamageWillBreak());
        }
        return flag;
    }
}
