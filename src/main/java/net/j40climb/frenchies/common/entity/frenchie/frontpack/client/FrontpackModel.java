package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieTravelGesture;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;

/**
 * Model for the Frenchie frontpack that renders on players.
 * This model will be positioned on the player's front.
 *
 */
public class FrontpackModel extends EntityModel<AvatarRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "french_frontpack"), "main");

    private final KeyframeAnimation idlingAnimation;
    private final AnimationState idleAnimationState = new AnimationState();

    // Model pixels in front of the player's chest. The original pouch projected 2 pixels.
    private static final float POUCH_PROJECTION = 4.0F;
    // Restore some of the packed pose's 0.6 depth scale along the player's front-to-back axis.
    private static final float DOG_DEPTH_SCALE = 0.8F / 0.6F;

    private final ModelPart root;
    private final ModelPart head;
    private final boolean fabricOnly;

    public FrontpackModel(ModelPart root) {
        this(root, false);
    }

    FrontpackModel(ModelPart root, boolean fabricOnly) {
        super(root);
        this.fabricOnly = fabricOnly;
        this.root = root.getChild("root");
        this.head = this.root.getChild("carried_dog").getChild("frenchie").getChild("body").getChild("head");

        // Bake the idle animation - converts the AnimationDefinition to a KeyframeAnimation
        this.idlingAnimation = FrontpackAnimations.ANIM_IDLE.bake(root);

        // Start the animation state so it's always active
        this.idleAnimationState.start(0);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition carrier = root.addOrReplaceChild("carrier", CubeListBuilder.create(), PartPose.offset(2.7F, -9.0F, -1.1F));
        // Anchor the top of the original six-pixel texture and extend the hem just below the rump.
        carrier.addOrReplaceChild("pouch", CubeListBuilder.create().texOffs(44, 0).addBox(-6.2F, 0, -3.0F, 7.0F, 6.0F, 3.0F),
                PartPose.ZERO.scaled(1, 6.75F / 6, (1 + POUCH_PROJECTION) / 3).translated(0, 2.0F, 0.1F));

        addShoulderStrap(carrier, "strap_left", 1);
        addShoulderStrap(carrier, "strap_right", -1);

        carrier.addOrReplaceChild("strap_waist", CubeListBuilder.create().texOffs(47, 1).addBox(-0.2F, -1.0F, -3.3F, 0.2F, 1.0F, 3.75F, new CubeDeformation(0.0F))
                .texOffs(58, 5).addBox(0.0F, -1.0F, -3.3F, 1.1F, 1.0F, 0.2F, new CubeDeformation(0.0F))
                .texOffs(46, 7).addBox(0.0F, -1.0F, 0.25F, 8.2F, 1.0F, 0.2F, new CubeDeformation(0.0F))
                .texOffs(58, 5).mirror().addBox(7.1F, -1.0F, -3.3F, 1.1F, 1.0F, 0.2F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(47, 1).addBox(8.2F, -1.0F, -3.3F, 0.2F, 1.0F, 3.75F, new CubeDeformation(0.0F)), PartPose.offset(-6.8F, 7.0F, 3.1F));

        // Map the old pouch front (Z = -4) onto the new one. The paws stay on its face
        // as the dog's body fills out, independently of the upright tilt of its pose.
        float pouchFrontZ = -2 - POUCH_PROJECTION;
        PartDefinition carriedDog = root.addOrReplaceChild("carried_dog", CubeListBuilder.create(),
                PartPose.ZERO.scaled(1, 1, DOG_DEPTH_SCALE).translated(0, 0, pouchFrontZ + 4 * DOG_DEPTH_SCALE));
        PartDefinition frenchie = carriedDog.addOrReplaceChild("frenchie", CubeListBuilder.create(), PartPose.offsetAndRotation(-0.5F, -6.0F, 0.0F, -1.4835F, 0.0F, 0.0F));

        PartDefinition body = frenchie.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 29).addBox(-2.983F, -3.3875F, -3.875F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(30, 21).addBox(-1.983F, -0.3875F, -4.875F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.483F, 1.2582F, -4.2464F, 1.0908F, 0.0F, 0.0F));

        PartDefinition earR = head.addOrReplaceChild("earR", CubeListBuilder.create(), PartPose.offset(-2.733F, -3.0498F, -5.8337F));

        earR.addOrReplaceChild("earR_behind_r1", CubeListBuilder.create().texOffs(1, 21).mirror().addBox(-1.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(1, 24).mirror().addBox(-1.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition earL = head.addOrReplaceChild("earL", CubeListBuilder.create(), PartPose.offset(0.767F, -3.3498F, -5.8337F));

        earL.addOrReplaceChild("earL_behind_r1", CubeListBuilder.create().texOffs(1, 21).addBox(0.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(1, 24).addBox(0.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2618F));

        // The eyes occupy one texel each. Lower a strip of forehead fur over each pupil.
        head.addOrReplaceChild("eyelids", CubeListBuilder.create()
                        .texOffs(5, 34).addBox(-1.983F, 0, 0, 1, 1, 0)
                        .texOffs(8, 34).addBox(1.017F, 0, 0, 1, 1, 0),
                PartPose.offset(0, -1.3875F, -3.90F));
        FrenchieTravelGesture.addTongue(head);

        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(1, 1).addBox(-3.5F, -4.25F, -5.5F, 7.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 1.25F, 1.5F));

        // In the upright pose the nub emerges under the pouch, clear of the player's torso.
        chest.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 8).addBox(-1.0F, -0.5F, 0.25F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 5.25F));

        body.addOrReplaceChild("leg_front_left", CubeListBuilder.create().texOffs(21, 21).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, -2.0F));

        body.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(21, 36).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, -2.0F));

        body.addOrReplaceChild("leg_back_left", CubeListBuilder.create().texOffs(21, 26).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, 5.0F));

        body.addOrReplaceChild("leg_back_right", CubeListBuilder.create().texOffs(21, 31).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, 5.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    private static void addShoulderStrap(PartDefinition carrier, String name, int side) {
        // Both ends share the shoulder's centerline and tuck into the pouch and waist belt.
        carrier.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(47, 1).addBox(-0.55F, -0.1F, -2.46F, 1.1F, 0.35F, 4.92F)
                        .texOffs(49, 2).addBox(-0.55F, 0.25F, -2.45F, 1.1F, 4.5F, 0.2F)
                        .texOffs(55, 0).addBox(-0.55F, 0.25F, 2.24F, 1.1F, 8.25F, 0.2F),
                PartPose.offset(side * 2.85F - 2.7F, -2.45F, 1.1F));
    }

    @Override
    public void setupAnim(AvatarRenderState renderState) {
        super.setupAnim(renderState);
        var pack = FrontpackRenderData.get(renderState);
        if (pack == null) return;
        boolean carrying = pack.coat() != null;
        var carrier = root.getChild("carrier");
        var dog = root.getChild("carried_dog");
        carrier.visible = fabricOnly || pack.tint() == null;
        dog.visible = carrying && !fabricOnly;
        if (!carrying) return;

        float glide = renderState.isFallFlying ? renderState.fallFlyingScale() : 0F;
        if (glide > 0F) {
            // Raise the rim to the shoulders without lifting the seam under the rump.
            var pouch = carrier.getChild("pouch");
            pouch.y -= 2.28F * glide;
            pouch.yScale += 0.38F * glide;
            pouch.zScale += 1.6F * glide;
            dog.z -= 3.8F * glide;
        }
        if (fabricOnly) return;
        this.idlingAnimation.apply(this.idleAnimationState, renderState.ageInTicks, 1.0F);
        var tongue = head.getChild("tongue");
        var lids = head.getChild("eyelids");
        float doze = renderState.isFallFlying || pack.alertYaw() != null ? 0F : pack.doze();
        lids.visible = doze > 0F;
        if (doze > 0F) {
            float breath = (float) Math.sin((renderState.ageInTicks + renderState.id) * 0.10F);
            lids.yScale = 0.82F * doze;
            head.xRot += (0.42F + 0.025F * breath) * doze;
            head.yRot += 0.12F * doze;
            head.getChild("earR").zRot += 0.14F * doze;
            head.getChild("earL").zRot -= 0.14F * doze;
            tongue.visible = false;
        } else if (renderState.isFallFlying) {
            FrenchieTravelGesture.applyInFlight(head, renderState.ageInTicks, renderState.id, glide);
            tongue.visible = true;
        } else {
            float phase = (renderState.ageInTicks + (renderState.id % 220) * 37) % 220;
            float tilt = phase < 44 ? (float) Math.pow(Math.sin(Math.PI * phase / 44), 2) : 0;
            // The dog's upright carrier pose points the parent's Y axis toward the viewer.
            head.yRot += tilt * 0.34F;
            tongue.visible = tilt > 0.35F;
            tongue.z += 0.8F * (1F - tilt);
            tongue.xRot += 0.28F * tilt;
        }
        Float yaw = pack.alertYaw();
        if (yaw != null) {
            head.yRot = (float) Math.toRadians(yaw);
            head.getChild("earR").zRot -= 0.2F;
            head.getChild("earL").zRot += 0.2F;
        }
    }
}
