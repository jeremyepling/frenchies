package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.assistance.DiggingAssistantEvents;
import net.j40climb.frenchies.common.entity.frenchie.play.TreeStickSearch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
abstract class PlayerBlockRemovalMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    // NeoForge's creative and survival break paths both return the actual removal result here.
    @Inject(method = "removeBlock", at = @At("RETURN"))
    private void frenchies$afterPlayerRemoval(BlockPos pos, BlockState state, boolean canHarvest, ItemStack tool,
                                             CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValueZ()) {
            DiggingAssistantEvents.ownerDigs(level, player, pos, state, tool);
            TreeStickSearch.ownerChops(level, player, pos, state);
        }
    }
}
