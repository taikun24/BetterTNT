package io.github.taikun24.bettertnt.mixin;

import io.github.taikun24.bettertnt.CollidableEntities;
import io.github.taikun24.bettertnt.Optimizations;
import io.github.taikun24.bettertnt.Verify;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//? if >=1.20 {
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
//?} else {
/*import net.minecraft.entity.Entity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
*///?}

/** Primed TNT asks {@link CollidableEntities} for entity collisions instead of scanning every nearby entity. */
@Mixin(Entity.class)
public abstract class EntityCollideMixin {
    //? if >=1.20 {
    @Redirect(method = "collide", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<VoxelShape> bettertnt$tntEntityCollisions(Level level, Entity self, AABB box) {
        if (Optimizations.fastTntCollisions && self instanceof PrimedTnt && level instanceof ServerLevel) {
            List<VoxelShape> shapes = CollidableEntities.collisions(level, self, box);
            if (shapes != null) {
                if (Verify.ENABLED) {
                    Verify.check(bettertnt$bounds(shapes.stream()).equals(bettertnt$bounds(level.getEntityCollisions(self, box).stream())),
                            "tntCollisions" + (shapes.isEmpty() ? "" : "Hit"), () -> String.valueOf(self.position()));
                }
                return shapes;
            }
        }
        return level.getEntityCollisions(self, box);
    }
    //?} else {
    /*@Redirect(method = "collide", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getEntityCollisions(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private Stream<VoxelShape> bettertnt$tntEntityCollisions(World level, Entity self, AxisAlignedBB box, Predicate<Entity> filter) {
        if (Optimizations.fastTntCollisions && self instanceof TNTEntity && level instanceof ServerWorld) {
            List<VoxelShape> shapes = CollidableEntities.collisions(level, self, box);
            if (shapes != null) {
                if (Verify.ENABLED) {
                    Verify.check(bettertnt$bounds(shapes.stream()).equals(bettertnt$bounds(level.getEntityCollisions(self, box, filter))),
                            "tntCollisions" + (shapes.isEmpty() ? "" : "Hit"), () -> self.position() + " vanilla hit "
                                    + level.getEntities(self, box.inflate(1.0E-7D), e -> e.getBoundingBox().intersects(box.inflate(1.0E-7D)) && self.canCollideWith(e))
                                    + " tracked " + CollidableEntities.debugTracked(level));
                }
                return shapes.stream();
            }
        }
        return level.getEntityCollisions(self, box, filter);
    }
    *///?}

    @Unique
    private static List<String> bettertnt$bounds(Stream<VoxelShape> shapes) {
        return shapes.map(shape -> shape.bounds().toString()).sorted().collect(Collectors.toList());
    }
}
