package net.j40climb.frenchies.setup;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.j40climb.frenchies.common.entity.frenchie.DeliveryPopupPayload;
import net.j40climb.frenchies.common.entity.frenchie.DeliveryScreen;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieModel;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieRenderer;
import net.j40climb.frenchies.common.entity.frenchie.PlayBubbleRenderer;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FabricFrontpackClient;
import net.minecraft.client.Minecraft;

public final class FabricFrenchieClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(FrenchieModel.FRENCHIE, FrenchieModel::createBodyLayer);
        EntityRendererRegistry.register(FrenchieRegistry.FRENCHIE.get(), FrenchieRenderer::new);
        EntityRendererRegistry.register(FrenchieRegistry.PLAY_BUBBLE.get(), PlayBubbleRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(DeliveryPopupPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new DeliveryScreen(payload)));
        FabricFrontpackClient.register();
    }
}
