package net.j40climb.frenchies.playtest;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieModel;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderState;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackModel;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/** Observe a real tracked player/boat and measure the same render state on a second baked rig. */
@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class TravelGestureClient {
    private enum Step { IDLE, PICKUP, START_GLIDE, PILOT, CAPTAIN, ELYTRA }
    private static Step step = Step.IDLE;
    private static Marker camera;
    private static int samples;
    private static int framingTicks;
    private static Pose neutral;
    private static float leftYaw;
    private record Pose(float leftEar, float rightEar, float yaw, float pitch, boolean tongueVisible, float tonguePitch) {}

    @SubscribeEvent
    public static void message(ClientChatReceivedEvent.System event) {
        switch (event.getMessage().getString()) {
            case "FF flight pickup" -> step = Step.PICKUP;
            case "FF flight glide" -> step = Step.START_GLIDE;
            case "FF rowing gesture" -> step = Step.CAPTAIN;
            case "FF flight gesture" -> step = Step.ELYTRA;
            default -> { return; }
        }
        samples = framingTicks = 0;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        if (step == Step.PICKUP) event.getInput().keyPresses = new Input(false, false, false, false, false, true, false);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (step == Step.IDLE || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (step == Step.PICKUP) {
            if (!(mc.level.getEntity(DeliveryMultiplayerServer.DOG) instanceof FrenchieEntity dog)
                    || mc.player.distanceToSqr(dog) > 9 || !mc.player.isShiftKeyDown()) return;
            if (sampleRig(mc, mc.player, dog).tongueVisible) {
                throw new IllegalStateException("Travel tongue still visible on courier standing on shore");
            }
            mc.gameMode.interact(mc.player, dog, new EntityHitResult(dog, dog.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
            com.mojang.logging.LogUtils.getLogger().info("Owner shift-clicked the courier for front-pack pickup");
            step = Step.IDLE;
            return;
        }
        if (step == Step.START_GLIDE || step == Step.PILOT) {
            mc.player.setXRot(0);
            mc.player.setYRot(0);
            if (step == Step.PILOT) return;
            if (!mc.player.tryToStartFallFlying()) return;
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            step = Step.PILOT;
            return;
        }
        var owner = mc.level.players().stream().filter(p -> p.getGameProfile().name().equals("FFOwner")).findFirst().orElse(null);
        if (owner == null) return;
        var dog = (FrenchieEntity) mc.level.getEntity(CompanionServer.CAPTAIN);
        boolean flight = step == Step.ELYTRA;
        if (flight) {
            if (!owner.isFallFlying() || owner.getFallFlyingTicks() < 55
                    || !owner.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie()) return;
            if (Math.abs(net.minecraft.util.Mth.wrapDegrees(owner.getYRot())) > 1 || Math.abs(owner.getXRot()) > 1) {
                if (framingTicks == 0) return;
                throw new IllegalStateException("Pilot moved during sampling: yaw=" + owner.getYRot() + ", pitch=" + owner.getXRot());
            }
        } else if (dog == null || !(dog.getVehicle() instanceof AbstractBoat)) {
            return;
        }
        if (camera == null) camera = new Marker(EntityType.MARKER, mc.level);
        var target = flight ? owner.position().add(0, 0.08, 0.6) : dog.getEyePosition().add(0, -0.06, 0);
        Vec3 eye = target.add(flight ? new Vec3(0.94, -0.38, 0.52) : new Vec3(0.6, 0.28, 1.25));
        camera.snapTo(eye.subtract(0, camera.getEyeHeight(), 0));
        camera.lookAt(Anchor.EYES, target);
        //? if <26.3 {
        mc.options.hideGui = true;
        //?} else {
        /*if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        *///?}
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
        if (++framingTicks < 5) return;
        if (framingTicks < 25) {
            Pose pose = sampleRig(mc, owner, flight ? null : dog);
            if (!pose.tongueVisible || Math.abs(pose.tonguePitch - 0.62F) > 0.001F) {
                throw new IllegalStateException(step + " expected a visible tongue fixed at 0.62 radians: " + pose);
            }
            if (framingTicks == 24) {
                com.mojang.logging.LogUtils.getLogger().info("{} travel tongue fixed at {} radians across 20 ticks", step, pose.tonguePitch);
                String name = flight ? "elytra-tongue.png" : "captain-tongue.png";
                //? if <26.3 {
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, ignored -> {});
                //?} else {
                /*Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
                *///?}
            }
            return;
        }
        int phase = Math.floorMod(mc.level.getGameTime() + 37L * owner.getId(), 360);
        String frame;
        if (samples == 0 && phase >= 35 && (!flight || phase < 45 || phase >= 290)) frame = "neutral";
        else if (samples == 1 && phase >= 6 && phase <= 9) frame = "ears";
        else if (samples == 2 && phase >= 15 && phase <= 17) frame = "glance";
        else if (samples == 3 && phase >= 35 && phase < 45) frame = "settled";
        else if (samples == 4 && phase >= 80 && phase <= 95) frame = "left";
        else if (samples == 5 && phase >= 140 && phase <= 146) frame = "forward";
        else if (samples == 6 && phase >= 228 && phase <= 244) frame = "right";
        else if (samples == 7 && phase >= 290) frame = "recentered";
        else return;

        Pose observed = sampleRig(mc, owner, flight ? null : dog);
        boolean valid = switch (samples) {
            case 0 -> !flight || (Math.abs(observed.pitch - 0.20F) < 0.05F && Math.abs(observed.yaw) < 0.02F);
            case 1 -> neutral.leftEar - observed.leftEar > 0.2 && observed.rightEar - neutral.rightEar > 0.2;
            case 2 -> flight ? Math.abs(observed.yaw) > 0.17F && Math.abs(observed.yaw) < 0.30F
                    && neutral.pitch - observed.pitch > 0.02F && neutral.pitch - observed.pitch < 0.06F
                    : Math.abs(neutral.yaw - observed.yaw) > 0.7F && neutral.pitch - observed.pitch > 0.1F;
            case 4, 6 -> Math.abs(observed.pitch - neutral.pitch) < 0.035F
                    && Math.abs(observed.yaw) > 0.20F && Math.abs(observed.yaw) < 0.32F
                    && (samples == 4 || observed.yaw * leftYaw < 0);
            case 3, 5, 7 -> Math.abs(neutral.yaw - observed.yaw) < 0.02 && Math.abs(neutral.pitch - observed.pitch) < 0.02
                    && Math.abs(neutral.leftEar - observed.leftEar) < 0.02 && Math.abs(neutral.rightEar - observed.rightEar) < 0.02;
            default -> false;
        };
        if (!valid) throw new IllegalStateException(step + " " + frame + " measured " + observed + " from neutral " + neutral);
        if (samples == 0) neutral = observed;
        if (samples == 4) leftYaw = observed.yaw;
        String name = (flight ? "elytra" : "captain") + (samples >= 4 ? "-gaze-" : "-gesture-") + frame;
        com.mojang.logging.LogUtils.getLogger().info("{} at phase {}: {}", name, phase, observed);
        //? if <26.3 {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, ignored -> {});
        //?} else {
        /*Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1, ignored -> {});
        *///?}
        if (++samples == (flight ? 8 : 4)) {
            mc.setCameraEntity(mc.player);
            camera = null;
            step = Step.IDLE;
        }
    }

    private static Pose sampleRig(Minecraft mc, AbstractClientPlayer owner, FrenchieEntity dog) {
        ModelPart body;
        if (dog == null) {
            var state = (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(owner).createRenderState(owner, 0.5F);
            var part = mc.getEntityModels().bakeLayer(FrontpackModel.LAYER_LOCATION);
            new FrontpackModel(part).setupAnim(state);
            //? if <26.3 {
            body = part.getChild("root").getChild("carried_dog").getChild("frenchie").getChild("body");
            //?} else {
            /*body = part.getChild("root").getChild("frenchie").getChild("body");
            *///?}
        } else {
            var state = (FrenchieRenderState) mc.getEntityRenderDispatcher().getRenderer(dog).createRenderState(dog, 0.5F);
            var part = mc.getEntityModels().bakeLayer(FrenchieModel.FRENCHIE);
            new FrenchieModel(part).setupAnim(state);
            body = part.getChild("root").getChild("body");
        }
        var head = body.getChild("head");
        if (head.hasChild("travelMouth")) throw new IllegalStateException("Travel jaw is still present");
        var tongue = head.getChild("tongue");
        var cubes = new ArrayList<ModelPart.Cube>();
        tongue.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        if (cubes.size() != 1 || Math.abs(cubes.getFirst().maxY - cubes.getFirst().minY - 0.1F) > 0.001F) {
            throw new IllegalStateException("Expected a single tongue cube 0.1 model pixels thick");
        }
        return new Pose(head.getChild("earL").zRot, head.getChild("earR").zRot, head.yRot, head.xRot,
                tongue.visible, tongue.xRot);
    }
}
