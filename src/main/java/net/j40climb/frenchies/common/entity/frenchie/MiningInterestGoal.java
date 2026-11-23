package net.j40climb.frenchies.common.entity.frenchie;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Look at a recent pickaxe break or an exposed ore from the current spot. */
final class MiningInterestGoal extends Goal {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(Frenchies.MOD_ID, "interesting_ores"));
    private final FrenchieEntity dog;
    private @Nullable BlockPos interest;
    private long nextScan = Long.MIN_VALUE;
    private long until;

    MiningInterestGoal(FrenchieEntity dog) {
        this.dog = dog;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    private boolean available() {
        LivingEntity owner = dog.getOwner();
        return dog.adventureOutfit() == AdventureOutfit.MINING && owner != null && dog.distanceToSqr(owner) <= 36
                && dog.onGround() && dog.getNavigation().isDone() && dog.canPlay() && !dog.isFrightened();
    }

    boolean watch(BlockPos pos) {
        long now = dog.level().getGameTime();
        if (now < nextScan || !available()) return false;
        interest = pos.immutable();
        return true;
    }

    @Override public boolean canUse() {
        if (!available()) return false;
        if (interest != null) return true;
        long now = dog.level().getGameTime();
        if (now < nextScan) return false;
        nextScan = now + 280 + dog.getRandom().nextInt(420);
        BlockPos center = dog.blockPosition();
        for (BlockPos pos : BlockPos.withinManhattan(center, 4, 2, 4)) {
            if (pos.distManhattan(center) > 5 || !dog.level().hasChunkAt(pos)) continue;
            if (dog.level().getBlockState(pos).is(ORES)
                    && dog.level().clip(new ClipContext(dog.getEyePosition(), Vec3.atCenterOf(pos),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dog))
                            .getBlockPos().equals(pos)) {
                interest = pos.immutable();
                return true;
            }
        }
        return false;
    }

    @Override public void start() {
        until = dog.level().getGameTime() + 28 + dog.getRandom().nextInt(25);
        nextScan = until + 100 + dog.getRandom().nextInt(240);
    }

    @Override public boolean canContinueToUse() {
        return interest != null && dog.level().getGameTime() < until && available();
    }

    @Override public void tick() {
        if (interest != null) {
            Vec3 center = Vec3.atCenterOf(interest);
            dog.getLookControl().setLookAt(center.x, center.y, center.z, 12, 35);
        }
    }

    @Override public void stop() { interest = null; }
}
