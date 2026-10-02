package io.github.taikun24.bettertnt;

/**
 * Switches for the exact (result-preserving) explosion optimizations, refreshed from the config every tick
 * so mixin hot paths only read a static boolean.
 */
public final class Optimizations {
    public static boolean fastRays = true;
    public static boolean fastExposure = true;
    public static boolean fastTntCollisions = true;

    private Optimizations() {
    }

    public static void onServerTickStart() {
        try {
            fastRays = TNTConfig.FAST_RAYS.get();
            fastExposure = TNTConfig.FAST_EXPOSURE.get();
            fastTntCollisions = TNTConfig.FAST_TNT_COLLISIONS.get();
        } catch (RuntimeException ignored) {
            // Server config not loaded yet; keep the previous values.
        }
    }
}
