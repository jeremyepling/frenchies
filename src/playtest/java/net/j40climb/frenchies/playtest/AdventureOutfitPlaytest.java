package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.common.entity.frenchie.AdventureOutfit;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderState;
import net.j40climb.frenchies.common.entity.frenchie.client.FrenchieRenderer;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackData;
import net.j40climb.frenchies.common.entity.frenchie.frontpack.FrontpackEvents;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/** Real client captures of adventure gear following the server's normal context checks. */
final class AdventureOutfitPlaytest {
    private static final BlockPos CAVE = new BlockPos(5, -52, 5);
    private static final BlockPos POOL = new BlockPos(5, 198, 5);
    private static final BlockPos SNOW = new BlockPos(5, 210, 5);
    private static final BlockPos NETHER = new BlockPos(5, 39, 5);
    private static final BlockPos END = new BlockPos(5, 180, 5);
    private static final BlockPos FLIGHT_PAD = new BlockPos(5, 235, 5);
    private static final BlockPos CAMERA_CAVE = CAVE.offset(-2, 1, 3);

    static void steps(Playtest test, int phase) {
        Playtest.require(phase == 1, "The adventure case runs in one client session");
        var dogId = new int[1];
        var settle = new int[1];
        mine(test, dogId, settle);
        dive(test, dogId);
        snow(test, dogId);
        visitNether(test, dogId);
        visitEnd(test, dogId);
    }

    static void nether(Playtest test, int phase) {
        Playtest.require(phase == 1, "The Nether case runs in one client session");
        test.server("Prepare a hidden observer with night vision in the disposable world", player -> {
            prepareObserver(player);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0, false, false));
        });
        visitNether(test, new int[1]);
    }

    static void flight(Playtest test, int phase) {
        Playtest.require(phase == 1, "The adventure flight case runs in one client session");
        test.server("Use the front pack to pick up the fixture Frenchie", player -> {
            var level = player.level().getServer().overworld();
            teleport(player, level, FLIGHT_PAD.south(2));
            level.getServer().setWeatherParameters(6000, 0, false, false);
            platform(level, FLIGHT_PAD, 4, Blocks.SMOOTH_STONE);
            player.setGameMode(GameType.CREATIVE);
            player.setData(FrenchieRegistry.FRENCH_FRONTPACK_DATA, FrontpackData.DEFAULT);
            player.getInventory().clearContent();
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(FrenchieRegistry.FRONT_PACK.get()));
            var dog = spawn(player, FLIGHT_PAD);
            player.setShiftKeyDown(true);
            FrontpackEvents.interact(player, dog, InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
            Playtest.require(player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie(),
                    "The fixture carrier could not pick up its dog");
        });
        test.awaitClient("Confirm the tracked player wears the pack with its dog", mc ->
                mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie());
        test.server("Mount the carrier on a harnessed Happy Ghast above the pad", player -> {
            var type =
                    //? if <26.3 {
                    EntityType.HAPPY_GHAST;
                    //?} else {
                    /*EntityTypes.HAPPY_GHAST;
                    *///?}
            var ghast = type.create(player.level(), EntitySpawnReason.COMMAND);
            Playtest.require(ghast != null, "Could not create a Happy Ghast");
            var harness =
                    //? if <26.3 {
                    Items.BLUE_HARNESS;
                    //?} else {
                    /*Items.HARNESS.blue();
                    *///?}
            ghast.setItemSlot(EquipmentSlot.BODY, new ItemStack(harness));
            ghast.setNoAi(true);
            ghast.snapTo(Vec3.atBottomCenterOf(FLIGHT_PAD.above(9)), 180, 0);
            Playtest.require(player.level().addFreshEntity(ghast) && player.startRiding(ghast, true, false),
                    "Could not mount the Happy Ghast");
        });
        test.awaitClient("Observe the packed dog aboard the airborne Happy Ghast", mc ->
                mc.player.getVehicle() instanceof HappyGhast ghast && AdventureOutfit.isGhastFlying(ghast));
        carrierPortrait(test, "aviator-happy-ghast", 8);
        restoreCamera(test);
        test.server("Dismount the carrier on the landing pad", player -> {
            player.stopRiding();
            teleport(player, player.level(), FLIGHT_PAD.south(2));
        });
        test.awaitClient("See the carrier stand on the pad while the dog stays packed", mc ->
                !mc.player.isPassenger() && mc.player.getY() < FLIGHT_PAD.getY() + 1
                        && mc.player.getData(FrenchieRegistry.FRENCH_FRONTPACK_DATA).hasCarriedFrenchie());
        carrierPortrait(test, "aviator-happy-ghast-landed", 5);
        carrierPortrait(test, "aviator-happy-ghast-rested", 55);
        restoreCamera(test);
    }

    private static void carrierPortrait(Playtest test, String name, int wait) {
        var ready = new int[1];
        test.client("Position the flight portrait camera for " + name, mc -> {
            aimAtCarrier(mc);
            ready[0] = mc.player.tickCount + wait;
        });
        test.awaitClient("Settle the flight portrait camera for " + name, mc -> {
            aimAtCarrier(mc);
            return mc.player.tickCount >= ready[0];
        });
        test.screenshot(name, AdventureOutfitPlaytest::aimAtCarrier);
    }

    private static void aimAtCarrier(Minecraft mc) {
        var player = mc.player;
        var front = Vec3.directionFromRotation(0, player.yBodyRot);
        var target = player.position().add(0, 0.95, 0).add(front.scale(0.35));
        camera(mc, target.add(front.scale(1.75)).add(0.38, 0.2, 0), target);
    }

    private static void mine(Playtest test, int[] dogId, int[] settle) {
        test.server("Build a dark closed stone room below the Overworld surface", player -> {
            ServerLevel level = player.level().getServer().overworld();
            prepareObserver(player);
            teleport(player, level, CAVE.south(2));
            level.getServer().setWeatherParameters(6000, 0, false, false);
            for (BlockPos pos : BlockPos.betweenClosed(CAVE.offset(-4, -1, -4), CAVE.offset(4, 5, 4))) {
                boolean wall = pos.getY() == CAVE.getY() - 1 || pos.getY() >= CAVE.getY() + 3
                        || Math.abs(pos.getX() - CAVE.getX()) == 4 || Math.abs(pos.getZ() - CAVE.getZ()) == 4;
                level.setBlock(pos, wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
        test.awaitClient("Wait for the rebuilt room to reach the observer", mc ->
                mc.player.blockPosition().closerThan(CAVE, 4)
                        && mc.level.getBlockState(CAVE.above(3)).is(Blocks.STONE)
                        && mc.level.getBlockState(CAVE).isAir());
        fixedCaveCamera(test, settle);
        test.screenshot("mining-cave-before-dog");
        test.server("Spawn the owner's Frenchie in the dark room", player -> dogId[0] = spawn(player, CAVE).getId());
        test.awaitServer("Confirm the dog equips its hat and enables level-11 light", player -> {
            FrenchieEntity dog = dog(player, dogId[0]);
            return dog.adventureOutfit() == AdventureOutfit.MINING && dog.miningLightLevel() == 11;
        });
        test.awaitClient("Wait for the synchronized mining hat and emitter", mc ->
                dog(mc, dogId[0], AdventureOutfit.MINING) != null);
        fixedCaveCamera(test, settle);
        test.screenshot("mining-cave-with-dog");
        test.client("Check that the optional provider illuminates the wall behind the dog", mc -> {
            if (!ModList.get().isLoaded("lambdynlights")) return;
            Path frames = mc.gameDirectory.toPath().resolve("screenshots");
            double before = wallBrightness(frames.resolve("mining-cave-before-dog.png"));
            double after = wallBrightness(frames.resolve("mining-cave-with-dog.png"));
            Playtest.require(after > before + 5, "The rear cave wall did not brighten: " + before + " -> " + after);
        });
        test.server("Turn the stationary Frenchie around", player -> {
            var dog = dog(player, dogId[0]);
            dog.snapTo(dog.position(), dog.getYRot() + 180, 0);
        });
        fixedCaveCamera(test, settle);
        test.screenshot("mining-cave-after-turn");
        test.client("Check that a turn does not change the brightness of the wall", mc -> {
            if (!ModList.get().isLoaded("lambdynlights")) return;
            Path frames = mc.gameDirectory.toPath().resolve("screenshots");
            double facing = wallBrightness(frames.resolve("mining-cave-with-dog.png"));
            double turned = wallBrightness(frames.resolve("mining-cave-after-turn.png"));
            Playtest.require(Math.abs(facing - turned) <= 3, "The rear cave wall changed brightness: " + facing + " -> " + turned);
        });
        test.server("Place a torch for a clear hat portrait on clients without the provider", player ->
                player.level().setBlockAndUpdate(CAVE.north(2), Blocks.TORCH.defaultBlockState()));
        portrait(test, dogId, AdventureOutfit.MINING, "mining-hard-hat", new Vec3(1.4, 0.8, 1.65));
        test.server("Remove the dog and the fixture torch from the cave", player -> {
            player.level().removeBlock(CAVE.north(2), false);
            dog(player, dogId[0]).discard();
        });
        test.awaitClient("Confirm the fixture dog and its torch are absent", mc ->
                mc.level.getEntity(dogId[0]) == null && mc.level.getBlockState(CAVE.north(2)).isAir());
        fixedCaveCamera(test, settle);
        test.screenshot("mining-cave-after-dog");
        test.client("Check that the rear cave wall returns to its initial brightness", mc -> {
            if (!ModList.get().isLoaded("lambdynlights")) return;
            Path frames = mc.gameDirectory.toPath().resolve("screenshots");
            double before = wallBrightness(frames.resolve("mining-cave-before-dog.png"));
            double after = wallBrightness(frames.resolve("mining-cave-after-dog.png"));
            Playtest.require(Math.abs(after - before) <= 5, "The rear cave wall stayed lit: " + before + " -> " + after);
        });
        restoreCamera(test);
    }

    private static void fixedCaveCamera(Playtest test, int[] settle) {
        test.client("Fix the observer at the same point and angle in the cave", mc -> {
            camera(mc, Vec3.atCenterOf(CAMERA_CAVE).add(0, 0.4, 0), Vec3.atBottomCenterOf(CAVE).add(0, 0.55, 0));
            settle[0] = mc.player.tickCount + 35;
        });
        test.awaitClient("Wait for the cave renderer to settle", mc -> mc.player.tickCount >= settle[0]);
    }

    private static double wallBrightness(Path png) {
        try {
            var image = ImageIO.read(png.toFile());
            double sum = 0;
            int count = 0;
            // This band lies above the dog and below the ceiling in the fixed cave view.
            for (int y = image.getHeight() / 4; y < image.getHeight() * 7 / 20; y++) {
                for (int x = image.getWidth() * 2 / 5; x < image.getWidth() * 3 / 5; x++) {
                    int pixel = image.getRGB(x, y);
                    sum += ((pixel >> 16 & 255) + (pixel >> 8 & 255) + (pixel & 255)) / 3.0;
                    count++;
                }
            }
            return sum / count;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read cave evidence " + png.getFileName(), ex);
        }
    }

    private static void dive(Playtest test, int[] dogId) {
        test.server("Build a deep clear pool and have the owner enter it", player -> {
            var level = player.level();
            for (BlockPos pos : BlockPos.betweenClosed(POOL.offset(-4, -1, -4), POOL.offset(4, 5, 4))) {
                boolean wall = pos.getY() == POOL.getY() - 1 || Math.abs(pos.getX() - POOL.getX()) == 4
                        || Math.abs(pos.getZ() - POOL.getZ()) == 4;
                level.setBlock(pos, wall ? Blocks.PRISMARINE.defaultBlockState() : Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            }
            teleport(player, level, POOL.south(2));
            dogId[0] = spawn(player, POOL).getId();
        });
        portrait(test, dogId, AdventureOutfit.SCUBA, "scuba-underwater-front", new Vec3(1.4, 0.55, 1.5));
        portrait(test, dogId, AdventureOutfit.SCUBA, "scuba-underwater-flippers", new Vec3(1.6, 0.6, -1.1));
        restoreCamera(test);
        test.server("Move the owner and Frenchie to the dry pool deck", player -> {
            var deck = POOL.offset(0, 7, 1);
            platform(player.level(), deck, 3, Blocks.PRISMARINE_BRICKS);
            teleport(player, player.level(), deck.south());
            dog(player, dogId[0]).snapTo(Vec3.atBottomCenterOf(deck), 0, 0);
        });
        portrait(test, dogId, AdventureOutfit.NONE, "scuba-surfaced", new Vec3(1.4, 0.8, 1.65));
        restoreCamera(test);
    }

    private static void snow(Playtest test, int[] dogId) {
        test.server("Stage an exposed snowy-plains platform for real snowfall", player -> {
            var level = player.level();
            dog(player, dogId[0]).discard();
            teleport(player, level, SNOW.south(2));
            platform(level, SNOW, 5, Blocks.SNOW_BLOCK);
            level.getServer().getCommands().performPrefixedCommand(
                    level.getServer().createCommandSourceStack().withLevel(level),
                    "fillbiome 0 204 0 10 220 10 minecraft:snowy_plains");
            level.getServer().setWeatherParameters(0, 6000, true, false);
            dogId[0] = spawn(player, SNOW).getId();
        });
        portrait(test, dogId, AdventureOutfit.WINTER, "winter-scarf-and-boots", new Vec3(1.45, 0.8, 1.8));
        test.awaitClient("Observe snowfall grow into visible layers on the coat", mc -> {
            FrenchieEntity dog = dog(mc, dogId[0], AdventureOutfit.WINTER);
            return dog != null && dog.snowLayer() >= 4;
        });
        portrait(test, dogId, AdventureOutfit.WINTER, "winter-accumulated-coat", new Vec3(1.45, 1.0, 1.8));
        restoreCamera(test);
        test.server("Shelter the snowy Frenchie under a roof without stopping the weather", player -> {
            for (BlockPos pos : BlockPos.betweenClosed(SNOW.offset(-4, 3, -4), SNOW.offset(4, 3, 4))) {
                player.level().setBlockAndUpdate(pos, Blocks.SPRUCE_PLANKS.defaultBlockState());
            }
            Playtest.require(dog(player, dogId[0]).snowLayer() >= 4, "Snow vanished upon entering shelter");
        });
        test.awaitClient("See the snow melt while the sheltered Frenchie keeps its winter outfit", mc -> {
            FrenchieEntity dog = dog(mc, dogId[0], AdventureOutfit.WINTER);
            return dog != null && dog.snowLayer() <= 2;
        });
        portrait(test, dogId, AdventureOutfit.WINTER, "winter-sheltered-coat", new Vec3(1.45, 1.0, 1.8));
        restoreCamera(test);
    }

    private static void visitNether(Playtest test, int[] dogId) {
        test.server("Stage the owner and Frenchie in a softly lit Nether alcove", player -> {
            if (dogId[0] != 0) dog(player, dogId[0]).discard();
            ServerLevel level = player.level().getServer().getLevel(Level.NETHER);
            Playtest.require(level != null, "Missing Nether dimension");
            teleport(player, level, NETHER.south(2));
            prepareNetherAlcove(level);
            dogId[0] = spawn(player, NETHER).getId();
        });
        portrait(test, dogId, AdventureOutfit.NETHER, "nether-goggles", new Vec3(1.4, 0.75, 1.7));
        waitForFanRate(test, dogId, false);
        portrait(test, dogId, AdventureOutfit.NETHER, "nether-cooler-front-quarter", new Vec3(1.0, 0.54, 1.1));
        portrait(test, dogId, AdventureOutfit.NETHER, "nether-cooler-fan-and-reservoir", new Vec3(-1.14, 0.62, 0.48));
        var fanView = new Vec3(1.0, 1.45, -1.9);
        portrait(test, dogId, AdventureOutfit.NETHER, "nether-fan", fanView);
        test.server("Add a contained lava pocket level with the platform floor", player -> {
            player.level().setBlockAndUpdate(NETHER.offset(1, -2, -1), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            player.level().setBlockAndUpdate(NETHER.offset(1, -1, -1), Blocks.LAVA.defaultBlockState());
        });
        waitForFanRate(test, dogId, true);
        portrait(test, dogId, AdventureOutfit.NETHER, "nether-fan-near-lava", fanView);
        restoreCamera(test);
    }

    private static void prepareNetherAlcove(ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(NETHER.offset(-6, -1, -6), NETHER.offset(6, 5, 6))) {
            int x = pos.getX() - NETHER.getX();
            int y = pos.getY() - NETHER.getY();
            int z = pos.getZ() - NETHER.getZ();
            boolean shell = Math.abs(x) == 6 || Math.abs(z) == 6 || y == 5;
            Block material = shell || y == -1 ? Blocks.NETHERRACK : Blocks.AIR;
            if (y == -1 && Math.abs(x) <= 2 && z >= -2) {
                material = (x + z) % 3 == 0 ? Blocks.POLISHED_BLACKSTONE : Blocks.BLACKSTONE;
            }
            level.setBlock(pos, material.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (BlockPos pos : BlockPos.betweenClosed(NETHER.offset(-4, -2, -5), NETHER.offset(4, -1, -4))) {
            Block material = pos.getY() == NETHER.getY() - 2 ? Blocks.NETHERRACK : Blocks.LAVA;
            level.setBlockAndUpdate(pos, material.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(NETHER.offset(-4, 0, -5), NETHER.offset(-4, 3, -5))) {
            level.setBlockAndUpdate(pos, Blocks.BASALT.defaultBlockState());
        }
        for (BlockPos pos : BlockPos.betweenClosed(NETHER.offset(4, 0, -4), NETHER.offset(4, 2, -4))) {
            level.setBlockAndUpdate(pos, Blocks.BASALT.defaultBlockState());
        }
        level.setBlockAndUpdate(NETHER.offset(-2, 2, 3), Blocks.SHROOMLIGHT.defaultBlockState());
        level.setBlockAndUpdate(NETHER.offset(3, 2, 3), Blocks.SHROOMLIGHT.defaultBlockState());
    }

    private static void waitForFanRate(Playtest test, int[] dogId, boolean hot) {
        var previousTick = new int[]{-1};
        var previousAngle = new float[1];
        test.awaitClient(hot ? "Observe the fan accelerate near lava" : "Observe the fan turning away from lava", mc -> {
            var dog = dog(mc, dogId[0], AdventureOutfit.NETHER);
            if (dog == null || dog.tickCount == previousTick[0]) return false;
            var state = renderState(mc, dog);
            float fullTurn = (float) (2 * Math.PI);
            float step = (state.fanAngle - previousAngle[0] + fullTurn) % fullTurn;
            boolean successive = dog.tickCount == previousTick[0] + 1;
            previousTick[0] = dog.tickCount;
            previousAngle[0] = state.fanAngle;
            return successive && (hot ? step > 0.65F : step > 0.15F && step < 0.5F);
        });
    }

    private static void visitEnd(Playtest test, int[] dogId) {
        test.server("Stage the owner and Frenchie above the End island", player -> {
            dog(player, dogId[0]).discard();
            ServerLevel level = player.level().getServer().getLevel(Level.END);
            Playtest.require(level != null, "Missing End dimension");
            teleport(player, level, END.south(2));
            platform(level, END, 4, Blocks.END_STONE_BRICKS);
            dogId[0] = spawn(player, END).getId();
        });
        portrait(test, dogId, AdventureOutfit.ASTRONAUT, "end-astronaut-visor", new Vec3(1.4, 0.8, 1.65));
        test.server("Stage a dragon passing directly over the Frenchie", player -> {
            var type =
                    //? if <26.3 {
                    EntityType.ENDER_DRAGON;
                    //?} else {
                    /*EntityTypes.ENDER_DRAGON;
                    *///?}
            var dragon = type.create(player.level(), EntitySpawnReason.COMMAND);
            Playtest.require(dragon != null, "Could not create the flyover dragon");
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            dragon.setNoAi(true);
            dragon.snapTo(Vec3.atBottomCenterOf(END.offset(-3, 10, 0)), 0, 0);
            Playtest.require(player.level().addFreshEntity(dragon), "Could not stage the flyover");
        });
        test.awaitClient("Wait for the synchronized astronaut duck to reach its low point", mc -> {
            var dog = dog(mc, dogId[0], AdventureOutfit.ASTRONAUT);
            if (dog == null) return false;
            float time = renderState(mc, dog).dragonDuckTime;
            return time > 8 && time <= 17;
        });
        test.screenshot("end-astronaut-duck");
        test.awaitClient("Confirm the astronaut stands again under the same dragon", mc -> {
            var dog = dog(mc, dogId[0], AdventureOutfit.ASTRONAUT);
            return dog != null && renderState(mc, dog).dragonDuckTime <= 0;
        });
        test.screenshot("end-astronaut-after-flyover");
        restoreCamera(test);
    }

    private static FrenchieRenderState renderState(Minecraft mc, FrenchieEntity dog) {
        var renderer = (FrenchieRenderer) mc.getEntityRenderDispatcher().getRenderer(dog);
        var state = renderer.createRenderState();
        renderer.extractRenderState(dog, state, 0);
        return state;
    }

    private static void portrait(Playtest test, int[] dogId, AdventureOutfit outfit, String name, Vec3 offset) {
        var settle = new int[1];
        test.awaitClient("Observe " + outfit + " on the tracked Frenchie", mc -> dog(mc, dogId[0], outfit) != null);
        test.client("Move the camera to the " + name + " angle", mc -> {
            aimAtDog(mc, dogId[0], offset);
            settle[0] = mc.player.tickCount + 5;
        });
        test.awaitClient("Settle the camera for " + name, mc -> {
            aimAtDog(mc, dogId[0], offset);
            return mc.player.tickCount >= settle[0];
        });
        test.screenshot(name, mc -> aimAtDog(mc, dogId[0], offset));
    }

    private static void aimAtDog(Minecraft mc, int id, Vec3 offset) {
        var dog = mc.level.getEntity(id);
        if (dog == null) return;
        var target = dog.position().add(0, 0.3, 0);
        camera(mc, target.add(offset.yRot((float) -Math.toRadians(((FrenchieEntity) dog).yBodyRot))), target);
    }

    private static void camera(Minecraft mc, Vec3 eye, Vec3 target) {
        var marker = mc.getCameraEntity() instanceof Marker existing ? existing : new Marker(
                //? if <26.3 {
                EntityType.MARKER,
                //?} else {
                /*EntityTypes.MARKER,
                *///?}
                mc.level);
        Vec3 aim = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance()));
        marker.snapTo(eye.subtract(0, marker.getEyeHeight(), 0), yaw, pitch);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        //? if <26.3 {
        mc.options.hideGui = true;
        //?} else {
        /*if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        *///?}
        mc.getToastManager().clear();
        if (mc.getCameraEntity() != marker) mc.setCameraEntity(marker);
    }

    private static void restoreCamera(Playtest test) {
        test.client("Restore the owner's normal camera", mc -> mc.setCameraEntity(mc.player));
    }

    private static void prepareObserver(ServerPlayer player) {
        player.level().getServer().setDifficulty(Difficulty.PEACEFUL, true);
        player.setGameMode(GameType.CREATIVE);
        player.setData(FrenchieRegistry.FRENCH_FRONTPACK_DATA, FrontpackData.DEFAULT);
        player.getInventory().clearContent();
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 6000, 0, false, false));
    }

    private static FrenchieEntity dog(ServerPlayer player, int id) {
        var entity = player.level().getEntity(id);
        Playtest.require(entity instanceof FrenchieEntity, "The fixture Frenchie disappeared");
        return (FrenchieEntity) entity;
    }

    private static FrenchieEntity dog(Minecraft mc, int id, AdventureOutfit outfit) {
        return mc.level.getEntity(id) instanceof FrenchieEntity dog && dog.adventureOutfit() == outfit ? dog : null;
    }

    private static FrenchieEntity spawn(ServerPlayer player, BlockPos feet) {
        var dog = FrenchieRegistry.FRENCHIE.get().create(player.level(), EntitySpawnReason.COMMAND);
        Playtest.require(dog != null, "Could not create a Frenchie");
        dog.setPersistenceRequired();
        dog.tame(player);
        dog.snapTo(Vec3.atBottomCenterOf(feet), 0, 0);
        Playtest.require(player.level().addFreshEntity(dog), "Could not add the Frenchie to the test world");
        return dog;
    }

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos feet) {
        player.teleportTo(level, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, Set.of(), 180, 0, true);
    }

    private static void platform(ServerLevel level, BlockPos feet, int radius, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-radius, -1, -radius), feet.offset(radius, -1, radius))) {
            level.setBlockAndUpdate(pos, block.defaultBlockState());
        }
    }
}
