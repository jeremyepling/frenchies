package net.j40climb.frenchies.common.entity.frenchie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.jetbrains.annotations.Nullable;

/** Server observations shared by outfits and the goals that respond to them. */
final class AdventureContext {
    private static final int SAMPLE = 10;
    private final FrenchieEntity dog;
    private final Hold cave = new Hold(40, 60);
    private final Hold winter = new Hold(30, 60);
    private long scubaUntil;
    private long flightUntil;
    private long fearUntil;
    private long nextWhimper;
    private long nextDuck;
    private boolean hot;
    private boolean ownerNearLava;
    private boolean wantsDive;

    AdventureContext(FrenchieEntity dog) {
        this.dog = dog;
    }

    void tick(ServerLevel level) {
        long now = level.getGameTime();
        LivingEntity owner = dog.getOwner();
        boolean together = owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == level;
        if ((dog.tickCount + dog.getId()) % SAMPLE == 0) {
            sampleEnvironment(level, together ? owner : null, now);
        }

        // Keep the outfit briefly after leaving the water, but give normal buoyancy back immediately.
        boolean deep = together && dog.distanceToSqr(owner) <= 24 * 24 && deepWater(owner, scubaUntil > now ? 1 : 2);
        boolean canDive = deep && dog.isInWater() && !dog.isPassenger();
        if (canDive) scubaUntil = now + 30;
        wantsDive = deep && !dog.isPassenger() && !dog.isOrderedToSit();

        boolean flying = dog.getVehicle() instanceof HappyGhast ghast && together && owner.getVehicle() == ghast
                && AdventureOutfit.isGhastFlying(ghast);
        if (flying) flightUntil = now + 50;

        AdventureOutfit outfit = CompanionSettings.automaticAdventureOutfits()
                ? AdventureOutfit.select(scubaUntil > now && !dog.isPassenger(), flightUntil > now,
                        dog.getVehicle() instanceof AbstractBoat, level.dimension() == Level.NETHER,
                        level.dimension() == Level.END, cave.active(), winter.active())
                : AdventureOutfit.NONE;
        dog.syncAdventure(outfit, flying, hot, CompanionSettings.miningHelmetDynamicLight());

        if (together && frightened() && now >= nextWhimper && dog.distanceToSqr(owner) <= 24 * 24
                && !dog.isPassenger() && !dog.isInWater() && !dog.isEscapingDanger()) {
            dog.playSound(SoundEvents.WOLF_WHINE_BABY.value(), 0.22F, 1.08F + dog.getRandom().nextFloat() * 0.18F);
            nextWhimper = now + 160 + dog.getRandom().nextInt(160);
        }
    }

    private void sampleEnvironment(ServerLevel level, @Nullable LivingEntity owner, long now) {
        BlockPos eyes = BlockPos.containing(dog.getEyePosition());
        boolean nearOwner = owner != null && dog.distanceToSqr(owner) <= 18 * 18;
        boolean overworld = level.dimension() == Level.OVERWORLD;
        boolean underground = overworld && nearOwner && !dog.isInWater()
                && level.getBrightness(LightLayer.SKY, eyes) <= 4
                && eyes.getY() <= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, eyes.getX(), eyes.getZ()) - 4;
        cave.sample(underground && level.getBrightness(LightLayer.BLOCK, eyes) <= (cave.active() ? 10 : 7));
        winter.sample(overworld && (level.getBiome(eyes).value().getPrecipitationAt(eyes, level.getSeaLevel()) == Precipitation.SNOW
                || dog.isInPowderSnow));

        boolean nether = level.dimension() == Level.NETHER;
        hot = nether && (dog.isInLava() || dog.isOnFire() || lavaNear(level, dog.blockPosition(), 3));
        ownerNearLava = nether && owner != null && dog.distanceToSqr(owner) <= 32 * 32
                && (owner.isInLava() || lavaNear(level, owner.blockPosition(), 2));

        if (level.dimension() == Level.END && now >= nextDuck && dog.onGround()
                && !dog.isPassenger() && !dog.isFrenchieSleeping() && dog.dangerReaction() == DangerReaction.NONE
                && !dog.isEscapingDanger()) {
            nextDuck = now + 30;
            var overhead = dog.getBoundingBox().inflate(13, 0, 13).expandTowards(0, 23, 0);
            if (!level.getEntitiesOfClass(EnderDragon.class, overhead, dragon -> dragon.isAlive()
                    && !dragon.getPhaseManager().getCurrentPhase().isSitting()
                    && dragon.getY() > dog.getY() + 3 && dragon.distanceToSqr(dog) <= 26 * 26).isEmpty()) {
                dog.startDragonDuck(now);
                nextDuck = now + 340 + dog.getRandom().nextInt(180);
            }
        }
    }

    static boolean deepWater(LivingEntity entity, int overhead) {
        if (!entity.isEyeInFluid(FluidTags.WATER)) return false;
        BlockPos eyes = BlockPos.containing(entity.getEyePosition());
        return entity.level().getFluidState(eyes.above(overhead)).is(FluidTags.WATER);
    }

    private static boolean lavaNear(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.withinManhattan(center, radius, 2, radius)) {
            if (pos.distManhattan(center) <= radius && level.hasChunkAt(pos)
                    && level.getFluidState(pos).is(FluidTags.LAVA)) return true;
        }
        return false;
    }

    void hearThunder() {
        if (!CompanionSettings.thunderFear() || !dog.isTame() || !dog.isAlive()) return;
        long now = dog.level().getGameTime();
        if (fearUntil <= now) nextWhimper = now + 8 + dog.getRandom().nextInt(16);
        fearUntil = Math.max(fearUntil, now + 300 + dog.getRandom().nextInt(301));
    }

    boolean frightened() { return fearUntil > dog.level().getGameTime(); }
    boolean wantsDive() { return wantsDive; }
    boolean ownerNearLava() { return ownerNearLava; }
    boolean mining() { return cave.active(); }

    static final class Hold {
        private final int enter;
        private final int exit;
        private int duration;
        private boolean active;

        Hold(int enter, int exit) {
            this.enter = enter;
            this.exit = exit;
        }

        void sample(boolean condition) {
            if (condition == active) {
                duration = 0;
            } else if ((duration += SAMPLE) >= (active ? exit : enter)) {
                active = condition;
                duration = 0;
            }
        }

        boolean active() { return active; }
    }
}
