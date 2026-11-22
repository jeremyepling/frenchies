package net.j40climb.frenchies.common.entity.frenchie;

import static net.j40climb.frenchies.common.entity.frenchie.PlayStages.Digging.*;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A cosmetic, bounded session beside the owner's excavation. No block or item is changed. */
final class DiggingAssistantGoal extends Goal {
    private static final int MAX_TICKS = 115;
    private final FrenchieEntity dog;
    private @Nullable Vec3 patch;
    private long requestedUntil;
    private int ticks;

    DiggingAssistantGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    boolean request(BlockPos broken) {
        long now = dog.level().getGameTime();
        if (now < dog.nextDigAt || requestedUntil >= now || dog.digStage() != NONE || !available()) return false;
        Vec3 chosen = findPatch(broken);
        if (chosen == null) return false;
        patch = chosen;
        requestedUntil = now + 12;
        return true;
    }

    private boolean available() {
        return dog.canPlay() && dog.isTame() && dog.onGround() && dog.getParcel().isEmpty()
                && dog.getOffhandItem().isEmpty();
    }

    @Override
    public boolean canUse() {
        return patch != null && requestedUntil >= dog.level().getGameTime() && available();
    }

    @Override
    public boolean canContinueToUse() {
        return patch != null && dog.digStage() != NONE && ticks < MAX_TICKS && available()
                && (ticks % 5 != 0 || PlaySafety.clearGroundSegment(dog, dog.position(), patch));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        requestedUntil = -1;
        ticks = 0;
        dog.nextDigAt = dog.level().getGameTime() + 900 + dog.getRandom().nextInt(300);
        dog.setDigStage(APPROACH);
        dog.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (patch == null) return;
        ++ticks;
        switch (dog.digStage()) {
            case APPROACH -> {
                if (dog.distanceToSqr(patch) < 0.4) {
                    dog.getNavigation().stop();
                    dog.setDigStage(DIG);
                    ticks = 0;
                } else if (ticks > 45 || !dog.getNavigation().moveTo(patch.x, patch.y, patch.z, 1.1)) {
                    stop();
                }
            }
            case DIG -> {
                dog.getNavigation().stop();
                BlockPos floor = dog.blockPosition().below();
                BlockState state = dog.level().getBlockState(floor);
                if (!diggable(state) || !PlaySafety.clearGroundSegment(dog, dog.position(), dog.position())) {
                    stop();
                    return;
                }
                if (ticks % 4 == 0 && dog.level() instanceof ServerLevel level) {
                    Vec3 forward = dog.getLookAngle().multiply(0.3, 0, 0.3);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            dog.getX() + forward.x, dog.getY() + 0.15, dog.getZ() + forward.z,
                            5, 0.28, 0.08, 0.28, 0.04);
                }
                if (ticks >= 48) {
                    // A reusable visual prop: never inserted into equipment, inventory or the world.
                    dog.setHasToyStick(true);
                    dog.setDigStage(REVEAL);
                    ticks = 0;
                }
            }
            case REVEAL -> {
                if (ticks >= 18) stop();
            }
            default -> stop();
        }
    }

    private @Nullable Vec3 findPatch(BlockPos broken) {
        BlockPos origin = dog.blockPosition();
        Vec3 from = dog.position();
        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                BlockPos feet = origin.offset(dx, 0, dz);
                Vec3 point = Vec3.atBottomCenterOf(feet);
                if (feet.distSqr(broken) < 9 || point.distanceToSqr(from) > 16
                        || !diggable(dog.level().getBlockState(feet.below()))
                        || !dog.level().getBlockState(feet.below()).isFaceSturdy(dog.level(), feet.below(), Direction.UP)
                        || !PlaySafety.clearGroundSegment(dog, from, point)) continue;
                double distance = point.distanceToSqr(from);
                if (distance < bestDist) {
                    bestDist = distance;
                    best = point;
                }
            }
        }
        return best;
    }

    static boolean diggable(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
    }

    @Override
    public void stop() {
        requestedUntil = -1;
        patch = null;
        dog.setDigStage(NONE);
        dog.getNavigation().stop();
    }
}
