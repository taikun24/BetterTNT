package io.github.taikun24.bettertnt.mixin;

import io.github.taikun24.bettertnt.ExposureCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.20 {
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//?} else {
/*import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
*///?}

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    //? if >=1.20 {
    @Shadow @Final private Level level;
    //?} else
    /*@Shadow @Final private World level;*/

    // getSeenPercent is static and never re-entered, so one slot is enough to carry the key from HEAD to RETURN.
    @Unique
    private static ExposureCache.Key bettertnt$pendingKey;

    //? if >=1.20 {
    @Inject(method = "getSeenPercent", at = @At("HEAD"), cancellable = true)
    private static void bettertnt$cachedSeenPercent(Vec3 center, Entity entity, CallbackInfoReturnable<Float> cir) {
        bettertnt$pendingKey = null;
        Level level = entity.level();
        if (!ExposureCache.enabled() || !(level instanceof ServerLevel)) {
            return;
        }
        AABB box = entity.getBoundingBox();
    //?} else {
    /*@Inject(method = "getSeenPercent", at = @At("HEAD"), cancellable = true)
    private static void bettertnt$cachedSeenPercent(Vector3d center, Entity entity, CallbackInfoReturnable<Float> cir) {
        bettertnt$pendingKey = null;
        World level = entity.level;
        if (!ExposureCache.enabled() || !(level instanceof ServerWorld)) {
            return;
        }
        AxisAlignedBB box = entity.getBoundingBox();
    *///?}
        ExposureCache.Key key = new ExposureCache.Key(level, center.x, center.y, center.z,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        Float cached = ExposureCache.get(key);
        if (cached != null) {
            cir.setReturnValue(cached);
        } else {
            bettertnt$pendingKey = key;
        }
    }

    @Inject(method = "getSeenPercent", at = @At("RETURN"))
    private static void bettertnt$storeSeenPercent(CallbackInfoReturnable<Float> cir) {
        if (bettertnt$pendingKey != null) {
            ExposureCache.put(bettertnt$pendingKey, cir.getReturnValueF());
            bettertnt$pendingKey = null;
        }
    }

    // Breaking blocks changes what later explosions can see through.
    // Client explosions (from packets) also finalize here; they must not touch the server-thread cache.
    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void bettertnt$invalidateExposure(boolean spawnParticles, CallbackInfo ci) {
        //? if >=1.20 {
        boolean server = level instanceof ServerLevel;
        //?} else
        /*boolean server = level instanceof ServerWorld;*/
        if (server && !((Explosion) (Object) this).getToBlow().isEmpty()) {
            ExposureCache.invalidate();
        }
    }
}
