package net.j40climb.frenchies.common.entity.frenchie.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.j40climb.frenchies.Frenchies;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Small pieces in Frenchie model pixels. Both dog models use the same head geometry and anchors. */
public final class AdventureOutfitModels {
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath(Frenchies.LEGACY_NAMESPACE,
            "textures/entity/frenchie/adventure_outfits.png");
    private static final int INK = 0, IVORY = 1, GOLD = 2, ORANGE = 3, TEAL = 4, AQUA = 5,
            BLUE = 6, RED = 7, LEATHER = 8, SUEDE = 9, METAL = 10, SILVER = 11,
            LENS = 12, AMBER = 13, GREEN = 14, WHITE = 15;
    static final float SCUBA_HALF_WIDTH = 4.9F, SCUBA_TOP = -6.85F, SCUBA_BOTTOM = 3.35F,
            SCUBA_FRONT = -6.24F, SCUBA_BACK = 0.65F;
    static final float ASTRONAUT_VISOR_Z = -5.61F;
    private static final float FAN_SIDE_X = 3.62F, FAN_CENTER_Y = -7.36F, FAN_CENTER_Z = 1.95F;
    private final ModelPart parts = bake();

    private static ModelPart bake() {
        // The captain's head scales depth by 1.4. Extend the crown 4 display pixels
        // at the back while preserving the atlas mapping and front edge.
        float crownPadding = 2F / 1.4F;
        Shape captain = new Shape()
                .box(INK, -3.90F, -3.86F, -5.30F, 7.80F, 0.56F, 2.50F)
                .box(INK, -3.52F, -4.01F, -3.95F, 7.04F, 0.69F, 4.96F)
                .box(IVORY, -3.58F, -5.70F, -4.07F + crownPadding, 7.16F, 1.79F, 2.17F,
                        new CubeDeformation(0, 0, crownPadding))
                .box(WHITE, -3.09F, -6.01F, -3.73F, 6.18F, 0.40F, 1.67F)
                .box(GOLD, -0.43F, -5.48F, -4.22F, 0.86F, 0.88F, 0.17F);
        Shape mining = new Shape()
                .box(ORANGE, -3.7F, -3.8F, -4.7F, 7.4F, 0.6F, 5.1F)
                .box(GOLD, -3.12F, -5.45F, -3.65F, 6.25F, 1.65F, 3.8F)
                .box(GOLD, -2.2F, -6.05F, -3.05F, 4.4F, 0.6F, 2.8F)
                .box(INK, -1.35F, -5.55F, -4.85F, 2.7F, 1.9F, 1.3F)
                .box(SILVER, -0.96F, -5.22F, -5.05F, 1.92F, 1.24F, 0.3F)
                .box(IVORY, -0.64F, -5.03F, -5.15F, 1.28F, 0.84F, 0.18F);
        Shape scuba = scubaFrame();
        Shape astronaut = new Shape()
                .box(WHITE, -4.7F, -7.12F, -5.68F, 9.4F, 1.24F, 6.23F)
                .box(IVORY, -4.7F, 2.35F, -5.68F, 9.4F, 0.95F, 6.23F)
                .box(IVORY, -4.7F, -5.88F, -5.68F, 0.66F, 8.23F, 6.23F)
                .box(IVORY, 4.04F, -5.88F, -5.68F, 0.66F, 8.23F, 6.23F)
                .box(WHITE, -4.7F, -5.88F, 0.54F, 9.4F, 8.23F, 0.7F)
                .box(BLUE, -4.48F, 2.66F, -5.75F, 8.96F, 0.34F, 0.14F)
                .box(SILVER, 4.59F, -2.8F, -2.12F, 0.70F, 2.42F, 2.36F)
                .box(RED, 5.21F, -2.36F, -1.63F, 0.17F, 1.48F, 1.37F)
                .box(SILVER, 3.12F, -7.51F, -1.67F, 1.10F, 0.43F, 1.12F)
                .box(METAL, 3.49F, -9.48F, -1.28F, 0.36F, 2.06F, 0.37F)
                .box(RED, 3.18F, -9.98F, -1.56F, 0.99F, 0.62F, 0.95F);
        Shape aviatorCap = new Shape()
                .box(LEATHER, -3.15F, -4.35F, -3.9F, 6.3F, 1.5F, 4.0F)
                .box(SUEDE, -2.55F, -4.9F, -3.35F, 5.1F, 0.58F, 3.2F)
                .box(LEATHER, -3.38F, -3.15F, -2.9F, 0.62F, 3.25F, 2.65F)
                .box(LEATHER, 2.76F, -3.15F, -2.9F, 0.62F, 3.25F, 2.65F);
        aviatorCap.box(SUEDE, -3.45F, -0.28F, -2.45F, 0.7F, 0.54F, 1.6F)
                .box(SUEDE, 2.75F, -0.28F, -2.45F, 0.7F, 0.54F, 1.6F);
        goggles(aviatorCap, INK, LENS);
        Shape netherGoggles = new Shape()
                .box(INK, -3.18F, -2.33F, -4.25F, 6.40F, 2.06F, 4.48F)
                .box(LENS, -2.86F, -2.05F, -4.43F, 2.42F, 1.48F, 0.27F)
                .box(LENS, 0.48F, -2.05F, -4.43F, 2.42F, 1.48F, 0.27F);
        Shape fanPack = new Shape()
                .box(LEATHER, -3.60F, -4.38F, -0.95F, 7.20F, 7.20F, 1.18F)
                .box(SILVER, -3.50F, -10.10F, -1.18F, 7.00F, 5.68F, 6.32F)
                .box(INK, -3.55F, -9.64F, -0.36F, 7.10F, 4.56F, 4.62F)
                .box(BLUE, -3.67F, -4.48F, 0.52F, 7.34F, 1.38F, 2.86F)
                .box(AQUA, -3.68F, -4.34F, 0.61F, 7.36F, 0.45F, 2.68F);
        Shape fan = new Shape()
                .box(AQUA, -0.16F, -2.12F, -0.43F, 0.32F, 4.24F, 0.86F)
                .box(AQUA, -0.16F, -0.43F, -2.12F, 0.32F, 0.86F, 4.24F)
                .box(SILVER, -0.23F, -0.46F, -0.46F, 0.46F, 0.92F, 0.92F);
        Shape boot = new Shape()
                .box(BLUE, -1.2F, 1.38F, -1.4F, 2.4F, 1.64F, 2.55F)
                .box(INK, -1.21F, 2.85F, -1.56F, 2.42F, 0.38F, 2.77F)
                .box(IVORY, -1.25F, 1.13F, -1.23F, 2.5F, 0.48F, 2.47F);
        Shape flipper = new Shape()
                .box(TEAL, -1.22F, 0.86F, -1.24F, 2.44F, 2.25F, 2.48F)
                .box(AQUA, -1.28F, 0.84F, -1.29F, 2.56F, 0.47F, 2.58F)
                .box(TEAL, -1.17F, 2.29F, -1.94F, 2.34F, 0.82F, 1.28F)
                .box(AQUA, -1.5F, 2.62F, -3.1F, 3.0F, 0.48F, 1.4F)
                .box(TEAL, -1.22F, 2.69F, -3.65F, 2.44F, 0.34F, 0.68F);
        Shape airTank = new Shape()
                .box(TEAL, -1.62F, -4.60F, -1.02F, 3.24F, 0.44F, 5.33F)
                .box(GOLD, -1.28F, -6.38F, -0.68F, 2.56F, 1.86F, 4.63F)
                .box(GOLD, -0.92F, -6.71F, -0.68F, 1.84F, 2.48F, 4.63F)
                .box(AMBER, -1.04F, -6.38F, 3.91F, 2.08F, 1.86F, 0.58F)
                .box(METAL, -0.79F, -6.23F, -1.26F, 1.58F, 1.57F, 0.64F)
                .box(SILVER, -0.42F, -5.93F, -1.67F, 0.84F, 0.87F, 0.45F)
                .box(RED, -0.30F, -6.50F, -1.42F, 0.60F, 0.32F, 0.58F)
                .box(INK, -1.32F, -6.42F, 1.88F, 2.64F, 1.92F, 0.54F)
                .box(INK, -0.94F, -6.76F, 1.88F, 1.88F, 0.40F, 0.54F)
                .box(SILVER, -0.27F, -6.87F, 1.81F, 0.54F, 0.20F, 0.68F);
        CubeDeformation scarfSideDrape = new CubeDeformation(0, 1.50F, 0);
        Shape winterScarf = new Shape()
                .box(RED, -3.63F, -4.57F, -4.80F, 7.26F, 0.45F, 1.75F)
                .box(IVORY, -3.64F, -4.59F, -3.44F, 7.28F, 0.20F, 0.32F)
                .box(RED, -3.79F, -2.92F, -4.80F, 0.39F, 2.02F, 1.75F, scarfSideDrape)
                .box(RED, 3.40F, -2.92F, -4.80F, 0.39F, 2.02F, 1.75F, scarfSideDrape)
                .box(IVORY, -3.83F, -2.93F, -3.44F, 0.16F, 2.00F, 0.32F, scarfSideDrape)
                .box(IVORY, 3.67F, -2.93F, -3.44F, 0.16F, 2.00F, 0.32F, scarfSideDrape)
                .box(RED, 3.55F, -2.92F, -5.10F, 0.65F, 1.17F, 1.35F)
                .box(RED, 3.52F, -0.98F, -4.84F, 0.48F, 1.95F, 1.06F, new CubeDeformation(0, 1, 0))
                .box(IVORY, 3.51F, 1.64F, -4.86F, 0.50F, 0.31F, 1.10F);
        Shape scarfKnot = new Shape()
                .box(RED, 2.87F, 1.25F, -3.15F, 0.53F, 0.78F, 2.12F)
                .box(RED, 2.99F, 1.00F, -1.24F, 1.22F, 1.44F, 1.48F)
                .box(IVORY, 3.96F, 1.17F, -1.29F, 0.28F, 0.94F, 0.27F);
        Shape scarfTail = new Shape().box(RED, -0.6F, -0.2F, -0.15F, 1.2F, 0.45F, 4.7F)
                .box(IVORY, -0.61F, -0.24F, 3.68F, 1.22F, 0.53F, 0.55F);
        Shape explorer = new Shape().box(LEATHER, -3.6F, -4.0F, -4.95F, 7.2F, 0.5F, 4.95F)
                .box(GREEN, -2.8F, -5.6F, -3.9F, 5.6F, 1.6F, 3.5F)
                .box(SUEDE, -2.88F, -4.42F, -4F, 5.76F, 0.42F, 3.7F);
        Shape pirate = new Shape().box(INK, -3.15F, -4.8F, -3.6F, 6.3F, 2.1F, 3.15F)
                .box(INK, -3.75F, -4.05F, -4.15F, 7.5F, 1.15F, 0.7F)
                .box(GOLD, -3.75F, -4.21F, -4.2F, 7.5F, 0.26F, 0.75F)
                .box(IVORY, -0.64F, -4.02F, -4.33F, 1.28F, 0.83F, 0.18F)
                .box(INK, -2.81F, -1.72F, -4.18F, 1.96F, 1.8F, 0.31F);
        Shape satchel = new Shape()
                .box(LEATHER, 2.92F, -1.55F, -0.45F, 1.42F, 3.4F, 3.38F)
                .box(SUEDE, 3.03F, -1.76F, -0.59F, 1.48F, 1.18F, 3.62F)
                .box(IVORY, 3.18F, -2.82F, -0.48F, 1.12F, 1.12F, 3.55F)
                .box(RED, 3.12F, -2.88F, 0.87F, 1.24F, 1.24F, 0.36F)
                .box(LEATHER, 3.07F, -1.9F, -4.72F, 0.64F, 0.55F, 4.8F)
                .box(GOLD, 3.36F, -1.49F, -4.38F, 0.52F, 1.32F, 1.3F)
                .box(WHITE, 3.78F, -1.27F, -4.18F, 0.16F, 0.88F, 0.9F)
                .box(RED, 3.91F, -1.10F, -3.84F, 0.1F, 0.6F, 0.25F);
        MeshDefinition mesh = new MeshDefinition();
        captain.add(mesh, "captain");
        mining.add(mesh, "mining");
        scuba.add(mesh, "scuba");
        astronaut.add(mesh, "astronaut");
        aviatorCap.add(mesh, "aviator");
        netherGoggles.add(mesh, "goggles");
        fanPack.add(mesh, "fan_pack");
        fan.add(mesh, "fan");
        boot.add(mesh, "boot");
        flipper.add(mesh, "flipper");
        airTank.add(mesh, "air_tank");
        winterScarf.add(mesh, "winter_scarf");
        scarfKnot.add(mesh, "scarf_knot");
        scarfTail.add(mesh, "scarf_tail");
        explorer.add(mesh, "explorer");
        pirate.add(mesh, "pirate");
        satchel.add(mesh, "satchel");
        return LayerDefinition.create(mesh, 128, 128).bakeRoot();
    }

    private static Shape scubaFrame() {
        float rail = 0.16F;
        float corner = 0.5F;
        float width = SCUBA_HALF_WIDTH * 2;
        float height = SCUBA_BOTTOM - SCUBA_TOP;
        float depth = SCUBA_BACK - SCUBA_FRONT;
        float[] sides = {-SCUBA_HALF_WIDTH, SCUBA_HALF_WIDTH};
        float[] levels = {SCUBA_TOP, SCUBA_BOTTOM};
        float[] ends = {SCUBA_FRONT, SCUBA_BACK};
        Shape frame = new Shape();
        for (float x : sides) {
            for (float y : levels) {
                frame.box(LENS, x - rail / 2, y - rail / 2, SCUBA_FRONT, rail, rail, depth);
                for (float z : ends) {
                    frame.box(AQUA, x - corner / 2, y - corner / 2, z - corner / 2, corner, corner, corner);
                }
            }
            for (float z : ends) {
                frame.box(LENS, x - rail / 2, SCUBA_TOP, z - rail / 2, rail, height, rail);
            }
        }
        for (float y : levels) {
            for (float z : ends) {
                frame.box(LENS, -SCUBA_HALF_WIDTH, y - rail / 2, z - rail / 2, width, rail, rail);
            }
        }
        for (int side : new int[]{-1, 1}) {
            float moduleX = side < 0 ? -SCUBA_HALF_WIDTH - 0.52F : SCUBA_HALF_WIDTH - 0.12F;
            float ventX = side < 0 ? moduleX - 0.08F : moduleX + 0.60F;
            frame.box(TEAL, moduleX, 1.68F, -2.38F, 0.64F, 1.82F, 2.1F)
                    .box(AQUA, ventX, 2.09F, -2.09F, 0.12F, 0.28F, 1.52F)
                    .box(METAL, ventX, 2.61F, -2.09F, 0.12F, 0.24F, 1.52F);
        }
        return frame.box(GOLD, 3.45F, 3.08F, SCUBA_FRONT - 0.16F, 0.63F, 0.5F, 0.3F);
    }

    private static Shape goggles(Shape shape, int rim, int glass) {
        for (float x : new float[]{-2.93F, 0.57F}) {
            shape.box(rim, x, -2.9F, -4.25F, 2.36F, 2.05F, 0.67F)
                    .box(glass, x + 0.31F, -2.60F, -4.42F, 1.74F, 1.45F, 0.3F)
                    .box(IVORY, x + 0.51F, -2.46F, -4.46F, 0.63F, 0.23F, 0.07F);
        }
        return shape.box(rim, -0.64F, -2.33F, -4.05F, 1.28F, 0.61F, 0.46F);
    }

    void submit(String part, PoseStack pose, OrderedSubmitNodeCollector collector, int light, int overlay) {
        collector.submitModelPart(parts.getChild(part), pose, RenderTypes.entityCutout(ATLAS), light, overlay, null);
    }

    void submitCoolingPack(PoseStack pose, OrderedSubmitNodeCollector collector, int light, int overlay, float angle) {
        submit("fan_pack", pose, collector, light, overlay);
        for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            pose.translate(side * FAN_SIDE_X / 16, FAN_CENTER_Y / 16, FAN_CENTER_Z / 16);
            rotateX(pose, side * angle);
            submit("fan", pose, collector, light, overlay);
            pose.popPose();
        }
    }

    void submitAviator(PoseStack pose, OrderedSubmitNodeCollector collector, int light, int overlay,
                       float age, boolean flying) {
        pose.pushPose();
        pose.translate(0, 1F / 16, 0);
        submitAviatorCap(pose, collector, light, overlay);
        pose.popPose();
        submitAviatorScarf(pose, collector, light, overlay, age, flying);
    }

    public void submitAviatorCap(PoseStack pose, OrderedSubmitNodeCollector collector, int light, int overlay) {
        submit("aviator", pose, collector, light, overlay);
    }

    public void submitAviatorScarf(PoseStack pose, OrderedSubmitNodeCollector collector, int light, int overlay,
                                   float age, boolean flying) {
        submit("scarf_knot", pose, collector, light, overlay);
        pose.translate(3.91 / 16, 1.75 / 16, -0.48 / 16);
        rotateY(pose, flying ? 0.38F : 0.18F);
        rotateX(pose, flying ? -0.19F + 0.16F * Mth.sin(age * 0.41F) : -1.22F);
        submit("scarf_tail", pose, collector, light, overlay);
    }

    static void rotateX(PoseStack pose, float radians) {
        //? if <26.3 {
        pose.mulPose(Axis.XP.rotation(radians));
        //?} else {
        /*pose.rotate(Axis.XP.rotation(radians));
        *///?}
    }

    static void rotateY(PoseStack pose, float radians) {
        //? if <26.3 {
        pose.mulPose(Axis.YP.rotation(radians));
        //?} else {
        /*pose.rotate(Axis.YP.rotation(radians));
        *///?}
    }

    private static final class Shape {
        private final CubeListBuilder cubes = CubeListBuilder.create();

        Shape box(int swatch, float x, float y, float z, float width, float height, float depth) {
            return box(swatch, x, y, z, width, height, depth, CubeDeformation.NONE);
        }

        Shape box(int swatch, float x, float y, float z, float width, float height, float depth,
                  CubeDeformation deformation) {
            cubes.texOffs(32 * (swatch % 4), 32 * (swatch / 4)).addBox(x, y, z, width, height, depth, deformation);
            return this;
        }

        void add(MeshDefinition mesh, String name) {
            mesh.getRoot().addOrReplaceChild(name, cubes, PartPose.ZERO);
        }
    }
}
