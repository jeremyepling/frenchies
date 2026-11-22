package net.j40climb.frenchies.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.client.FrontpackAppearance;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void frenchies$extractPack(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        FrontpackAppearance.extract(avatar, state);
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL"))
    private void frenchies$drawPackLabel(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                        CameraRenderState camera, CallbackInfo ci) {
        FrontpackAppearance.drawLabel(state, pose, collector, camera);
    }
}
