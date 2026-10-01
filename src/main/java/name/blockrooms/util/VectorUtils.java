package name.blockrooms.util;

import net.minecraft.world.phys.Vec3;

public class VectorUtils {
    public static double horizontalDisSqr(Vec3 vec1, Vec3 vec2){
        double d0 = vec1.x - vec2.x;
        double d1 = vec1.z - vec2.z;
        return d0 * d0 + d1 * d1;
    }

    public static Vec3 directionTo(Vec3 from, Vec3 to, float speed) {
        return to.subtract(from).normalize().scale(speed);
    }

}
