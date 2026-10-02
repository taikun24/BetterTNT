package io.github.taikun24.bettertnt;

import java.util.IdentityHashMap;
import java.util.Map;

/** Not vanilla: caps how many explosion packets (sound + particles) each player gets per tick. */
public final class ClientEffects {
    private static final Map<Object, int[]> SENT_THIS_TICK = new IdentityHashMap<>();
    private static long skipped;

    private ClientEffects() {
    }

    /** Whether to send this explosion to the player. Explosions that push the player always go through. */
    public static boolean allow(Object player, boolean pushesPlayer) {
        int max = Optimizations.maxEffectsPerPlayer;
        if (max <= 0) {
            return true;
        }
        int[] sent = SENT_THIS_TICK.computeIfAbsent(player, p -> new int[1]);
        if (pushesPlayer || sent[0] < max) {
            sent[0]++;
            return true;
        }
        skipped++;
        return false;
    }

    public static void onServerTickStart() {
        SENT_THIS_TICK.clear();
    }

    public static void onServerStopping() {
        SENT_THIS_TICK.clear();
        skipped = 0;
    }

    public static long skipped() {
        return skipped;
    }
}
