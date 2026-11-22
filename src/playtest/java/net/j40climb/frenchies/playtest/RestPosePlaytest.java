package net.j40climb.frenchies.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieModel;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderState;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackAttachments;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackDoze;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.client.FrontpackModel;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;

import java.util.Set;

@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.CLIENT)
public final class RestPosePlaytest {
    private static final BlockPos CENTER = new BlockPos(8, 290, 8);
    private static volatile boolean heardSnore;

    static void steps(Playtest t) {
        int[] dogId = new int[1];
        t.server("Prepare one stationary Frenchie and a lit platform in the disposable Overworld", owner -> {
            var level = owner.level().getServer().overworld();
            time(level, false);
            level.getServer().setWeatherParameters(6000, 0, false, false);
            for (var pos : BlockPos.betweenClosed(CENTER.offset(-5, -1, -5), CENTER.offset(5, 4, 5))) {
                var block = pos.getY() >= CENTER.getY() ? Blocks.AIR
                        : pos.getX() % 4 == 2 && pos.getZ() % 4 == 2 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_STONE;
                level.setBlockAndUpdate(pos, block.defaultBlockState());
            }
            // Put the pillow beside the dog's resting spot to make bed and ground clearance visible.
            var pillow = CENTER.west();
            var bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
            level.setBlock(pillow.south(), bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
            level.setBlock(pillow, bed.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_CLIENTS);
            owner.teleportTo(level, 8.5, 290, 10.5, Set.of(), 180, 0, true);
            owner.setGameMode(GameType.SURVIVAL);
            owner.getInventory().clearContent();
            var dog = FrenchieRegistry.FRENCHIE.get().spawn(level, CENTER, EntitySpawnReason.COMMAND);
            Playtest.require(dog != null, "Missing Frenchie");
            dog.tame(owner);
            // A short natural landing gives tracking clients the grounded state used by the idle animation.

            dog.snapTo(Vec3.atBottomCenterOf(CENTER).add(0, 0.3, 0), 180, 0);
            dog.yBodyRot = dog.yHeadRot = 180;
            dogId[0] = dog.getId();
        });
        t.awaitServer("Let the Frenchie land normally before fixing its position for photographs", player -> {
            var dog = (FrenchieEntity) player.level().getEntity(dogId[0]);
            if (dog == null || !dog.onGround()) return false;
            dog.setNoAi(true);
            dog.setDeltaMovement(Vec3.ZERO);
            dog.snapTo(Vec3.atBottomCenterOf(CENTER), 180, 0);
            dog.yBodyRot = dog.yHeadRot = 180;
            logPose("server", dog);
            return true;
        });
        t.awaitClient("Wait for a quiet, grounded Frenchie at the camera mark", mc -> {
            if (!(mc.level.getEntity(dogId[0]) instanceof FrenchieEntity dog)) return false;
            if (dog.onGround() && dog.idleAnimationState.isStarted()) logPose("client", dog);
            return dog.onGround() && dog.idleAnimationState.isStarted() && !dog.isFrenchieSleeping();
        });
        Vec3 center = Vec3.atBottomCenterOf(CENTER);
        camera(t, center.add(0.95, 0.65, -1.45), center.add(0, 0.25, 0));
        t.awaitClient("Sample the standing Frenchie's relaxed chest", mc ->
                pose(mc, dogId[0]).chestScale() <= 1.005F);
        t.screenshot("observer-standing-exhale");
        t.awaitClient("Observe the same standing chest expand as the dog breathes", mc ->
                pose(mc, dogId[0]).chestScale() >= 1.06F);
        t.screenshot("observer-standing-inhale");
        t.awaitClient("Observe a standing ear twitch", mc ->
                pose(mc, dogId[0]).earAngle() > 0.09F);
        t.screenshot("observer-standing-ear-twitch");
        sleeping(t, dogId, false);
        sleeping(t, dogId, true);
        t.server("Bring daylight back to the sleeping dog", owner -> time(owner.level(), false));
        t.awaitClient("Wake upright from side sleeping", mc -> {
            var dog = (FrenchieEntity) mc.level.getEntity(dogId[0]);
            return !dog.isFrenchieSleeping() && dog.getSleepTime() >= 25 && Math.abs(pose(mc, dogId[0]).roll()) < 0.1F;
        });
        t.screenshot("observer-awake-after-side-sleep");
        packed(t, dogId);
    }

    private static void logPose(String observer, FrenchieEntity dog) {
        LogUtils.getLogger().info("Rest fixture {} dog={} ground={} pose={} sleep={} idle={} play={} walkSpeed={} sit={} pos={} below={}",
                observer, dog.getId(), dog.onGround(), dog.getPose(), dog.isFrenchieSleeping(), dog.idleAnimationState.isStarted(),
                dog.playPose(), dog.walkAnimation.speed(), dog.isOrderedToSit(), dog.position(),
                dog.level().getBlockState(dog.blockPosition().below()));
    }

    private static void sleeping(Playtest t, int[] dogId, boolean side) {
        String label = side ? "side" : "original";
        t.awaitServer("Let the dog choose its " + label + " pose on a natural sleep episode", owner -> {
            var dog = (FrenchieEntity) owner.level().getEntity(dogId[0]);
            if (dog.isFrenchieSleeping() && dog.usesSideSleepPose() == side) return true;
            time(owner.level(), !dog.isFrenchieSleeping());
            return false;
        });
        t.awaitClient("Hold the synchronized " + label + " pose through an entire sleep loop", mc -> {
            var dog = (FrenchieEntity) mc.level.getEntity(dogId[0]);
            if (!dog.isFrenchieSleeping() || !dog.sleepAnimationState.isStarted() || dog.getSleepTime() < 65) return false;
            Playtest.require(dog.usesSideSleepPose() == side, "Client and server selected different sleep poses");
            float roll = Math.abs(pose(mc, dogId[0]).roll());
            Playtest.require(side ? roll > 0.9F : roll < 0.3F, "Unexpected " + label + " sleeping body roll: " + roll);
            return true;
        });
        t.screenshot("observer-sleep-" + label);
    }

    private record Pose(float chestScale, float earAngle, float roll) {}

    private static Pose pose(Minecraft mc, int id) {
        var dog = (FrenchieEntity) mc.level.getEntity(id);
        var state = (FrenchieRenderState) mc.getEntityRenderDispatcher().getRenderer(dog).createRenderState(dog, 0.5F);
        ModelPart parts = mc.getEntityModels().bakeLayer(FrenchieModel.FRENCHIE);
        new FrenchieModel(parts).setupAnim(state);
        var root = parts.getChild("root");
        var body = root.getChild("body");
        var head = body.getChild("head");
        return new Pose(body.getChild("chest").yScale,
                Math.max(Math.abs(head.getChild("earL").zRot), Math.abs(head.getChild("earR").zRot)),
                root.zRot + body.zRot);
    }

    private static void time(ServerLevel level, boolean night) {
        level.getServer().clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(),
                night ? ClockTimeMarkers.MIDNIGHT : ClockTimeMarkers.NOON);
    }

    private static void camera(Playtest t, Vec3 from, Vec3 target) {
        t.client("Position a fixed close-up camera", mc -> {
            //? if <26.3 {
            mc.options.hideGui = true;
            var marker = new Marker(EntityType.MARKER, mc.level);
            //?} else {
            /*if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
            var marker = new Marker(EntityTypes.MARKER, mc.level);
            *///?}
            mc.options.fov().set(40);
            var aim = target.subtract(from);
            marker.snapTo(from.add(0, -marker.getEyeHeight(), 0), (float) Math.toDegrees(Math.atan2(-aim.x, aim.z)),
                    (float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance())));
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setCameraEntity(marker);
        });
        t.awaitClient("Wait for the camera to reach its mark", mc ->
                mc.gameRenderer.getMainCamera().position().distanceToSqr(from) < 0.00001);
    }

    private static void packed(Playtest t, int[] dogId) {
        t.client("Return to the owner's view for pickup", mc -> {
            mc.setCameraEntity(mc.player);
            mc.options.keyShift.setDown(true);
        });
        t.server("Fit the owner with a front pack", owner ->
                owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FrenchieRegistry.FRONT_PACK.get())));
        t.awaitClient("Wait for a worn pack and a crouching owner", mc -> mc.player.isShiftKeyDown()
                && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(FrenchieRegistry.FRONT_PACK.get()));
        t.client("Pick up the Frenchie with an empty hand", mc -> {
            var dog = (FrenchieEntity) mc.level.getEntity(dogId[0]);
            mc.gameMode.interact(mc.player, dog, new EntityHitResult(dog, dog.getEyePosition()), InteractionHand.MAIN_HAND);
        });
        t.awaitClient("See the same dog enter the pack in daylight", mc -> FrontpackAttachments.pack(mc.player).hasCarriedFrenchie()
                && mc.level.getEntity(dogId[0]) == null && FrontpackDoze.amount(mc.player, 0) == 0);
        t.client("Stand still with the dog", mc -> {
            mc.options.keyShift.setDown(false);
            FrontpackExpressionPlaytest.faceCamera(mc);
        });
        t.awaitClient("Wait for the wearer to stand", mc -> !mc.player.isCrouching());
        Vec3 wearer = Vec3.atBottomCenterOf(CENTER.south(2));
        camera(t, wearer.add(0.35, 1.30, -1.32), wearer.add(0, 1.23, -0.20));
        t.client("See the awake passenger with open eyes", mc -> Playtest.require(!packHead(mc).getChild("eyelids").visible, "Awake eyes covered"));
        t.screenshot("observer-packed-day");
        t.server("Move the quiet wearer's Overworld clock to midnight", p -> time(p.level(), true));
        t.awaitClient("See the packed Frenchie fully doze at night", mc -> FrontpackDoze.amount(mc.player, 0) >= 0.99F);
        t.client("Check the lowered head, closed eyes and tucked tongue on the carried model", mc -> {
            var head = packHead(mc);
            Playtest.require(head.xRot > 0.61F && head.getChild("eyelids").visible && !head.getChild("tongue").visible,
                    "Expected a dozing face in the fitted carrier, head pitch=" + head.xRot);
        });
        t.screenshot("observer-packed-night-dozing");
        t.awaitClient("Hear the packed dog's soft snore at the wearer's position", mc -> heardSnore);
        int[] creeperId = new int[1];
        t.server("Introduce a visible creeper while the packed Frenchie dozes", p -> {
            //? if <26.3 {
            var creeper = EntityType.CREEPER.spawn(p.level(), p.blockPosition().east(4), EntitySpawnReason.COMMAND);
            //?} else {
            /*var creeper = EntityTypes.CREEPER.spawn(p.level(), p.blockPosition().east(4), EntitySpawnReason.COMMAND);
            *///?}
            Playtest.require(creeper != null, "Missing creeper");
            creeper.setNoAi(true);
            creeperId[0] = creeper.getId();
        });
        t.awaitClient("Wake the packed Frenchie for a creeper warning", mc -> {
            var warning = FrontpackAttachments.warningOrNull(mc.player);
            return warning != null && warning.visible(mc.level.getGameTime()) && FrontpackDoze.amount(mc.player, 0) == 0;
        });
        t.screenshot("observer-packed-night-alert");
        t.server("Remove the creeper", p -> p.level().getEntity(creeperId[0]).discard());
        t.awaitClient("Doze again after the warning subsides", mc -> FrontpackDoze.amount(mc.player, 0) >= 0.99F);
        t.server("Bring daylight to the dozing passenger", p -> time(p.level(), false));
        t.awaitClient("Wake the still-packed dog at dawn", mc -> FrontpackAttachments.pack(mc.player).hasCarriedFrenchie()
                && FrontpackDoze.amount(mc.player, 0) == 0);
        t.client("See the passenger reopen its eyes", mc -> Playtest.require(!packHead(mc).getChild("eyelids").visible, "Daylight passenger still has closed eyes"));
        t.screenshot("observer-packed-day-awake");
    }

    private static ModelPart packHead(Minecraft mc) {
        var state = (AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, 0.5F);
        var parts = mc.getEntityModels().bakeLayer(FrontpackModel.LAYER_LOCATION);
        new FrontpackModel(parts).setupAnim(state);
        return parts.getChild("root").getChild("carried_dog").getChild("frenchie").getChild("body").getChild("head");
    }

    @SubscribeEvent
    public static void hearSnore(PlaySoundSourceEvent event) {
        if (!"rest".equals(System.getProperty("florafauna_frenchies.playtest.scenario"))) return;
        var mc = Minecraft.getInstance();
        var sound = event.getSound();
        if (mc.player != null && sound.getIdentifier().equals(SoundEvents.FOX_SLEEP.location())
                && mc.player.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) < 1
                && sound.getVolume() < 0.5F) heardSnore = true;
    }
}
