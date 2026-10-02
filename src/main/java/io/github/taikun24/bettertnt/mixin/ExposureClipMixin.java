package io.github.taikun24.bettertnt.mixin;

import io.github.taikun24.bettertnt.FastBlockAccess;
import io.github.taikun24.bettertnt.Optimizations;
import io.github.taikun24.bettertnt.Verify;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//? if >=1.20 {
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
//?} else {
/*import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
*///?}

/** The raycasts of {@code Explosion.getSeenPercent} go through {@link FastBlockAccess#clip}. */
@Mixin(Explosion.class)
public abstract class ExposureClipMixin {
    //? if >=1.20 {
    @Redirect(method = "getSeenPercent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult bettertnt$fastClip(Level level, ClipContext ctx) {
        if (Optimizations.fastExposure && level instanceof ServerLevel) {
            BlockHitResult result = FastBlockAccess.clip(level, ctx);
            if (Verify.ENABLED) {
                Verify.check(result.getType() == level.clip(ctx).getType(), "exposureClip" + result.getType(), () -> String.valueOf(ctx.getFrom()));
            }
            return result;
        }
        return level.clip(ctx);
    }
    //?} else {
    /*@Redirect(method = "getSeenPercent", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;clip(Lnet/minecraft/util/math/RayTraceContext;)Lnet/minecraft/util/math/BlockRayTraceResult;"))
    private static BlockRayTraceResult bettertnt$fastClip(World level, RayTraceContext ctx) {
        if (Optimizations.fastExposure && level instanceof ServerWorld) {
            BlockRayTraceResult result = FastBlockAccess.clip(level, ctx);
            if (Verify.ENABLED) {
                Verify.check(result.getType() == level.clip(ctx).getType(), "exposureClip" + result.getType(), () -> String.valueOf(ctx.getFrom()));
            }
            return result;
        }
        return level.clip(ctx);
    }
    *///?}
}
