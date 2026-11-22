package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieAwareness;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerWakeMixin {
    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void frenchies$finishNight(boolean wakeImmediately, boolean updateLevel, CallbackInfo ci) {
        FrenchieAwareness.onWake((Player) (Object) this, wakeImmediately, updateLevel);
    }
}
