package net.j40climb.frenchies.common.entity.frenchie.client;

import net.j40climb.frenchies.common.entity.frenchie.play.PlayPose;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayStages;
import net.j40climb.frenchies.common.entity.frenchie.reaction.DangerReaction;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class FrenchieModel extends EntityModel<FrenchieRenderState> {

    public static final ModelLayerLocation FRENCHIE = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "frenchie"), "frenchie"); // is path is folder under textures/entity?

    private final KeyframeAnimation walkingAnimation;
    private final KeyframeAnimation swimmingAnimation;
    private final KeyframeAnimation sleepingAnimation;
    private final KeyframeAnimation idlingAnimation;
    private final KeyframeAnimation standToSitAnimation;
    private final KeyframeAnimation sitToStandAnimation;
    private final KeyframeAnimation sitPoseAnimation;
    private final KeyframeAnimation standToSleepAnimation;
    private final KeyframeAnimation sleepToStandAnimation;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;

    public FrenchieModel(ModelPart root) {
        super(root);
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");

        this.walkingAnimation = FrenchieAnimations.ANIM_WALK.bake(root);
        this.swimmingAnimation = FrenchieAnimations.ANIM_SWIM.bake(root);
        this.sleepingAnimation = FrenchieAnimations.ANIM_SLEEP.bake(root);
        this.idlingAnimation = FrenchieAnimations.ANIM_IDLE.bake(root);
        this.standToSitAnimation = FrenchieAnimations.ANIM_STAND_TO_SIT.bake(root);
        this.sitToStandAnimation = FrenchieAnimations.ANIM_SIT_TO_STAND.bake(root);
        this.sitPoseAnimation = FrenchieAnimations.ANIM_SIT.bake(root);
        this.standToSleepAnimation = FrenchieAnimations.ANIM_STAND_TO_SLEEP.bake(root);
        this.sleepToStandAnimation = FrenchieAnimations.ANIM_SLEEP_TO_STAND.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 18.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 29).addBox(-2.983F, -3.3875F, -3.875F, 6.0F, 6.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(30, 21).addBox(-1.983F, -0.3875F, -4.875F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.483F, -1.6125F, -4.125F));

        PartDefinition earR = head.addOrReplaceChild("earR", CubeListBuilder.create(), PartPose.offset(-2.733F, -3.0498F, -3.8337F));

        earR.addOrReplaceChild("earR_behind_r1", CubeListBuilder.create().texOffs(1, 21).mirror().addBox(-1.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(1, 24).mirror().addBox(-1.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition earL = head.addOrReplaceChild("earL", CubeListBuilder.create(), PartPose.offset(0.767F, -3.3498F, -3.8337F));

        earL.addOrReplaceChild("earL_behind_r1", CubeListBuilder.create().texOffs(1, 21).addBox(0.0F, -2.8377F, 2.9587F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(1, 24).addBox(0.0F, -2.8377F, 2.9087F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.2618F));

        FrenchieTravelGesture.addTongue(head);

        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(1, 1).addBox(-3.5F, -4.25F, -5.5F, 7.0F, 7.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 1.25F, 1.5F));

        chest.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 8).addBox(-1.0F, -0.5F, 0.25F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.75F, 5.25F));

        body.addOrReplaceChild("leg_front_left", CubeListBuilder.create().texOffs(21, 21).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, -2.0F));

        body.addOrReplaceChild("leg_front_right", CubeListBuilder.create().texOffs(21, 36).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, -2.0F));

        body.addOrReplaceChild("leg_back_left", CubeListBuilder.create().texOffs(21, 26).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, 3.0F, 5.0F));

        body.addOrReplaceChild("leg_back_right", CubeListBuilder.create().texOffs(21, 31).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 3.0F, 5.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(FrenchieRenderState renderState) {
        super.setupAnim(renderState);
        head.getChild("tongue").visible = renderState.captain;

        if (renderState.dangerReaction != DangerReaction.NONE) {
            if (renderState.captain || renderState.isSitting) sitPoseAnimation.applyStatic();
            applyDangerReaction(renderState);
            return;
        }
        if (renderState.trickStage != PlayStages.Trick.NONE) {
            applyTrick(renderState);
            return;
        }
        if (renderState.bellyStage != PlayStages.BellyRub.NONE) {
            applyBellyRub(renderState);
            return;
        }

        if (renderState.digStage == PlayStages.Digging.DIG) {
            // Deep shoulder dip and alternating paw sweeps read as digging at normal distance.
            body.y += 1.1F;
            body.xRot += 0.36F;
            head.xRot += 0.40F;
            float stroke = (float) Math.sin(renderState.digStageAge * 1.45F);
            ModelPart left = body.getChild("leg_front_left");
            ModelPart right = body.getChild("leg_front_right");
            left.xRot = -0.85F + 0.95F * stroke;
            right.xRot = -0.85F - 0.95F * stroke;
            left.z -= 0.55F;
            right.z -= 0.55F;
            body.getChild("leg_back_left").xRot = 0.35F;
            body.getChild("leg_back_right").xRot = 0.35F;
            return;
        }
        if (renderState.digStage == PlayStages.Digging.REVEAL) {
            body.xRot -= 0.27F;
            body.y -= 0.5F;
            head.xRot -= 0.35F;
            body.getChild("leg_front_left").xRot = -0.55F;
            body.getChild("leg_front_right").xRot = -0.55F;
            return;
        }
        if (renderState.weatherStage != PlayStages.Weather.NONE) {
            applyWeatherPlay(renderState);
            return;
        }

        if (renderState.playPose == PlayPose.SLIME_BOW || renderState.playPose == PlayPose.BUBBLE_BOW) {
            // Pivot the chest down, stretch the forepaws out and keep the hind paws planted.
            body.xRot = 0.42F;
            body.y += 0.3F;
            ModelPart left = body.getChild("leg_front_left");
            ModelPart right = body.getChild("leg_front_right");
            left.xRot = right.xRot = -1.12F;
            left.z -= 0.65F;
            right.z -= 0.65F;
            body.getChild("leg_back_left").y += 1.9F;
            body.getChild("leg_back_right").y += 1.9F;
            applyHeadRotation(renderState.yRot, renderState.xRot * 0.4F);
            head.xRot -= 0.25F;
            return;
        }
        if (renderState.playPose == PlayPose.AMBIENT_SIT) {
            sitPoseAnimation.applyStatic();
            applyHeadRotation(renderState.yRot, renderState.xRot);
            return;
        }
        if (renderState.playPose == PlayPose.AMBIENT_REST) {
            body.y += 1.9F;
            head.y += 1.0F;
            head.xRot = -0.1F;
            body.getChild("leg_front_left").xRot = -1.25F;
            body.getChild("leg_front_right").xRot = -1.25F;
            // Splay the hind paws beyond the hips so the resting silhouette reads from either side.
            ModelPart left = body.getChild("leg_back_left");
            ModelPart right = body.getChild("leg_back_right");
            left.xRot = right.xRot = 1.4F;
            left.yRot = 0.38F;
            right.yRot = -0.38F;
            left.z += 0.9F;
            right.z += 0.9F;
            return;
        }

        if (renderState.captain) {
            sitPoseAnimation.applyStatic();
            body.zRot += renderState.turnLean;
            head.xRot -= 0.12F;
            FrenchieTravelGesture.applyInBoat(head, renderState.ageInTicks, renderState.travelOwnerId);
            applyEndermanLook(renderState);
            return;
        }

        // Don't rotate head/body when sleeping or sitting
        if (!renderState.isSleeping && (!renderState.isSitting || renderState.presenting)) {
            this.applyHeadRotation(renderState.yRot, renderState.xRot);
        }

        // Apply walk animation (swimming uses its own animation state below)
        if (!renderState.isSwimming) {
            this.walkingAnimation.applyWalk(renderState.walkAnimationPos, renderState.walkAnimationSpeed, 2.5F, 2.5F);
        }

        // Apply swim animation when in water
        if (renderState.isSwimming) {
            this.swimmingAnimation.apply(renderState.swimAnimationState, renderState.ageInTicks, 1.0F);
        }

        // Apply sleeping transitions and pose
        this.standToSleepAnimation.apply(renderState.standToSleepAnimationState, renderState.ageInTicks, 1.0F);
        this.sleepToStandAnimation.apply(renderState.sleepToStandAnimationState, renderState.ageInTicks, 1.0F);
        this.sleepingAnimation.apply(renderState.sleepAnimationState, renderState.ageInTicks, 1.0F);
        if (renderState.sideSleepAmount > 0) applySideSleep(renderState.sideSleepAmount);

        // Apply sitting transitions and pose
        this.standToSitAnimation.apply(renderState.standToSitAnimationState, renderState.ageInTicks, 1.0F);
        this.sitToStandAnimation.apply(renderState.sitToStandAnimationState, renderState.ageInTicks, 1.0F);
        this.sitPoseAnimation.apply(renderState.sitPoseAnimationState, renderState.ageInTicks, 1.0F);

        // Apply idle animation
        this.idlingAnimation.apply(renderState.idleAnimationState, renderState.ageInTicks, 1.0F);
        float celebration = renderState.isSleeping || renderState.isSwimming || renderState.presenting ? 0f : renderState.celebrationBow;
        if (celebration > 0f) {
            float bow = celebration * (1f - Mth.clamp(renderState.walkAnimationSpeed * 10f, 0f, 1f));
            head.xRot -= 0.25F * celebration;
            if (!renderState.isSitting && renderState.playPose == PlayPose.NONE) {
                body.xRot += 0.32F * bow;
                body.y += 1.2F * bow;
                body.getChild("leg_front_left").xRot -= 1.05F * bow;
                body.getChild("leg_front_right").xRot -= 1.05F * bow;
            }
        }
        float attention = Math.max(renderState.toyEarLift, celebration);
        if (renderState.playPose == PlayPose.SLIME_NOTICE || renderState.playPose == PlayPose.BUBBLE_NOSE) {
            head.zRot += renderState.playPose == PlayPose.SLIME_NOTICE ? 0.23F : -0.15F;
            attention = 1;
        }
        if (renderState.playPose == PlayPose.BUBBLE_WATCH && (renderState.isSleeping || renderState.isSitting)) {
            head.yRot += Mth.clamp(renderState.yRot, -25F, 25F) * Mth.DEG_TO_RAD;
            head.xRot -= 0.1F;
        }
        if (attention > 0 && !renderState.isSleeping && !renderState.isSitting) {
            float lift = attention * (renderState.isBaby ? 0.15F : 0.3F);
            head.getChild("earL").zRot -= lift;
            head.getChild("earR").zRot += lift;
            head.getChild("earL").y -= lift;
            head.getChild("earR").y -= lift;
        }
        applyEndermanLook(renderState);
    }

    private void applySideSleep(float blend) {
        // Roll the curled pose around the ribcage and bring the cheek to the ground.
        float roll = blend * blend * (3 - 2 * blend);
        body.zRot += 1.38F * roll;
        body.x += 2.6F * roll;
        body.y += 0.65F * roll;
        head.zRot -= 0.22F * roll;
        head.yRot -= 0.32F * roll;
        body.getChild("leg_front_left").xRot -= 0.85F * roll;
        body.getChild("leg_front_right").xRot -= 0.55F * roll;
    }

    private void applyDangerReaction(FrenchieRenderState state) {
        float t = Math.clamp(state.dangerAge / state.dangerReaction.duration, 0, 1);
        float envelope = (float) Math.sin(Math.PI * t);
        applyHeadRotation(state.yRot, state.xRot);
        switch (state.dangerReaction) {
            case STARTLE -> {
                if (!state.isSitting && !state.captain && t < 0.5F) {
                    body.y -= 1.8F * (float) Math.sin(Math.PI * t * 2);
                }
                head.zRot += 0.34F * envelope;
                head.yRot -= 0.28F * envelope;
                head.getChild("earL").zRot -= 0.30F * envelope;
            }
            case DUCK -> {
                float crouch = Math.min(t * 8, 1) * Math.clamp((0.70F - t) * 4, 0, 1);
                body.y += 1.4F * crouch;
                head.y += 0.9F * crouch;
                head.xRot += 0.32F * crouch;
                head.getChild("earL").zRot += 1.05F * crouch;
                head.getChild("earR").zRot -= 1.05F * crouch;
                float peek = (float) Math.sin(Math.PI * Math.clamp((t - 0.45F) / 0.55F, 0, 1));
                head.yRot += 0.42F * peek;
                head.xRot -= 0.23F * peek;
            }
            case TUMBLE -> {
                if (t < 0.55F) {
                    float roll = t / 0.55F;
                    body.zRot += 2 * (float) Math.PI * roll;
                    body.y -= 2.0F * (float) Math.sin(Math.PI * roll);
                } else {
                    float shake = (t - 0.55F) / 0.45F;
                    head.yRot += (float) (0.45 * Math.sin(4 * Math.PI * shake) * (1 - shake));
                }
            }
            default -> {}
        }
    }

    private void applyWeatherPlay(FrenchieRenderState state) {
        float t = state.weatherAge;
        ModelPart foreLeft = body.getChild("leg_front_left");
        ModelPart foreRight = body.getChild("leg_front_right");
        ModelPart hindLeft = body.getChild("leg_back_left");
        ModelPart hindRight = body.getChild("leg_back_right");
        switch (state.weatherStage) {
            case PlayStages.Weather.SHAKE -> {
                root.zRot = 0.28F * Mth.sin(t * 2.7F);
                head.zRot = 0.42F * Mth.sin(t * 3.4F);
                body.yRot = 0.22F * Mth.sin(t * 2.7F);
                foreLeft.xRot = foreRight.xRot = -0.25F;
            }
            case PlayStages.Weather.GRASS_APPROACH, PlayStages.Weather.SNOW_APPROACH ->
                    walkingAnimation.applyWalk(state.walkAnimationPos, state.walkAnimationSpeed, 2.5F, 2.5F);
            case PlayStages.Weather.GRASS_SHOULDER -> {
                root.zRot = 0.9F * Math.min(1, t / 7F);
                root.y += 2.0F;
                foreLeft.xRot = -1.1F;
                foreRight.xRot = -0.4F;
            }
            case PlayStages.Weather.GRASS_ROLL -> {
                root.zRot = 0.9F + 5.383F * Math.min(1, t / 27F);
                root.y += 2.7F;
                foreLeft.xRot = foreRight.xRot = -1.15F;
                hindLeft.xRot = hindRight.xRot = 1.25F;
            }
            case PlayStages.Weather.GRASS_DRAG -> {
                root.y += 2.3F;
                body.xRot = 0.12F;
                foreLeft.xRot = -1.35F + 0.3F * Mth.sin(t * 1.4F);
                foreRight.xRot = -1.35F - 0.3F * Mth.sin(t * 1.4F);
                hindLeft.xRot = hindRight.xRot = 1.2F;
            }
            case PlayStages.Weather.SNOW_DIVE -> {
                root.xRot = 0.95F * Math.min(1, t / 6F);
                root.y += 1.4F;
                foreLeft.xRot = foreRight.xRot = -0.8F;
                hindLeft.xRot = hindRight.xRot = 0.5F;
            }
            case PlayStages.Weather.SNOW_PLOW -> {
                root.xRot = 0.38F;
                root.y += 1.8F;
                foreLeft.xRot = -0.7F + 0.55F * Mth.sin(t * 1.8F);
                foreRight.xRot = -0.7F - 0.55F * Mth.sin(t * 1.8F);
                hindLeft.xRot = hindRight.xRot = 0.5F;
            }
            case PlayStages.Weather.SNOW_POP -> {
                root.y -= 1.0F * Mth.sin((float) Math.PI * Math.min(1, t / 11F));
                head.xRot = -0.55F;
                foreLeft.xRot = foreRight.xRot = -0.55F;
            }
        }
    }

    private void applyTrick(FrenchieRenderState state) {
        float t = state.trickAge;
        switch (state.trickStage) {
            case PlayStages.Trick.CROUCH -> {
                body.y += Math.min(t / 5F, 1F) * 1.9F;
                body.xRot = 0.32F;
                head.xRot = -0.24F;
                body.getChild("leg_front_left").xRot = -0.6F;
                body.getChild("leg_front_right").xRot = -0.6F;
            }
            case PlayStages.Trick.SPRING -> {
                float arc = (float) Math.sin(Math.PI * Math.clamp(t / 9F, 0F, 1F));
                body.y -= 3.2F * arc;
                body.xRot = -0.48F * Math.min(t / 9F, 1F);
                body.getChild("leg_front_left").xRot = -0.8F;
                body.getChild("leg_front_right").xRot = -0.8F;
                body.getChild("leg_back_left").xRot = 0.7F;
                body.getChild("leg_back_right").xRot = 0.7F;
            }
            case PlayStages.Trick.FLOP -> {
                float progress = Math.clamp(t / 6F, 0F, 1F);
                body.xRot = 0.45F + progress * 2.65F;
                body.y += progress * 2F;
                splayPaws();
            }
            case PlayStages.Trick.WIGGLE -> {
                body.xRot = 3.1F;
                body.y += 2F;
                body.zRot = (float) Math.sin(t * 1.8F) * 0.22F;
                splayPaws();
                body.getChild("leg_back_right").xRot += (float) Math.sin(t * 2F) * 0.65F;
            }
            case PlayStages.Trick.ROLL_UP -> {
                float progress = Math.clamp(t / 11F, 0F, 1F);
                body.xRot = 3.1F + progress * (float) Math.PI;
                body.y += 2F * (1F - progress);
                splayPaws();
            }
            case PlayStages.Trick.LAP -> {
                walkingAnimation.applyWalk(state.walkAnimationPos, Math.max(0.35F, state.walkAnimationSpeed), 2.5F, 2.5F);
                body.y -= (float) Math.abs(Math.sin(state.ageInTicks * 0.8F)) * 0.6F;
            }
        }
    }

    private void applyBellyRub(FrenchieRenderState state) {
        float t = state.bellyAge;
        float progress = state.bellyStage == PlayStages.BellyRub.ROLL_OVER ? Math.clamp(t / 10F, 0F, 1F)
                : state.bellyStage == PlayStages.BellyRub.ROLL_UP ? 1F - Math.clamp(t / 11F, 0F, 1F) : 1F;
        body.zRot = progress * (float) Math.PI;
        body.y += 2F * progress;
        splayPaws();
        if (state.bellyStage == PlayStages.BellyRub.RUBBING) {
            // Move one hind paw beyond the flank so its full kick silhouette reads at play distance.
            float kick = Mth.sin(t * 0.85F);
            ModelPart leg = body.getChild("leg_back_right");
            leg.x -= 2.0F;
            leg.z += 1.0F;
            leg.xRot = 0.65F + kick * 1.45F;
            leg.zRot = 0.8F + kick * 0.35F;
            body.zRot += kick * 0.08F;
        }
    }

    private void splayPaws() {
        body.getChild("leg_front_left").xRot = -0.9F;
        body.getChild("leg_front_right").xRot = -0.9F;
        body.getChild("leg_back_left").xRot = 0.8F;
        body.getChild("leg_back_right").xRot = 0.8F;
        body.getChild("leg_front_left").zRot = -0.38F;
        body.getChild("leg_front_right").zRot = 0.38F;
        body.getChild("leg_back_left").zRot = -0.38F;
        body.getChild("leg_back_right").zRot = 0.38F;
    }

    void translateToChest(PoseStack pose) {
        root.translateAndRotate(pose);
        body.translateAndRotate(pose);
        body.getChild("chest").translateAndRotate(pose);
    }

    void translateToHead(PoseStack pose) {
        root.translateAndRotate(pose);
        body.translateAndRotate(pose);
        head.translateAndRotate(pose);
    }

    void translateToMouth(PoseStack pose) {
        translateToHead(pose);
        pose.translate(0, 3.0 / 16, -5.5 / 16);
    }

    private void applyEndermanLook(FrenchieRenderState state) {
        if (state.endermanLook > 0 && !state.isSleeping && !state.isSwimming) {
            float pitch = Mth.lerp(state.endermanLook, head.xRot * Mth.RAD_TO_DEG, 40F);
            applyHeadRotation(head.yRot * Mth.RAD_TO_DEG, pitch);
        }
    }

    private void applyHeadRotation(float headYaw, float headPitch) {
        headYaw = Mth.clamp(headYaw, -30f, 30f);
        headPitch = Mth.clamp(headPitch, -32.5f, 45f);

        this.head.yRot = headYaw * ((float)Math.PI / 180f);
        this.head.xRot = headPitch *  ((float)Math.PI / 180f);
    }
}
