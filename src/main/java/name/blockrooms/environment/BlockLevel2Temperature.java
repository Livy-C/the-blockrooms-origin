package name.blockrooms.environment;

import name.blockrooms.item.HeatInsulationArmor;
import name.blockrooms.world.generator.BlockLevel2Generator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Predicate;


public final class BlockLevel2Temperature {
    public static final float BASE_TEMPERATURE = 37.0F;
    public static final float DAMAGE_THRESHOLD = 40.0F;
    public static final float ENCLOSED_BONUS = 8.0F;
    public static final float MACHINE_RADIUS = 400.0F;
    public static final float MACHINE_EDGE_OVERHEAT = 4.0F;
    public static final float MACHINE_PEAK_OVERHEAT = 11.0F;
    public static final float DAMAGE_PER_HIT = 1.0F;
    public static final float PROTECTION_PER_PIECE = 0.25F;
    public static final int MAX_INTERVAL = 60;
    public static final int MIN_INTERVAL = 10;
    public static final Predicate<ServerPlayer> CAN_BE_HURT = player ->
            !player.isCreative() && !player.isSpectator() && !player.isDeadOrDying() && !player.isInvulnerable();
    public static final Predicate<ServerPlayer> IS_OVERHEATED = player ->
            temperatureAt(player.level(), player.blockPosition()) > DAMAGE_THRESHOLD;
    public static final Consumer<ServerPlayer> APPLY_HEAT_DAMAGE = player -> {
        int worn = HeatInsulationArmor.wornCount(player);
        float multiplier = Math.max(0.0F, 1.0F - PROTECTION_PER_PIECE * worn);
        var sl = player.level();
        if (multiplier <= 0.0F) {
            return;
        }
        player.hurtServer(sl, player.damageSources().onFire(), DAMAGE_PER_HIT * multiplier);
        Vec3 eye = player.getEyePosition();
        sl.sendParticles(new DustParticleOptions(0xFFFF5533, 1.0F),
                eye.x, eye.y, eye.z, 6, 0.25, 0.25, 0.25, 0.0);
    };

    public static float temperatureAt(Level level, BlockPos pos) {
        float t = BASE_TEMPERATURE + machineHeat(level, pos);
        if (isEnclosed(level, pos)) {
            t += ENCLOSED_BONUS;
        }
        return t;
    }

    private static float machineHeat(Level level, BlockPos pos) {
        BlockPos machine = nearestMachine(level, pos);
        if (machine == null) {
            return 0.0F;
        }
        double d = Math.hypot(pos.getX() - machine.getX(), pos.getZ() - machine.getZ());
        if (d >= MACHINE_RADIUS) {
            return 0.0F;
        }
        return MACHINE_EDGE_OVERHEAT + (MACHINE_PEAK_OVERHEAT - MACHINE_EDGE_OVERHEAT) * (float) (1.0 - d / MACHINE_RADIUS);
    }
    private static BlockPos nearestMachine(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) {
            return null;
        }
        long seed = sl.getSeed();
        int cx0 = pos.getX() >> 4;
        int cz0 = pos.getZ() >> 4;
        int r = (int) Math.ceil(MACHINE_RADIUS / 16.0);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int cx = cx0 - r; cx <= cx0 + r; cx++) {
            for (int cz = cz0 - r; cz <= cz0 + r; cz++) {
                if (!BlockLevel2Generator.isHeatMachineChunk(seed, cx, cz)) {
                    continue;
                }
                BlockPos m = BlockLevel2Generator.heatMachineCenter(cx, cz);
                double d = Math.hypot(pos.getX() - m.getX(), pos.getZ() - m.getZ());
                if (d < bestD) {
                    bestD = d;
                    best = m;
                }
            }
        }
        return best;
    }
    public static boolean isEnclosed(Level level, BlockPos pos) {
        int blocked = 0;
        if (directionBlocked(level, pos, 1, 0)) blocked++;
        if (directionBlocked(level, pos, -1, 0)) blocked++;
        if (directionBlocked(level, pos, 0, 1)) blocked++;
        if (directionBlocked(level, pos, 0, -1)) blocked++;
        return blocked >= 3;
    }
    private static boolean directionBlocked(Level level, BlockPos pos, int dx, int dz) {
        for (int i = 1; i <= 3; i++) {
            if (!level.getBlockState(pos.offset(dx * i, 0, dz * i)).isAir()) {
                return true;
            }
        }
        return false;
    }

    public static int damageInterval(float temperature) {
        if (temperature <= DAMAGE_THRESHOLD) {
            return Integer.MAX_VALUE;
        }
        int interval = MAX_INTERVAL - Math.round((temperature - DAMAGE_THRESHOLD) * 5.0F);
        return Math.clamp(interval, MIN_INTERVAL, MAX_INTERVAL);
    }

    public static void onPlayerTick(ServerPlayer player) {
        float temperature = temperatureAt(player.level(), player.blockPosition());
        int interval = damageInterval(temperature);
        if (player.tickCount % interval != 0) {
            return;
        }
        if (CAN_BE_HURT.and(IS_OVERHEATED).test(player)) {
            APPLY_HEAT_DAMAGE.accept(player);
        }
    }

    private BlockLevel2Temperature() {
    }
}
