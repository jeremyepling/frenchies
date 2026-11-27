package net.j40climb.frenchies.common.entity.frenchie.frontpack.networking;

import io.netty.buffer.ByteBuf;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackEvents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Network payload for putting down a carried Frenchie.
 * Client sends this when player shift-right-clicks air while carrying a Frenchie.
 * Server handles by spawning the Frenchie from NBT and clearing the attachment.
 */
public enum PutDownFrenchiePayload implements CustomPacketPayload {
    INSTANCE;

    public static final Type<PutDownFrenchiePayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "put_down_frenchie"));

    public static final StreamCodec<ByteBuf, PutDownFrenchiePayload> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    public static void handle(ServerPlayer player) {
        if (player.isAlive() && !player.isSpectator() && player.isShiftKeyDown()) {
            FrontpackEvents.putDown(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
