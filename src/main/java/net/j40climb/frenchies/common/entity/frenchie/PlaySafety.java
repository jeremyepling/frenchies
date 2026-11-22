package net.j40climb.frenchies.common.entity.frenchie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PlaySafety {
    private PlaySafety() {}

    static boolean dangerNearby(FrenchieEntity dog) {
        return !dog.level().getEntitiesOfClass(LivingEntity.class, dog.getBoundingBox().inflate(8),
                other -> other != dog && other.isAlive() && !other.isSpectator() && dog.distanceToSqr(other) <= 64
                        && isThreatOrFighting(other) && dog.hasLineOfSight(other)).isEmpty();
    }

    private static boolean isThreatOrFighting(LivingEntity other) {
        if (other.hurtTime > 0 || other.getLastHurtMob() != null
                && other.tickCount - other.getLastHurtMobTimestamp() < 40) return true;
        // A tiny vanilla slime can target a player normally throughout the game.
        if (other.getType() == EntityType.SLIME && ((Slime) other).getSize() == 1) return false;
        return other instanceof Enemy || other instanceof Mob mob && mob.getTarget() != null;
    }

    /** Check a short run or hop on level ground, with room for the whole dog along the diagonal. */
    public static boolean clearGroundSegment(FrenchieEntity dog, Vec3 from, Vec3 to) {
        if (Math.abs(from.y - to.y) > 0.125) return false;
        var level = dog.level();
        double radius = dog.getBbWidth() * 0.5 + 0.025;
        AABB whole = new AABB(from, to).inflate(radius, 0, radius).expandTowards(0, dog.getBbHeight() + 0.65, 0);
        if (!level.getWorldBorder().isWithinBounds(whole)
                || !level.hasChunksAt(BlockPos.containing(whole.minX, whole.minY, whole.minZ),
                        BlockPos.containing(whole.maxX, whole.maxY, whole.maxZ))) return false;
        var others = level.getEntities(dog, whole, e -> !e.isSpectator() && (e instanceof LivingEntity || e.isPushable()));
        var path = new PathfindingContext(level, dog);
        int steps = Math.max(1, (int) Math.ceil(from.distanceTo(to) * 2));
        Vec3 start = from;
        for (int i = 1; i <= steps; i++) {
            Vec3 end = from.lerp(to, (double) i / steps);
            AABB sweep = new AABB(start, end).inflate(radius, 0, radius).expandTowards(0, dog.getBbHeight() + 0.65, 0);
            if (!level.noCollision(dog, sweep) || level.containsAnyLiquid(sweep)) return false;
            for (Entity other : others) {
                if (other.getBoundingBox().intersects(sweep)) return false;
            }
            for (BlockPos feet : BlockPos.betweenClosed(BlockPos.containing(sweep.minX, sweep.minY, sweep.minZ),
                    BlockPos.containing(sweep.maxX, sweep.minY, sweep.maxZ))) {
                BlockPos floor = feet.below();
                if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                        || WalkNodeEvaluator.getPathTypeStatic(path, feet.mutable()) != PathType.WALKABLE) return false;
            }
            start = end;
        }
        return true;
    }
}
