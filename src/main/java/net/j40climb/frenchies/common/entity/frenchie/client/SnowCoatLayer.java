package net.j40climb.frenchies.common.entity.frenchie.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** Sparse masks use the head and chest UVs of both coats, following the animated parent model. */
final class SnowCoatLayer extends RenderLayer<FrenchieRenderState, FrenchieModel> {
    private static final Identifier[] TEXTURES = new Identifier[5];
    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE,
                    "textures/entity/frenchie/snow_" + (i + 1) + ".png");
        }
    }

    SnowCoatLayer(RenderLayerParent<FrenchieRenderState, FrenchieModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, FrenchieRenderState state,
                       float yaw, float pitch) {
        if (state.snowLayer > 0 && state.snowLayer <= TEXTURES.length) {
            coloredCutoutModelCopyLayerRender(getParentModel(), TEXTURES[state.snowLayer - 1],
                    poses, collector, light, state, -1, 1);
        }
    }
}
