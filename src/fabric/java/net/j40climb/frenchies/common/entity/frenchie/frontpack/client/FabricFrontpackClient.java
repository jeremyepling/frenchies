package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;

public final class FabricFrontpackClient {
    public static void register() {
        var id = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "front_pack");
        ItemTintSources.ID_MAPPER.put(id, FrontpackTint.CODEC);
        SpecialModelRenderers.ID_MAPPER.put(id, FrontpackItemRenderer.Unbaked.CODEC);
        ModelLayerRegistry.registerModelLayer(FrontpackModel.LAYER_LOCATION, FrontpackModel::createBodyLayer);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, layers, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) {
                layers.register(new FrontpackLayer(avatar, context.getModelSet()));
            }
        });
        HudElementRegistry.addLast(FrontpackClient.WARNING_HUD, FrontpackClient::drawWarning);
    }
}
