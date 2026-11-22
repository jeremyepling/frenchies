package net.j40climb.frenchies.common.entity.frenchie.reaction;

import java.util.HashSet;
import net.j40climb.frenchies.common.entity.frenchie.CompanionRoster;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

/** Voluntary outings use reachable, supported paths with at most a single stair per node. */
final class CompanionPaths {
    private CompanionPaths() {}

    static boolean walkBeside(FrenchieEntity dog, BlockPos center, double speed) {
        var candidates = new HashSet<BlockPos>();
        for (var side : Direction.Plane.HORIZONTAL) {
            var pos = center.relative(side);
            if (CompanionRoster.safeAt((ServerLevel) dog.level(), pos) != null) candidates.add(pos);
        }
        return !candidates.isEmpty() && walk(dog, dog.getNavigation().createPath(candidates, 0), speed);
    }

    static boolean walk(FrenchieEntity dog, @Nullable Path path, double speed) {
        return safeToWalk(dog, path) && dog.getNavigation().moveTo(path, speed);
    }

    static boolean safeToWalk(FrenchieEntity dog, @Nullable Path path) {
        if (path == null || !path.canReach()) return false;
        int lastY = dog.blockPosition().getY();
        for (int i = 0; i < path.getNodeCount(); ++i) {
            var node = path.getNode(i);
            if (Math.abs(node.y - lastY) > 1 || node.type != PathType.WALKABLE
                    && node.type != PathType.WALKABLE_DOOR) return false;
            var pos = path.getNodePos(i);
            if (!dog.level().getBlockState(pos.below()).isFaceSturdy(dog.level(), pos.below(), Direction.UP)) return false;
            lastY = node.y;
        }
        return true;
    }
}
