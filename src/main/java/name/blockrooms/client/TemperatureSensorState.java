package name.blockrooms.client;

public final class TemperatureSensorState {

    public static final long STALE_MS = 3000;

    private static volatile float temperature = Float.NaN;
    private static volatile long lastReceivedAt = Long.MIN_VALUE;

    public static void set(float value) {
        temperature = value;
        lastReceivedAt = System.currentTimeMillis();
    }

    public static Float get() {
        if (Float.isNaN(temperature)) {
            return null;
        }
        if (System.currentTimeMillis() - lastReceivedAt > STALE_MS) {
            return null;
        }
        return temperature;
    }

    private TemperatureSensorState() {
    }
}
