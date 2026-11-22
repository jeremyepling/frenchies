package net.j40climb.frenchies.common.entity.frenchie;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Cosmetic weather play. Every movement is bounded and checked before it starts. */
final class WeatherPlayGoal extends Goal {
    static final int NONE = 0, SHAKE = 1, GRASS_APPROACH = 2, GRASS_SHOULDER = 3,
            GRASS_ROLL = 4, GRASS_DRAG = 5, SNOW_APPROACH = 6, SNOW_DIVE = 7,
            SNOW_PLOW = 8, SNOW_POP = 9;
    private final FrenchieEntity dog;
    private final WeatherExposure exposure = new WeatherExposure();
    private Precipitation shakeMaterial = Precipitation.RAIN;
    private long nextHeatSample;
    private boolean warm;
    private int swimTicks;
    private int dryTicks;
    private int ticks;
    private long nextDrying;
    private long nextSnow;
    private long nextSnowScan;
    private boolean dryingRequested;
    private long dryingRequestUntil;
    private boolean startDrying;
    private @Nullable Vec3 snowInterest;
    private long snowInterestUntil;
    private @Nullable Vec3 target;
    private @Nullable Vec3 plowEnd;
    private @Nullable Vec3 dragEnd;

    WeatherPlayGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    void observeWeather() {
        if ((dog.tickCount + dog.getId()) % WeatherExposure.SAMPLE_TICKS == 0) {
            long now = dog.level().getGameTime();
            boolean enabled = CompanionSettings.weatherReactions();
            Precipitation precipitation = enabled && !dog.isInWater()
                    ? dog.level().precipitationAt(BlockPos.containing(dog.getEyePosition())) : Precipitation.NONE;
            if (now >= nextHeatSample && (exposure.snowy() || precipitation == Precipitation.SNOW)) {
                warm = nearHeat();
                nextHeatSample = now + 20;
            }
            exposure.sample(precipitation, warm, now, dog.getRandom());
            syncSnowLayer();
        }
        if (dryingRequested && dog.level().getGameTime() > dryingRequestUntil) dryingRequested = false;
        if (dog.isPassenger()) { swimTicks = dryTicks = 0; return; }
        if (dog.isInWater()) {
            ++swimTicks;
            dryTicks = 0;
        } else if (swimTicks > 0 && ++dryTicks >= 8) {
            if (swimTicks >= 35 && dog.level().getGameTime() >= nextDrying) {
                dryingRequested = true;
                dryingRequestUntil = dog.level().getGameTime() + 60;
            }
            swimTicks = dryTicks = 0;
        }
    }

    private void syncSnowLayer() {
        dog.setSnowLayer(CompanionSettings.weatherReactions() && CompanionSettings.visibleSnow()
                ? exposure.snowLayer() : 0);
    }

    private boolean nearHeat() {
        if (dog.isOnFire() || dog.isInLava()) return true;
        BlockPos feet = dog.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, -1, -1), feet.offset(1, 1, 1))) {
            if (!dog.level().hasChunkAt(pos)) continue;
            var state = dog.level().getBlockState(pos);
            if (state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE)
                    || CampfireBlock.isLitCampfire(state) || state.is(Blocks.MAGMA_BLOCK)) return true;
        }
        return false;
    }

    void inviteSnowball(Vec3 impact) {
        if (dog.level().getGameTime() < nextSnow || active() || !available()
                || dog.distanceToSqr(impact) > 36 || ownerTooFar(impact)) return;
        Vec3 candidate = snowSpotNear(impact);
        if (candidate != null && safeSnowSegment(dog.position(), candidate)) {
            snowInterest = candidate;
            snowInterestUntil = dog.level().getGameTime() + 30;
        }
    }

    boolean active() { return dog.weatherStage() != NONE; }

    void interrupt() {
        dryingRequested = false;
        snowInterest = null;
        stop();
    }

    private boolean available() {
        return dog.canWatchPlay(true) && dog.playPose() == PlayPose.NONE
                && !dog.isOrderedToSit() && !dog.isSitting()
                && !dog.isFrenchieSleeping() && !dog.isPassenger() && !dog.isInWater()
                && dog.onGround()
                && !dog.celebrationRequestedOrRunning();
    }

    private boolean ownerTooFar(Vec3 at) {
        LivingEntity owner = dog.getOwner();
        return owner != null && owner.distanceToSqr(at) > 100;
    }

    @Override
    public boolean canUse() {
        long now = dog.level().getGameTime();
        if (!available()) return false;
        if (dryingRequested) {
            dryingRequested = false;
            startDrying = true;
            shakeMaterial = Precipitation.RAIN;
            nextDrying = now + 1800;
            target = dog.getRandom().nextInt(3) == 0 ? grassSpot() : null;
            return true;
        }
        if (CompanionSettings.weatherReactions()) {
            Precipitation material = exposure.readyToShake(now);
            if (material != Precipitation.NONE) {
                startDrying = true;
                shakeMaterial = material;
                target = null;
                return true;
            }
        }
        if (now < nextSnow || !dog.getNavigation().isDone()) return false;
        startDrying = false;
        if (snowInterest != null) {
            if (now > snowInterestUntil) { snowInterest = null; return false; }
            target = snowInterest;
            snowInterest = null;
            return safeSnowSegment(dog.position(), target);
        }
        if (now < nextSnowScan) return false;
        nextSnowScan = now + 100 + dog.getRandom().nextInt(80);
        if (dog.getRandom().nextInt(8) != 0) return false;
        target = snowSpotNear(dog.position());
        return target != null && safeSnowSegment(dog.position(), target) && !ownerTooFar(target);
    }

    @Override
    public boolean canContinueToUse() {
        return active() && dog.canWatchPlay(true) && !dog.isOrderedToSit() && !dog.isSitting()
                && !dog.isFrenchieSleeping() && !dog.isPassenger() && !dog.isInWater()
                && dog.onGround() && ticks < 130;
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        dog.getNavigation().stop();
        if (!startDrying && target != null && isSnow(target)) {
            nextSnow = dog.level().getGameTime() + 2200 + dog.getRandom().nextInt(1200);
            stage(SNOW_APPROACH);
        } else stage(SHAKE);
    }

    @Override
    public void tick() {
        ++ticks;
        switch (dog.weatherStage()) {
            case SHAKE -> {
                if (ticks == 6 && dog.level() instanceof ServerLevel level) {
                    exposure.shake(level.getGameTime(), dog.getRandom());
                    syncSnowLayer();
                    level.sendParticles(shakeMaterial == Precipitation.SNOW ? ParticleTypes.SNOWFLAKE : ParticleTypes.SPLASH,
                            dog.getX(), dog.getY() + dog.getBbHeight() * 0.7, dog.getZ(),
                            14, 0.25, 0.12, 0.25, 0.05);
                }
                if (ticks >= 18) stage(target != null ? GRASS_APPROACH : NONE);
            }
            case GRASS_APPROACH -> {
                if (target == null || ticks >= 35 || !PlaySafety.clearGroundSegment(dog, dog.position(), target))
                    stage(NONE);
                else if (dog.distanceToSqr(target) < 0.22) stage(GRASS_SHOULDER);
                else moveTo(target, 1.1);
            }
            case GRASS_SHOULDER -> { if (ticks >= 9) stage(GRASS_ROLL); }
            case GRASS_ROLL -> {
                if (ticks >= 28) {
                    Vec3 forward = dog.position().add(dog.getLookAngle().multiply(0.75, 0, 0.75));
                    dragEnd = PlaySafety.clearGroundSegment(dog, dog.position(), forward) ? forward : null;
                    stage(dragEnd == null ? NONE : GRASS_DRAG);
                }
            }
            case GRASS_DRAG -> {
                if (dragEnd == null || ticks >= 17 || !PlaySafety.clearGroundSegment(dog, dog.position(), dragEnd))
                    stage(NONE);
                else if (dog.distanceToSqr(dragEnd) < 0.08) stage(NONE);
                else moveTo(dragEnd, 0.38);
            }
            case SNOW_APPROACH -> {
                if (target == null || ticks >= 38 || !safeSnowSegment(dog.position(), target)) stage(NONE);
                else if (dog.distanceToSqr(target) < 0.3) {
                    Vec3 forward = dog.position().add(dog.getLookAngle().multiply(0.9, 0, 0.9));
                    plowEnd = safeSnowSegment(dog.position(), forward) && isSnow(forward) ? forward : null;
                    stage(SNOW_DIVE);
                } else moveTo(target, 1.1);
            }
            case SNOW_DIVE -> { if (ticks >= 10) stage(plowEnd == null ? SNOW_POP : SNOW_PLOW); }
            case SNOW_PLOW -> {
                if (plowEnd == null || ticks >= 16 || !safeSnowSegment(dog.position(), plowEnd)) stage(SNOW_POP);
                else if (dog.distanceToSqr(plowEnd) < 0.08) stage(SNOW_POP);
                else moveTo(plowEnd, 0.42);
            }
            case SNOW_POP -> { if (ticks >= 11) stage(NONE); }
        }
    }

    private void moveTo(Vec3 to, double speed) {
        dog.getMoveControl().setWantedPosition(to.x, to.y, to.z, speed);
    }

    private void stage(int stage) {
        ticks = 0;
        dog.setWeatherStage(stage);
        if (stage == SNOW_POP && dog.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, dog.getX(), dog.getY() + 0.35, dog.getZ(),
                    18, 0.35, 0.25, 0.35, 0.08);
        }
        if (stage == GRASS_ROLL || stage == SNOW_DIVE || stage == SNOW_PLOW || stage == GRASS_DRAG)
            dog.getNavigation().stop();
    }

    private @Nullable Vec3 grassSpot() {
        for (int radius = 0; radius <= 2; ++radius) {
            for (int x = -radius; x <= radius; ++x) for (int z = -radius; z <= radius; ++z) {
                Vec3 candidate = new Vec3(Math.floor(dog.getX()) + x + 0.5, dog.getY(),
                        Math.floor(dog.getZ()) + z + 0.5);
                BlockPos floor = BlockPos.containing(candidate).below();
                if ((dog.level().getBlockState(floor).is(Blocks.GRASS_BLOCK)
                        || dog.level().getBlockState(floor).is(Blocks.MOSS_BLOCK))
                        && PlaySafety.clearGroundSegment(dog, dog.position(), candidate)) return candidate;
            }
        }
        return null;
    }

    private @Nullable Vec3 snowSpotNear(Vec3 interest) {
        BlockPos center = BlockPos.containing(interest);
        for (int radius = 0; radius <= 2; ++radius) {
            for (int x = -radius; x <= radius; ++x) for (int z = -radius; z <= radius; ++z) {
                BlockPos snow = new BlockPos(center.getX() + x, (int) Math.floor(dog.getY()), center.getZ() + z);
                var state = dog.level().getBlockState(snow);
                if (!state.is(Blocks.SNOW)) continue;
                Vec3 candidate = new Vec3(snow.getX() + 0.5,
                        snow.getY() + state.getValue(SnowLayerBlock.LAYERS) / 8.0, snow.getZ() + 0.5);
                if (dog.distanceToSqr(candidate) <= 36 && isSnow(candidate)
                        && safeSnowSegment(dog.position(), candidate)) return candidate;
            }
        }
        return null;
    }

    private boolean isSnow(Vec3 at) {
        BlockPos snow = BlockPos.containing(at.x, dog.getY(), at.z);
        var state = dog.level().getBlockState(snow);
        BlockPos below = snow.below();
        return state.is(Blocks.SNOW) && state.getValue(SnowLayerBlock.LAYERS) <= 3
                && !dog.level().getBlockState(below).is(Blocks.POWDER_SNOW)
                && dog.level().getBlockState(below).isFaceSturdy(dog.level(), below, Direction.UP);
    }

    private boolean safeSnowSegment(Vec3 from, Vec3 to) {
        if (Math.abs(from.y - to.y) > 0.26 || from.distanceToSqr(to) > 36) return false;
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) * 4));
        for (int i = 0; i <= steps; ++i) {
            Vec3 point = from.lerp(to, i / (double) steps);
            BlockPos feet = BlockPos.containing(point.x, dog.getY(), point.z);
            var state = dog.level().getBlockState(feet);
            BlockPos floor = feet.below();
            boolean support = state.is(Blocks.SNOW) && state.getValue(SnowLayerBlock.LAYERS) <= 3
                    || state.isAir();
            if (!support || !dog.level().getBlockState(floor).isFaceSturdy(dog.level(), floor, Direction.UP)
                    || !dog.level().getBlockState(feet.above()).isAir()
                    || !dog.level().getBlockState(feet.above(2)).isAir()
                    || !dog.level().getWorldBorder().isWithinBounds(feet)) return false;
        }
        return dog.level().getEntities(dog, dog.getBoundingBox().expandTowards(to.subtract(from)).inflate(0.2),
                e -> e instanceof LivingEntity && e != dog).isEmpty();
    }

    @Override
    public void stop() {
        dog.setWeatherStage(NONE);
        dog.getNavigation().stop();
        target = plowEnd = dragEnd = null;
    }

    void save(ValueOutput output) {
        exposure.save(output);
        output.putLong("NextWeatherDrying", nextDrying);
        output.putLong("NextWeatherSnow", nextSnow);
    }

    void load(ValueInput input) {
        exposure.load(input);
        syncSnowLayer();
        nextDrying = input.getLongOr("NextWeatherDrying", 0);
        nextSnow = input.getLongOr("NextWeatherSnow", 0);
        dryingRequested = false;
        snowInterest = null;
        dog.setWeatherStage(NONE);
    }
}
