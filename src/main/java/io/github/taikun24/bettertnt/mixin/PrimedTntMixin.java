package io.github.taikun24.bettertnt.mixin;

import io.github.taikun24.bettertnt.ExplosionThrottle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.20 {
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.Redirect;
//?} else {
/*import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.world.World;
*///?}

/**
 * Primed TNT that the throttle holds back stays in the world instead of vanishing: it keeps moving,
 * shows as about to blow, is saved with the world, and retries every tick until there is room.
 */
//? if >=1.20 {
@Mixin(PrimedTnt.class)
//?} else
/*@Mixin(TNTEntity.class)*/
public abstract class PrimedTntMixin extends Entity {
    // Fuse shown to clients while waiting. Under 10 the client draws TNT swelling and flashing, and a few
    // ticks of margin keep the client from removing it locally between two server updates.
    @Unique
    private static final int bettertnt$WAIT_FUSE = 3;

    @Unique
    private boolean bettertnt$waiting;

    //? if >=1.20 {
    protected PrimedTntMixin(EntityType<?> type, Level level) {
        super(type, level);
    }
    //?} else {
    /*@Shadow private int life;

    protected PrimedTntMixin(EntityType<?> type, World level) {
        super(type, level);
    }
    *///?}

    @Shadow
    public abstract void setFuse(int fuse);

    //? if >=1.20 {
    // A waiting TNT retries every tick: report fuse 1 so this tick's countdown reaches 0 again.
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/PrimedTnt;getFuse()I"))
    private int bettertnt$retryWhileWaiting(PrimedTnt self) {
        return bettertnt$waiting ? 1 : self.getFuse();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/PrimedTnt;discard()V"), cancellable = true)
    private void bettertnt$waitForRoom(CallbackInfo ci) {
        if (level().isClientSide) {
            return;
        }
    //?} else {
    /*// A waiting TNT retries every tick: this tick's countdown reaches 0 again.
    @Inject(method = "tick", at = @At("HEAD"))
    private void bettertnt$retryWhileWaiting(CallbackInfo ci) {
        if (bettertnt$waiting) {
            life = 1;
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/TNTEntity;remove()V"), cancellable = true)
    private void bettertnt$waitForRoom(CallbackInfo ci) {
        if (level.isClientSide) {
            return;
        }
    *///?}
        if (!ExplosionThrottle.tntWaits()) {
            return;
        }
        if (!ExplosionThrottle.shouldDefer()) {
            // The time budget can run out between this check and the one in ServerLevel.explode;
            // stick to this decision instead of queueing an explosion whose TNT was meant to stay.
            ExplosionThrottle.letNextThrough();
            return;
        }
        bettertnt$waiting = true;
        ExplosionThrottle.onTntWait();
        // Change the synced value twice so it is sent again even if it already was WAIT_FUSE.
        setFuse(0);
        setFuse(bettertnt$WAIT_FUSE);
        // What vanilla does on a tick that does not explode.
        updateInWaterStateAndDoFluidPushing();
        ci.cancel();
    }
}
