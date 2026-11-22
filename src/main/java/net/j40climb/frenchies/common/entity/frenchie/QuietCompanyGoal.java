package net.j40climb.frenchies.common.entity.frenchie;

import java.util.EnumSet;
import net.j40climb.frenchies.mixin.MiningState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A transient resting pose never changes the owner's sit order or the dog's bedtime state. */
final class QuietCompanyGoal extends Goal {
    private final FrenchieEntity dog;
    private @Nullable Vec3 ownerAnchor, spot;
    private long tryAfter, sampleAfter, approachUntil, settledAt;
    private boolean finished;

    QuietCompanyGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean busy(ServerPlayer owner) {
        return !owner.isAlive() || owner.isSpectator() || owner.isSleeping() || owner.isPassenger()
                || owner.isUsingItem() || owner.swinging || owner.isSprinting() || owner.isInWater()
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
            ownerAnchor = null;
        } else if (ownerAnchor == null || owner.distanceToSqr(ownerAnchor) > 0.25) {
            ownerAnchor = owner.position();
            tryAfter = now + 400 + dog.getRandom().nextInt(401);
        }
    }

    @Override
    public boolean canUse() {
        long now = dog.level().getGameTime();
        if (ownerAnchor == null || now < tryAfter || !dog.canPlay() || dog.playPose() != PlayPose.NONE
                || !(dog.getOwner() instanceof ServerPlayer owner) || busy(owner)) return false;
        tryAfter = now + 400 + dog.getRandom().nextInt(401);
        var facing = owner.getDirection();
        var side = dog.getRandom().nextBoolean() ? facing.getClockWise() : facing.getCounterClockWise();
        for (var dir : new net.minecraft.core.Direction[]{side, side.getOpposite(), facing.getOpposite()}) {
            var pos = owner.blockPosition().relative(dir, 2);
            var target = Vec3.atBottomCenterOf(pos);
            if (dog.distanceToSqr(target) < 0.7 || CompanionRoster.safeAt((ServerLevel) dog.level(), pos) != null
                    && CompanionPaths.walk(dog, dog.getNavigation().createPath(pos, 0), 0.9)) {
                spot = target;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        finished = false;
        settledAt = 0;
        approachUntil = dog.level().getGameTime() + 120;
    }

    @Override
    public boolean canContinueToUse() {
        return !finished && ownerAnchor != null && dog.canPlay()
                && dog.getOwner() instanceof ServerPlayer owner && !busy(owner)
                && owner.distanceToSqr(ownerAnchor) <= 0.25 && spot != null
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
        ownerAnchor = spot = null;
    }
}
