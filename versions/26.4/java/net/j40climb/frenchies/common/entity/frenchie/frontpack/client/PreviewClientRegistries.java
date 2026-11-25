package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderer;
import net.j40climb.frenchies.common.entity.frenchie.client.PlayBubbleRenderer;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayBubbleEntity;
import net.minecraft.client.core.registries.BuiltInClientRegistries;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public final class PreviewClientRegistries {
    private PreviewClientRegistries() {}

    public static void registerRenderers() {
        EntityRendererProvider<FrenchieEntity> frenchie = FrenchieRenderer::new;
        EntityRendererProvider<PlayBubbleEntity> bubble = PlayBubbleRenderer::new;
        Registry.register(BuiltInClientRegistries.ENTITY_RENDERER, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frenchie"), frenchie);
        Registry.register(BuiltInClientRegistries.ENTITY_RENDERER, Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "play_bubble"), bubble);
    }

    public static void registerFrontpackModels(Identifier id) {
        Registry.register(BuiltInClientRegistries.ITEM_TINT_SOURCE_TYPE, id, FrontpackTint.CODEC);
        Registry.register(BuiltInClientRegistries.SPECIAL_MODEL_RENDERER_TYPE, id, FrontpackItemRenderer.Unbaked.CODEC);
    }
}
