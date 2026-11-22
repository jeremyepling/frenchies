package net.j40climb.frenchies.common.entity.frenchie.play;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;

import static net.j40climb.frenchies.common.entity.frenchie.play.PlayStages.BellyRub.*;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** A deliberately invited, stationary belly-rub session. */
final class BellyRubGoal extends Goal {
    private final FrenchieEntity dog;
    private int invitationTicks;
    private long nextInvitation;
    private long sessionEnd;
    private long sessionLimit;
    private long lastRub;
    private int ticks;
    private ServerPlayer partner;

    boolean invitationPending() { return invitationTicks > 0 || partner != null; }
    long cooldown() { return nextInvitation; }
    void loadCooldown(long tick) { nextInvitation = tick; }

    BellyRubGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** Called on the server. Crouching in place is the invitation; walking past is too brief. */
    void observeInvitation() {
        if (dog.bellyStage() != NONE) return;
        if (dog.level().getGameTime() < nextInvitation || !available() || !dog.play().canWatch(false)) {
            invitationTicks = 0;
            return;
        }
        if (!(dog.getOwner() instanceof ServerPlayer owner) || !owner.isShiftKeyDown()
                || !owner.getMainHandItem().isEmpty() || !owner.getOffhandItem().isEmpty()
                || owner.distanceToSqr(dog) > 6.25 || owner.getDeltaMovement().horizontalDistanceSqr() > 0.0064) {
            invitationTicks = 0;
            return;
        }
        dog.getNavigation().stop();
        dog.setDeltaMovement(0, dog.getDeltaMovement().y, 0);
        if (++invitationTicks >= 20) {
            partner = owner;
            invitationTicks = 0;
        }
    }

    boolean rub(ServerPlayer player) {
        if (dog.bellyStage() != WAITING && dog.bellyStage() != RUBBING) return false;
        if (partner == null || !partner.getUUID().equals(player.getUUID()) || player.distanceToSqr(dog) > 9) return false;
        long now = dog.level().getGameTime();
        lastRub = now;
        sessionEnd = Math.min(sessionLimit, now + 30);
        stage(RUBBING);
        return true;
    }

    void command() {
        nextInvitation = dog.level().getGameTime() + 60;
        if (dog.bellyStage() != NONE) stop();
    }

    private boolean available() {
        return dog.isTame() && dog.isAlive() && dog.onGround() && !dog.isOrderedToSit()
                && !dog.isFrenchieSleeping() && !dog.isPassenger() && !dog.isLeashed()
                && !dog.hasCompanionJob() && !dog.hasToyJob() && !dog.play().interruptedByOtherActivity()
                && dog.trickStage() == PlayStages.Trick.NONE
                && !dog.isInWater() && !dog.isOnFire() && !dog.isInLava()
                && dog.getTarget() == null && dog.hurtTime == 0 && !dog.isInLove()
                && dog.getNavigation().isDone() && dog.getDeltaMovement().horizontalDistanceSqr() < 0.0064
                && PlaySafety.clearGroundSegment(dog, dog.position(), dog.position());
    }

    @Override public boolean canUse() { return partner != null && available() && dog.play().canWatch(false); }

    @Override public boolean canContinueToUse() {
        return dog.bellyStage() != NONE && dog.isAlive() && dog.onGround()
                && !dog.isPassenger() && !dog.isLeashed() && !dog.hasCompanionJob() && !dog.hasToyJob()
                && !dog.play().interruptedByOtherActivity() && !dog.isInWater() && dog.hurtTime == 0
                && partner != null && partner.isAlive() && partner.level() == dog.level()
                && partner.distanceToSqr(dog) <= 16;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void start() {
        dog.getNavigation().stop();
        dog.setDeltaMovement(0, 0, 0);
        sessionEnd = dog.level().getGameTime() + 100;
        sessionLimit = dog.level().getGameTime() + 160;
        lastRub = 0;
        stage(ROLL_OVER);
    }

    @Override public void tick() {
        ++ticks;
        long now = dog.level().getGameTime();
        switch (dog.bellyStage()) {
            case ROLL_OVER -> { if (ticks >= 10) stage(WAITING); }
            case WAITING -> { if (now >= sessionEnd) stage(ROLL_UP); }
            case RUBBING -> { if (now - lastRub >= 15) { sessionEnd = Math.min(sessionLimit, now + 25); stage(WAITING); } }
            case ROLL_UP -> { if (ticks >= 11) stage(NONE); }
        }
    }

    private void stage(int value) {
        ticks = 0;
        dog.setBellyStage(value);
    }

    @Override public void stop() {
        nextInvitation = dog.level().getGameTime() + 60;
        invitationTicks = 0;
        partner = null;
        stage(NONE);
        dog.getNavigation().stop();
    }
}
