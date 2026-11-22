package net.j40climb.frenchies.common.entity.frenchie.play;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.reaction.FrenchieAwareness;

import java.util.Comparator;
import java.util.EnumSet;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A short invitation to play. The dog owns the entire session, including the cooldown. */
final class SlimeTeaseGoal extends Goal {
    private static final double PLAY_RADIUS = 2.1;
    private static final int SESSION_LIMIT = 190;
    private final FrenchieEntity dog;
    private long nextSlimePlayAt;
    private @Nullable Slime slime;
    private long nextScan;
    private long finishAt;
    private int stageTicks;
    private int roundsLeft;
    private int hopsLeft;
    private int nextHopTick;
    private int hopSide;
    private double circleAngle;
    private boolean barkedDuringTease;

    long cooldown() { return nextSlimePlayAt; }
    void loadCooldown(long tick) { nextSlimePlayAt = tick; }

    SlimeTeaseGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        long now = dog.level().getGameTime();
        if (now < nextSlimePlayAt || now < nextScan) return false;
        nextScan = now + 16 + dog.getRandom().nextInt(7);
        if (!dog.onGround() || !dog.canPlay()) return false;
        slime = dog.level().getEntitiesOfClass(Slime.class, dog.getBoundingBox().inflate(7),
                        candidate -> tinyAndVisible(candidate) && dog.distanceToSqr(candidate) <= 49)
                .stream().sorted(Comparator.comparingDouble(dog::distanceToSqr))
                .filter(candidate -> clear(radialSpot(candidate)) && openSpaceBetween(candidate))
                .findFirst().orElse(null);
        return slime != null;
    }

    private boolean tinyAndVisible(Slime candidate) {
        double heightDifference = Math.abs(candidate.getY() - dog.getY());
        return candidate.getType() == EntityType.SLIME && candidate.getSize() == 1 && candidate.isAlive()
                && heightDifference < 1.65 && (!candidate.onGround() || !dog.onGround() || heightDifference < 0.6)
                && dog.hasLineOfSight(candidate);
    }

    // A dog can see over a fence or across a trench. Check the ground between the ring and the slime too.
    private boolean openSpaceBetween(Slime target) {
        double angle = bearing(target);
        // The axis-aligned footprints are widest across their diagonals.
        double clearance = (dog.getBbWidth() + target.getBbWidth()) * Math.sqrt(0.5) + 0.1;
        Vec3 close = new Vec3(target.getX() + clearance * Math.cos(angle), dog.getY(),
                target.getZ() + clearance * Math.sin(angle));
        return PlaySafety.clearGroundSegment(dog, radialSpot(target), close);
    }

    @Override
    public boolean canContinueToUse() {
        return slime != null && dog.canPlay() && tinyAndVisible(slime)
                && dog.distanceToSqr(slime) <= 81 && dog.level().getGameTime() < finishAt
                && (!dog.onGround() || dog.tickCount % 8 != 0 || openSpaceBetween(slime));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        long now = dog.level().getGameTime();
        nextSlimePlayAt = now + SESSION_LIMIT + 900 + dog.getRandom().nextInt(450);
        finishAt = now + SESSION_LIMIT;
        roundsLeft = 2 + dog.getRandom().nextInt(2);
        hopSide = dog.getRandom().nextBoolean() ? 1 : -1;
        barkedDuringTease = false;
        dog.getNavigation().stop();
        dog.perkToyEars();
        FrenchieAwareness.bark(dog, 1.4F);
        stage(PlayPose.SLIME_NOTICE);
    }

    @Override
    public void tick() {
        if (slime == null) return;
        ++stageTicks;
        dog.getLookControl().setLookAt(slime, 50, 35);
        switch (dog.playPose()) {
            case SLIME_NOTICE -> {
                faceSlime();
                if (stageTicks >= 11) stage(PlayPose.SLIME_APPROACH);
            }
            case SLIME_APPROACH -> {
                if (atPlayDistance()) stage(PlayPose.SLIME_BOW);
                else if (stageTicks > 36 || !runTo(radialSpot(slime))) stop();
            }
            case SLIME_BOW -> {
                faceSlime();
                if (!dog.onGround() || !clear(dog.position())) stop();
                else if (!nearPlayDistance()) stage(PlayPose.SLIME_APPROACH);
                else if (stageTicks >= 11) {
                    hopsLeft = 2 + dog.getRandom().nextInt(2);
                    nextHopTick = 1;
                    stage(PlayPose.SLIME_HOP);
                }
            }
            case SLIME_HOP -> tease();
            case SLIME_CIRCLE -> circle();
        }
    }

    private void tease() {
        faceSlime();
        if (!dog.onGround() || stageTicks < nextHopTick) return;
        if (!nearPlayDistance()) {
            if (!runTo(radialSpot(slime))) stop();
            return;
        }
        if (hopsLeft == 0) {
            if (--roundsLeft == 0 || !chooseArc()) stop();
            else stage(PlayPose.SLIME_CIRCLE);
            return;
        }
        double angle = bearing(slime);
        // Sideways with a small radial correction. The head and chest stay aimed at the slime.
        double radial = horizontalDistanceSquared() > PLAY_RADIUS * PLAY_RADIUS ? -0.05 : 0.05;
        double speed = 0.11 + dog.getRandom().nextDouble() * 0.025;
        for (int attempt = 0; attempt < 2; ++attempt) {
            Vec3 motion = new Vec3(Math.cos(angle) * radial - hopSide * Math.sin(angle) * speed, 0,
                    Math.sin(angle) * radial + hopSide * Math.cos(angle) * speed);
            hopSide = -hopSide;
            if (clear(dog.position().add(motion.scale(7)))) {
                dog.setDeltaMovement(motion.x, 0.23, motion.z);
                --hopsLeft;
                nextHopTick = stageTicks + 9 + dog.getRandom().nextInt(4);
                if (!barkedDuringTease && dog.getRandom().nextInt(5) == 0) {
                    FrenchieAwareness.bark(dog, 1.55F);
                    barkedDuringTease = true;
                }
                return;
            }
        }
        stop();
    }

    /** Test chords along either side of the ring so a run never cuts through the slime. */
    private boolean chooseArc() {
        double start = bearing(slime);
        double sweep = 1.6 + dog.getRandom().nextDouble() * 0.65;
        if (dog.getRandom().nextBoolean()) sweep = -sweep;
        for (int attempt = 0; attempt < 2; ++attempt, sweep = -sweep) {
            Vec3 from = dog.position();
            boolean safe = true;
            for (int part = 1; part <= 4; ++part) {
                Vec3 to = ringSpot(slime, start + sweep * part / 4);
                if (!PlaySafety.clearGroundSegment(dog, from, to)) {
                    safe = false;
                    break;
                }
                from = to;
            }
            if (safe) {
                circleAngle = start + sweep;
                return true;
            }
        }
        return false;
    }

    private void circle() {
        double angle = bearing(slime);
        double remaining = Math.atan2(Math.sin(circleAngle - angle), Math.cos(circleAngle - angle));
        if (Math.abs(remaining) < 0.18 && atPlayDistance()) {
            faceSlime();
            stage(PlayPose.SLIME_BOW);
        } else if (stageTicks > 36 || !runTo(ringSpot(slime, angle + Math.clamp(remaining, -0.55, 0.55)))) {
            stop();
        }
    }

    private void faceSlime() {
        dog.lookAt(slime, 45, 35);
        dog.setYBodyRot(dog.getYRot());
        dog.setYHeadRot(dog.getYRot());
    }

    private boolean atPlayDistance() {
        double distanceSq = horizontalDistanceSquared();
        return distanceSq >= 1.75 * 1.75 && distanceSq <= 2.45 * 2.45;
    }

    private boolean nearPlayDistance() {
        double distanceSq = horizontalDistanceSquared();
        return distanceSq >= 1.25 * 1.25 && distanceSq <= 2.8 * 2.8;
    }

    private double horizontalDistanceSquared() {
        double dx = dog.getX() - slime.getX(), dz = dog.getZ() - slime.getZ();
        return dx * dx + dz * dz;
    }

    private double bearing(Slime target) {
        return Math.atan2(dog.getZ() - target.getZ(), dog.getX() - target.getX());
    }

    private Vec3 radialSpot(Slime target) {
        return ringSpot(target, bearing(target));
    }

    private Vec3 ringSpot(Slime target, double angle) {
        return new Vec3(target.getX() + PLAY_RADIUS * Math.cos(angle), dog.getY(),
                target.getZ() + PLAY_RADIUS * Math.sin(angle));
    }

    private boolean clear(Vec3 to) {
        return PlaySafety.clearGroundSegment(dog, dog.position(), to);
    }

    private boolean runTo(Vec3 to) {
        if (!dog.onGround() || !clear(to)) return false;
        dog.getMoveControl().setWantedPosition(to.x, to.y, to.z, 1.35);
        return true;
    }

    private void stage(PlayPose pose) {
        stageTicks = 0;
        dog.setPlayPose(pose);
        if (pose == PlayPose.SLIME_BOW) {
            Vec3 motion = dog.getDeltaMovement();
            dog.setDeltaMovement(0, motion.y, 0);
        }
    }

    @Override
    public void stop() {
        slime = null;
        dog.getNavigation().stop();
        stage(PlayPose.NONE);
    }
}
