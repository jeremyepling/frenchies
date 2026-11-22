package net.j40climb.frenchies.playtest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.client.FrontpackModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

final class FrontpackFlightPosePlaytest {
    static void capture(Playtest t) {
        Pose[] samples = new Pose[3];
        // Sample the independent head turns and steady tongue in a level glide.
        sample(t, "right", 80, samples, 0);
        sample(t, "center", 140, samples, 1);
        sample(t, "left", 228, samples, 2);
        t.awaitClient("See a forward-looking head, opposite glances and a continuously visible tongue",
                mc -> check(mc.gameDirectory.toPath().resolve("screenshots"), samples));
    }

    private static void sample(Playtest t, String label, int tick, Pose[] samples, int index) {
        String filename = "frontpack-flight-" + label + ".png";
        var written = new AtomicBoolean();
        t.awaitClient("Capture the glider in its " + label + " flight pose", mc -> {
            followFace(mc);
            if (samples[index] != null) {
                if (!written.getAndSet(false)) return false;
                if (gliderInFrame(mc.gameDirectory.toPath().resolve("screenshots").resolve(filename))) return true;
                samples[index] = null;
            }
            int phase = Math.floorMod(mc.level.getGameTime() + 37L * mc.player.getId(), 360);
            if (!mc.player.isFallFlying() || phase < tick || phase > tick + 12) return false;
            samples[index] = pose(mc);
            //? if <26.3 {
            Screenshot.grab(mc.gameDirectory, filename, mc.getMainRenderTarget(), 1, ignored -> written.set(true));
            //?} else {
            /*Screenshot.grab(mc.gameDirectory, filename, mc.gameRenderer.mainRenderTarget(), 1, ignored -> written.set(true));
            *///?}
            return false;
        });
    }

    private static boolean gliderInFrame(Path file) {
        try {
            var image = ImageIO.read(file.toFile());
            // Only accept a frame containing the dark brindle glider. Check the tongue separately below.
            for (int y = image.getHeight() / 4; y < image.getHeight() * 3 / 4; y += 8) {
                for (int x = image.getWidth() / 3; x < image.getWidth() * 2 / 3; x += 8) {
                    int rgb = image.getRGB(x, y);
                    if ((rgb >> 16 & 255) < 80 && (rgb >> 8 & 255) < 80 && (rgb & 255) < 80) return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static void followFace(Minecraft mc) {
        FrontpackExpressionPlaytest.faceCamera(mc);
        // LocalPlayer stops sending position updates while the test's marker is the camera.
        // Keep the integrated server tracking the real glide and loading chunks along its path.
        var pilot = mc.player;
        pilot.connection.send(new ServerboundMovePlayerPacket.PosRot(pilot.position(), pilot.getYRot(),
                pilot.getXRot(), pilot.onGround(), pilot.horizontalCollision));
        var target = mc.player.position().add(0, 0.23, -0.4);
        var position = target.add(0, -0.64, -2.2);
        var camera = mc.getCameraEntity();
        camera.snapTo(position.add(0, -camera.getEyeHeight(), 0), 0,
                (float) -Math.toDegrees(Math.atan2(target.y - position.y, target.z - position.z)));
    }

    private record Pose(Vector3f muzzle, float tonguePitch) {}

    private static Pose pose(Minecraft mc) {
        var renderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
        var state = (AvatarRenderState) renderer.createRenderState(mc.player, 0);
        var part = mc.getEntityModels().bakeLayer(FrontpackModel.LAYER_LOCATION);
        new FrontpackModel(part).setupAnim(state);
        var transforms = new PoseStack();
        for (String child : new String[]{"root", "carried_dog", "frenchie", "body", "head"}) {
            part = part.getChild(child);
            part.translateAndRotate(transforms);
        }
        // The player's torso pitches forward 90 degrees in a level glide. The rig's upward
        // direction (-Y) becomes forward in world space; the muzzle points down its local -Z.
        var muzzle = transforms.last().pose().transformDirection(new Vector3f(0, 0, -1)).normalize();
        var tongue = part.getChild("tongue");
        Playtest.require(tongue.visible, "The tongue disappeared while gliding");
        return new Pose(muzzle, tongue.xRot);
    }

    private static boolean check(Path shots, Pose[] samples) {
        var right = samples[0];
        var center = samples[1];
        var left = samples[2];
        Playtest.require(center.muzzle.y < -0.85 && Math.abs(center.muzzle.x) < 0.12
                        && right.muzzle.x < -0.20 && left.muzzle.x > 0.20
                        && right.muzzle.y < -0.70 && left.muzzle.y < -0.70,
                "Expected the muzzle to face the flight path and glance to each side: "
                        + right.muzzle + ", " + center.muzzle + ", " + left.muzzle);
        for (Pose sample : samples) {
            Playtest.require(Math.abs(sample.tonguePitch - 0.62F) < 0.001F, "The travel tongue changed pitch");
        }
        try {
            for (String label : new String[]{"right", "center", "left"}) {
                var image = ImageIO.read(shots.resolve("frontpack-flight-" + label + ".png").toFile());
                if (image == null) return false;
                int h = image.getHeight(), cx = image.getWidth() / 2, pink = 0;
                for (int y = h / 9; y < h * 8 / 9; y++) {
                    for (int x = cx - h / 3; x < cx + h / 3; x++) {
                        int rgb = image.getRGB(x, y);
                        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                        if (r > g + 30 && b > g + 4 && r > b + 28) pink++;
                    }
                }
                Playtest.require(pink >= h * h / 15000, "No visible pink tongue in the " + label + " frame: " + pink);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
