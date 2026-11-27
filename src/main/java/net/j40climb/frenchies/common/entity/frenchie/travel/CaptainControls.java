package net.j40climb.frenchies.common.entity.frenchie.travel;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CaptainControls {
    public static boolean canJoin(FrenchieEntity dog, AbstractBoat boat) {
        LivingEntity owner = dog.getOwner();
        return !dog.respondingToWhistle() && owner != null && owner.getVehicle() == boat && !dog.isOrderedToSit() && !dog.isLeashed()
                && (!dog.hasCompanionJob() || dog.explorerOutfit() != ExplorerOutfit.NONE) && !dog.hasToyJob() && !boat.isUnderWater()
                && boat.getPassengers().stream().noneMatch(p -> p instanceof FrenchieEntity other && other.isTame());
    }

    public void tick(FrenchieEntity dog) {
        if (!dog.isTame()) return;
        LivingEntity owner = dog.getOwner();
        if (dog.getVehicle() instanceof AbstractBoat boat) {
            if (owner != null && owner.getVehicle() == boat) return;
            Vec3 landing = findShore(dog, boat);
            if (landing == null) landing = findWater(dog, boat);
            if (landing == null) return;
            dog.stopRiding();
            if (!dog.isPassenger()) {
                dog.snapTo(landing.x, landing.y, landing.z, boat.getYRot(), 0F);
            }
        } else if (!dog.isPassenger() && owner != null && owner.getVehicle() instanceof AbstractBoat boat
                && dog.distanceToSqr(boat) <= 36 && canJoin(dog, boat) && dog.hasLineOfSight(boat)
                && dog.startRiding(boat)) {
            dog.getNavigation().stop();
            dog.setFrenchieSleeping(false);
        }
    }

    private static Vec3 findWater(FrenchieEntity dog, AbstractBoat boat) {
        var level = boat.level();
        for (BlockPos pos : BlockPos.withinManhattan(boat.blockPosition(), 2, 1, 2)) {
            if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getBlockState(pos.above()).isAir()) continue;
            var landing = Vec3.atBottomCenterOf(pos).add(0, level.getFluidState(pos).getHeight(level, pos), 0);
            var bounds = dog.getDimensions(dog.getPose()).makeBoundingBox(landing);
            if (!bounds.intersects(boat.getBoundingBox()) && level.noCollision(dog, bounds)
                    && level.getEntities(dog, bounds, e -> e.isPushable()).isEmpty()
                    && level.clip(new ClipContext(dog.getEyePosition(), landing.add(0, dog.getEyeHeight(), 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dog)).getType() == HitResult.Type.MISS) return landing;
        }
        return null;
    }

    private static Vec3 findShore(FrenchieEntity dog, AbstractBoat boat) {
        var level = boat.level();
        var center = boat.blockPosition();
        for (BlockPos pos : BlockPos.withinManhattan(center, 2, 1, 2)) {
            var support = level.getBlockState(pos.below());
            if (!support.isFaceSturdy(level, pos.below(), Direction.UP) || support.is(Blocks.FARMLAND)
                    || support.is(Blocks.MAGMA_BLOCK) || support.is(Blocks.CAMPFIRE) || support.is(Blocks.SOUL_CAMPFIRE)
                    || !level.getBlockState(pos).isAir() || !level.getFluidState(pos.below()).isEmpty()) continue;
            Vec3 landing = DismountHelper.findSafeDismountLocation(dog.getType(), level, pos, true);
            if (landing == null || boat.distanceToSqr(landing) > 9) continue;
            var bounds = dog.getDimensions(dog.getPose()).makeBoundingBox(landing);
            if (bounds.intersects(boat.getBoundingBox()) || !level.noCollision(dog, bounds)
                    || !level.getEntities(dog, bounds, e -> e.isPushable()).isEmpty()) continue;
            var sight = new ClipContext(dog.getEyePosition(), landing.add(0, dog.getEyeHeight(), 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dog);
            if (level.clip(sight).getType() == HitResult.Type.MISS) return landing;
        }
        return null;
    }
}
