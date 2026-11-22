package net.j40climb.frenchies.common.entity.frenchie.client;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieVariant;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayPose;
import net.j40climb.frenchies.common.entity.frenchie.reaction.DangerReaction;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.AnimationState;

public class FrenchieRenderState extends LivingEntityRenderState {
    public boolean captain;
    public int travelOwnerId;
    public boolean hasVest;
    public float turnLean;
    public final ItemStackRenderState mouthItem = new ItemStackRenderState();
    public boolean toyStick;
    public boolean presenting;
    public float toyEarLift;
    public FrenchieVariant variant;
    public int celebrationStage;
    public int digStage;
    public float digStageAge;
    public int trickStage;
    public float trickAge;
    public int bellyStage;
    public float bellyAge;
    public PlayPose playPose = PlayPose.NONE;
    public DangerReaction dangerReaction = DangerReaction.NONE;
    public float dangerAge;
    public int weatherStage;
    public int snowLayer;
    public float weatherAge;
    public PoseStack.Pose noseBubbleFacing;
    public boolean isSleeping;
    public boolean isSwimming;
    public boolean isSitting;
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState sleepAnimationState = new AnimationState();
    public final AnimationState swimAnimationState = new AnimationState();
    public final AnimationState standToSitAnimationState = new AnimationState();
    public final AnimationState sitToStandAnimationState = new AnimationState();
    public final AnimationState sitPoseAnimationState = new AnimationState();
    public final AnimationState standToSleepAnimationState = new AnimationState();
    public final AnimationState sleepToStandAnimationState = new AnimationState();

}
