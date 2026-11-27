package net.j40climb.frenchies.common.entity.frenchie.travel;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.play.PlayStages;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** One companion guidance job for death trails and structure maps. */
public final class DeathTrackingGoal extends Goal {
    private final FrenchieEntity dog;
    private final ExplorerRoute route;
    private @Nullable GlobalPos target;
    private @Nullable ExplorerMap map;
    private int nextPathTick;
    private int stalledTicks;
    private double closestDistance = Double.POSITIVE_INFINITY;
    private boolean waiting;
    private @Nullable Vec3 lastOwnerPosition;
    private long ownerMovedAt;
    private boolean establishedLead;
    private long pathExpiresAt;
    private long nextGlance;
    private long nextTreasureCheck;
    private @Nullable BuriedTreasureSite treasure;
    private @Nullable BlockPos adjacentDigCover;
    private boolean reportedExact;
    private boolean reportedArea;
    private long nextDig;
    private long digUntil;
    private long nextExcitement;

    public DeathTrackingGoal(FrenchieEntity dog) {
        this.dog = dog;
        this.route = new ExplorerRoute(dog);
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean startTracking(ServerPlayer owner) {
        if (!dog.isOwnedBy(owner)) return false;
        var death = owner.getLastDeathLocation();
        if (death.isEmpty()) {
            tell(owner, "no_scent");
            return false;
        }
        if (!death.get().dimension().equals(dog.level().dimension())) {
            tell(owner, "other_dimension");
            return false;
        }
        cancel();
        target = death.get();
        dog.standUp();
        dog.setFrenchieSleeping(false);
        tell(owner, "start");
        return true;
    }

    public void useMap(ServerPlayer owner, ItemStack stack) {
        if (!dog.isOwnedBy(owner) || !(dog.level() instanceof ServerLevel level)) return;
        ExplorerMap supplied = ExplorerMap.read(level, stack);
        if (supplied == null) {
            tellExplorer(owner, "no_destination");
            return;
        }
        if (supplied.equals(map)) {
            cancel();
            tellExplorer(owner, "cancel");
            return;
        }
        if (!validMarker(level, supplied)) {
            tellExplorer(owner, "wrong_place");
            return;
        }
        cancel();
        dog.interruptPlay();
        map = supplied;
        dog.standUp();
        dog.setFrenchieSleeping(false);
        dog.perkToyEars();
        tellExplorer(owner, "start");
    }

    public ExplorerOutfit explorerOutfit() {
        return map == null ? ExplorerOutfit.NONE : map.treasure() ? ExplorerOutfit.PIRATE : ExplorerOutfit.EXPLORER;
    }

    public boolean active() {
        return target != null || map != null;
    }

    public boolean isDeathTracking() {
        return target != null;
    }

    public void cancel() {
        stop();
        target = null;
        map = null;
        stalledTicks = 0;
        closestDistance = Double.POSITIVE_INFINITY;
        nextTreasureCheck = 0;
        treasure = null;
        adjacentDigCover = null;
        reportedExact = false;
        reportedArea = false;
        nextDig = 0;
        nextExcitement = 0;
    }

    @Override
    public boolean canUse() {
        if (map == null) return target != null;
        return dog.getOwner() instanceof ServerPlayer owner && owner.isAlive() && !owner.isSpectator()
                && owner.level() == dog.level() && !owner.isPassenger() && !owner.isFallFlying()
                && !dog.isPassenger() && !dog.isOrderedToSit() && !dog.isLeashed() && dog.getTarget() == null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        dog.getNavigation().stop();
        nextPathTick = 0;
        waiting = false;
        lastOwnerPosition = null;
        establishedLead = false;
        nextGlance = 0;
        stopDigging();
    }

    @Override
    public void tick() {
        if (map != null) {
            if (dog.getOwner() instanceof ServerPlayer owner && dog.level() instanceof ServerLevel level) {
                tickMap(level, owner);
            }
            return;
        }
        if (target == null) return;
        if (!target.dimension().equals(dog.level().dimension())) {
            if (dog.getOwner() instanceof ServerPlayer owner) tell(owner, "other_dimension");
            cancel();
            return;
        }
        if (!(dog.getOwner() instanceof ServerPlayer owner) || !owner.isAlive()) {
            dog.getNavigation().stop();
            nextPathTick = 0;
            return;
        }

        Vec3 destination = Vec3.atBottomCenterOf(target.pos());
        Vec3 remaining = destination.subtract(dog.position());
        double distance = remaining.length();
        if (dog.distanceToSqr(owner) > (waiting ? 49 : 100)
                || distance <= 2.5 && owner.position().distanceToSqr(destination) > 25) {
            if (!waiting) tell(owner, "wait");
            waiting = true;
            dog.getNavigation().stop();
            dog.getLookControl().setLookAt(owner);
            nextPathTick = 0;
            return;
        }
        waiting = false;
        if (distance <= 2.5) {
            dog.getLookControl().setLookAt(destination.x, destination.y, destination.z);
            tell(owner, "arrived");
            cancel();
            return;
        }

        if (distance < closestDistance - 0.5) {
            closestDistance = distance;
            stalledTicks = 0;
        } else if (++stalledTicks >= 20 * 15) {
            tell(owner, "blocked");
            cancel();
            return;
        }

        if (--nextPathTick > 0) return;
        nextPathTick = 20;
        if (!dog.getNavigation().isDone()) return;
        Vec3 waypoint = dog.position().add(remaining.scale(Math.min(12.0, distance) / distance));
        dog.getNavigation().moveTo(dog.getNavigation().createPath(BlockPos.containing(waypoint), 1), 1.15);
    }

    private void tickMap(ServerLevel level, ServerPlayer owner) {
        BlockPos marker = map.marker().pos();
        long now = level.getGameTime();
        double separation = dog.distanceToSqr(owner);
        if (lastOwnerPosition == null || lastOwnerPosition.distanceToSqr(owner.position()) > 0.36) {
            lastOwnerPosition = owner.position();
            ownerMovedAt = now;
            establishedLead = false;
        }

        boolean near = horizontalDistanceSqr(dog.position(), marker) <= 144;
        boolean ownerNear = horizontalDistanceSqr(owner.position(), marker) <= 49 && separation <= 64;
        if (near && now >= nextExcitement && !reportedExact && !reportedArea) {
            dog.perkToyEars();
            nextExcitement = now + 85;
        }
        if (map.treasure() && near && now >= nextTreasureCheck) {
            nextTreasureCheck = now + 60;
            treasure = BuriedTreasureSite.find(level, marker);
        }

        boolean readyToDig = ownerNear && separation <= 36 && (dog.onGround() || dog.isInWater());
        if (treasure != null && readyToDig && (ExplorerRoute.directlyAbove(dog.position(), treasure.cover())
                || treasure.cover().equals(adjacentDigCover) && ExplorerRoute.atCover(dog.position(), treasure.cover()))) {
            indicateTreasure(level, now, owner);
            return;
        }
        stopDigging();

        if (ownerNear && horizontalDistanceSqr(dog.position(), marker) <= 16
                && atMappedArea(level, marker, owner)) {
            if (!map.treasure()) {
                tellExplorer(owner, "arrived");
                cancel();
                dog.perkToyEars();
                return;
            }
            if (treasure == null && !reportedArea) {
                tellExplorer(owner, "approximate");
                reportedArea = true;
            }
        }

        if (now >= pathExpiresAt || !route.canKeepPath()
                || separation > 121 && !route.headingBackTo(owner)) dog.getNavigation().stop();
        if (now >= nextGlance && separation >= 25) {
            dog.getLookControl().setLookAt(owner);
            nextGlance = now + 47;
        }
        if (--nextPathTick > 0 || !dog.getNavigation().isDone()) return;
        nextPathTick = 18;
        pathExpiresAt = now + 50;

        if (treasure != null && ownerNear && separation <= 100) {
            if (route.reachCover(owner, treasure.cover())) return;
            if (readyToDig && ExplorerRoute.atCover(dog.position(), treasure.cover())) {
                adjacentDigCover = treasure.cover();
                indicateTreasure(level, now, owner);
                return;
            }
            if (route.reachBesideCover(owner, treasure.cover())) return;
            if (atMappedArea(level, marker, owner) && horizontalDistanceSqr(dog.position(), marker) <= 9) {
                if (!reportedArea && !reportedExact) {
                    tellExplorer(owner, "inaccessible");
                    reportedArea = true;
                }
                dog.getLookControl().setLookAt(Vec3.atCenterOf(treasure.cover()));
                return;
            }
        }
        boolean waitingAhead = separation >= 53 && separation <= 121 && isAhead(owner.position(), dog.position(), marker);
        if (waitingAhead) establishedLead = true;
        boolean ownerPaused = now - ownerMovedAt >= 100 && establishedLead;
        if (waitingAhead && !ownerPaused) return;
        if (separation <= 100 && (!ownerPaused || separation < 16 && ownerNear)
                && !(reportedArea && horizontalDistanceSqr(dog.position(), marker) <= 4)
                && route.lead(owner, marker)) return;
        if (separation > 25) route.rejoin(owner);
    }

    private void indicateTreasure(ServerLevel level, long now, ServerPlayer owner) {
        dog.getNavigation().stop();
        var cover = Vec3.atCenterOf(treasure.cover());
        dog.getLookControl().setLookAt(cover);
        Vec3 bearing = cover.subtract(dog.position());
        if (bearing.horizontalDistanceSqr() > 0.2) {
            float yaw = (float) Math.toDegrees(Math.atan2(-bearing.x, bearing.z));
            dog.setYRot(yaw);
            dog.setYBodyRot(yaw);
        }
        if (!reportedExact) {
            reportedExact = true;
            tellExplorer(owner, "treasure");
            dog.perkToyEars();
            dog.setDigStage(PlayStages.Digging.REVEAL);
            digUntil = now + 9;
            nextDig = now + 10;
            return;
        }
        if (now >= nextDig) {
            dog.setDigStage(PlayStages.Digging.DIG);
            var coverState = level.getBlockState(treasure.cover());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, coverState),
                    cover.x, cover.y + 0.55, cover.z, 4, 0.14, 0.04, 0.14, 0.02);
            digUntil = now + 23;
            nextDig = now + 105 + dog.getRandom().nextInt(25);
        } else if (now >= digUntil) {
            stopDigging();
        }
    }

    private void stopDigging() {
        if (digUntil == 0) return;
        dog.setDigStage(PlayStages.Digging.NONE);
        digUntil = 0;
    }

    private boolean atMappedArea(ServerLevel level, BlockPos marker, ServerPlayer owner) {
        if (!level.hasChunkAt(marker)) return false;
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                marker.getX(), marker.getZ());
        return dog.getY() >= surface - 4 || Math.abs(dog.getY() - owner.getY()) <= 3
                && horizontalDistanceSqr(owner.position(), marker) <= 9;
    }

    private static double horizontalDistanceSqr(Vec3 from, BlockPos to) {
        double dx = from.x - to.getX() - 0.5;
        double dz = from.z - to.getZ() - 0.5;
        return dx * dx + dz * dz;
    }

    private static boolean isAhead(Vec3 owner, Vec3 dog, BlockPos marker) {
        double dx = marker.getX() + 0.5 - owner.x;
        double dz = marker.getZ() + 0.5 - owner.z;
        double lead = (dog.x - owner.x) * dx + (dog.z - owner.z) * dz;
        return lead > 6 * Math.sqrt(dx * dx + dz * dz);
    }

    public void validateMapDimension(ServerLevel level) {
        if (map == null) return;
        if (!validMarker(level, map)) {
            cancel();
            return;
        }
        var ownerRef = dog.getOwnerReference();
        ServerPlayer owner = ownerRef == null ? null : level.getServer().getPlayerList().getPlayer(ownerRef.getUUID());
        if (owner != null && owner.level() != level) {
            tellExplorer(owner, "dimension_changed");
            cancel();
        }
    }

    private static boolean validMarker(ServerLevel level, ExplorerMap candidate) {
        return level.dimension().equals(candidate.marker().dimension())
                && level.getWorldBorder().isWithinBounds(candidate.marker().pos());
    }

    public void save(ValueOutput output) {
        if (target != null) output.store("FrenchieDeathTrail", GlobalPos.CODEC, target);
        if (map != null) output.store(ExplorerMap.SAVE_KEY, ExplorerMap.CODEC, map);
    }

    public void load(ValueInput input) {
        cancel();
        target = input.read("FrenchieDeathTrail", GlobalPos.CODEC).orElse(null);
        if (target == null) map = input.read(ExplorerMap.SAVE_KEY, ExplorerMap.CODEC).orElse(null);
    }

    public static void clearSavedMap(@Nullable CompoundTag savedDog) {
        if (savedDog != null) savedDog.remove(ExplorerMap.SAVE_KEY);
    }

    private void tell(ServerPlayer owner, String key) {
        owner.sendOverlayMessage(Component.translatable("frenchie.florafauna.tracking." + key, dog.getName()));
    }

    private void tellExplorer(ServerPlayer owner, String key) {
        owner.sendOverlayMessage(Component.translatable("frenchie.florafauna.explorer." + key, dog.getName()));
    }
}
