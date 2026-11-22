package net.j40climb.frenchies.common.entity.frenchie.reaction;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import net.j40climb.frenchies.common.entity.frenchie.CompanionSettings;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayPose;
import net.j40climb.frenchies.common.entity.frenchie.recall.CompanionRoster;
import net.j40climb.frenchies.mixin.MiningState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A transient resting pose never changes the owner's sit order or the dog's bedtime state. */
final class QuietCompanyGoal extends Goal {
    private final FrenchieEntity dog;
    private @Nullable Vec3 idleAnchor;
    private @Nullable Vec3 visitAnchor;
    private @Nullable Vec3 spot;
    private @Nullable Path approach;
    private long tryAfter, sampleAfter, approachUntil, settledAt;
    private boolean finished;

    QuietCompanyGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean busy(ServerPlayer owner) {
        //? if <26.3 {
        boolean swinging = owner.swinging;
        //?} else {
        /*boolean swinging = owner.isSwinging();
        *///?}
        return !owner.isAlive() || owner.isSpectator() || owner.isSleeping() || owner.isPassenger()
                || owner.isUsingItem() || swinging || owner.isSprinting() || owner.isInWater()
                || ((MiningState) owner.gameMode).frenchies$isMining()
                || owner.getLastHurtMob() != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100
                || owner.getLastHurtByMob() != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100;
    }

    // Observe at a low rate even while higher-priority goals own MOVE.
    void observeOwner() {
        long now = dog.level().getGameTime();
        if (!CompanionSettings.ambientCompanionship() || now < sampleAfter) return;
        sampleAfter = now + 10;
        if (!(dog.getOwner() instanceof ServerPlayer owner) || dog.distanceToSqr(owner) > 100 || busy(owner)) {
            idleAnchor = null;
        } else if (idleAnchor == null || owner.distanceToSqr(idleAnchor) > 0.25) {
            idleAnchor = owner.position();
            tryAfter = now + 400 + dog.getRandom().nextInt(401);
        }
    }

    @Override
    public boolean canUse() {
        long now = dog.level().getGameTime();
        if (idleAnchor == null || now < tryAfter || !dog.canPlay() || dog.playPose() != PlayPose.NONE
                || !(dog.getOwner() instanceof ServerPlayer owner) || busy(owner)) return false;
        tryAfter = now + 400 + dog.getRandom().nextInt(401);
        var level = (ServerLevel) dog.level();
        var anchor = owner.blockPosition();
        var facing = owner.getDirection();
        var side = dog.getRandom().nextBoolean() ? facing.getClockWise() : facing.getCounterClockWise();
        var places = new BlockPos[]{anchor.relative(side, 2), anchor.relative(side.getOpposite(), 2),
                anchor.relative(facing.getOpposite(), 2)};
        var fire = nearbyCampfire(level, anchor);
        Arrays.sort(places, Comparator.comparingInt(pos -> comfortPenalty(level, pos, fire)));
        for (var pos : places) {
            var target = Vec3.atBottomCenterOf(pos);
            approach = null;
            if (dog.distanceToSqr(target) >= 0.7) {
                if (CompanionRoster.safeAt(level, pos) == null) continue;
                approach = dog.getNavigation().createPath(pos, 0);
                if (!CompanionPaths.safeToWalk(dog, approach)) continue;
            }
            visitAnchor = owner.position();
            spot = target;
            return true;
        }
        approach = null;
        return false;
    }

    private static @Nullable BlockPos nearbyCampfire(ServerLevel level, BlockPos owner) {
        for (var pos : BlockPos.withinManhattan(owner, 4, 1, 4)) {
            if (level.hasChunkAt(pos) && CampfireBlock.isLitCampfire(level.getBlockState(pos))) return pos.immutable();
        }
        return null;
    }

    private static int comfortPenalty(ServerLevel level, BlockPos pos, @Nullable BlockPos fire) {
        int penalty = level.canSeeSky(pos) ? 1 : 0;
        if (fire != null && (pos.distSqr(fire) < 4 || pos.distSqr(fire) > 16)) penalty += 2;
        return penalty;
    }

    @Override
    public void start() {
        // The selector has now stopped the previous goal, which may have cleared navigation.
        finished = approach != null && !dog.getNavigation().moveTo(approach, 0.9);
        approach = null;
        settledAt = 0;
        approachUntil = dog.level().getGameTime() + 120;
    }

    @Override
    public boolean canContinueToUse() {
        return !finished && visitAnchor != null && dog.canPlay()
                && dog.getOwner() instanceof ServerPlayer owner && !busy(owner)
                && owner.distanceToSqr(visitAnchor) <= 0.25 && spot != null
                && (settledAt == 0 || dog.distanceToSqr(spot) < 1.2);
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        var owner = dog.getOwner();
        if (owner == null || spot == null) return;
        long now = dog.level().getGameTime();
        if (settledAt == 0) {
            if (dog.distanceToSqr(spot) > 0.7) {
                finished = now >= approachUntil || dog.getNavigation().isDone();
                return;
            }
            dog.getNavigation().stop();
            dog.setPlayPose(PlayPose.AMBIENT_SIT);
            settledAt = now;
        }
        if (now - settledAt == 300) dog.setPlayPose(PlayPose.AMBIENT_REST);
        Vec3 facing = owner.getLookAngle();
        dog.getLookControl().setLookAt(dog.getX() + facing.x * 5, dog.getEyeY(), dog.getZ() + facing.z * 5);
        finished = now - settledAt >= 1200;
    }

    @Override
    public void stop() {
        if (dog.playPose() == PlayPose.AMBIENT_SIT || dog.playPose() == PlayPose.AMBIENT_REST) dog.setPlayPose(PlayPose.NONE);
        dog.getNavigation().stop();
        idleAnchor = visitAnchor = spot = null;
    }
}
