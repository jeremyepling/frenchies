package net.j40climb.frenchies.mixin.client;

import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.client.PreviewClientRegistries;
import net.minecraft.client.core.registries.BuiltInClientRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BuiltInClientRegistries.class)
abstract class PreviewClientRegistriesMixin {
    // The snapshot port does not yet fire NeoForge registration events for vanilla client registries.
    @Inject(method = "bootStrap", at = @At("HEAD"))
    private static void frenchies$registerRenderers(CallbackInfo ci) {
        PreviewClientRegistries.registerRenderers();
        PreviewClientRegistries.registerFrontpackModels(Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "front_pack"));
    }
}
