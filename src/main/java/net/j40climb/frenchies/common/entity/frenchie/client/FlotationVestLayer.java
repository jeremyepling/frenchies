package net.j40climb.frenchies.common.entity.frenchie.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

final class FlotationVestLayer extends RenderLayer<FrenchieRenderState, FrenchieModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Frenchies.MOD_ID,
            "textures/entity/frenchie/flotation_vest.png");

    private final ModelPart vest;

    FlotationVestLayer(RenderLayerParent<FrenchieRenderState, FrenchieModel> parent) {
        super(parent);
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("jacket", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.5F, -4.25F, -4.5F, 7, 7, 7, new CubeDeformation(0.32F)), PartPose.ZERO);
        vest = LayerDefinition.create(mesh, 32, 16).bakeRoot();
        // Fit the existing jacket panels from the shoulder to just ahead of the tail.
        ModelPart jacket = vest.getChild("jacket");
        jacket.zScale = 10.0F / 7.0F;
        jacket.z = -5.5F + 4.5F * jacket.zScale;
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, FrenchieRenderState state,
                       float yaw, float pitch) {
        if (!state.hasVest || state.isInvisible) return;
        poses.pushPose();
        getParentModel().setupAnim(state);
        getParentModel().translateToChest(poses);
        collector.order(1).submitModelPart(vest, poses, RenderTypes.entityCutout(TEXTURE),
                light, LivingEntityRenderer.getOverlayCoords(state, 0F), null);
        poses.popPose();
    }
}
