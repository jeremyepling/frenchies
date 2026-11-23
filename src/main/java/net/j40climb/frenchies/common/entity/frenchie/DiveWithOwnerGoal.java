package net.j40climb.frenchies.common.entity.frenchie;

import java.util.EnumSet;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Vanilla water-only pathfinding, with three-dimensional steering on the selected nodes. */
final class DiveWithOwnerGoal extends Goal {
    private final FrenchieEntity dog;
    private @Nullable WaterBoundPathNavigation waterNavigation;
    private @Nullable Path path;
    private @Nullable Path shorePath;
    private int repath;
    private long nextShoreSearch;
    private boolean holdingDepth;
    private boolean wasSwimming;

    DiveWithOwnerGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!dog.wantsToDiveWithOwner()) return false;
        if (dog.isInWater()) return true;
        long now = dog.level().getGameTime();
        if (!dog.onGround() || now < nextShoreSearch) return false;
        nextShoreSearch = now + 35;
        shorePath = findShorePath();
        return shorePath != null && shorePath.canReach();
    }

    @Override
    public boolean canContinueToUse() {
        return dog.wantsToDiveWithOwner() && (dog.isInWater() || !wasSwimming && !dog.getNavigation().isDone());
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        wasSwimming = dog.isInWater();
        if (wasSwimming) {
            dog.getNavigation().stop();
            dog.setSpeed(0);
        } else {
            dog.getNavigation().moveTo(shorePath, 1.12);
        }
        holdingDepth = false;
        repath = 0;
    }

    @Override
    public void tick() {
        LivingEntity owner = dog.getOwner();
        if (owner == null) return;
        dog.getLookControl().setLookAt(owner, 15, 35);
        if (!dog.isInWater()) return;
        if (!wasSwimming) {
            // Fluid contact begins while the dog can still be standing on the dock.
            // Let the ground path carry its center into the water before switching navigation.
            if (!dog.level().getFluidState(dog.blockPosition()).is(FluidTags.WATER)
                    && !dog.getNavigation().isDone()) return;
            dog.getNavigation().stop();
            dog.setSpeed(0);
            wasSwimming = true;
        }
        double distance = dog.distanceToSqr(owner);
        holdingDepth = distance <= (holdingDepth ? 5.0 : 2.8);
        if (holdingDepth) {
            path = null;
            repath = 0;
            drift(Vec3.ZERO);
            return;
        }
        if (repath-- <= 0) {
            repath = 15;
            BlockPos target = owner.blockPosition();
            if (!dog.level().getFluidState(target).is(FluidTags.WATER)) {
                target = BlockPos.containing(owner.getEyePosition());
            }
            if (waterNavigation == null) waterNavigation = new WaterBoundPathNavigation(dog, dog.level());
            path = waterNavigation.createPath(target, 0, 28);
            // The vanilla swim evaluator starts at the bounding-box corner, which may
            // still be inside the bank or just above the surface on the entry tick.
            if (path != null && !path.isDone() && !dog.level().getFluidState(path.getNextNodePos()).is(FluidTags.WATER)) {
                path.advance();
            }
        }
        if (path != null && !path.isDone()) {
            Vec3 next = path.getNextEntityPos(dog).add(0, 0.14, 0);
            if (dog.position().distanceToSqr(next) < 0.26) {
                path.advance();
                if (path.isDone()) {
                    drift(Vec3.ZERO);
                    return;
                }
                next = path.getNextEntityPos(dog).add(0, 0.14, 0);
            }
            if (dog.level().getFluidState(BlockPos.containing(next)).is(FluidTags.WATER)) {
                Vec3 direction = next.subtract(dog.position());
                drift(direction.normalize().scale(Math.min(0.27, direction.length() * 0.6)));
                return;
            }
            path = null;
        }
        drift(Vec3.ZERO);
    }

    private void drift(Vec3 wanted) {
        // Vanilla still applies collisions, fluid drag and currents.
        dog.setDeltaMovement(dog.getDeltaMovement().lerp(wanted.add(0, 0.003, 0), 0.72));
    }

    private @Nullable Path findShorePath() {
        BlockPos feet = dog.blockPosition();
        var level = dog.level();
        var entrances = new HashSet<BlockPos>();
        for (BlockPos candidate : BlockPos.withinManhattan(feet, 6, 2, 6)) {
            if (candidate.distManhattan(feet) > 7 || !level.hasChunkAt(candidate)) continue;
            if (level.getFluidState(candidate).is(FluidTags.WATER)
                    && level.getFluidState(candidate.below()).is(FluidTags.WATER)
                    && level.getBlockState(candidate.above()).isAir()) {
                entrances.add(candidate.immutable());
                if (entrances.size() >= 14) break;
            }
        }
        return entrances.isEmpty() ? null : dog.getNavigation().createPath(entrances, 0);
    }

    @Override
    public void stop() {
        path = null;
        shorePath = null;
        dog.getNavigation().stop();
    }
}
