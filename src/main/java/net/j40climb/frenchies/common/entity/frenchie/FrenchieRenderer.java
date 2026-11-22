package net.j40climb.frenchies.common.entity.frenchie;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FrenchieRenderer extends MobRenderer<FrenchieEntity, FrenchieRenderState, FrenchieModel> {

    // Scale factors for Frenchie entity - adjust these to resize the model
    // 1.0 = normal size, 0.5 = half size, 1.5 = 1.5x size
    private static final float ADULT_SCALE = 0.8f;
    private static final float BABY_SCALE = 0.4f;

    private static final Map<FrenchieVariant, Identifier> TEXTURE_LOCATION_BY_VARIANT =
            Util.make(Maps.newEnumMap(FrenchieVariant.class), map -> {
                map.put(FrenchieVariant.FAWN,
                        Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "textures/entity/frenchie/frenchie_fawn_texture.png"));
                map.put(FrenchieVariant.BRINDLE,
                        Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "textures/entity/frenchie/frenchie_brindle_texture.png"));
            });

    private final ItemModelResolver itemResolver;

    public FrenchieRenderer(EntityRendererProvider.Context context) {
        super(context, new FrenchieModel(context.bakeLayer(FrenchieModel.FRENCHIE)), 0.4f);
        addLayer(new SnowCoatLayer(this));
        addLayer(new FlotationVestLayer(this));
        itemResolver = context.getItemModelResolver();
        addLayer(new FrenchieMouthLayer(this));
    }

    @Override
    protected @Nullable Component getNameTag(FrenchieEntity dog) {
        var status = FrenchieStatusIcon.of(dog);
        return status == null ? super.getNameTag(dog) : status.label(dog.hasCustomName() ? dog.getDisplayName() : null);
    }

    @Override
    public Identifier getTextureLocation(FrenchieRenderState renderState) {
        return TEXTURE_LOCATION_BY_VARIANT.get(renderState.variant);
    }

    @Override
    public void submit(FrenchieRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState) {
        renderState.noseBubbleFacing = null;
        if (renderState.playPose == PlayPose.BUBBLE_NOSE) {
            poseStack.pushPose();
            //? if <26.3 {
            poseStack.mulPose(cameraRenderState.orientation);
            //?} else {
            /*poseStack.rotate(cameraRenderState.orientation);
            *///?}
            renderState.noseBubbleFacing = poseStack.last().copy();
            poseStack.popPose();
        }
        if(renderState.isBaby) {
            poseStack.scale(BABY_SCALE, BABY_SCALE, BABY_SCALE);
        } else {
            poseStack.scale(ADULT_SCALE, ADULT_SCALE, ADULT_SCALE);
        }
        super.submit(renderState, poseStack, submitNodeCollector, cameraRenderState);
    }

    @Override
    public FrenchieRenderState createRenderState() {
        return new FrenchieRenderState();
    }

    @Override
    public void extractRenderState(FrenchieEntity entity, FrenchieRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.captain = false;
        reusedState.turnLean = 0F;
        if (entity.getVehicle() instanceof AbstractBoat boat) {
            reusedState.captain = true;
            var owner = entity.getOwner();
            reusedState.travelOwnerId = owner == null ? entity.getId() : owner.getId();
            reusedState.bodyRot = Mth.rotLerp(partialTick, boat.yRotO, boat.getYRot());
            reusedState.turnLean = Mth.clamp(Mth.wrapDegrees(boat.getYRot() - boat.yRotO) * 0.023F, -0.16F, 0.16F);
        }
        reusedState.hasVest = entity.hasFlotationVest();
        itemResolver.updateForLiving(reusedState.mouthItem, entity.getMouthItem(),
                ItemDisplayContext.GROUND, entity);
        reusedState.toyStick = entity.isShowingToyStick();
        reusedState.presenting = entity.handoffPhase() != 0;
        reusedState.toyEarLift = entity.toyEarLift(partialTick);
        reusedState.variant = entity.getVariant();
        reusedState.celebrationStage = entity.celebrationStage();
        reusedState.digStage = entity.digStage();
        reusedState.digStageAge = entity.digStageAge(partialTick);
        reusedState.trickStage = entity.trickStage();
        reusedState.trickAge = entity.level().getGameTime() - entity.trickStageStart() + partialTick;
        reusedState.bellyStage = entity.bellyStage();
        reusedState.bellyAge = entity.level().getGameTime() - entity.bellyStageStart() + partialTick;
        reusedState.playPose = entity.playPose();
        reusedState.dangerReaction = entity.dangerReaction();
        reusedState.dangerAge = entity.dangerReactionAge(partialTick);
        reusedState.weatherStage = entity.weatherStage();
        reusedState.snowLayer = entity.snowLayer();
        reusedState.weatherAge = entity.weatherAge(partialTick);

        reusedState.isSleeping = entity.isFrenchieSleeping();
        reusedState.isSwimming = entity.isSwimming();
        reusedState.isSitting = entity.getPose() == Pose.SITTING;
        reusedState.idleAnimationState.copyFrom(entity.idleAnimationState);
        reusedState.sleepAnimationState.copyFrom(entity.sleepAnimationState);
        reusedState.swimAnimationState.copyFrom(entity.swimAnimationState);
        reusedState.standToSitAnimationState.copyFrom(entity.standToSitAnimationState);
        reusedState.sitToStandAnimationState.copyFrom(entity.sitToStandAnimationState);
        reusedState.sitPoseAnimationState.copyFrom(entity.sitPoseAnimationState);
        reusedState.standToSleepAnimationState.copyFrom(entity.standToSleepAnimationState);
        reusedState.sleepToStandAnimationState.copyFrom(entity.sleepToStandAnimationState);
    }
}
