package io.github.taikun24.bettertnt;

import com.mojang.brigadier.CommandDispatcher;

//? if >=1.20 {
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
//?} else {
/*import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
*///?}

/**
 * /bettertnt          - show queue and cache stats
 * /bettertnt flush    - run every queued explosion now
 * /bettertnt clear    - throw away every queued explosion
 */
public final class BetterTNTCommand {
    //? if >=1.20 {
    private static void reply(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    //?} else {
    /*private static void reply(CommandSource source, String message) {
        source.sendSuccess(new StringTextComponent(message), true);
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
    *///?}
        dispatcher.register(Commands.literal("bettertnt")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    long hits = ExposureCache.hits();
                    long total = hits + ExposureCache.misses();
                    reply(ctx.getSource(), String.format(
                            "Better TNT: %d queued, %d explosions (%.1f ms) last tick, %d deferred so far, exposure cache hit %.1f%%",
                            ExplosionThrottle.pending(), ExplosionThrottle.lastTickCount(), ExplosionThrottle.lastTickMillis(),
                            ExplosionThrottle.totalDeferred(), total == 0 ? 0.0 : hits * 100.0 / total));
                    return ExplosionThrottle.pending();
                })
                .then(Commands.literal("flush").executes(ctx -> {
                    int ran = ExplosionThrottle.flush();
                    reply(ctx.getSource(), "Better TNT: ran " + ran + " queued explosions");
                    return ran;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    int dropped = ExplosionThrottle.clear();
                    reply(ctx.getSource(), "Better TNT: discarded " + dropped + " queued explosions");
                    return dropped;
                })));
    }

    private BetterTNTCommand() {
    }
}
