package net.j40climb.frenchies.common.entity.frenchie;

import java.util.Comparator;
import net.j40climb.frenchies.mixin.LooseItemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** A chopped log opens one short search around its canopy. No inventory exists here. */
public final class TreeStickSearch {
    private final FrenchieEntity dog;
    private @Nullable BlockPos trunk;
    private long choppedAt;
    private long nextScan;
    private long nextTree;

    TreeStickSearch(FrenchieEntity dog) {
        this.dog = dog;
    }

    public static void ownerChops(ServerLevel level, Player player, BlockPos pos, BlockState state) {
        if (!CompanionSettings.stickRetrieval() || !state.is(BlockTags.LOGS) || player.isSpectator()) return;
        level.getEntitiesOfClass(FrenchieEntity.class, player.getBoundingBox().inflate(10),
                        dog -> dog.isOwnedBy(player) && dog.distanceToSqr(player) <= 100)
                .stream().sorted(Comparator.comparingDouble(dog -> dog.distanceToSqr(player)))
                .anyMatch(dog -> dog.getRandom().nextInt(5) == 0 && dog.toyPlay().noticeChoppedLog(pos));
    }

    boolean notice(BlockPos pos) {
        long now = dog.level().getGameTime();
        if (now < nextTree || !dog.canPlay() || !dog.getOffhandItem().isEmpty()) return false;
        trunk = pos.immutable();
        choppedAt = now;
        nextScan = now + 45;
        nextTree = now + 1800 + dog.getRandom().nextInt(1200);
        return true;
    }

    @Nullable ItemEntity find() {
        if (trunk == null) return null;
        long now = dog.level().getGameTime();
        if (now - choppedAt > 600 || !CompanionSettings.stickRetrieval()
                || !(dog.getOwner() instanceof Player owner) || !owner.isAlive()
                || owner.distanceToSqr(trunk.getCenter()) > 16 * 16 || !dog.getOffhandItem().isEmpty()) {
            clear();
            return null;
        }
        if (now < nextScan) return null;
        nextScan = now + 30;
        if (dog.level().getBlockState(trunk).is(BlockTags.LOGS) || !dog.canPlay()) return null;
        // Saplings in the same fall are left for replanting.
        var drops = dog.level().getEntitiesOfClass(ItemEntity.class, new AABB(trunk).inflate(5, 6, 5),
                item -> item.getItem().is(Items.STICK) || item.getItem().is(ItemTags.SAPLINGS));
        return drops.stream().filter(this::availableStick)
                .min(Comparator.comparingDouble(dog::distanceToSqr)).orElse(null);
    }

    boolean availableStick(ItemEntity item) {
        if (trunk == null || !CompanionSettings.stickRetrieval() || !item.isAlive() || !item.getItem().is(Items.STICK)
                || item.getAge() < 35 || item.getAge() > dog.level().getGameTime() - choppedAt + 2
                || !item.onGround() || item.hasPickUpDelay() || dog.distanceToSqr(item) > 14 * 14) return false;
        LooseItemAccess provenance = (LooseItemAccess) item;
        if (provenance.frenchies$thrower() != null || provenance.frenchies$pickupTarget() != null) return false;
        return dog.level().getNearestPlayer(item.getX(), item.getY(), item.getZ(), 3, false) == null;
    }

    void clear() {
        trunk = null;
    }
}
