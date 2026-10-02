package io.github.taikun24.bettertnt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//? if >=1.20 {
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
//?} else {
/*import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
*///?}

/**
 * Per level, the loaded entities that something could collide with ({@code canBeCollidedWith}: boats,
 * shulkers, some modded entities).
 * <p>
 * Vanilla's {@code getEntityCollisions} collects every entity around the mover and only then drops the ones
 * that are not collidable. Thousands of primed TNT in a pile turn that into O(n^2) work per tick, although
 * TNT itself is never collidable. Checking just this short list gives the same shapes.
 * <p>
 * An entity is tracked when its class overrides {@code canBeCollidedWith} (the answer can change at runtime,
 * e.g. a shulker returns {@code isAlive()}), or when it already answers true on joining.
 */
public final class CollidableEntities {
    private static final Logger LOGGER = LogManager.getLogger();
    // Above this many tracked entities in a level, scanning the list is no longer clearly cheaper.
    private static final int MAX_TRACKED = 256;

    //? if >=1.20 {
    private static final String CAN_BE_COLLIDED_WITH = "m_5829_";
    //?} else
    /*private static final String CAN_BE_COLLIDED_WITH = "func_241845_aY";*/

    private static final Map<Object, Set<Entity>> BY_LEVEL = new IdentityHashMap<>();
    private static final Map<Class<?>, Boolean> OVERRIDES = new ConcurrentHashMap<>();
    private static final String METHOD_NAME = findMethodName();

    private CollidableEntities() {
    }

    private static String findMethodName() {
        try {
            Method method = ObfuscationReflectionHelper.findMethod(Entity.class, CAN_BE_COLLIDED_WITH);
            return method.getName();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not find Entity.canBeCollidedWith; fast TNT collisions are disabled", e);
            return null;
        }
    }

    public static boolean available() {
        return METHOD_NAME != null;
    }

    private static boolean mayBeCollidable(Entity entity) {
        return OVERRIDES.computeIfAbsent(entity.getClass(), c -> {
            try {
                return c.getMethod(METHOD_NAME).getDeclaringClass() != Entity.class;
            } catch (NoSuchMethodException e) {
                return true;
            }
        }) || entity.canBeCollidedWith();
    }

    public static void onJoin(Object level, Entity entity) {
        if (METHOD_NAME != null && mayBeCollidable(entity)) {
            BY_LEVEL.computeIfAbsent(level, l -> new HashSet<>()).add(entity);
        }
    }

    public static void onLeave(Object level, Entity entity) {
        Set<Entity> set = BY_LEVEL.get(level);
        if (set != null) {
            set.remove(entity);
        }
    }

    public static void onUnload(Object level) {
        BY_LEVEL.remove(level);
    }

    public static String debugTracked(Object level) {
        return String.valueOf(BY_LEVEL.get(level)) + " method=" + METHOD_NAME;
    }

    public static void clear() {
        BY_LEVEL.clear();
    }

    /**
     * Entity collision shapes for {@code self} moving through {@code box}, or null to let vanilla do it.
     * Mirrors {@code EntityGetter.getEntityCollisions}.
     */
    //? if >=1.20 {
    public static List<VoxelShape> collisions(Object level, Entity self, AABB box) {
        if (METHOD_NAME == null) {
            return null;
        }
        if (box.getSize() < 1.0E-7D) {
            return List.of();
        }
        Set<Entity> set = BY_LEVEL.get(level);
        if (set == null || set.isEmpty()) {
            return List.of();
        }
        if (set.size() > MAX_TRACKED) {
            return null;
        }
        AABB inflated = box.inflate(1.0E-7D);
        List<VoxelShape> shapes = null;
        for (Entity other : set) {
            // 1.20.1 takes an entity out of the section storage the moment it is removed, so vanilla never sees it either.
            if (other != self && !other.isRemoved() && other.getBoundingBox().intersects(inflated)
                    && EntitySelector.NO_SPECTATORS.test(other) && self.canCollideWith(other)) {
                if (shapes == null) {
                    shapes = new ArrayList<>();
                }
                shapes.add(Shapes.create(other.getBoundingBox()));
            }
        }
        return shapes == null ? List.of() : shapes;
    }
    //?} else {
    /*public static List<VoxelShape> collisions(Object level, Entity self, AxisAlignedBB box) {
        if (METHOD_NAME == null) {
            return null;
        }
        List<VoxelShape> shapes = new ArrayList<>();
        if (box.getSize() < 1.0E-7D) {
            return shapes;
        }
        Set<Entity> set = BY_LEVEL.get(level);
        if (set == null || set.isEmpty()) {
            return shapes;
        }
        if (set.size() > MAX_TRACKED) {
            return null;
        }
        AxisAlignedBB inflated = box.inflate(1.0E-7D);
        for (Entity other : set) {
            // 1.16.5 keeps a removed entity in the chunk lists (so vanilla still collides with it) until the
            // world's tick loop reaches it; it is gone from getEntity() at the same moment.
            if (other != self && other.getBoundingBox().intersects(inflated) && self.canCollideWith(other)
                    && (!other.removed || other.level.getEntity(other.getId()) == other)) {
                shapes.add(VoxelShapes.create(other.getBoundingBox()));
            }
        }
        return shapes;
    }
    *///?}
}
