package net.j40climb.frenchies.common.entity.frenchie.recall;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Give the caller a moment to greet the dog or collect its parcel before idle movement resumes. */
public final class WhistleApproachGoal extends Goal {
    private final FrenchieEntity dog;
    private int repath;

    public WhistleApproachGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return dog.respondingToWhistle() && !dog.isOrderedToSit() && !dog.isPassenger() && !dog.hasCompanionJob()
                && dog.getOwner() != null && dog.getOwner().isAlive();
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void tick() {
        LivingEntity owner = dog.getOwner();
        if (owner == null) return;
        dog.getLookControl().setLookAt(owner, 30, 25);
        if (dog.distanceToSqr(owner) <= 4) dog.getNavigation().stop();
        else if (--repath <= 0) {
            dog.getNavigation().moveTo(owner, 1.25);
            repath = adjustedTickDelay(8);
        }
    }

    @Override
    public void stop() {
        dog.getNavigation().stop();
    }
}
