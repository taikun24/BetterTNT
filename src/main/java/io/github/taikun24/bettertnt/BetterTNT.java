package io.github.taikun24.bettertnt;

import io.github.taikun24.bettertnt.client.DebugOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

import net.minecraftforge.eventbus.api.EventPriority;

//? if >=1.20 {
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
//?} else {
/*import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
*///?}

@Mod(BetterTNT.MOD_ID)
public class BetterTNT {
    public static final String MOD_ID = "bettertnt";

    public BetterTNT() {
        // Kept the 1.x file name so existing servers find their config in the same place.
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TNTConfig.SPEC, "bettertnt-server-config.toml");
        MinecraftForge.EVENT_BUS.register(BetterTNT.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(DebugOverlay.class);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Optimizations.onServerTickStart();
            FastBlockAccess.onServerTickStart();
            ExposureCache.onServerTickStart();
            ExplosionThrottle.onServerTickStart();
        }
    }

    // Block ticks, block events (pistons) and players act between level ticks; drop exposures cached before them.
    //? if >=1.20 {
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide) {
            ExposureCache.invalidate();
        }
    }
    //?} else {
    /*@SubscribeEvent
    public static void onLevelTick(TickEvent.WorldTickEvent event) {
        if (!event.world.isClientSide) {
            ExposureCache.invalidate();
        }
    }
    *///?}

    @SubscribeEvent
    //? if >=1.20 {
    public static void onServerStopping(ServerStoppingEvent event) {
    //?} else
    /*public static void onServerStopping(FMLServerStoppingEvent event) {*/
        ExplosionThrottle.onServerStopping();
        ExposureCache.onServerStopping();
        CollidableEntities.clear();
    }

    // Keep CollidableEntities in sync with what is loaded. LOWEST so a cancelled join is never tracked.
    //? if >=1.20 {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            CollidableEntities.onJoin(event.getLevel(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            CollidableEntities.onLeave(event.getLevel(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        CollidableEntities.onUnload(event.getLevel());
    }
    //?} else {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().isClientSide) {
            CollidableEntities.onJoin(event.getWorld(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveWorldEvent event) {
        if (!event.getWorld().isClientSide) {
            CollidableEntities.onLeave(event.getWorld(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(WorldEvent.Unload event) {
        CollidableEntities.onUnload(event.getWorld());
    }
    *///?}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BetterTNTCommand.register(event.getDispatcher());
    }
}
