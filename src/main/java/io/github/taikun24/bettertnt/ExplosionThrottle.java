package io.github.taikun24.bettertnt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;

/**
 * Limits how much explosion work runs in one server tick.
 * <p>
 * Explosions over budget are never created: {@code ServerLevelMixin} captures the arguments of
 * {@code ServerLevel.explode} and this class replays the same call on a later tick, so the explosion
 * runs exactly like vanilla (Forge events, block breaking, drops, packets to clients), only later.
 * <p>
 * Everything here runs on the server thread only.
 */
public final class ExplosionThrottle {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final ArrayDeque<Runnable> PENDING = new ArrayDeque<>();

    // Settings, refreshed every tick from the config so the hot path never touches ForgeConfigSpec.
    private static boolean enabled = true;
    private static int maxCount = 100;
    private static long maxNanos = 20_000_000L;

    // Budget used in the current tick.
    private static int tickCount;
    private static long tickNanos;

    // Nesting of explode calls currently running (an explosion can trigger another one synchronously).
    private static int depth;
    private static long startNanos;
    // Set right before a replayed call so the mixin lets exactly that call through.
    private static boolean bypassNext;

    // Stats for the F3 screen and /bettertnt.
    private static int lastTickCount;
    private static long lastTickNanos;
    private static long totalDeferred;

    private ExplosionThrottle() {
    }

    /** Called by the mixin at the head of ServerLevel.explode. True means: queue it instead of running it now. */
    public static boolean shouldDefer() {
        if (bypassNext) {
            bypassNext = false;
            return false;
        }
        if (!enabled || depth > 0) {
            return false;
        }
        // Keep FIFO order: once anything is queued, newcomers wait behind it.
        return !PENDING.isEmpty() || budgetExhausted();
    }

    public static void defer(Runnable replay) {
        PENDING.addLast(replay);
        totalDeferred++;
    }

    public static void onExplodeStart() {
        if (depth++ == 0) {
            startNanos = System.nanoTime();
        }
    }

    public static void onExplodeEnd() {
        if (depth > 0 && --depth == 0) {
            tickNanos += System.nanoTime() - startNanos;
            tickCount++;
        }
    }

    private static boolean budgetExhausted() {
        return (maxCount > 0 && tickCount >= maxCount) || (maxNanos > 0 && tickNanos >= maxNanos);
    }

    /** Start of a server tick: reset the budget and spend it on queued explosions first. */
    public static void onServerTickStart() {
        readConfig();
        lastTickCount = tickCount;
        lastTickNanos = tickNanos;
        tickCount = 0;
        tickNanos = 0;
        depth = 0;
        bypassNext = false;

        // budgetExhausted() is false right after the reset, so at least one queued explosion runs every tick.
        while (!PENDING.isEmpty() && !budgetExhausted()) {
            replay(PENDING.pollFirst());
        }
    }

    public static int flush() {
        int count = 0;
        while (!PENDING.isEmpty()) {
            replay(PENDING.pollFirst());
            count++;
        }
        return count;
    }

    public static int clear() {
        int count = PENDING.size();
        PENDING.clear();
        return count;
    }

    public static void onServerStopping() {
        boolean flushOnShutdown = true;
        try {
            flushOnShutdown = TNTConfig.FLUSH_ON_SHUTDOWN.get();
        } catch (RuntimeException ignored) {
        }
        if (flushOnShutdown) {
            int ran = flush();
            if (ran > 0) {
                LOGGER.info("Ran {} queued explosions before shutdown", ran);
            }
        } else {
            int dropped = clear();
            if (dropped > 0) {
                LOGGER.info("Discarded {} queued explosions", dropped);
            }
        }
        // The integrated server can be restarted in the same JVM; start the next world clean.
        depth = 0;
        bypassNext = false;
        tickCount = 0;
        tickNanos = 0;
        lastTickCount = 0;
        lastTickNanos = 0;
        totalDeferred = 0;
    }

    private static void replay(Runnable explosion) {
        bypassNext = true;
        try {
            explosion.run();
        } catch (RuntimeException e) {
            LOGGER.error("Queued explosion failed", e);
            depth = 0;
        } finally {
            bypassNext = false;
        }
    }

    private static void readConfig() {
        try {
            enabled = TNTConfig.THROTTLE_ENABLED.get();
            maxCount = TNTConfig.MAX_EXPLOSIONS_PER_TICK.get();
            maxNanos = (long) (TNTConfig.MAX_EXPLOSION_MILLIS_PER_TICK.get() * 1_000_000.0);
        } catch (RuntimeException ignored) {
            // Server config not loaded yet; keep the previous values.
        }
    }

    public static int pending() {
        return PENDING.size();
    }

    public static int lastTickCount() {
        return lastTickCount;
    }

    public static double lastTickMillis() {
        return lastTickNanos / 1_000_000.0;
    }

    public static long totalDeferred() {
        return totalDeferred;
    }
}
