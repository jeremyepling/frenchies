package net.j40climb.frenchies.common.entity.frenchie;

import static net.j40climb.frenchies.common.entity.frenchie.PlayStages.Celebration.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** A short-lived movement goal. Commands and jobs can preempt any stage. */
final class CelebrationGoal extends Goal {
    private static final int[][] LAP = {
            {1, 1}, {0, 2}, {-1, 1}, {-2, 0}, {-1, -1}, {0, -2}, {1, -1}, {2, 0}
    };

    private final FrenchieEntity dog;
    private long requestUntil = -1;
    private boolean guest;
    private BlockPos center;
    private int waypoint;
    private int stageTicks;

    CelebrationGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    boolean requestedOrRunning() {
        return requestUntil >= dog.level().getGameTime() || dog.celebrationStage() != NONE;
    }

    boolean request(boolean asGuest) {
        long now = dog.level().getGameTime();
        if (now < dog.nextCelebration || requestedOrRunning() || !available()) {
            return false;
        }
        guest = asGuest;
        requestUntil = now + 12;
        return true;
    }

    private boolean available() {
        return dog.weatherStage() == PlayStages.Weather.NONE && !dog.personalityInterrupted()
                && dog.isAlive() && dog.isTame() && !dog.isOrderedToSit() && !dog.isSitting()
                && !dog.isFrenchieSleeping() && !dog.isPassenger() && !dog.isLeashed()
                && !dog.isInWater() && !dog.isInLava() && !dog.isOnFire() && dog.hurtTime == 0
                && dog.getTarget() == null && !dog.isInLove() && !dog.hasCompanionJob() && !dog.hasToyJob();
    }

    @Override
    public boolean canUse() {
        return requestUntil >= dog.level().getGameTime() && dog.onGround() && available();
    }

    @Override
    public boolean canContinueToUse() {
        return dog.celebrationStage() != NONE && available();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        requestUntil = -1;
        dog.nextCelebration = dog.level().getGameTime() + 2400 + dog.getRandom().nextInt(1200);
        dog.getNavigation().stop();
        center = dog.blockPosition().west(2);
        waypoint = 0;
        stage(BOW);
        if (!guest && dog.getRandom().nextBoolean()) {
            for (FrenchieEntity friend : dog.level().getEntitiesOfClass(FrenchieEntity.class,
                    dog.getBoundingBox().inflate(5), f -> f != dog && f.getNavigation().isDone())) {
                if (friend.requestGuestCelebration()) break;
            }
        }
    }

    @Override
    public void tick() {
        ++stageTicks;
        switch (dog.celebrationStage()) {
            case BOW -> {
                if (stageTicks >= 12) {
                    if (safeLap()) stage(RUN);
                    else finishRun();
                }
            }
            case RUN -> {
                Vec3 target = point(waypoint);
                if (stageTicks > 120 || !dog.onGround() || !clearSegment(dog.position(), target)) {
                    finishRun();
                } else if (dog.distanceToSqr(target) < 0.17) {
                    if (++waypoint == (guest ? LAP.length : LAP.length * 2)) finishRun();
                } else {
                    dog.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.55);
                }
            }
            case HOP -> {
                if (stageTicks > 1 && dog.onGround()) stage(SPLOOT);
                else if (stageTicks > 15) stage(NONE);
            }
            case SPLOOT -> {
                if (stageTicks >= 22) stage(NONE);
            }
        }
    }

    private void finishRun() {
        if (dog.onGround() && clearSegment(dog.position(), dog.position())) {
            dog.setDeltaMovement(0, 0.24, 0);
            stage(HOP);
        } else {
            stage(dog.onGround() ? SPLOOT : NONE);
        }
    }

    private void stage(int stage) {
        stageTicks = 0;
        dog.setCelebrationStage(stage);
    }

    private Vec3 point(int index) {
        int[] offset = LAP[index % LAP.length];
        return Vec3.atBottomCenterOf(center.offset(offset[0], 0, offset[1]));
    }

    private boolean safeLap() {
        Vec3 from = dog.position();
        for (int i = 0; i < LAP.length; ++i) {
            Vec3 to = point(i);
            if (!clearSegment(from, to)) return false;
            from = to;
        }
        return true;
    }

    // Check the dog's whole swept footprint, including the clearance needed for a hop.
    private boolean clearSegment(Vec3 from, Vec3 to) {
        double margin = dog.getBbWidth() / 2.0 + 0.15;
        AABB sweep = new AABB(from, to).inflate(margin, 0, margin).expandTowards(0, 1.5, 0);
        if (!dog.level().getWorldBorder().isWithinBounds(sweep)
                || !dog.level().getEntities(dog, sweep, e -> e instanceof LivingEntity || e.isPushable()).isEmpty()) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(sweep.minX, from.y, sweep.minZ),
                BlockPos.containing(sweep.maxX, from.y, sweep.maxZ))) {
            BlockPos floor = pos.below();
            BlockState support = dog.level().getBlockState(floor);
            // Restrict running to familiar, full, stable ground. Unknown modded floors may damage or move a pet.
            boolean ordinaryFloor = support.is(BlockTags.DIRT) || support.is(BlockTags.PLANKS)
                    || support.is(BlockTags.BASE_STONE_OVERWORLD) || support.is(Blocks.COBBLESTONE)
                    || support.is(Blocks.STONE_BRICKS) || support.is(Blocks.SMOOTH_STONE);
            if (!ordinaryFloor || !support.isFaceSturdy(dog.level(), floor, Direction.UP)
                    || !dog.level().getBlockState(pos).isAir() || !dog.level().getBlockState(pos.above()).isAir()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void stop() {
        requestUntil = -1;
        stage(NONE);
        dog.getNavigation().stop();
    }
}
