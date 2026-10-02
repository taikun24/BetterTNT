package io.github.taikun24.bettertnt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;

/**
 * Limits how much explosion work runs in one server tick.
 * <p>
 * Explosions over budget are never created: {@code ServerLevelMixin} captures the arguments of
 * {@code ServerLevel.explode} and this class replays the same call on a later tick, so it runs exactly like
 * vanilla (Forge events, block breaking, drops, packets), only later. Optionally ({@code tntWaitsInWorld})
 * primed TNT over budget instead stays in the world and retries every tick ({@code PrimedTntMixin}).
 * <p>
 * By default the budget is the server tick itself: explosions are only held back once the current tick has
 * already taken {@code maxTickMillis}, so a server that keeps up never sees a difference from vanilla.
 * <p>
 * Everything here runs on the server thread only.
 */
public final class ExplosionThrottle {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final ArrayDeque<Runnable> PENDING = new ArrayDeque<>();

    // Settings, refreshed every tick from the config so the hot path never touches ForgeConfigSpec.
    private static boolean enabled = true;
    private static boolean tntWaits;
    private static long maxTickNanos = 40_000_000L;
    private static int maxCount;
    private static long maxNanos;
    private static int minCount = 4;

    // The current tick.
    private static long tickStartNanos;
    private static long drainDeadlineNanos;
    private static int tickCount;
    private static long tickNanos;
    private static int tickTntWaits;
    // Time the last tick spent on everything except explosions; queued explosions get what is left of the budget.
    private static long lastOtherNanos;

    // Nesting of explode calls currently running (an explosion can trigger another one synchronously).
    private static int depth;
    private static long startNanos;
    // Set right before a replayed call so the mixin lets exactly that call through.
    private static boolean bypassNext;

    // Stats for the F3 screen and /bettertnt.
    private static int lastTickCount;
    private static long lastTickNanos;
    private static int lastTickTntWaits;
    private static long totalDeferred;

    private ExplosionThrottle() {
    }

    /**
     * True means: hold this explosion back. Called by the mixins at the head of ServerLevel.explode and
     * by primed TNT about to explode; both see the same answer within one call chain.
     */
    public static boolean shouldDefer() {
        if (bypassNext) {
            bypassNext = false;
            return false;
        }
        if (!enabled || depth > 0) {
            return false;
        }
        // Keep FIFO order: once anything is queued, newcomers wait behind it.
        return !PENDING.isEmpty() || budgetExhausted(tickStartNanos + maxTickNanos);
    }

    public static boolean tntWaits() {
        return enabled && tntWaits;
    }

    public static void defer(Runnable replay) {
        PENDING.addLast(replay);
        totalDeferred++;
    }

    /** The next ServerLevel.explode call runs now, whatever the budget says by then. */
    public static void letNextThrough() {
        bypassNext = true;
    }

    public static void onTntWait() {
        tickTntWaits++;
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

    private static boolean budgetExhausted(long tickDeadline) {
        if (tickCount < minCount) {
            // Always let a few through, so a server that lags for other reasons still makes progress.
            return false;
        }
        return (maxCount > 0 && tickCount >= maxCount)
                || (maxNanos > 0 && tickNanos >= maxNanos)
                || (maxTickNanos > 0 && System.nanoTime() >= tickDeadline);
    }

    /** Start of a server tick: reset the budget and spend it on queued explosions first. */
    public static void onServerTickStart() {
        readConfig();
        lastTickCount = tickCount;
        lastTickNanos = tickNanos;
        lastTickTntWaits = tickTntWaits;
        tickStartNanos = System.nanoTime();
        tickCount = 0;
        tickNanos = 0;
        tickTntWaits = 0;
        depth = 0;
        bypassNext = false;

        // Leave the rest of the tick the time it needed last tick.
        drainDeadlineNanos = tickStartNanos + Math.max(0, maxTickNanos - lastOtherNanos);
        while (!PENDING.isEmpty() && !budgetExhausted(drainDeadlineNanos)) {
            replay(PENDING.pollFirst());
        }
    }

    public static void onServerTickEnd() {
        lastOtherNanos = Math.max(0, System.nanoTime() - tickStartNanos - tickNanos);
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
        tickTntWaits = 0;
        lastTickCount = 0;
        lastTickNanos = 0;
        lastTickTntWaits = 0;
        lastOtherNanos = 0;
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
            tntWaits = TNTConfig.TNT_WAITS.get();
            maxTickNanos = (long) (TNTConfig.MAX_TICK_MILLIS.get() * 1_000_000.0);
            maxCount = TNTConfig.MAX_EXPLOSIONS_PER_TICK.get();
            maxNanos = (long) (TNTConfig.MAX_EXPLOSION_MILLIS_PER_TICK.get() * 1_000_000.0);
            minCount = TNTConfig.MIN_EXPLOSIONS_PER_TICK.get();
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

    public static int lastTickTntWaits() {
        return lastTickTntWaits;
    }

    public static long totalDeferred() {
        return totalDeferred;
    }
}
