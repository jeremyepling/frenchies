package net.j40climb.frenchies.setup;

import net.j40climb.frenchies.common.entity.frenchie.delivery.DeliveryPopupPayload;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class FrenchieNetworking {
    public static void sendPopup(ServerPlayer player, DeliveryPopupPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
