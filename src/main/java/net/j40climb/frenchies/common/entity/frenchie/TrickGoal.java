package net.j40climb.frenchies.common.entity.frenchie;

import static net.j40climb.frenchies.common.entity.frenchie.PlayStages.Trick.*;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Owner-requested cartoon flop. Only the small upright lap moves the entity. */
final class TrickGoal extends Goal {
    private final FrenchieEntity dog;
    private long requestUntil = -1;
    private long nextTrick;
    private int ticks;
    private int lapPoint;
    private Vec3 center;
    private boolean lapSafe;

    TrickGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    long cooldown() { return nextTrick; }
    void loadCooldown(long tick) { nextTrick = tick; }

    boolean request() {
        long now = dog.level().getGameTime();
        if (now < nextTrick || dog.trickStage() != NONE || !available() || !dog.canWatchPlay()
                || !PlaySafety.clearGroundSegment(dog, dog.position(), dog.position())) return false;
        requestUntil = now + 10;
        return true;
    }

    private boolean available() {
        return dog.isTame() && dog.isAlive() && dog.onGround() && !dog.isOrderedToSit()
                && !dog.isFrenchieSleeping() && !dog.isPassenger() && !dog.isLeashed()
                && !dog.hasCompanionJob() && !dog.hasToyJob() && !dog.personalityInterrupted()
                && dog.bellyStage() == PlayStages.BellyRub.NONE
                && !dog.isInWater() && !dog.isOnFire() && !dog.isInLava()
                && dog.getTarget() == null && dog.hurtTime == 0 && !dog.isInLove();
    }

    @Override public boolean canUse() {
        return dog.level().getGameTime() <= requestUntil && available() && dog.canWatchPlay()
                && PlaySafety.clearGroundSegment(dog, dog.position(), dog.position());
    }

    @Override public boolean canContinueToUse() {
        return dog.trickStage() != NONE && available();
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void start() {
        requestUntil = -1;
        nextTrick = dog.level().getGameTime() + 160;
        center = dog.position();
        lapPoint = 0;
        lapSafe = safeLap();
        dog.getNavigation().stop();
        dog.setDeltaMovement(Vec3.ZERO);
        stage(CROUCH);
    }

    @Override public void tick() {
        ++ticks;
        switch (dog.trickStage()) {
            case CROUCH -> { if (ticks >= 10) stage(SPRING); }
            case SPRING -> { if (ticks >= 9) stage(FLOP); }
            case FLOP -> { if (ticks >= 8) stage(WIGGLE); }
            case WIGGLE -> { if (ticks >= 18) stage(ROLL_UP); }
            case ROLL_UP -> { if (ticks >= 11) stage(lapSafe ? LAP : NONE); }
            case LAP -> {
                if (ticks > 65 || !dog.onGround()) { stage(NONE); break; }
                Vec3 target = lapTarget(lapPoint);
                if (!PlaySafety.clearGroundSegment(dog, dog.position(), target)) { stage(NONE); break; }
                if (dog.distanceToSqr(target) < 0.2) {
                    if (++lapPoint == 4) stage(NONE);
                } else dog.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.4);
            }
        }
    }

    private Vec3 lapTarget(int point) {
        // A small diamond avoids crossing the owner or sweeping a large area.
        double[][] offsets = {{0.9, 0}, {0, 0.9}, {-0.9, 0}, {0, -0.9}};
        return center.add(offsets[point][0], 0, offsets[point][1]);
    }

    private boolean safeLap() {
        Vec3 from = center;
        for (int i = 0; i < 4; i++) {
            Vec3 to = lapTarget(i);
            if (!PlaySafety.clearGroundSegment(dog, from, to)) return false;
            from = to;
        }
        return true;
    }

    private void stage(int value) {
        ticks = 0;
        dog.setTrickStage(value);
        dog.setTrickStageStart(dog.level().getGameTime());
        if (value != LAP) dog.getNavigation().stop();
    }

    @Override public void stop() {
        requestUntil = -1;
        stage(NONE);
        dog.getNavigation().stop();
        dog.setDeltaMovement(Vec3.ZERO);
    }
}
