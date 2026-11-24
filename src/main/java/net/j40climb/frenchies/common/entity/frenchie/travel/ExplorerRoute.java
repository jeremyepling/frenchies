package net.j40climb.frenchies.common.entity.frenchie.travel;

import java.util.LinkedHashSet;
import java.util.Set;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Short, loaded legs built by the companion's normal navigation. */
final class ExplorerRoute {
    private static final double LEAD = 8;
    private final FrenchieEntity dog;

    ExplorerRoute(FrenchieEntity dog) {
        this.dog = dog;
    }

    boolean lead(ServerPlayer owner, BlockPos marker) {
        Vec3 bearing = new Vec3(marker.getX() + 0.5 - owner.getX(), 0, marker.getZ() + 0.5 - owner.getZ());
        double distance = bearing.horizontalDistance();
        Vec3 forward = bearing.normalize();
        Vec3 side = new Vec3(-forward.z, 0, forward.x);
        Vec3 lead = owner.position().add(forward.scale(Math.min(LEAD, distance)));
        var direct = new LinkedHashSet<BlockPos>();
        addColumn(direct, lead, 4, 4);
        if (move(direct, owner, 1.18, false)) return true;

        var detours = new LinkedHashSet<BlockPos>();
        addColumn(detours, lead.add(side.scale(3)), 3, 2);
        addColumn(detours, lead.subtract(side.scale(3)), 3, 2);
        Vec3 shorter = owner.position().add(forward.scale(Math.min(5, distance)));
        addColumn(detours, shorter, 3, 2);
        addColumn(detours, shorter.add(side.scale(2)), 3, 2);
        addColumn(detours, shorter.subtract(side.scale(2)), 3, 2);
        addColumn(detours, owner.position().add(forward.scale(Math.min(3, distance))), 2, 3);
        return move(detours, owner, 1.12, false);
    }

    boolean rejoin(ServerPlayer owner) {
        var nearby = new LinkedHashSet<BlockPos>();
        for (BlockPos pos : BlockPos.withinManhattan(owner.blockPosition(), 2, 1, 2)) {
            if (canStand(pos)) nearby.add(pos.immutable());
        }
        return move(nearby, owner, 1.15, true);
    }

    boolean reachCover(ServerPlayer owner, BlockPos cover) {
        BlockPos above = cover.above();
        return canStand(above) && move(Set.of(above), owner, 1.02, false);
    }

    boolean reachBesideCover(ServerPlayer owner, BlockPos cover) {
        BlockPos above = cover.above();
        var adjacent = new LinkedHashSet<BlockPos>();
        for (BlockPos pos : BlockPos.withinManhattan(above, 1, 1, 1)) {
            if (!pos.equals(above) && atCover(Vec3.atBottomCenterOf(pos), cover) && canStand(pos)) {
                adjacent.add(pos.immutable());
            }
        }
        return move(adjacent, owner, 1.02, false);
    }

    static boolean atCover(Vec3 feet, BlockPos cover) {
        Vec3 stand = Vec3.atBottomCenterOf(cover.above());
        return feet.subtract(stand).horizontalDistanceSqr() <= 2.25 && Math.abs(feet.y - stand.y) <= 1.25;
    }

    static boolean directlyAbove(Vec3 feet, BlockPos cover) {
        Vec3 stand = Vec3.atBottomCenterOf(cover.above());
        return feet.subtract(stand).horizontalDistanceSqr() <= 0.49 && Math.abs(feet.y - stand.y) <= 0.8;
    }

    boolean canKeepPath() {
        Path path = dog.getNavigation().getPath();
        if (path == null || path.isDone()) return false;
        BlockPos next = path.getNextNodePos();
        return safeNode(next) && hasSupport(next) && dog.getY() - next.getY() <= 2;
    }

    boolean headingBackTo(ServerPlayer owner) {
        Path path = dog.getNavigation().getPath();
        return path != null && owner.distanceToSqr(Vec3.atBottomCenterOf(path.getTarget())) <= 12.25;
    }

    private void addColumn(Set<BlockPos> candidates, Vec3 point, int up, int down) {
        BlockPos center = BlockPos.containing(point);
        for (int dy = -down; dy <= up; dy++) {
            BlockPos feet = center.above(dy);
            if (dog.distanceToSqr(Vec3.atBottomCenterOf(feet)) > 1.5 && canStand(feet)) candidates.add(feet);
        }
    }

    private boolean move(Set<BlockPos> candidates, ServerPlayer owner, double speed, boolean returning) {
        if (candidates.isEmpty()) return false;
        var navigation = dog.getNavigation();
        Path path = navigation.createPath(candidates, 0);
        if (path == null || !path.canReach() || path.getNodeCount() < 2) return false;

        double maxDistanceSqr = returning ? Math.max(121, dog.distanceToSqr(owner) + 4) : 121;
        BlockPos previous = dog.blockPosition();
        for (int i = 0; i < path.getNodeCount(); i++) {
            var node = path.getNode(i);
            BlockPos pos = node.asBlockPos();
            if (previous.getY() - pos.getY() > 2 || !safeNode(pos)
                    || owner.distanceToSqr(Vec3.atBottomCenterOf(pos)) > maxDistanceSqr
                    || !hasSupport(pos) || dangerous(node.type)) return false;
            previous = pos;
        }
        return navigation.moveTo(path, speed);
    }

    private boolean canStand(BlockPos pos) {
        if (!safeNode(pos) || !hasSupport(pos)) return false;
        var level = dog.level();
        var bounds = dog.getDimensions(dog.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(pos));
        return level.getWorldBorder().isWithinBounds(bounds) && level.noCollision(dog, bounds);
    }

    private boolean hasSupport(BlockPos feet) {
        var level = dog.level();
        var floor = feet.below();
        return !level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
                || level.getFluidState(floor).is(FluidTags.WATER) || level.getFluidState(feet).is(FluidTags.WATER);
    }

    private boolean safeNode(BlockPos pos) {
        var level = (ServerLevel) dog.level();
        var chunks = level.getChunkSource();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || chunks.getChunkNow(cx, cz) == null) return false;
        return safeBlock(pos) && safeBlock(pos.below());
    }

    private boolean safeBlock(BlockPos pos) {
        var state = dog.level().getBlockState(pos);
        var fluid = state.getFluidState();
        return (fluid.isEmpty() || fluid.is(FluidTags.WATER)) && !dog.getType().isBlockDangerous(state)
                && !state.is(Blocks.POWDER_SNOW) && !state.is(Blocks.POINTED_DRIPSTONE)
                && !state.is(Blocks.NETHER_PORTAL) && !state.is(Blocks.END_PORTAL) && !state.is(Blocks.END_GATEWAY);
    }

    private static boolean dangerous(PathType type) {
        return switch (type) {
            case LAVA, FIRE, FIRE_IN_NEIGHBOR, DAMAGING, DAMAGING_IN_NEIGHBOR, DAMAGE_CAUTIOUS,
                    POWDER_SNOW, ON_TOP_OF_POWDER_SNOW -> true;
            default -> false;
        };
    }
}
