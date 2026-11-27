package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.networking.PutDownFrenchiePayload;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Client input and rendering for the carried Frenchie.
 */
@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public class FrontpackClientEvents {
    @SubscribeEvent
    public static void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "front_pack"), FrontpackTint.CODEC);
    }

    @SubscribeEvent
    public static void registerItemModels(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "front_pack"), FrontpackItemRenderer.Unbaked.CODEC);
    }

    @SubscribeEvent
    public static void registerWarningHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(FrontpackClient.WARNING_HUD, FrontpackClient::drawWarning);
    }

    /**
     * Registers the frontpack model layer definition.
     * This bakes the 3D geometry from FrenchFrontpackModel.createBodyLayer().
     */
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                FrontpackModel.LAYER_LOCATION,
                FrontpackModel::createBodyLayer
        );
    }

    /**
     * Adds the frontpack render layer to all player renderers.
     * This includes both "default" and "slim" player models.
     */
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = event.getEntityModels();

        // Iterate through all player skin types (default, slim)
        for (PlayerModelType skinType : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skinType);
            if (renderer != null) {
                // Add the Frenchie frontpack layer to this player renderer
                renderer.addLayer(new FrontpackLayer(renderer, modelSet));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<AvatarRenderer<?>>() {}, FrontpackAppearance::extract);
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        if (FrontpackClient.shouldPutDown(event.getEntity())) {
            ClientPacketDistributor.sendToServer(PutDownFrenchiePayload.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void drawLabel(RenderPlayerEvent.Post<?> event) {
        var camera = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        FrontpackAppearance.drawLabel(event.getRenderState(), event.getPoseStack(), event.getSubmitNodeCollector(), camera);
    }
}
