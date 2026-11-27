package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.client.AdventureOutfitModels;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieVariant;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the Frenchie frontpack on players who are carrying a Frenchie.
 * This layer is added to the player renderer and checks the FrenchFrontpackData attachment.
 * Uses variant-specific textures for fawn and brindle Frenchies.
 * The texture for this model is the same as the frenchie entity
 */
public class FrontpackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final AdventureOutfitModels clothes = new AdventureOutfitModels();

    static final Identifier FABRIC = Identifier.withDefaultNamespace("textures/block/white_wool.png");

    private final FrontpackModel frontpackModel;
    private final FrontpackModel dyedFabricModel;

    // Variant-specific textures for the entire frontpack model
    static final Identifier FAWN_TEXTURE =
            Identifier.fromNamespaceAndPath(Frenchies.MOD_ID,
                    "textures/entity/frenchie/frenchie_fawn_texture.png");

    private static final Identifier BRINDLE_TEXTURE =
            Identifier.fromNamespaceAndPath(Frenchies.MOD_ID,
                    "textures/entity/frenchie/frenchie_brindle_texture.png");

    public FrontpackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet modelSet) {
        super(parent);

        // Bake the frontpack model from registered layer definition
        this.frontpackModel = new FrontpackModel(
                modelSet.bakeLayer(FrontpackModel.LAYER_LOCATION)
        );
        this.dyedFabricModel = new FrontpackModel(modelSet.bakeLayer(FrontpackModel.LAYER_LOCATION), true);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector,
                      int packedLight, AvatarRenderState renderState, float yRot, float xRot) {

        var pack = FrontpackRenderData.get(renderState);
        if (pack == null || renderState.isInvisible) return;

        Identifier texture = pack.coat() == FrenchieVariant.BRINDLE ? BRINDLE_TEXTURE : FAWN_TEXTURE;

        // Position the frontpack model on the player's front
        poseStack.pushPose();

        // Align with the player's body part (this positions us at the player's torso)
        this.getParentModel().body.translateAndRotate(poseStack);

        // Adjust position: Y negative = up, X negative = left, Z positive = toward back
        poseStack.translate(0.0, -0.8, 0.0);

        // Render the frontpack model with the variant-specific texture
        renderColoredCutoutModel(
                this.frontpackModel,
                texture,
                poseStack,
                nodeCollector,
                packedLight,
                renderState,
                0xFFFFFFFF, // White (no color tint)
                0 // No outline
        );

        Integer tint = pack.tint();
        if (tint != null) {
            renderColoredCutoutModel(dyedFabricModel, FABRIC, poseStack, nodeCollector, packedLight,
                    renderState, tint, renderState.outlineColor);
        }
        if (pack.aviator()) {
            frontpackModel.setupAnim(renderState);
            var clothesCollector = nodeCollector.order(2);
            int overlay = LivingEntityRenderer.getOverlayCoords(renderState, 0);
            // The carried cap moves toward the front of the pouch in carrier coordinates.
            poseStack.pushPose();
            poseStack.translate(0, 0, -1F / 16);
            frontpackModel.translateToHead(poseStack);
            clothes.submitAviatorCap(poseStack, clothesCollector, packedLight, overlay);
            poseStack.popPose();

            poseStack.pushPose();
            frontpackModel.translateToHead(poseStack);
            clothes.submitAviatorScarf(poseStack, clothesCollector, packedLight, overlay, renderState.ageInTicks, pack.flying());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

}
