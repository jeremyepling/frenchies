package net.j40climb.frenchies.common.entity.frenchie;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Stay within a few strides of the owner while exploring a cave. */
final class AdventureFollowGoal extends Goal {
    private final FrenchieEntity dog;
    private long retryAt;

    AdventureFollowGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean eligible(LivingEntity owner) {
        return owner != null && owner.isAlive() && owner.level() == dog.level() && !owner.isInWater()
                && !dog.isInWater() && dog.canTakeAdventureMovement() && dog.miningWithOwner()
                && !dog.isFrightened() && dog.distanceToSqr(owner) <= 24 * 24;
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = dog.getOwner();
        return eligible(owner) && dog.level().getGameTime() >= retryAt
                && dog.distanceToSqr(owner) > 6 * 6;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity owner = dog.getOwner();
        return eligible(owner) && dog.distanceToSqr(owner) > 3.5 * 3.5 && !dog.getNavigation().isDone();
    }

    @Override
    public void start() {
        retryAt = 0;
        tick();
    }

    @Override
    public void tick() {
        LivingEntity owner = dog.getOwner();
        if (owner == null) return;
        dog.getLookControl().setLookAt(owner, 12, 30);
        long now = dog.level().getGameTime();
        if (now >= retryAt) {
            boolean started = dog.getNavigation().moveTo(owner, 1.15);
            retryAt = now + (started ? 14 : 65);
        }
    }

    @Override
    public void stop() {
        dog.getNavigation().stop();
    }
}
