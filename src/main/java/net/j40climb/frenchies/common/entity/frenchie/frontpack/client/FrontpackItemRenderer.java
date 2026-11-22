package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

record FrontpackItemRenderer(ModelPart carrier) implements SpecialModelRenderer<Integer> {
    @Override
    public @Nullable Integer extractArgument(ItemStack stack) {
        return FrontpackTint.dyedFabric(stack);
    }

    @Override
    public void submit(@Nullable Integer tint, PoseStack pose, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outline) {
        // The undyed fabric occupies the same UVs on both Frenchie coat textures.
        var texture = tint == null ? FrontpackLayer.FAWN_TEXTURE : FrontpackLayer.FABRIC;
        //? if <26.3 {
        collector.submitModelPart(carrier, pose, RenderTypes.entityCutout(texture), light, overlay,
                null, false, foil, tint == null ? -1 : tint, null, outline);
        //?} else {
        /*var layer = foil ? RenderTypes.itemCutoutGlint(texture) : RenderTypes.entityCutout(texture);
        collector.submitModelPart(carrier, pose, layer, light, overlay, null, tint == null ? -1 : tint, outline);
        *///?}
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        carrier.getExtentsForGui(new PoseStack(), output);
    }

    record Unbaked() implements SpecialModelRenderer.Unbaked<Integer> {
        static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public FrontpackItemRenderer bake(BakingContext context) {
            var root = context.entityModelSet().bakeLayer(FrontpackModel.LAYER_LOCATION);
            var carrier = root.getChild("root").getChild("carrier");
            // Center the wearable mesh in item space, including the shoulder and waist straps.
            var bounds = new AABB.Builder();
            carrier.getExtentsForGui(new PoseStack(), bounds::include);
            carrier.offsetPos(bounds.build().getCenter().toVector3f().mul(-16));
            return new FrontpackItemRenderer(carrier);
        }
    }
}
