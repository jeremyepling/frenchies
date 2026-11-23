package net.j40climb.frenchies.common.entity.frenchie;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Short-range cave and fear following. Ordinary long-range recall stays with the owner goal. */
final class AdventureFollowGoal extends Goal {
    private final FrenchieEntity dog;
    private long retryAt;
    private boolean reachedOwner;

    AdventureFollowGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean eligible(LivingEntity owner) {
        return owner != null && owner.isAlive() && owner.level() == dog.level() && !owner.isInWater()
                && !dog.isInWater() && dog.canTakeAdventureMovement() && !dog.waitingAtLava()
                && (dog.isFrightened() || dog.miningWithOwner()) && dog.distanceToSqr(owner) <= 24 * 24;
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = dog.getOwner();
        return eligible(owner) && dog.level().getGameTime() >= retryAt
                && (dog.isFrightened() || dog.distanceToSqr(owner) > 6 * 6);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = dog.getOwner();
        return eligible(owner) && (dog.isFrightened() || dog.distanceToSqr(owner) > 3.5 * 3.5)
                && (reachedOwner || !dog.getNavigation().isDone() || dog.level().getGameTime() >= retryAt);
    }

    @Override
    public void start() {
        reachedOwner = false;
        retryAt = 0;
        tick();
    }

    @Override
    public void tick() {
        LivingEntity owner = dog.getOwner();
        if (owner == null) return;
        dog.getLookControl().setLookAt(owner, 12, 30);
        if (dog.distanceToSqr(owner) <= 3 * 3) {
            if (!reachedOwner) dog.getNavigation().stop();
            reachedOwner = true;
            return;
        }
        long now = dog.level().getGameTime();
        if (now >= retryAt) {
            reachedOwner = false;
            boolean started = dog.getNavigation().moveTo(owner, dog.isFrightened() ? 1.43 : 1.15);
            retryAt = now + (started ? 14 : 65);
        }
    }

    @Override
    public void stop() {
        dog.getNavigation().stop();
    }
}
