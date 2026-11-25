package net.j40climb.frenchies.setup;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if <26.4 {
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
//?} else {
/*import net.j40climb.frenchies.common.entity.frenchie.frontpack.client.PreviewClientRegistries;
*///?}
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieModel;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderer;
import net.j40climb.frenchies.common.entity.frenchie.client.PlayBubbleRenderer;
import net.j40climb.frenchies.common.entity.frenchie.delivery.DeliveryPopupPayload;
import net.j40climb.frenchies.common.entity.frenchie.delivery.DeliveryScreen;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.client.FabricFrontpackClient;
import net.minecraft.client.Minecraft;

public final class FabricFrenchieClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(FrenchieModel.FRENCHIE, FrenchieModel::createBodyLayer);
        //? if <26.4 {
        EntityRendererRegistry.register(FrenchieRegistry.FRENCHIE.get(), FrenchieRenderer::new);
        EntityRendererRegistry.register(FrenchieRegistry.PLAY_BUBBLE.get(), PlayBubbleRenderer::new);
        //?} else {
        /*PreviewClientRegistries.registerRenderers();
        *///?}
        ClientPlayNetworking.registerGlobalReceiver(DeliveryPopupPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().setScreen(new DeliveryScreen(payload)));
        FabricFrontpackClient.register();
    }
}
