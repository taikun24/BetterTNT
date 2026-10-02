package io.github.taikun24.bettertnt.mixin;

import io.github.taikun24.bettertnt.ExplosionThrottle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

//? if >=1.20 {
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
//?} else {
/*import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import net.minecraft.world.ExplosionContext;
import net.minecraft.world.server.ServerWorld;
*///?}

/**
 * Every server-side explosion goes through this one method, in both 1.16.5 and 1.20.1.
 * Over budget, the call is captured and replayed later by {@link ExplosionThrottle}.
 */
//? if >=1.20 {
@Mixin(ServerLevel.class)
//?} else
/*@Mixin(ServerWorld.class)*/
public abstract class ServerLevelMixin {

    //? if >=1.20 {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void bettertnt$throttle(@Nullable Entity source, @Nullable DamageSource damageSource,
                                    @Nullable ExplosionDamageCalculator calculator,
                                    double x, double y, double z, float radius, boolean fire,
                                    Level.ExplosionInteraction interaction,
                                    CallbackInfoReturnable<Explosion> cir) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (ExplosionThrottle.shouldDefer()) {
            ExplosionThrottle.defer(() -> self.explode(source, damageSource, calculator, x, y, z, radius, fire, interaction));
            // Callers expect an Explosion back; hand them one that never ran and touches nothing.
            cir.setReturnValue(new Explosion(self, source, damageSource, calculator, x, y, z, radius, fire, Explosion.BlockInteraction.KEEP));
            return;
        }
        ExplosionThrottle.onExplodeStart();
    }
    //?} else {
    /*@Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void bettertnt$throttle(@Nullable Entity source, @Nullable DamageSource damageSource,
                                    @Nullable ExplosionContext calculator,
                                    double x, double y, double z, float radius, boolean fire,
                                    Explosion.Mode interaction,
                                    CallbackInfoReturnable<Explosion> cir) {
        ServerWorld self = (ServerWorld) (Object) this;
        if (ExplosionThrottle.shouldDefer()) {
            ExplosionThrottle.defer(() -> self.explode(source, damageSource, calculator, x, y, z, radius, fire, interaction));
            // Callers expect an Explosion back; hand them one that never ran and touches nothing.
            cir.setReturnValue(new Explosion(self, source, damageSource, calculator, x, y, z, radius, fire, Explosion.Mode.NONE));
            return;
        }
        ExplosionThrottle.onExplodeStart();
    }
    *///?}

    @Inject(method = "explode", at = @At("RETURN"))
    private void bettertnt$measure(CallbackInfoReturnable<Explosion> cir) {
        ExplosionThrottle.onExplodeEnd();
    }
}
