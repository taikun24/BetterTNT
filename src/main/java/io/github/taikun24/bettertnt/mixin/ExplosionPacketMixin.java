package io.github.taikun24.bettertnt.mixin;

import io.github.taikun24.bettertnt.ClientEffects;
import io.github.taikun24.bettertnt.Optimizations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

//? if >=1.20 {
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
//?} else {
/*import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.SExplosionPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
*///?}

/** Not vanilla: trims the explosion packets {@code ServerLevel.explode} sends (see the clientEffects config). */
//? if >=1.20 {
@Mixin(ServerLevel.class)
//?} else
/*@Mixin(ServerWorld.class)*/
public abstract class ExplosionPacketMixin {
    // Clients get the same blocks from the regular block update packets, so the list only makes them do it twice.
    //? if >=1.20 {
    @ModifyArg(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;<init>(DDDFLjava/util/List;Lnet/minecraft/world/phys/Vec3;)V"), index = 4)
    //?} else
    /*@ModifyArg(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/SExplosionPacket;<init>(DDDFLjava/util/List;Lnet/minecraft/util/math/vector/Vector3d;)V"), index = 4)*/
    private List<BlockPos> bettertnt$leanPacket(List<BlockPos> toBlow) {
        return Optimizations.leanPackets ? Collections.emptyList() : toBlow;
    }

    //? if >=1.20 {
    @Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void bettertnt$limitEffects(ServerGamePacketListenerImpl connection, Packet<?> packet) {
        ClientboundExplodePacket explode = (ClientboundExplodePacket) packet;
    //?} else {
    /*@Redirect(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/play/ServerPlayNetHandler;send(Lnet/minecraft/network/IPacket;)V"))
    private void bettertnt$limitEffects(ServerPlayNetHandler connection, IPacket<?> packet) {
        SExplosionPacket explode = (SExplosionPacket) packet;
    *///?}
        boolean pushesPlayer = explode.getKnockbackX() != 0 || explode.getKnockbackY() != 0 || explode.getKnockbackZ() != 0;
        if (ClientEffects.allow(connection.player, pushesPlayer)) {
            connection.send(packet);
        }
    }
}
