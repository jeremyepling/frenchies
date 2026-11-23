package net.j40climb.frenchies.common.entity.frenchie;

import java.util.EnumSet;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;

/** First look for a walkable exit. A companion that cannot reach one returns through the roster. */
final class EscapeDangerGoal extends Goal {
    private final FrenchieEntity dog;
    private int hazardTicks;
    private int submergedTicks;
    private int repath;

    EscapeDangerGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    void checkFooting(ServerLevel level) {
        if (dog.getY() < level.getMinY() - 2) {
            CompanionRoster.emergency(dog);
            return;
        }
        submergedTicks = dog.isEyeInFluid(FluidTags.WATER) && !dog.isPassenger() && !dog.inScubaContext()
                ? submergedTicks + 1 : 0;
        if (!dog.isInLava() && !dog.isInPowderSnow && !dog.isInWall() && submergedTicks < 60
                && !touchingDanger()) {
            hazardTicks = 0;
            return;
        }
        if (++hazardTicks == 1) {
            dog.standUp();
            dog.reactToDanger(DangerReaction.STARTLE);
        }
        if (hazardTicks >= 40) CompanionRoster.emergency(dog);
    }

    private boolean touchingDanger() {
        // Include the supporting block (magma, campfires) and a cactus touching the dog's flank.
        for (var pos : BlockPos.betweenClosed(dog.getBoundingBox().inflate(0.03))) {
            if (dog.getType().isBlockDangerous(dog.level().getBlockState(pos))) return true;
        }
        return false;
    }

    @Override
    public boolean canUse() {
        return hazardTicks > 0;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (repath-- > 0) return;
        repath = 9;
        var level = (ServerLevel) dog.level();
        var exits = new HashSet<BlockPos>();
        for (BlockPos pos : BlockPos.withinManhattan(dog.blockPosition(), 3, 2, 3)) {
            if (CompanionRoster.safeAt(level, pos) != null) exits.add(pos.immutable());
            if (exits.size() >= 12) break;
        }
        if (!exits.isEmpty()) dog.getNavigation().moveTo(dog.getNavigation().createPath(exits, 0), 1.35);
    }

    @Override
    public void stop() {
        dog.getNavigation().stop();
    }
}
