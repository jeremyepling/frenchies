package net.j40climb.frenchies.mixin;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Brain.class)
abstract class BrainTargetMixin {
    @ModifyVariable(method = {
            "setMemoryInternal(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;)V",
            "setMemoryInternal(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;J)V"
    }, at = @At("HEAD"), argsOnly = true, index = 2)
    private Object frenchies$skipCompanion(Object value, MemoryModuleType<?> memory) {
        return memory == MemoryModuleType.ATTACK_TARGET && FrenchieEntity.isProtectedMobTarget(value) ? null : value;
    }
}
