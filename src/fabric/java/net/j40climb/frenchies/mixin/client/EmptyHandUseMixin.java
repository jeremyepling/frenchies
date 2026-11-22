package net.j40climb.frenchies.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackClient;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.networking.PutDownFrenchiePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class EmptyHandUseMixin {
    // Vanilla skips MultiPlayerGameMode.useItem for an empty stack, and thus skips Fabric's UseItemCallback.
    @Inject(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 1))
    private void frenchies$useEmptyHand(CallbackInfo ci, @Local InteractionHand hand) {
        var mc = (Minecraft) (Object) this;
        if (mc.player == null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.MISS) return;
        if (!mc.player.getItemInHand(hand).isEmpty()) return;
        if (hand == InteractionHand.OFF_HAND && mc.player.getMainHandItem().isEmpty()) return;
        if (FrontpackClient.shouldPutDown(mc.player)) ClientPlayNetworking.send(PutDownFrenchiePayload.INSTANCE);
    }
}
