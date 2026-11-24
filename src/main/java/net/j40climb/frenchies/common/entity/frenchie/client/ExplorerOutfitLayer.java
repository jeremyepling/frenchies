package net.j40climb.frenchies.common.entity.frenchie.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.travel.ExplorerOutfit;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

final class ExplorerOutfitLayer extends RenderLayer<FrenchieRenderState, FrenchieModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE,
            "textures/entity/frenchie/explorer_outfit.png");

    private final ModelPart gear;
    private final ModelPart explorerHat;
    private final ModelPart pirateHat;

    ExplorerOutfitLayer(RenderLayerParent<FrenchieRenderState, FrenchieModel> parent) {
        super(parent);
        var mesh = new MeshDefinition();
        var root = mesh.getRoot();
        var satchel = root.addOrReplaceChild("gear", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.85F, -4.64F, 0.04F, 7.7F, 0.45F, 1.07F)
                .texOffs(0, 4).addBox(3.74F, -4.25F, 0.04F, 0.47F, 2.6F, 1.07F)
                .texOffs(0, 10).addBox(3.84F, -2.64F, -0.075F, 2.05F, 3.7F, 3.75F,
                        new CubeDeformation(0, 0, 1.675F))
                .texOffs(13, 10).addBox(4.53F, -2.8F, -0.185F, 1.22F, 1.25F, 3.97F,
                        new CubeDeformation(0.35F, 0, 1.675F))
                .texOffs(0, 20).addBox(4.42F, -3.46F, 0.24F, 0.97F, 0.97F, 3.44F)
                .texOffs(11, 20).addBox(4.37F, -3.5F, 1.37F, 1.06F, 1.06F, 0.48F),
                PartPose.offset(0, 0, -1.35F));
        // Center both the brass rim and the compass face on the bottom edge of the flap.
        satchel.addOrReplaceChild("snap", CubeListBuilder.create()
                .texOffs(24, 10).addBox(-0.58F, -0.59F, -0.60F, 0.61F, 1.18F, 1.2F,
                        new CubeDeformation(0, 0, 0.60F))
                .texOffs(28, 10).addBox(-0.01F, -0.36F, -0.415F, 0.10F, 0.72F, 0.83F,
                        new CubeDeformation(0, 0, 0.415F)),
                PartPose.offset(6.43F, -1.55F, 1.80F));

        root.addOrReplaceChild("explorer", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-3.05F, -3.77F, -0.72F, 6.1F, 0.46F, 3.8F)
                .texOffs(19, 27).addBox(-1.79F, -4.42F, -0.4F, 3.58F, 0.65F, 2.74F)
                .texOffs(34, 27).addBox(-1.65F, -5.98F, -0.28F, 3.3F, 1.56F, 2.51F), PartPose.ZERO);

        var pirate = root.addOrReplaceChild("pirate", CubeListBuilder.create()
                .texOffs(17, 36).addBox(0.56F, -2.37F, -4.05F, 1.72F, 1.43F, 0.23F)
                .texOffs(24, 36).addBox(1.86F, -2.2F, -4.01F, 1.14F, 0.31F, 0.16F)
                .texOffs(28, 36).addBox(2.95F, -2.2F, -3.91F, 0.14F, 0.31F, 3.83F), PartPose.ZERO);
        // Center the hat over the skull. Keep the eyes and strap in their original head coordinates.
        var tricorn = pirate.addOrReplaceChild("tricorn", CubeListBuilder.create()
                .texOffs(0, 36).addBox(-1.9F, -0.85F, -1.715F, 3.8F, 0.85F, 3.43F,
                        new CubeDeformation(1.18F, 0, 0.45F))
                .texOffs(0, 36).addBox(-1.9F, -2.10F, -1.715F, 3.8F, 1.3F, 3.43F,
                        new CubeDeformation(0.58F, 0, 0.05F)),
                PartPose.offset(0.017F, -3.34F, -1.875F));
        addPirateBrim(tricorn, "front_brim", 0, -2.38F, 7.5F, -0.20F, 0);
        addPirateBrim(tricorn, "left_brim", -1.875F, 0.45F, 6.8F, 0, -0.99F);
        addPirateBrim(tricorn, "right_brim", 1.875F, 0.45F, 6.8F, 0, 0.99F);

        var baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        gear = baked.getChild("gear");
        explorerHat = baked.getChild("explorer");
        pirateHat = baked.getChild("pirate");
    }

    private static void addPirateBrim(PartDefinition hat, String name, float x, float z,
                                      float width, float pitch, float yaw) {
        hat.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(0, 45).addBox(-width / 2, -1.36F, -0.27F, width, 1.46F, 0.54F)
                .texOffs(0, 49).addBox(-width / 2, -1.57F, -0.29F, width, 0.25F, 0.58F),
                PartPose.offsetAndRotation(x, -0.20F, z, pitch, yaw, 0));
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, FrenchieRenderState state,
                       float yaw, float pitch) {
        if (state.explorerOutfit == ExplorerOutfit.NONE || state.isInvisible) return;
        var model = getParentModel();
        model.setupAnim(state);
        poses.pushPose();
        model.translateToChest(poses);
        submitPart(gear, poses, collector, light, state);
        poses.popPose();

        if (state.hideExplorerHeadwear) return;
        poses.pushPose();
        model.translateToHead(poses);
        submitPart(state.explorerOutfit == ExplorerOutfit.PIRATE ? pirateHat : explorerHat, poses, collector, light, state);
        poses.popPose();
    }

    private void submitPart(ModelPart part, PoseStack poses, SubmitNodeCollector collector, int light, FrenchieRenderState state) {
        collector.order(2).submitModelPart(part, poses, RenderTypes.entityCutout(TEXTURE),
                light, LivingEntityRenderer.getOverlayCoords(state, 0F), null);
    }
}
