package io.github.taikun24.bettertnt;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TNTConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue THROTTLE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue MAX_TICK_MILLIS;
    public static final ForgeConfigSpec.IntValue MAX_EXPLOSIONS_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue MAX_EXPLOSION_MILLIS_PER_TICK;
    public static final ForgeConfigSpec.IntValue MIN_EXPLOSIONS_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue TNT_WAITS;
    public static final ForgeConfigSpec.BooleanValue FLUSH_ON_SHUTDOWN;

    public static final ForgeConfigSpec.BooleanValue EXPOSURE_CACHE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue FAST_RAYS;
    public static final ForgeConfigSpec.BooleanValue FAST_EXPOSURE;
    public static final ForgeConfigSpec.BooleanValue FAST_TNT_COLLISIONS;

    public static final ForgeConfigSpec.BooleanValue LEAN_EXPLOSION_PACKETS;
    public static final ForgeConfigSpec.IntValue MAX_EFFECTS_PER_PLAYER_PER_TICK;
    public static final ForgeConfigSpec.IntValue EXPLOSION_RAY_GRID;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Holds explosions back when the server cannot keep up, and runs them on later ticks.",
                "Explosions that are held back happen later than in vanilla; nothing else about them changes.").push("throttle");
        THROTTLE_ENABLED = builder
                .comment("Enable explosion throttling.")
                .define("enabled", true);
        MAX_TICK_MILLIS = builder
                .comment("Hold explosions back once the current server tick has taken this long (milliseconds). 0 = off.",
                        "A tick has 50ms. While the server keeps up, no explosion is ever held back, so behaviour stays vanilla.")
                .defineInRange("maxTickMillis", 40.0, 0.0, 1000.0);
        MAX_EXPLOSIONS_PER_TICK = builder
                .comment("Also hold back once this many explosions ran in one tick, even if the server keeps up. 0 = off.",
                        "Setting this makes TNT behave differently from vanilla even on a fast server.")
                .defineInRange("maxExplosionsPerTick", 0, 0, 1_000_000);
        MAX_EXPLOSION_MILLIS_PER_TICK = builder
                .comment("Also hold back once explosions used this much time (milliseconds) in one tick. 0 = off.")
                .defineInRange("maxExplosionMillisPerTick", 0.0, 0.0, 1000.0);
        MIN_EXPLOSIONS_PER_TICK = builder
                .comment("Always let at least this many explosions through per tick, so they keep progressing",
                        "even when the server lags for other reasons.")
                .defineInRange("minExplosionsPerTick", 4, 1, 1_000_000);
        TNT_WAITS = builder
                .comment("false (default): held-back TNT vanishes and its explosion is queued; it later happens exactly where",
                        "and as the TNT would have exploded. Cheapest, but the TNT is invisible meanwhile and a crash loses the queue.",
                        "true: held-back TNT stays in the world (visible, about to blow, saved with the world) and explodes where it",
                        "is once there is room. Meanwhile other explosions keep pushing it, so it can land far from where vanilla",
                        "would have blown it up, and the extra entities cost noticeably more TPS.")
                .define("tntWaitsInWorld", false);
        FLUSH_ON_SHUTDOWN = builder
                .comment("Run every queued explosion when the server stops. If false, queued explosions are discarded.",
                        "(Waiting TNT is saved with the world either way.)")
                .define("flushOnShutdown", true);
        builder.pop();

        builder.comment("Caches how much of an entity an explosion can see. Big win for stacked TNT and TNT in water.").push("exposureCache");
        EXPOSURE_CACHE_ENABLED = builder
                .comment("Enable the exposure cache.")
                .define("enabled", true);
        builder.pop();

        builder.comment("Faster versions of vanilla explosion code. They give exactly the vanilla result;",
                "turn one off only if another mod that changes explosions misbehaves.").push("optimizations");
        FAST_RAYS = builder
                .comment("Reuse block lookups while an explosion's rays step through the same block.")
                .define("fastExplosionRays", true);
        FAST_EXPOSURE = builder
                .comment("Faster raycasts when checking how much of an entity an explosion can see (skips air, direct chunk access).")
                .define("fastExposureRaycast", true);
        FAST_TNT_COLLISIONS = builder
                .comment("Moving primed TNT only checks the few entity types that can be collided with (boats, shulkers, ...)",
                        "instead of every entity around it.")
                .define("fastTntCollisions", true);
        builder.pop();

        builder.comment("NOT VANILLA: what players see and hear. Does not change what happens in the world.").push("clientEffects");
        LEAN_EXPLOSION_PACKETS = builder
                .comment("Send explosion packets without their list of broken blocks. Clients then remove those blocks",
                        "from the normal block update instead (up to one tick later) rather than doing it twice.",
                        "Saves bandwidth and client work; the visual difference is hard to notice.")
                .define("leanExplosionPackets", true);
        MAX_EFFECTS_PER_PLAYER_PER_TICK = builder
                .comment("At most this many explosion sounds/particles per player per tick. Explosions that push the",
                        "player are always sent (the push needs the packet). 0 = unlimited (vanilla).")
                .defineInRange("maxExplosionEffectsPerPlayerPerTick", 0, 0, 10_000);
        builder.pop();

        builder.comment("NOT VANILLA: changes the outcome of explosions.").push("gameplay");
        EXPLOSION_RAY_GRID = builder
                .comment("Rays per cube face edge an explosion casts to find blocks to break. Vanilla is 16 (1352 rays).",
                        "12 casts 728 rays (about 1.8x less work); craters can get small gaps at their edge.",
                        "Big modded explosions get gaps sooner.")
                .defineInRange("explosionRayGrid", 16, 8, 16);
        builder.pop();

        SPEC = builder.build();
    }

    private TNTConfig() {
    }
}
