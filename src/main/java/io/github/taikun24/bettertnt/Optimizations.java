package io.github.taikun24.bettertnt;

/**
 * Switches read by mixin hot paths, refreshed from the config every tick so they only read a static field.
 */
public final class Optimizations {
    // Exact (result-preserving).
    public static boolean fastRays = true;
    public static boolean fastExposure = true;
    public static boolean fastTntCollisions = true;

    // Not vanilla.
    public static boolean leanPackets = true;
    public static int maxEffectsPerPlayer = 0;
    public static int rayGrid = 16;

    private Optimizations() {
    }

    public static void onServerTickStart() {
        try {
            fastRays = TNTConfig.FAST_RAYS.get();
            fastExposure = TNTConfig.FAST_EXPOSURE.get();
            fastTntCollisions = TNTConfig.FAST_TNT_COLLISIONS.get();
            leanPackets = TNTConfig.LEAN_EXPLOSION_PACKETS.get();
            maxEffectsPerPlayer = TNTConfig.MAX_EFFECTS_PER_PLAYER_PER_TICK.get();
            rayGrid = TNTConfig.EXPLOSION_RAY_GRID.get();
        } catch (RuntimeException ignored) {
            // Server config not loaded yet; keep the previous values.
        }
    }
}
