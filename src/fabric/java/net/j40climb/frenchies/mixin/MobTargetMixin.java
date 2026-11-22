package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mob.class)
abstract class MobTargetMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity frenchies$skipCompanion(LivingEntity target) {
        return target instanceof FrenchieEntity dog && dog.isTame() ? null : target;
    }
}
