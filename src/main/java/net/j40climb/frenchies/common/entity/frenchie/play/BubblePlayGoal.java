package net.j40climb.frenchies.common.entity.frenchie.play;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Each invitation reserves one bubble for one dog. Neither side saves the invitation. */
public final class BubblePlayGoal extends Goal {
    private final FrenchieEntity dog;
    private int targetId = -1;
    private long deadline;
    private long nextInvitation;
    private boolean watchOnly;
    private boolean sleepySpectator;
    private boolean running;
    private boolean recovering;
    private int stageTicks;
    private int blockedTicks;
    private int pounces;

    public BubblePlayGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private PlayBubbleEntity bubble() {
        return dog.level().getEntity(targetId) instanceof PlayBubbleEntity b && b.isAlive() ? b : null;
    }

    public boolean engaged() {
        return targetId != -1 && dog.level().getGameTime() < deadline && (recovering || bubble() != null);
    }

    public boolean offer(PlayBubbleEntity bubble) {
        long now = dog.level().getGameTime();
        if (running || engaged() || now < nextInvitation || dog.level() != bubble.level()
                || dog.distanceToSqr(bubble) > 12 * 12 || !dog.hasLineOfSight(bubble)) return false;
        boolean resting = dog.isOrderedToSit() || dog.isSitting() || dog.isFrenchieSleeping();
        if (!dog.play().canWatch(false) || (!resting && !dog.onGround())) return false;
        if (!bubble.claim(dog)) return false;
        targetId = bubble.getId();
        watchOnly = resting;
        sleepySpectator = dog.isFrenchieSleeping();
        // A resting spectator can share the sit goal. Set flags before the selector considers the invitation.
        setFlags(watchOnly ? EnumSet.of(Flag.LOOK) : EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        deadline = now + (sleepySpectator ? 32 : 10);
        if (sleepySpectator) stage(PlayPose.BUBBLE_WATCH);
        nextInvitation = now + 100 + dog.getRandom().nextInt(41);
        return true;
    }

    @Override
    public boolean canUse() {
        return !sleepySpectator && engaged() && available();
    }

    // Sleeping Frenchies are immobile, so vanilla skips their goal selector and look control.
    public void tickSleepySpectator() {
        if (!sleepySpectator) return;
        PlayBubbleEntity bubble = bubble();
        if (!dog.isFrenchieSleeping() || !engaged() || !dog.play().canWatch(false) || bubble == null || !dog.hasLineOfSight(bubble)) {
            stop();
            return;
        }
        dog.getLookControl().setLookAt(bubble, 20, 15);
        dog.getLookControl().tick();
    }

    public void wakeUp() {
        if (sleepySpectator) stop();
    }

    private boolean available() {
        if (watchOnly) return (dog.isOrderedToSit() || dog.isSitting() || dog.isFrenchieSleeping()) && dog.play().canWatch(false);
        return dog.canPlay();
    }

    @Override
    public boolean canContinueToUse() {
        return engaged() && available();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        running = true;
        deadline = dog.level().getGameTime() + (watchOnly ? 32 : 90);
        blockedTicks = 0;
        pounces = 0;
        if (!watchOnly) halt();
        stage(PlayPose.BUBBLE_WATCH);
    }

    @Override
    public void tick() {
        stageTicks++;
        if (recovering) {
            if (stageTicks > 2 && dog.onGround() || stageTicks >= 12) deadline = 0;
            return;
        }
        PlayBubbleEntity bubble = bubble();
        if (bubble == null || !dog.hasLineOfSight(bubble) || dog.distanceToSqr(bubble) > 144) {
            deadline = 0;
            return;
        }
        if (dog.playPose() == PlayPose.BUBBLE_NOSE) {
            // Looking straight ahead keeps the muzzle steady as the dog examines the bubble.
            if (stageTicks >= 13) {
                bubble.pop(true);
                if (dog.onGround() && PlaySafety.clearGroundSegment(dog, dog.position(), dog.position())) {
                    dog.setDeltaMovement(0, 0.25, 0);
                }
                recover();
            }
            return;
        }
        dog.getLookControl().setLookAt(bubble.getX(), bubble.getY() + 0.09, bubble.getZ(), 32F, 30F);
        if (watchOnly) return;
        switch (dog.playPose()) {
            case BUBBLE_WATCH -> {
                if (stageTicks >= 5) stage(PlayPose.BUBBLE_APPROACH);
            }
            case BUBBLE_APPROACH -> approach(bubble);
            case BUBBLE_BOW -> {
                // Look control alone turns the head only partway when a drifting bubble passes the shoulder.
                dog.lookAt(bubble, 35, 25);
                dog.setYBodyRot(dog.getYRot());
                dog.setYHeadRot(dog.getYRot());
                if (stageTicks >= 8) pounce(bubble);
            }
            case BUBBLE_POUNCE -> {
                if (PlayBubbleEntity.nosePosition(dog).distanceToSqr(bubble.position()) <= 0.43 * 0.43) {
                    if (bubble.getRandom().nextInt(4) == 0) {
                        bubble.settleOn(dog);
                        stage(PlayPose.BUBBLE_NOSE);
                    } else {
                        bubble.pop(true);
                        recover();
                    }
                } else if (stageTicks > 2 && dog.onGround() && pounces < 2) {
                    stage(PlayPose.BUBBLE_APPROACH);
                } else if (stageTicks >= 12 || stageTicks > 2 && dog.onGround()) {
                    deadline = 0;
                }
            }
            default -> deadline = 0;
        }
    }

    private void approach(PlayBubbleEntity bubble) {
        Vec3 offset = bubble.position().subtract(dog.position()).multiply(1, 0, 1);
        double height = bubble.getY() - PlayBubbleEntity.nosePosition(dog).y;
        if (height > 1.65 || height < -0.8) {
            deadline = 0;
            return;
        }
        if (offset.lengthSqr() <= 0.82 * 0.82) {
            halt();
            if (dog.onGround() && height <= 0.72 && height >= -0.4) stage(PlayPose.BUBBLE_BOW);
            return;
        }
        if (!dog.onGround()) {
            deadline = 0;
            return;
        }
        Vec3 direction = offset.normalize();
        // Try a short step around a dog that is pursuing a neighboring bubble.
        float side = dog.getId() % 2 == 0 ? 0.85F : -0.85F;
        if (step(direction) || step(direction.yRot(side)) || step(direction.yRot(-side))) {
            blockedTicks = 0;
        } else {
            halt();
            if (++blockedTicks >= 6) deadline = 0;
        }
    }

    private boolean step(Vec3 direction) {
        Vec3 goal = dog.position().add(direction.scale(0.73));
        if (!PlaySafety.clearGroundSegment(dog, dog.position(), goal)) return false;
        dog.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, 0.96);
        return true;
    }

    private void pounce(PlayBubbleEntity bubble) {
        Vec3 intercept = bubble.position().add(bubble.getDeltaMovement().scale(3));
        Vec3 offset = intercept.subtract(dog.position()).multiply(1, 0, 1);
        if (offset.lengthSqr() > 1.25 * 1.25) {
            stage(PlayPose.BUBBLE_APPROACH);
            return;
        }
        Vec3 reach = offset.normalize().scale(Math.min(0.70, Math.max(0, offset.length() - 0.48)));
        if (!dog.onGround() || !PlaySafety.clearGroundSegment(dog, dog.position(), dog.position().add(reach))) {
            deadline = 0;
            return;
        }
        dog.setDeltaMovement(reach.scale(0.17).add(0, 0.26, 0));
        ++pounces;
        stage(PlayPose.BUBBLE_POUNCE);
    }

    private void recover() {
        recovering = true;
        stage(PlayPose.BUBBLE_POUNCE);
    }

    private void stage(PlayPose pose) {
        stageTicks = 0;
        dog.setPlayPose(pose);
    }

    private void halt() {
        dog.getNavigation().stop();
        dog.getMoveControl().setWantedPosition(dog.getX(), dog.getY(), dog.getZ(), 0);
    }

    @Override
    public void stop() {
        PlayBubbleEntity bubble = bubble();
        if (bubble != null) bubble.release(dog);
        if (!watchOnly) halt();
        stage(PlayPose.NONE);
        targetId = -1;
        sleepySpectator = false;
        recovering = false;
        running = false;
    }
}
