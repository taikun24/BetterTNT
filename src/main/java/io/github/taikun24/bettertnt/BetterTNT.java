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

//? if >=1.20 {
import net.minecraftforge.event.server.ServerStoppingEvent;
//?} else
/*import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;*/

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
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BetterTNTCommand.register(event.getDispatcher());
    }
}
