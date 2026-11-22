package net.j40climb.frenchies.common.entity.frenchie.assistance;

import net.j40climb.frenchies.common.entity.frenchie.CompanionSettings;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlaySafety;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Work outside a flock already walking after wheat. Vanilla keeps control of each animal's route. */
public final class HerdLivestockGoal extends Goal {
    private final FrenchieEntity dog;
    private @Nullable Animal straggler;
    private @Nullable Vec3 lastOwnerPosition;
    private @Nullable Vec3 station;
    private long nextSample;
    private long walkingUntil;

    public HerdLivestockGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long now = dog.level().getGameTime();
        if (!CompanionSettings.automaticHerding() || !(dog.getOwner() instanceof Player owner)
                || !holdsWheat(owner) || !owner.isAlive() || owner.isSpectator() || dog.distanceToSqr(owner) > 16 * 16) {
            lastOwnerPosition = null;
            walkingUntil = 0;
            return false;
        }
        if (now < nextSample || !dog.canPlay()) return false;
        nextSample = now + 10;
        sampleHerd(owner, now);
        return station != null;
    }

    private void sampleHerd(Player owner, long now) {
        Vec3 current = owner.position();
        if (lastOwnerPosition != null) {
            double moved = current.subtract(lastOwnerPosition).horizontalDistanceSqr();
            if (moved > 0.04 && moved < 16) walkingUntil = now + 35;
        }
        lastOwnerPosition = current;
        straggler = null;
        station = null;
        if (now >= walkingUntil) return;
        straggler = dog.level().getEntitiesOfClass(Animal.class, owner.getBoundingBox().inflate(11, 3, 11),
                        animal -> (animal instanceof Sheep || animal instanceof Cow) && following(animal, owner))
                .stream().max(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
        if (straggler != null) station = chooseStation(straggler, owner);
    }

    private static boolean holdsWheat(Player player) {
        return player.getMainHandItem().is(Items.WHEAT) || player.getOffhandItem().is(Items.WHEAT);
    }

    private static boolean following(Animal animal, Player owner) {
        var target = animal.getNavigation().getTargetPos();
        double distance = animal.distanceToSqr(owner);
        return target != null && !animal.getNavigation().isDone() && target.distSqr(owner.blockPosition()) <= 2
                && animal.isAlive() && animal.onGround() && !animal.isInLove() && !animal.isLeashed()
                && !animal.isPassenger() && animal.getTarget() == null && animal.hurtTime == 0
                && distance >= 9 && distance <= 110 && animal.hasLineOfSight(owner);
    }

    private @Nullable Vec3 chooseStation(Animal animal, Player owner) {
        Vec3 rear = animal.position().subtract(owner.position()).multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-rear.z, 0, rear.x);
        Vec3 relative = dog.position().subtract(animal.position());
        if (relative.dot(side) < 0) side = side.scale(-1);
        for (int direction : new int[]{1, -1}) {
            Vec3 flank = side.scale(direction * 2.1);
            Vec3 spot = animal.position().add(rear.scale(1.5)).add(flank);
            if (PlaySafety.clearGroundSegment(dog, dog.position(), spot)) return spot;
            // If stock blocks the diagonal, first move abreast on its outside.
            spot = animal.position().add(rear.scale(Math.min(-0.5, relative.dot(rear)))).add(flank.scale(1.5));
            if (PlaySafety.clearGroundSegment(dog, dog.position(), spot)) return spot;
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!CompanionSettings.automaticHerding() || !dog.canPlay() || !(dog.getOwner() instanceof Player owner)
                || !owner.isAlive() || owner.isSpectator() || !holdsWheat(owner) || dog.distanceToSqr(owner) > 16 * 16) return false;
        long now = dog.level().getGameTime();
        if (now >= nextSample) {
            nextSample = now + 10;
            sampleHerd(owner, now);
        }
        return station != null && straggler != null && straggler.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        if (station == null || straggler == null || !(dog.getOwner() instanceof Player owner)) return;
        dog.getLookControl().setLookAt(straggler, 25, 20);
        // The sampled straight segment rejects herd members, hazards and drops.
        // Drive it directly so navigation cannot choose a shortcut through the flock.
        if (dog.distanceToSqr(station) > 0.5 && PlaySafety.clearGroundSegment(dog, dog.position(),
                dog.position().add(station.subtract(dog.position()).normalize().scale(0.6)))) {
            dog.getMoveControl().setWantedPosition(station.x, station.y, station.z, 1.15);
        }
        if (dog.tickCount % 10 != 0 || dog.distanceToSqr(straggler) > 10 || !following(straggler, owner)) return;
        Vec3 towardOwner = owner.position().subtract(straggler.position()).multiply(1, 0, 1).normalize();
        if (dog.position().subtract(straggler.position()).dot(towardOwner) > 0.25) return;
        var path = straggler.getNavigation().getPath();
        if (path == null || path.isDone()) return;
        Vec3 step = path.getNextEntityPos(straggler).subtract(straggler.position()).multiply(1, 0, 1).normalize();
        if (step.dot(towardOwner) < 0.7 || straggler.getDeltaMovement().dot(step) > 0.1) return;
        if (PlaySafety.clearGroundSegment(straggler, straggler.position(), straggler.position().add(step.scale(1.5)))) {
            straggler.push(step.x * 0.055, 0, step.z * 0.055);
        }
    }

    @Override
    public void start() {
        dog.getNavigation().stop();
    }

    @Override
    public void stop() {
        straggler = null;
        station = null;
        dog.getNavigation().stop();
    }
}
