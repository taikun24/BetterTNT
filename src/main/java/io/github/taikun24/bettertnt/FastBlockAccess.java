package io.github.taikun24.bettertnt;

//? if >=1.20 {
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//?} else {
/*import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
*///?}

/**
 * {@code Level.getBlockState} for code that reads many nearby blocks in a row.
 * <p>
 * Vanilla goes through {@code ServerChunkCache.getChunk} on every call, which showed up as the single
 * hottest method when profiling mass TNT. This keeps recent chunks and asks them directly; the result is
 * identical to {@code Level.getBlockState}. Server thread only; chunks are only cached while an explosion is
 * running (see {@link #beginExplosion}), so a cached chunk can never be stale.
 */
public final class FastBlockAccess {
    public static final FastBlockAccess RAYS = new FastBlockAccess();
    public static final FastBlockAccess CLIP = new FastBlockAccess();

    //? if >=1.20 {
    public static void onExplodeStart(Level level) {
    //?} else
    /*public static void onExplodeStart(World level) {*/
        RAYS.beginExplosion(level);
        CLIP.beginExplosion(level);
    }

    public static void onExplodeEnd() {
        RAYS.endExplosion();
        CLIP.endExplosion();
    }

    /** Safety net: if an explosion threw before its end hook ran, never carry chunks into another tick. */
    public static void onServerTickStart() {
        RAYS.explosionDepth = 0;
        RAYS.reset(null);
        CLIP.explosionDepth = 0;
        CLIP.reset(null);
    }

    // Direct-mapped by the low 2 bits of the chunk x/z: any 4x4 chunk window (far more than one explosion
    // reaches) fits without two chunks evicting each other.
    private static final int SLOTS = 16;

    //? if >=1.20 {
    private Level level;
    private final LevelChunk[] chunks = new LevelChunk[SLOTS];
    //?} else {
    /*private World level;
    private final Chunk[] chunks = new Chunk[SLOTS];
    *///?}
    private final int[] chunkX = new int[SLOTS];
    private final int[] chunkZ = new int[SLOTS];
    // While an explosion runs, chunks can neither unload nor be replaced, so the cache may span its whole
    // duration. Outside of one (e.g. another mod calling getSeenPercent) it is rebuilt on every use.
    private int explosionDepth;

    //? if >=1.20 {
    public void reset(Level level) {
    //?} else
    /*public void reset(World level) {*/
        this.level = level;
        java.util.Arrays.fill(chunks, null);
    }

    //? if >=1.20 {
    public void beginExplosion(Level level) {
    //?} else
    /*public void beginExplosion(World level) {*/
        if (explosionDepth++ == 0 || level != this.level) {
            reset(level);
        }
    }

    public void endExplosion() {
        if (explosionDepth > 0 && --explosionDepth == 0) {
            reset(null);
        }
    }

    //? if >=1.20 {
    private void prepare(Level level) {
    //?} else
    /*private void prepare(World level) {*/
        if (explosionDepth == 0 || level != this.level) {
            reset(level);
        }
    }

    public BlockState getBlockState(BlockPos pos) {
        //? if >=1.20 {
        if (level.isOutsideBuildHeight(pos)) {
        //?} else
        /*if (World.isOutsideBuildHeight(pos)) {*/
            return Blocks.VOID_AIR.defaultBlockState();
        }
        int x = pos.getX() >> 4;
        int z = pos.getZ() >> 4;
        int slot = (x & 3) << 2 | (z & 3);
        if (chunks[slot] == null || chunkX[slot] != x || chunkZ[slot] != z) {
            chunks[slot] = level.getChunk(x, z);
            chunkX[slot] = x;
            chunkZ[slot] = z;
        }
        return chunks[slot].getBlockState(pos);
    }

    /** Air never has a collision shape, so a ray can pass it without asking for one. */
    public static boolean isAir(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
    }

    /**
     * Same hit/miss answer as {@code level.clip(ctx)} for a COLLIDER / fluid NONE context, which is the
     * only kind {@code Explosion.getSeenPercent} makes.
     * <p>
     * Vanilla's per-block step reports a hit exactly when the block's collision shape intersects the ray
     * (the interaction-shape pass can only change the hit direction, and fluid NONE never hits), so that is
     * all this checks. On a miss it returns the same miss result vanilla builds.
     */
    //? if >=1.20 {
    public static BlockHitResult clip(Level level, ClipContext ctx) {
        FastBlockAccess access = CLIP;
        access.prepare(level);
        return BlockGetter.traverseBlocks(ctx.getFrom(), ctx.getTo(), ctx, (c, pos) -> {
            BlockState state = access.getBlockState(pos);
            if (isAir(state)) {
                return null;
            }
            return c.getBlockShape(state, level, pos).clip(c.getFrom(), c.getTo(), pos);
        }, c -> {
            Vec3 back = c.getFrom().subtract(c.getTo());
            return BlockHitResult.miss(c.getTo(), Direction.getNearest(back.x, back.y, back.z), BlockPos.containing(c.getTo()));
        });
    }
    //?} else {
    /*public static BlockRayTraceResult clip(World level, RayTraceContext ctx) {
        FastBlockAccess access = CLIP;
        access.prepare(level);
        return IBlockReader.traverseBlocks(ctx, (c, pos) -> {
            BlockState state = access.getBlockState(pos);
            if (isAir(state)) {
                return null;
            }
            return c.getBlockShape(state, level, pos).clip(c.getFrom(), c.getTo(), pos);
        }, c -> {
            Vector3d back = c.getFrom().subtract(c.getTo());
            return BlockRayTraceResult.miss(c.getTo(), Direction.getNearest(back.x, back.y, back.z), new BlockPos(c.getTo()));
        });
    }
    *///?}
}
