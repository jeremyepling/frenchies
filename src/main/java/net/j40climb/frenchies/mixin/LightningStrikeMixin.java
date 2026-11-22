package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.CompanionEvents;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
abstract class LightningStrikeMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LightningBolt;gameEvent(Lnet/minecraft/core/Holder;)V"))
    private void frenchies$hearThunder(CallbackInfo ci) {
        CompanionEvents.thunder((LightningBolt) (Object) this);
    }
}
