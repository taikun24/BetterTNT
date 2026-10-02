package io.github.taikun24.bettertnt;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TNTConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue THROTTLE_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_EXPLOSIONS_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue MAX_EXPLOSION_MILLIS_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue FLUSH_ON_SHUTDOWN;
    public static final ForgeConfigSpec.BooleanValue EXPOSURE_CACHE_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Spreads explosions over several ticks when too many happen at once.").push("throttle");
        THROTTLE_ENABLED = builder
                .comment("Enable explosion throttling.")
                .define("enabled", true);
        MAX_EXPLOSIONS_PER_TICK = builder
                .comment("Maximum number of explosions processed per server tick. Extra explosions are queued for later ticks. 0 = no count limit.")
                .defineInRange("maxExplosionsPerTick", 100, 0, 1_000_000);
        MAX_EXPLOSION_MILLIS_PER_TICK = builder
                .comment("Maximum time (milliseconds) spent on explosions per server tick before the rest are queued. 0 = no time limit.",
                        "A tick is 50ms, so keep this well below that.")
                .defineInRange("maxExplosionMillisPerTick", 20.0, 0.0, 1000.0);
        FLUSH_ON_SHUTDOWN = builder
                .comment("Run every queued explosion when the server stops. If false, queued explosions are discarded.")
                .define("flushOnShutdown", true);
        builder.pop();

        builder.comment("Caches how much of an entity an explosion can see. Big win for stacked TNT and TNT in water.").push("exposureCache");
        EXPOSURE_CACHE_ENABLED = builder
                .comment("Enable the exposure cache.")
                .define("enabled", true);
        builder.pop();

        SPEC = builder.build();
    }

    private TNTConfig() {
    }
}
