package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningBolt.class)
abstract class LightningThunderMixin {
    @Unique private boolean frenchies$reportedThunder;

    @Inject(method = "tick", at = @At("HEAD"))
    private void frenchies$reactToThunder(CallbackInfo ci) {
        LightningBolt strike = (LightningBolt) (Object) this;
        if (frenchies$reportedThunder || !(strike.level() instanceof ServerLevel level)) return;
        frenchies$reportedThunder = true;
        int range = 40;
        for (FrenchieEntity dog : level.getEntitiesOfClass(FrenchieEntity.class, strike.getBoundingBox().inflate(range),
                dog -> dog.isTame() && strike.distanceToSqr(dog) <= range * range)) {
            dog.hearThunder();
        }
    }
}
