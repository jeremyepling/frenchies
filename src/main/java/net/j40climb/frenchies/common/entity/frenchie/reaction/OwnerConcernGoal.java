package net.j40climb.frenchies.common.entity.frenchie.reaction;

import java.util.EnumSet;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlaySafety;
import net.j40climb.frenchies.common.entity.frenchie.recall.CompanionRoster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;

final class OwnerConcernGoal extends Goal {
    private final FrenchieEntity dog;
    private long thunderUntil, fallUntil, nextFall, nextSound, nextPath, nextThreatCheck;
    private boolean running, nearbyThreat;
    private int failedPaths;
    private BlockPos lastOwnerPos;

    OwnerConcernGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    void hearThunder() {
        if (!dog.canRespondToOwner()) return;
        long now = dog.level().getGameTime();
        thunderUntil = Math.max(thunderUntil, now + 300 + dog.getRandom().nextInt(301));
        dog.setFrenchieSleeping(false);
    }

    void ownerFell() {
        long now = dog.level().getGameTime();
        if (now < nextFall || now < thunderUntil || !dog.canRespondToOwner()) return;
        nextFall = now + 600;
        fallUntil = now + 110;
        dog.setFrenchieSleeping(false);
    }

    boolean active() { return running; }

    boolean pending() {
        long now = dog.level().getGameTime();
        return (now < thunderUntil || now < fallUntil) && dog.canRespondToOwner();
    }

    @Override
    public boolean canUse() {
        long now = dog.level().getGameTime();
        if ((now >= thunderUntil && now >= fallUntil) || !dog.canRespondToOwner()) return false;
        if (now >= nextThreatCheck) {
            nearbyThreat = PlaySafety.dangerNearby(dog, dog.getOwner());
            nextThreatCheck = now + 10;
        }
        return !nearbyThreat;
    }

    @Override
    public void start() {
        running = true;
        failedPaths = 0;
        nextPath = 0;
        lastOwnerPos = null;
        dog.getNavigation().stop();
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        long now = dog.level().getGameTime();
        var owner = dog.getOwner();
        boolean frightened = now < thunderUntil;
        if (owner == null || !owner.isAlive() || owner.isSpectator() || dog.distanceToSqr(owner) > 32 * 32) {
            if (!frightened) { fallUntil = 0; return; }
            if (nextPath != Long.MAX_VALUE) {
                nextPath = Long.MAX_VALUE;
                dog.getNavigation().stop();
                seekCover();
            }
        } else {
            dog.getLookControl().setLookAt(owner, 35, 30);
            if (dog.distanceToSqr(owner) < 3.25 && Math.abs(dog.getY() - owner.getY()) < 1.2) {
                dog.getNavigation().stop();
                if (!frightened) fallUntil = Math.min(fallUntil, now + 36);
            } else if (now >= nextPath || nextPath == Long.MAX_VALUE) {
                var ownerPos = owner.blockPosition();
                if (!ownerPos.equals(lastOwnerPos)) failedPaths = 0;
                if (failedPaths < 2 && !CompanionPaths.walkBeside(dog, ownerPos, 1.3)) {
                    dog.getNavigation().stop();
                    if (++failedPaths == 2 && !frightened) fallUntil = Math.min(fallUntil, now + 26);
                }
                lastOwnerPos = ownerPos;
                nextPath = now + 20;
            }
        }
        if (now >= nextSound) {
            //? if <26.4 {
            var sound = dog.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                    .getOrThrow(WolfSoundVariants.PUGLIN).value().adultSounds().whineSound().value();
            dog.level().playSound(null, dog.getX(), dog.getY(), dog.getZ(), sound, SoundSource.NEUTRAL, 0.32F, 1.12F);
            //?} else {
            /*dog.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                    .getOrThrow(WolfSoundVariants.PUGLIN).value().adultSounds().whineSound().ifPresent(event ->
                            dog.level().playSound(null, dog.getX(), dog.getY(), dog.getZ(), event.value(), SoundSource.NEUTRAL, 0.32F, 1.12F));
            *///?}
            nextSound = now + 140 + dog.getRandom().nextInt(101);
        }
    }

    private void seekCover() {
        if (!dog.level().canSeeSky(dog.blockPosition())) return;
        var level = (ServerLevel) dog.level();
        var candidates = new java.util.HashSet<BlockPos>();
        for (var pos : BlockPos.withinManhattan(dog.blockPosition(), 5, 1, 5)) {
            if (!level.canSeeSky(pos) && CompanionRoster.safeAt(level, pos) != null) candidates.add(pos.immutable());
            if (candidates.size() >= 8) break;
        }
        if (!candidates.isEmpty()) CompanionPaths.walk(dog, dog.getNavigation().createPath(candidates, 0), 1.12);
    }

    @Override
    public void stop() {
        running = false;
        thunderUntil = fallUntil = 0;
        dog.getNavigation().stop();
    }
}
