package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.CompanionEvents;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatTracker.class)
abstract class CompanionFallMixin {
    @Shadow @Final private LivingEntity mob;

    // Players record the actual health loss here after armor, enchantments and absorption.
    @Inject(method = "recordDamage", at = @At("RETURN"))
    private void frenchies$noticeFall(DamageSource source, float damage, CallbackInfo ci) {
        if (damage < mob.getHealth()) CompanionEvents.afterDamage(mob, source, damage);
    }
}
