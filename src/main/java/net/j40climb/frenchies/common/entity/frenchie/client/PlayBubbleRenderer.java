package net.j40climb.frenchies.common.entity.frenchie.client;

import net.j40climb.frenchies.common.entity.frenchie.PlayBubbleEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class PlayBubbleRenderer extends EntityRenderer<PlayBubbleEntity, EntityRenderState> {
    //? if <26.3 {
    private static final RenderType FILM = RenderTypes.entityTranslucentCullItemTarget(
    //?} else {
    /*private static final RenderType FILM = RenderTypes.entityTranslucentCull(
    *///?}
            Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE, "textures/entity/play_bubble.png"));

    public PlayBubbleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void extractRenderState(PlayBubbleEntity bubble, EntityRenderState state, float partialTick) {
        super.extractRenderState(bubble, state, partialTick);
        // The attached film is rendered with the dog's interpolated head by FrenchieMouthLayer.
        state.isInvisible |= bubble.noseDogId() != -1;
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poses, SubmitNodeCollector nodes, CameraRenderState camera) {
        if (state.isInvisible) return;
        poses.pushPose();
        poses.translate(0, 0.09, 0);
        //? if <26.3 {
        poses.mulPose(camera.orientation);
        //?} else {
        /*poses.rotate(camera.orientation);
        *///?}
        submitFilm(poses, nodes, state.lightCoords, 0.11F);
        poses.popPose();
        super.submit(state, poses, nodes, camera);
    }

    static void submitFilm(PoseStack poses, SubmitNodeCollector nodes, int light, float radius) {
        nodes.submitCustomGeometry(poses, FILM, (pose, vertices) -> {
            vertex(vertices, pose, light, -radius, -radius, 0, 1);
            vertex(vertices, pose, light, radius, -radius, 1, 1);
            vertex(vertices, pose, light, radius, radius, 1, 0);
            vertex(vertices, pose, light, -radius, radius, 0, 0);
        });
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, float x, float y, int u, int v) {
        buffer.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }
}
