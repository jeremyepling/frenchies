package net.j40climb.frenchies.common.entity.frenchie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Comparator;

public final class DiggingAssistantEvents {
    private DiggingAssistantEvents() {}

    public static void ownerDigs(ServerLevel level, Player player, BlockPos pos, BlockState state) {
        if (!player.getMainHandItem().is(ItemTags.SHOVELS)
                || !DiggingAssistantGoal.diggable(state)
                || level.getRandom().nextInt(5) != 0) return;
        level.getEntitiesOfClass(FrenchieEntity.class, player.getBoundingBox().inflate(7),
                        dog -> dog.isOwnedBy(player) && dog.distanceToSqr(player) < 49)
                .stream().sorted(Comparator.comparingDouble(dog -> dog.distanceToSqr(player)))
                .anyMatch(dog -> dog.requestDigging(pos));
    }
}
