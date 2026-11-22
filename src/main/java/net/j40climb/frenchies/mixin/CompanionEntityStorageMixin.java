package net.j40climb.frenchies.mixin;

import java.util.concurrent.Executor;
import net.j40climb.frenchies.common.entity.frenchie.recall.LegacyCompanionIndex;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityStorage.class)
abstract class CompanionEntityStorageMixin {
    // Use the level's own serialized region I/O, including any pending writes.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void frenchies$registerEntityStorage(SimpleRegionStorage storage, ServerLevel level, Executor executor, CallbackInfo ci) {
        LegacyCompanionIndex.register(level, storage);
    }
}
