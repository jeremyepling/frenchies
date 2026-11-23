package net.j40climb.frenchies.common.entity.frenchie.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.AdventureOutfit;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import static net.j40climb.frenchies.common.entity.frenchie.client.AdventureOutfitModels.*;

final class AdventureOutfitLayer extends RenderLayer<FrenchieRenderState, FrenchieModel> {
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE,
            "textures/entity/frenchie/adventure_outfits.png");
    private static final int RINGS = 8, SIDES = 12;
    private static final Point[][] DOME = makeDome();
    private static final Point[] SCUBA_CORNERS = {
            new Point(-SCUBA_HALF_WIDTH, SCUBA_TOP, SCUBA_FRONT),
            new Point(SCUBA_HALF_WIDTH, SCUBA_TOP, SCUBA_FRONT),
            new Point(SCUBA_HALF_WIDTH, SCUBA_BOTTOM, SCUBA_FRONT),
            new Point(-SCUBA_HALF_WIDTH, SCUBA_BOTTOM, SCUBA_FRONT),
            new Point(-SCUBA_HALF_WIDTH, SCUBA_TOP, SCUBA_BACK),
            new Point(SCUBA_HALF_WIDTH, SCUBA_TOP, SCUBA_BACK),
            new Point(SCUBA_HALF_WIDTH, SCUBA_BOTTOM, SCUBA_BACK),
            new Point(-SCUBA_HALF_WIDTH, SCUBA_BOTTOM, SCUBA_BACK)
    };
    private final AdventureOutfitModels clothes = new AdventureOutfitModels();

    AdventureOutfitLayer(RenderLayerParent<FrenchieRenderState, FrenchieModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, FrenchieRenderState state,
                       float yaw, float pitch) {
        if (state.isInvisible || state.outfit == AdventureOutfit.NONE && state.explorer == FrenchieEntity.ExplorerOutfit.NONE) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0);
        var solid = collector.order(2);
        FrenchieModel dog = getParentModel();
        dog.setupAnim(state);

        poses.pushPose();
        dog.translateToHead(poses);
        switch (state.outfit) {
            case CAPTAIN -> clothes.submit("captain", poses, solid, light, overlay);
            case MINING -> clothes.submit("mining", poses, solid, light, overlay);
            case NETHER -> clothes.submit("goggles", poses, solid, light, overlay);
            case SCUBA -> {
                clothes.submit("scuba", poses, solid, light, overlay);
                collector.order(3).submitCustomGeometry(poses, RenderTypes.entityTranslucent(ATLAS, false),
                        (pose, vertices) -> drawScubaGlass(pose, vertices, light, overlay));
            }
            case ASTRONAUT -> {
                clothes.submit("astronaut", poses, solid, light, overlay);
                collector.order(3).submitCustomGeometry(poses, RenderTypes.entityTranslucent(ATLAS, false),
                        (pose, vertices) -> drawDome(pose, vertices, light, overlay));
            }
            case AVIATOR -> clothes.submitAviator(poses, solid, light, overlay, state.ageInTicks, state.adventureFlying);
            default -> {
                if (!state.outfit.hasHeadwear() && state.explorer != FrenchieEntity.ExplorerOutfit.NONE) {
                    clothes.submit(state.explorer == FrenchieEntity.ExplorerOutfit.PIRATE ? "pirate" : "explorer",
                            poses, solid, light, overlay);
                }
            }
        }
        poses.popPose();

        poses.pushPose();
        dog.translateToChest(poses);
        if (state.explorer != FrenchieEntity.ExplorerOutfit.NONE) clothes.submit("satchel", poses, solid, light, overlay);
        if (state.outfit == AdventureOutfit.WINTER) clothes.submit("winter_scarf", poses, solid, light, overlay);
        if (state.outfit == AdventureOutfit.SCUBA) clothes.submit("air_tank", poses, solid, light, overlay);
        if (state.outfit == AdventureOutfit.NETHER) {
            clothes.submit("fan_pack", poses, solid, light, overlay);
            poses.translate(0, -6.21 / 16, 3.20 / 16);
            AdventureOutfitModels.rotateY(poses, state.fanAngle);
            clothes.submit("fan", poses, solid, light, overlay);
        }
        poses.popPose();

        if (state.outfit == AdventureOutfit.WINTER || state.outfit == AdventureOutfit.SCUBA) {
            String footwear = state.outfit == AdventureOutfit.WINTER ? "boot" : "flipper";
            for (int foot = 0; foot < 4; foot++) {
                poses.pushPose();
                dog.translateToFoot(poses, foot);
                clothes.submit(footwear, poses, solid, light, overlay);
                poses.popPose();
            }
        }
    }

    private static Point[][] makeDome() {
        Point[][] ring = new Point[RINGS + 1][SIDES];
        for (int y = 0; y <= RINGS; y++) {
            double latitude = Math.PI * y / RINGS;
            for (int side = 0; side < SIDES; side++) {
                double longitude = 2 * Math.PI * side / SIDES;
                ring[y][side] = new Point((float) (Math.cos(longitude) * Math.sin(latitude)),
                        (float) -Math.cos(latitude), (float) (Math.sin(longitude) * Math.sin(latitude)));
            }
        }
        return ring;
    }

    private static void drawScubaGlass(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay) {
        scubaPane(pose, vertices, light, overlay, Direction.NORTH, 0, 3, 2, 1);
        scubaPane(pose, vertices, light, overlay, Direction.SOUTH, 4, 5, 6, 7);
        scubaPane(pose, vertices, light, overlay, Direction.WEST, 0, 4, 7, 3);
        scubaPane(pose, vertices, light, overlay, Direction.EAST, 1, 2, 6, 5);
        scubaPane(pose, vertices, light, overlay, Direction.DOWN, 0, 1, 5, 4);
        scubaPane(pose, vertices, light, overlay, Direction.UP, 3, 7, 6, 2);
        // Stepped highlights give the flat pane a pixelated reflection.
        visorQuad(pose, vertices, light, overlay, -4.25F, -6.2F, -2.35F, -5.66F, SCUBA_FRONT - 0.02F, 0x76E6F9FF);
        visorQuad(pose, vertices, light, overlay, -4.25F, -5.66F, -3.68F, -4.88F, SCUBA_FRONT - 0.02F, 0x64E6F9FF);
    }

    private static void scubaPane(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay,
                                  Direction normal, int first, int second, int third, int fourth) {
        scubaVertex(pose, vertices, light, overlay, normal, SCUBA_CORNERS[first]);
        scubaVertex(pose, vertices, light, overlay, normal, SCUBA_CORNERS[second]);
        scubaVertex(pose, vertices, light, overlay, normal, SCUBA_CORNERS[third]);
        scubaVertex(pose, vertices, light, overlay, normal, SCUBA_CORNERS[fourth]);
    }

    private static void scubaVertex(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay,
                                    Direction normal, Point corner) {
        glassVertex(pose, vertices, light, overlay, corner.x, corner.y, corner.z,
                normal.getStepX(), normal.getStepY(), normal.getStepZ(), 0x24B1ECFF);
    }

    private static void visorQuad(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay,
                                  float left, float top, float right, float bottom, float z, int color) {
        glassVertex(pose, vertices, light, overlay, left, top, z, 0, 0, -1, color);
        glassVertex(pose, vertices, light, overlay, left, bottom, z, 0, 0, -1, color);
        glassVertex(pose, vertices, light, overlay, right, bottom, z, 0, 0, -1, color);
        glassVertex(pose, vertices, light, overlay, right, top, z, 0, 0, -1, color);
    }

    private static void drawDome(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay) {
        for (int y = 0; y < RINGS; y++) {
            for (int side = 0; side < SIDES; side++) {
                int next = (side + 1) % SIDES;
                int color = 0x35C5E6FF;
                if ((y == 1 && (side == 0 || side == 5 || side == 8))
                        || (y == 2 && (side == 5 || side == 7 || side == 8))
                        || (y == 3 && side == 8)) {
                    color = 0x98E9FCFF;
                } else if (y == 6) {
                    color = 0x54B8ECFF;
                }
                domeVertex(pose, vertices, light, overlay, DOME[y][side], color);
                domeVertex(pose, vertices, light, overlay, DOME[y + 1][side], color);
                domeVertex(pose, vertices, light, overlay, DOME[y + 1][next], color);
                domeVertex(pose, vertices, light, overlay, DOME[y][next], color);
            }
        }
    }

    private static void domeVertex(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay,
                                   Point point, int color) {
        glassVertex(pose, vertices, light, overlay, point.x * 5.75F, point.y * 6.25F - 2.2F, point.z * 4.75F - 2.1F,
                point.x, point.y, point.z, color);
    }

    private static void glassVertex(PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay,
                                    float x, float y, float z, float nx, float ny, float nz, int color) {
        // Sample the white swatch. Vertex color carries the tint and transparency.
        vertices.addVertex(pose, x / 16, y / 16, z / 16).setColor(color).setUv(0.87F, 0.87F)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private record Point(float x, float y, float z) {}
}
