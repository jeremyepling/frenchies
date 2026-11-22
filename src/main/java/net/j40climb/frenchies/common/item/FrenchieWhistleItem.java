package net.j40climb.frenchies.common.item;

import net.j40climb.frenchies.common.entity.frenchie.recall.CompanionRoster;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class FrenchieWhistleItem extends Item {
    public FrenchieWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer owner) {
            CompanionRoster.whistle(owner, owner.isShiftKeyDown());
            owner.getCooldowns().addCooldown(owner.getItemInHand(hand), 8);
        }
        return InteractionResult.SUCCESS;
    }
}
