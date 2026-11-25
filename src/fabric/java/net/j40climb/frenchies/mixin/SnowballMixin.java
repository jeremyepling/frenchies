package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.weather.SnowballPlayEvents;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <26.4 {
@Mixin(Snowball.class)
//?} else {
/*@Mixin(net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableBallProjectile.class)
*///?}
abstract class SnowballMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void frenchies$inviteSnowPlay(HitResult hit, CallbackInfo ci) {
        if ((Object) this instanceof Snowball snowball) SnowballPlayEvents.onImpact(snowball, hit);
    }
}
