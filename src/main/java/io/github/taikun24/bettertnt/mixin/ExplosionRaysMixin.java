package io.github.taikun24.bettertnt.mixin;

import io.github.taikun24.bettertnt.FastBlockAccess;
import io.github.taikun24.bettertnt.Optimizations;
import io.github.taikun24.bettertnt.Verify;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;

//? if >=1.20 {
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
//?} else {
/*import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.ExplosionContext;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
*///?}

/**
 * Speeds up the ray loop in {@code Explosion.explode} without changing its result.
 * <p>
 * 1352 rays advance 0.3 blocks per step, so each ray asks for the same block about three times in a row.
 * The block state, fluid state and resistance of the previous step are reused when the position repeats,
 * and blocks already queued for breaking skip the expensive {@code HashSet<BlockPos>.add}.
 * Blocks do not change while the rays are cast, so every reused value is what vanilla would compute.
 */
@Mixin(Explosion.class)
public abstract class ExplosionRaysMixin {
    //? if >=1.20 {
    @Shadow @Final private Level level;
    //?} else
    /*@Shadow @Final private World level;*/

    @Unique private boolean bettertnt$fast;
    @Unique private BlockPos bettertnt$statePos;
    @Unique private BlockState bettertnt$state;
    @Unique private BlockPos bettertnt$resistancePos;
    @Unique private Optional<Float> bettertnt$resistance;
    @Unique private BlockPos bettertnt$queuedPos;
    @Unique private LongOpenHashSet bettertnt$queued;

    @Inject(method = "explode", at = @At("HEAD"))
    private void bettertnt$beginRays(CallbackInfo ci) {
        //? if >=1.20 {
        bettertnt$fast = Optimizations.fastRays && level instanceof ServerLevel;
        //?} else
        /*bettertnt$fast = Optimizations.fastRays && level instanceof ServerWorld;*/
        bettertnt$statePos = null;
        bettertnt$state = null;
        bettertnt$resistancePos = null;
        bettertnt$resistance = null;
        bettertnt$queuedPos = null;
        bettertnt$queued = bettertnt$fast ? new LongOpenHashSet() : null;
        if (bettertnt$fast) {
            FastBlockAccess.onExplodeStart(level);
        }
    }

    @Inject(method = "explode", at = @At("RETURN"))
    private void bettertnt$endRays(CallbackInfo ci) {
        if (bettertnt$fast) {
            FastBlockAccess.onExplodeEnd();
        }
        bettertnt$queued = null;
    }

    //? if >=1.20 {
    @Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState bettertnt$rayBlockState(Level level, BlockPos pos) {
    //?} else {
    /*@Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState bettertnt$rayBlockState(World level, BlockPos pos) {
    *///?}
        if (!bettertnt$fast) {
            return level.getBlockState(pos);
        }
        if (!pos.equals(bettertnt$statePos)) {
            bettertnt$statePos = pos;
            bettertnt$state = FastBlockAccess.RAYS.getBlockState(pos);
        }
        if (Verify.ENABLED) {
            Verify.check(bettertnt$state == level.getBlockState(pos), "rayBlock", () -> pos.toString());
        }
        return bettertnt$state;
    }

    // Level.getFluidState(pos) is by definition the fluid of the block state at pos (EMPTY outside the world,
    // which is also VOID_AIR's fluid), so the state fetched just before already holds the answer.
    //? if >=1.20 {
    @Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState bettertnt$rayFluidState(Level level, BlockPos pos) {
    //?} else {
    /*@Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getFluidState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/fluid/FluidState;"))
    private FluidState bettertnt$rayFluidState(World level, BlockPos pos) {
    *///?}
        if (bettertnt$fast && pos.equals(bettertnt$statePos)) {
            FluidState fluid = bettertnt$state.getFluidState();
            if (Verify.ENABLED) {
                Verify.check(fluid == level.getFluidState(pos), "rayFluid", () -> pos.toString());
            }
            return fluid;
        }
        return level.getFluidState(pos);
    }

    //? if >=1.20 {
    @Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getBlockExplosionResistance(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Ljava/util/Optional;"))
    private Optional<Float> bettertnt$rayResistance(ExplosionDamageCalculator calculator, Explosion explosion, BlockGetter getter,
                                                    BlockPos pos, BlockState state, FluidState fluid) {
    //?} else {
    /*@Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/ExplosionContext;getBlockExplosionResistance(Lnet/minecraft/world/Explosion;Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Ljava/util/Optional;"))
    private Optional<Float> bettertnt$rayResistance(ExplosionContext calculator, Explosion explosion, IBlockReader getter,
                                                    BlockPos pos, BlockState state, FluidState fluid) {
    *///?}
        if (!bettertnt$fast) {
            return calculator.getBlockExplosionResistance(explosion, getter, pos, state, fluid);
        }
        if (!pos.equals(bettertnt$resistancePos)) {
            bettertnt$resistancePos = pos;
            bettertnt$resistance = calculator.getBlockExplosionResistance(explosion, getter, pos, state, fluid);
        }
        if (Verify.ENABLED) {
            Verify.check(bettertnt$resistance.equals(calculator.getBlockExplosionResistance(explosion, getter, pos, state, fluid)),
                    "rayResistance", () -> pos.toString());
        }
        return bettertnt$resistance;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "explode", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean bettertnt$queueBlock(Set set, Object pos) {
        if (bettertnt$fast) {
            // Consecutive steps of a ray usually queue the block they just queued.
            if (pos.equals(bettertnt$queuedPos)) {
                return false;
            }
            bettertnt$queuedPos = (BlockPos) pos;
            if (!bettertnt$queued.add(bettertnt$queuedPos.asLong())) {
                return false;
            }
        }
        return set.add(pos);
    }
}
