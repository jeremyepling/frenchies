package net.j40climb.frenchies.common.entity.frenchie.rest;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Temporarily takes precedence over the sit order while the owner is in a nearby bed. */
public final class BedtimeBuddyGoal extends Goal {
    private final FrenchieEntity dog;
    private int repath;

    public BedtimeBuddyGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    public static void onWake(Player owner, boolean wakeImmediately, boolean updateLevel) {
        // The level calls stopSleepInBed(false, false) when it finishes the night's sleep.
        if (!(owner.level() instanceof ServerLevel level) || wakeImmediately || updateLevel
                || !owner.isSleepingLongEnough()) return;
        boolean hasBuddy = !level.getEntitiesOfClass(FrenchieEntity.class, owner.getBoundingBox().inflate(4),
                dog -> dog.isAlive() && dog.isOwnedBy(owner) && dog.isBedtimeBuddy()
                        && dog.isFrenchieSleeping() && besideBed(dog, owner.getSleepingPos().orElseThrow())).isEmpty();
        if (hasBuddy) {
            owner.addEffect(new MobEffectInstance(FrenchieRegistry.WELL_RESTED, 20 * 120, 0, false, false, true));
            owner.sendOverlayMessage(Component.translatable("frenchie.florafauna.bedtime.rested"));
        }
    }

    public static @Nullable BlockPos ownerBed(FrenchieEntity dog) {
        if (dog.hasCompanionJob() || dog.isInWater() || !(dog.getOwner() instanceof Player owner)) return null;
        var bed = owner.getSleepingPos().orElse(null);
        if (bed == null || dog.distanceToSqr(owner) > 100) return null;
        var state = dog.level().getBlockState(bed);
        return state.getBlock() instanceof BedBlock && state.getValue(BedBlock.OCCUPIED) ? bed : null;
    }

    public static boolean besideBed(FrenchieEntity dog, BlockPos bed) {
        // Both the seated dog and the sleeping player's eyes can be below the mattress.
        return Math.abs(dog.getY() - bed.getY()) < 1
                && Vec3.atBottomCenterOf(bed).distanceToSqr(dog.position()) <= 2.25;
    }

    @Override
    public boolean canUse() {
        return ownerBed(dog) != null;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void tick() {
        BlockPos bed = ownerBed(dog);
        if (bed == null || besideBed(dog, bed)) {
            dog.getNavigation().stop();
            return;
        }
        if (repath-- > 0) return;
        repath = adjustedTickDelay(10);
        // Choose among the open squares around the pillow, excluding the mattress.
        var candidates = Direction.Plane.HORIZONTAL.stream().map(bed::relative)
                .filter(pos -> dog.level().getBlockState(pos).isAir());
        dog.getNavigation().moveTo(dog.getNavigation().createPath(candidates, 0), 1.25);
    }

    @Override
    public void stop() {
        dog.getNavigation().stop();
    }
}
