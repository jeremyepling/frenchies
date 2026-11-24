package net.j40climb.frenchies.playtest;

import com.mojang.logging.LogUtils;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.travel.ExplorerOutfit;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
*///?}
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class ExplorerServer {
    static final UUID DOG = UUID.fromString("5545fdfa-7fe6-4a2d-a208-c89da65ef3d4");
    static final String PREFIX = "Explorer fixture ";
    private static final BlockPos YARD = new BlockPos(0, 200, 0);
    private static final BlockPos CHEST = YARD.offset(0, -3, -4);
    private static final BlockPos TREASURE_FLOOR = CHEST.above(2);
    private static final BlockPos POOL = YARD.offset(8, 0, 0);
    private enum Step { JOIN, EXPLORER, PIRATE, DIG, BOAT, SWIM, CANCEL, HEAD_BEFORE, HEAD_DOWN, HEAD_AFTER, EXIT }
    private static Step step = Step.JOIN;
    private static Path root;
    private static long stepStarted;
    private static ItemStack explorerMap = ItemStack.EMPTY;
    private static ItemStack treasureMap = ItemStack.EMPTY;
    private static AbstractBoat boat;
    //? if <26.3 {
    private static EnderMan enderman;
    //?} else {
    /*private static Enderman enderman;
    *///?}

    private ExplorerServer() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) throws IOException {
        String path = System.getProperty("florafauna_frenchies.explorerTest.root");
        if (path == null) return;
        root = Path.of(path).toRealPath();
        require(event.getServer().getWorldPath(LevelResource.ROOT).toRealPath()
                .equals(root.resolve("server/world")), "Wrong disposable server world");
        Files.writeString(root.resolve("ready-1"), "ready");
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) throws IOException {
        if (root == null) return;
        var server = event.getServer();
        if (step == Step.EXIT) {
            if (server.getPlayerList().getPlayerCount() == 0) server.halt(false);
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayerByName("FFOwner");
        ServerPlayer observer = server.getPlayerList().getPlayerByName("FFVisitor");
        if (owner == null || observer == null || !owner.connection.hasClientLoaded()
                || !observer.connection.hasClientLoaded()) return;
        ServerLevel level = owner.level();
        long now = level.getGameTime();
        FrenchieEntity dog = (FrenchieEntity) level.getEntity(DOG);
        try {
            if (stepStarted != 0 && now - stepStarted > 280) throw new AssertionError("Timed out in " + step);
            if (step != Step.JOIN) {
                require(dog != null, "Frenchie disappeared");
                require(ItemStack.matches(explorerMap, owner.getInventory().getItem(0))
                        && ItemStack.matches(treasureMap, owner.getInventory().getItem(1)), "A map changed in the owner's inventory");
            }
            switch (step) {
                case JOIN -> {
                    server.setDifficulty(Difficulty.NORMAL, true);
                    server.clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(), ClockTimeMarkers.NOON);
                    prepare(level, owner, observer);
                    cue(owner, "use-0");
                    advance(Step.EXPLORER, now);
                }
                case EXPLORER -> {
                    if (dog.explorerOutfit() != ExplorerOutfit.EXPLORER) break;
                    if (!frame("explorer")) {
                        cueFrame(observer, "explorer", now);
                        break;
                    }
                    cue(owner, "use-1");
                    advance(Step.PIRATE, now);
                }
                case PIRATE -> {
                    if (dog.explorerOutfit() != ExplorerOutfit.PIRATE) break;
                    if (!frame("pirate")) {
                        cueFrame(observer, "pirate", now);
                        break;
                    }
                    observe(observer, -1.75, 199.35, -5.15);
                    cue(observer, "dig");
                    dog.setNoAi(false);
                    advance(Step.DIG, now);
                }
                case DIG -> {
                    if (!frame("dig") || dog.digStage() != 0) break;
                    require(dog.distanceToSqr(Vec3.atBottomCenterOf(TREASURE_FLOOR.above())) <= 0.65,
                            "The dog dug beside reachable treasure cover");
                    require(level.getBlockState(TREASURE_FLOOR).is(Blocks.SAND), "The dog broke the treasure cover");
                    require(level.getBlockEntity(CHEST) instanceof ChestBlockEntity chest
                            && BuiltInLootTables.BURIED_TREASURE.equals(chest.getLootTable()), "The dog opened or removed the chest");
                    launchBoat(level, owner, observer, dog);
                    advance(Step.BOAT, now);
                }
                case BOAT -> {
                    if (!frame("boat")) break;
                    require(boat.getControllingPassenger() == owner && dog.getVehicle() == boat
                            && dog.hasFlotationVest() && dog.explorerOutfit() == ExplorerOutfit.PIRATE,
                            "Boat ride did not retain the guide and flotation vest");
                    owner.stopRiding();
                    dog.stopRiding();
                    boat.discard();
                    placeDog(dog, POOL, true);
                    observe(observer, 6.75, 199.5, -2);
                    cue(observer, "swim");
                    advance(Step.SWIM, now);
                }
                case SWIM -> {
                    if (!frame("swim")) break;
                    placeDog(dog, YARD, true);
                    placeOwner(owner);
                    observePortrait(observer);
                    cue(owner, "use-1");
                    advance(Step.CANCEL, now);
                }
                case CANCEL -> {
                    if (dog.explorerOutfit() != ExplorerOutfit.NONE) break;
                    cue(observer, "head-before");
                    advance(Step.HEAD_BEFORE, now);
                }
                case HEAD_BEFORE -> {
                    if (!frame("head-before")) break;
                    enderman = EntityType.ENDERMAN.create(level, EntitySpawnReason.COMMAND);
                    require(enderman != null, "Could not create Enderman");
                    enderman.setNoAi(true);
                    enderman.setPersistenceRequired();
                    enderman.snapTo(-3.5, 200, -3.5, 0, 0);
                    require(level.addFreshEntity(enderman), "Could not add Enderman");
                    cue(observer, "head-down");
                    advance(Step.HEAD_DOWN, now);
                }
                case HEAD_DOWN -> {
                    if (!dog.isAvoidingEnderman() || !frame("head-down")) break;
                    enderman.discard();
                    cue(observer, "head-after");
                    advance(Step.HEAD_AFTER, now);
                }
                case HEAD_AFTER -> {
                    if (!dog.isAvoidingEnderman() && frame("head-after")) finish(owner, observer, "PASS");
                }
                case EXIT -> {}
            }
        } catch (AssertionError | RuntimeException failure) {
            LogUtils.getLogger().error("Explorer fixture failed in {}", step, failure);
            finish(owner, observer, "FAIL " + step + ": " + failure);
        }
    }

    private static void prepare(ServerLevel level, ServerPlayer owner, ServerPlayer observer) {
        for (BlockPos pos : BlockPos.betweenClosed(YARD.offset(-11, -1, -11), YARD.offset(13, -1, 11))) {
            level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
        }
        level.setBlockAndUpdate(CHEST.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
        if (!(level.getBlockEntity(CHEST) instanceof ChestBlockEntity chest)) {
            throw new AssertionError("Could not create a treasure chest");
        }
        chest.setLootTable(BuiltInLootTables.BURIED_TREASURE);
        level.setBlockAndUpdate(CHEST.above(), Blocks.SANDSTONE.defaultBlockState());
        level.setBlockAndUpdate(TREASURE_FLOOR, Blocks.SAND.defaultBlockState());
        for (BlockPos pos : BlockPos.betweenClosed(POOL.offset(-3, 0, -3), POOL.offset(3, 1, 3))) {
            boolean wall = Math.abs(pos.getX() - POOL.getX()) == 3 || Math.abs(pos.getZ() - POOL.getZ()) == 3;
            level.setBlockAndUpdate(pos, (wall ? Blocks.OAK_PLANKS : Blocks.WATER).defaultBlockState());
        }
        level.setBlockAndUpdate(POOL.offset(-4, 1, 0), Blocks.OAK_PLANKS.defaultBlockState());

        owner.setGameMode(GameType.SURVIVAL);
        owner.getInventory().clearContent();
        explorerMap = MapItem.create(level, 0, 0, (byte) 2, true, true);
        MapItemSavedData.addTargetDecoration(explorerMap, YARD.north(32), "+", MapDecorationTypes.PLAINS_VILLAGE);
        treasureMap = MapItem.create(level, 0, 0, (byte) 1, true, true);
        MapItemSavedData.addTargetDecoration(treasureMap, CHEST, "+", MapDecorationTypes.RED_X);
        owner.getInventory().setItem(0, explorerMap.copy());
        owner.getInventory().setItem(1, treasureMap.copy());
        placeOwner(owner);
        observer.setGameMode(GameType.SPECTATOR);
        observePortrait(observer);
        FrenchieEntity dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
        require(dog != null, "Could not create Frenchie");
        dog.setUUID(DOG);
        dog.setPersistenceRequired();
        dog.tame(owner);
        dog.standUp();
        placeDog(dog, YARD, true);
        require(level.addFreshEntity(dog), "Could not add Frenchie");
    }

    private static void launchBoat(ServerLevel level, ServerPlayer owner, ServerPlayer observer, FrenchieEntity dog) {
        boat = EntityType.OAK_BOAT.create(level, EntitySpawnReason.COMMAND);
        require(boat != null, "Could not create an oak boat");
        boat.snapTo(POOL.getX() + 0.5, 201.9, POOL.getZ() + 0.5, 180, 0);
        require(level.addFreshEntity(boat), "Could not add the boat");
        placeDog(dog, POOL.offset(-4, 2, 0), false);
        owner.teleportTo(level, 8.5, 202, 0.5, Set.of(), 180, 0, true);
        // The fixture boards the owner, then waits for the dog's ordinary captain behavior.
        require(owner.startRiding(boat), "Could not board the boat");
        observe(observer, 6.45, 201.15, -1.35);
        cue(observer, "boat");
    }

    private static void placeDog(FrenchieEntity dog, BlockPos pos, boolean holdPose) {
        dog.getNavigation().stop();
        dog.setDeltaMovement(Vec3.ZERO);
        dog.snapTo(Vec3.atBottomCenterOf(pos), 180, 0);
        dog.yBodyRot = 180;
        dog.setYHeadRot(180);
        dog.setNoAi(holdPose);
    }

    private static void placeOwner(ServerPlayer owner) {
        owner.teleportTo(owner.level(), 1.7, 200, -1.5, Set.of(), 0, 0, true);
    }

    private static void observe(ServerPlayer observer, double x, double y, double z) {
        observer.teleportTo(observer.level(), x, y, z, Set.of(), 0, 0, true);
    }

    private static void observePortrait(ServerPlayer observer) {
        observe(observer, -1.4, 199.4, -1.25);
    }

    private static void cueFrame(ServerPlayer observer, String name, long now) {
        if ((now - stepStarted) % 20 == 0) cue(observer, name);
    }

    private static void cue(ServerPlayer player, String name) {
        player.sendSystemMessage(Component.literal(PREFIX + name));
    }

    private static boolean frame(String name) {
        return Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-" + name + ".png"));
    }

    private static void advance(Step next, long now) {
        step = next;
        stepStarted = now;
        LogUtils.getLogger().info("Explorer fixture reached {}", next);
    }

    private static void finish(ServerPlayer owner, ServerPlayer observer, String result) throws IOException {
        Files.writeString(root.resolve("phase-1.result"), result);
        cue(owner, "done");
        cue(observer, "done");
        step = Step.EXIT;
    }

    private static void require(boolean supported, String message) {
        if (!supported) throw new AssertionError(message);
    }
}
