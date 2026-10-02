package io.github.taikun24.bettertnt.mixin;

import io.github.taikun24.bettertnt.Optimizations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//? if >=1.20 {
import net.minecraft.world.level.Explosion;
//?} else
/*import net.minecraft.world.Explosion;*/

/**
 * Not vanilla: the size of the ray grid in {@code Explosion.explode}. Vanilla casts a ray through every point
 * on the surface of a 16x16x16 grid ({@code j < 16}, edges at {@code j == 15}, directions {@code j / 15F}).
 * The setting is read once per tick, so one explosion always uses one grid.
 */
@Mixin(Explosion.class)
public abstract class ExplosionRayGridMixin {
    @ModifyConstant(method = "explode", constant = @Constant(intValue = 16))
    private int bettertnt$gridSize(int vanilla) {
        return Optimizations.rayGrid;
    }

    @ModifyConstant(method = "explode", constant = @Constant(intValue = 15))
    private int bettertnt$gridEdge(int vanilla) {
        return Optimizations.rayGrid - 1;
    }

    @ModifyConstant(method = "explode", constant = @Constant(floatValue = 15.0F))
    private float bettertnt$gridScale(float vanilla) {
        return Optimizations.rayGrid - 1;
    }
}
