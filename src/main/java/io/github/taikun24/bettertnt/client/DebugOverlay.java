package io.github.taikun24.bettertnt.client;

import io.github.taikun24.bettertnt.ExplosionThrottle;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

//? if >=1.20 {
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
//?} else
/*import net.minecraftforge.client.event.RenderGameOverlayEvent;*/

/**
 * Adds the queue size to the F3 screen. The numbers live on the server, so this only works in
 * singleplayer / LAN host; on a dedicated server use /bettertnt instead.
 */
public final class DebugOverlay {
    //? if >=1.20 {
    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        addLines(event.getLeft());
    }
    //?} else {
    /*@SubscribeEvent
    public static void onDebugText(RenderGameOverlayEvent.Text event) {
        if (Minecraft.getInstance().options.renderDebug) {
            addLines(event.getLeft());
        }
    }
    *///?}

    private static void addLines(List<String> lines) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) {
            return;
        }
        lines.add("");
        lines.add(String.format("[Better TNT] last tick: %d explosions / %.1f ms, TNT waiting: %d, queued: %d",
                ExplosionThrottle.lastTickCount(), ExplosionThrottle.lastTickMillis(), ExplosionThrottle.lastTickTntWaits(),
                ExplosionThrottle.pending()));
    }

    private DebugOverlay() {
    }
}
