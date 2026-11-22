package net.j40climb.frenchies.common.entity.frenchie.reaction;

import java.util.EnumSet;
import net.j40climb.frenchies.common.entity.frenchie.CompanionSettings;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlaySafety;
import net.j40climb.frenchies.mixin.MiningState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Back out along verified ground before the existing emergency escape is needed. */
final class LavaEdgeGoal extends Goal {
    private final FrenchieEntity dog;
    private long lastProbeTick, nextStep, checkRetreatAt;
    private @Nullable Vec3 lastProbePosition;
    private @Nullable BlockPos lava;
    private @Nullable Vec3 retreat;
    private boolean active;

    LavaEdgeGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    boolean active() { return active; }

    private boolean eligible() {
        return CompanionSettings.lavaReactions() && dog.isTame() && dog.onGround()
                && !dog.isNoAi() && !dog.isPassenger() && !dog.isInWater() && !dog.isInLava()
                && !dog.isEscapingDanger() && !dog.isOrderedToSit() && !dog.isFrenchieSleeping();
    }

    @Override
    public boolean canUse() {
        if (!eligible()) return false;
        long now = dog.level().getGameTime();
        if (lastProbePosition != null) {
            // Probe moving dogs frequently. A stationary dog still notices a newly exposed flow.
            int interval = dog.distanceToSqr(lastProbePosition) < 0.0625 ? 20 : 5;
            if (now - lastProbeTick < interval) return false;
        }
        lastProbePosition = dog.position();
        lastProbeTick = now;
        lava = exposedLava();
        return lava != null;
    }

    private @Nullable BlockPos exposedLava() {
        BlockPos origin = dog.blockPosition();
        BlockPos closest = null;
        double best = 9;
        Vec3 eyes = dog.getEyePosition();
        for (var pos : BlockPos.betweenClosed(origin.offset(-2, -3, -2), origin.offset(2, 0, 2))) {
            if (!dog.level().hasChunkAt(pos) || !dog.level().getFluidState(pos).is(FluidTags.LAVA)) continue;
            Vec3 surface = Vec3.atBottomCenterOf(pos).add(0, 0.9, 0);
            double d = dog.position().distanceToSqr(surface);
            if (d < best && dog.level().clip(new ClipContext(eyes, surface, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, dog)).getType() == HitResult.Type.MISS) {
                closest = pos.immutable();
                best = d;
            }
        }
        return closest;
    }

    @Override
    public void start() {
        dog.getNavigation().stop();
        active = true;
        long now = dog.level().getGameTime();
        retreat = retreatFrom(Vec3.atCenterOf(lava));
        nextStep = now + 12;
        checkRetreatAt = now + 3;
    }

    @Override
    public boolean canContinueToUse() {
        return active && eligible();
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        long now = dog.level().getGameTime();
        if (now >= nextStep) {
            nextStep = now + 12;
            var near = exposedLava();
            retreat = null;
            if (near != null) {
                lava = near;
                retreat = retreatFrom(Vec3.atCenterOf(near));
                checkRetreatAt = now + 3;
            } else if (!waitingNearMiner()) {
                active = false;
                return;
            }
        }
        if (lava == null) return;
        dog.getLookControl().setLookAt(lava.getX() + 0.5, lava.getY() + 0.8, lava.getZ() + 0.5);
        if (retreat == null || dog.distanceToSqr(retreat) <= 0.12) return;
        if (now >= checkRetreatAt) {
            checkRetreatAt = now + 3;
            if (!PlaySafety.clearGroundSegment(dog, dog.position(), retreat, true)) {
                retreat = null;
                return;
            }
        }
        dog.getMoveControl().setWantedPosition(retreat.x, retreat.y, retreat.z, 1.05);
    }

    private boolean waitingNearMiner() {
        return lava != null && dog.getOwner() instanceof ServerPlayer owner && dog.distanceToSqr(owner) < 100
                && owner.blockPosition().distSqr(lava) <= 16 && ((MiningState) owner.gameMode).frenchies$isMining();
    }

    private @Nullable Vec3 retreatFrom(Vec3 danger) {
        Vec3 from = dog.position();
        Vec3 away = from.subtract(danger).multiply(1, 0, 1).normalize();
        if (away.lengthSqr() < 0.01) away = dog.getDirection().getOpposite().getUnitVec3();
        for (double distance : STEP_LENGTHS) {
            for (float angle : TURNS) {
                Vec3 to = from.add(away.yRot(angle).scale(distance));
                if (PlaySafety.clearGroundSegment(dog, from, to, true)) return to;
            }
        }
        return null;
    }

    private static final double[] STEP_LENGTHS = {1.8, 0.75};
    private static final float RIGHT_ANGLE = (float) (Math.PI * 0.5);
    private static final float[] TURNS = {0, 0.55F, -0.55F, 1.1F, -1.1F, RIGHT_ANGLE, -RIGHT_ANGLE};

    @Override
    public void stop() {
        active = false;
        lava = null;
        retreat = null;
        dog.getNavigation().stop();
    }
}
