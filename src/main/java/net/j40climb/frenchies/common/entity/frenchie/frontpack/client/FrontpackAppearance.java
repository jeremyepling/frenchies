package net.j40climb.frenchies.common.entity.frenchie.frontpack.client;

import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackAttachments;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackData;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackEquipment;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackDoze;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieStatusIcon;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Snapshot used by the pack layer, its model, and the carried dog's status label. */
public record FrontpackAppearance(@Nullable FrenchieVariant coat, @Nullable Integer tint,
                                 @Nullable Float alertYaw, @Nullable Component label, float doze) {
    public static void extract(LivingEntity avatar, AvatarRenderState state) {
        var worn = FrontpackEquipment.wornBy(avatar);
        if (worn.isEmpty()) {
            FrontpackRenderData.set(state, null);
            return;
        }
        var data = FrontpackAttachments.packOrNull(avatar);
        var warning = FrontpackAttachments.warningOrNull(avatar);
        boolean alert = warning != null && warning.visible(avatar.level().getGameTime());
        Float yaw = alert ? Math.clamp(Mth.wrapDegrees(warning.bearing() - state.bodyRot), -60F, 60F) : null;
        FrontpackRenderData.set(state, new FrontpackAppearance(data == null ? null : data.carriedCoat(),
                FrontpackTint.dyedFabric(worn), yaw, statusLabel(data, state, alert),
                FrontpackDoze.amount(avatar, state.ageInTicks - avatar.tickCount)));
    }

    private static @Nullable Component statusLabel(@Nullable FrontpackData pack, AvatarRenderState state, boolean alert) {
        if (pack == null || !pack.hasCarriedFrenchie() || state.isInvisible || !Minecraft.renderNames()) return null;
        var appearance = pack.frenchieNBT();
        FrenchieStatusIcon icon;
        if (alert) icon = FrenchieStatusIcon.CREEPER;
        else if (appearance.contains("FrenchieParcel")) icon = FrenchieStatusIcon.PARCEL;
        else if (appearance.contains("FrenchieDeathTrail")) icon = FrenchieStatusIcon.COMPASS;
        else return null;
        return icon.label(appearance.read("CustomName", ComponentSerialization.CODEC).orElse(null));
    }

    public static void drawLabel(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var pack = FrontpackRenderData.get(state);
        if (pack == null || pack.label == null || state.distanceToCameraSq >= 4096) return;
        var attachment = Vec3.directionFromRotation(0, state.bodyRot).scale(0.46).add(0, state.eyeHeight - 0.65, 0);
        collector.submitNameTag(pose, attachment, 0, pack.label, !state.isDiscrete,
                //? if <26.3 {
                state.lightCoords, state.distanceToCameraSq, camera);
                //?} else {
                /*state.lightCoords, camera);
                *///?}
    }
}
