package net.j40climb.frenchies.setup;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.DeliveryPopupPayload;
import net.j40climb.frenchies.common.entity.frenchie.DeliveryScreen;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieModel;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieRenderer;
import net.j40climb.frenchies.common.entity.frenchie.PlayBubbleRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class FrenchieClient {
    @SubscribeEvent
    public static void models(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FrenchieModel.FRENCHIE, FrenchieModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FrenchieRegistry.FRENCHIE.get(), FrenchieRenderer::new);
        event.registerEntityRenderer(FrenchieRegistry.PLAY_BUBBLE.get(), PlayBubbleRenderer::new);
    }

    @SubscribeEvent
    public static void networking(RegisterClientPayloadHandlersEvent event) {
        event.register(DeliveryPopupPayload.TYPE, (data, context) ->
                Minecraft.getInstance().setScreen(new DeliveryScreen(data)));
    }
}
