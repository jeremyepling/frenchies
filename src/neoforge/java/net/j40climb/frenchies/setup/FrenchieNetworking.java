package net.j40climb.frenchies.setup;

import net.j40climb.frenchies.common.entity.frenchie.DeliveryPopupPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FrenchieNetworking {
    public static void sendPopup(ServerPlayer player, DeliveryPopupPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
