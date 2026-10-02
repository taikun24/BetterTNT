package io.github.taikun24.bettertnt;

import java.util.HashMap;
import java.util.Map;

/**
 * Memoizes {@code Explosion.getSeenPercent}: the fraction of an entity's bounding box visible from the
 * explosion center. It casts up to dozens of rays per entity, and stacked TNT means thousands of entities
 * with the identical box, so most calls repeat.
 * <p>
 * The result only depends on the blocks in the level, so the cache is dropped whenever an explosion
 * breaks blocks and at every level tick boundary (most other block changes happen outside the entity tick).
 */
public final class ExposureCache {
    private static final int MAX_SIZE = 1 << 16;
    private static final Map<Key, Float> CACHE = new HashMap<>();

    private static boolean enabled = true;
    private static long hits;
    private static long misses;

    private ExposureCache() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static Float get(Key key) {
        Float value = CACHE.get(key);
        if (value != null) {
            hits++;
        } else {
            misses++;
        }
        return value;
    }

    public static void put(Key key, float value) {
        if (CACHE.size() >= MAX_SIZE) {
            CACHE.clear();
        }
        CACHE.put(key, value);
    }

    public static void invalidate() {
        CACHE.clear();
    }

    public static void onServerTickStart() {
        CACHE.clear();
        try {
            enabled = TNTConfig.EXPOSURE_CACHE_ENABLED.get();
        } catch (RuntimeException ignored) {
        }
    }

    public static void onServerStopping() {
        CACHE.clear();
        hits = 0;
        misses = 0;
    }

    public static long hits() {
        return hits;
    }

    public static long misses() {
        return misses;
    }

    public static final class Key {
        private final Object level;
        private final double x, y, z;
        private final double minX, minY, minZ, maxX, maxY, maxZ;
        private final int hash;

        public Key(Object level, double x, double y, double z,
                   double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            int h = System.identityHashCode(level);
            h = 31 * h + Double.hashCode(x);
            h = 31 * h + Double.hashCode(y);
            h = 31 * h + Double.hashCode(z);
            h = 31 * h + Double.hashCode(minX);
            h = 31 * h + Double.hashCode(minY);
            h = 31 * h + Double.hashCode(minZ);
            h = 31 * h + Double.hashCode(maxX);
            h = 31 * h + Double.hashCode(maxY);
            h = 31 * h + Double.hashCode(maxZ);
            this.hash = h;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return level == k.level && hash == k.hash
                    && x == k.x && y == k.y && z == k.z
                    && minX == k.minX && minY == k.minY && minZ == k.minZ
                    && maxX == k.maxX && maxY == k.maxY && maxZ == k.maxZ;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
