package net.j40climb.frenchies.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerGameMode.class)
public interface MiningState {
    @Accessor("isDestroyingBlock")
    boolean frenchies$isMining();
}
