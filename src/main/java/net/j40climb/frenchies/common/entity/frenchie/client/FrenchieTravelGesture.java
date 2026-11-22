package net.j40climb.frenchies.common.entity.frenchie.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Additive head and ear movement for the bow seat and the front pack. */
public final class FrenchieTravelGesture {
    private static final int INTERVAL = 360;

    private FrenchieTravelGesture() {}

    public static void addTongue(PartDefinition head) {
        head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(52, 55)
                        .addBox(-0.75F, -0.05F, -1.52F, 1.5F, 0.1F, 1.52F),
                PartPose.offsetAndRotation(0.017F, 2.32F, -4.62F, 0.62F, 0F, 0F));
    }

    public static void applyInBoat(ModelPart head, float ageInTicks, int ownerId) {
        float cycle = cycleTime(ageInTicks, ownerId);
        float tick = cycle % INTERVAL;
        if (tick >= 30F) return;

        perkEars(head, tick);
        float glance = envelope(tick, 5, 14, 18, 30);
        float shoulder = cycle < INTERVAL ? 1F : -1F;
        head.xRot -= 0.16F * glance;
        head.yRot += shoulder * 0.95F * glance;
        head.zRot += shoulder * 0.06F * glance;
    }

    public static void applyInFlight(ModelPart head, float ageInTicks, int ownerId, float glideAmount) {
        float cycle = cycleTime(ageInTicks, ownerId);
        float tick = cycle % INTERVAL;
        float shoulder = cycle < INTERVAL ? 1F : -1F;
        perkEars(head, tick);

        // The carrier tips forward during Elytra takeoff. Ease the muzzle up as the rider levels out.
        float glance = envelope(tick, 0, 17, 19, 35);
        head.xRot -= glideAmount * (1.20F + 0.035F * glance);

        // Look around independently of the player's aim, with a pause facing ahead between looks.
        float left = envelope(tick, 38, 76, 98, 140);
        float right = envelope(tick, 172, 224, 248, 285);
        head.yRot += glideAmount * (shoulder * 0.24F * glance + 0.26F * (left - right));
    }

    private static float cycleTime(float ageInTicks, int ownerId) {
        var level = Minecraft.getInstance().level;
        long time = level == null ? (long) ageInTicks : level.getGameTime();
        // World time keeps the pose consistent for observers who started tracking at different times.
        return Math.floorMod(time + 37L * ownerId, 2 * INTERVAL) + Mth.frac(ageInTicks);
    }

    private static void perkEars(ModelPart head, float tick) {
        float lift = 0.28F * envelope(tick, 0, 5, 10, 18);
        head.getChild("earL").zRot -= lift;
        head.getChild("earR").zRot += lift;
    }

    private static float envelope(float tick, int start, int peak, int release, int end) {
        float progress = Math.min((tick - start) / (peak - start), (end - tick) / (end - release));
        float t = Mth.clamp(progress, 0F, 1F);
        return t * t * (3F - 2F * t);
    }
}
