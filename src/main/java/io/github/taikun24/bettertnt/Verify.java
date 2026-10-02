package io.github.taikun24.bettertnt;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

/**
 * Debug mode ({@code -Dbettertnt.verify=true}): every optimized path also runs the vanilla code and counts
 * differences. Slow; meant for testing that the optimizations really are exact. Results show in /bettertnt.
 */
public final class Verify {
    public static final boolean ENABLED = Boolean.getBoolean("bettertnt.verify");

    // category -> {checks, mismatches}
    private static final Map<String, long[]> COUNTS = new TreeMap<>();
    private static String firstMismatch;

    private Verify() {
    }

    public static void check(boolean same, String category, Supplier<String> detail) {
        long[] counts = COUNTS.computeIfAbsent(category, c -> new long[2]);
        counts[0]++;
        if (!same) {
            counts[1]++;
            if (firstMismatch == null) {
                firstMismatch = category + ": " + detail.get();
            }
        }
    }

    public static String summary() {
        StringBuilder sb = new StringBuilder("verify (checks/mismatches):");
        COUNTS.forEach((category, counts) -> sb.append(' ').append(category).append('=').append(counts[0]).append('/').append(counts[1]));
        if (firstMismatch != null) {
            sb.append(" first mismatch: ").append(firstMismatch);
        }
        return sb.toString();
    }
}
