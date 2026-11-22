package net.j40climb.frenchies.playtest;

import net.j40climb.frenchies.setup.NeoForgeGameplayEvents;
import com.mojang.logging.LogUtils;
import net.j40climb.frenchies.Frenchies;
import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/** Self-contained two-observer fixture. All terrain is disposable and local to this world. */
@EventBusSubscriber(modid = Frenchies.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class PlayfulInteractionServer {
    static final UUID DOG = UUID.fromString("4b71b711-69b1-489d-960c-b7a3044860be");
    static final String TRICK = "Playful fixture: trick";
    static final String BELLY = "Playful fixture: belly";
    static final String RUB = "Playful fixture: rub";
    static final String STICK = "Playful fixture: stick portrait";
    static final String REJOIN = "Playful fixture: rejoin";
    static final String DONE = "Playful fixture: done";
    private static final BlockPos YARD = new BlockPos(0, 200, 0);
    private static final String[] TRICK_FRAMES = {"trick-crouch", "trick-spring", "trick-flop",
            "trick-wiggle", "trick-roll-up"};
    private static final String[] BELLY_FRAMES = {"belly-up", "belly-kick-high", "belly-kick-low"};
    private static final String[] DRY_FRAMES = {"dry-shake", "dry-shoulder", "dry-roll", "dry-drag"};
    private static final String[] SNOW_FRAMES = {"snow-dive", "snow-plow", "snow-pop"};
    private enum Step { JOIN, TRICK, BELLY, RUB, BELLY_EXIT, SWIM, DRY, SNOW_INVITE, SNOW, DIG, DIG_STICK, REJOIN, EXIT }
    private static Step step = Step.JOIN;
    private static Path root;
    private static int phase;
    private static long since;
    private static int attempts;
    private static boolean forcedRoll;

    private PlayfulInteractionServer() {}

    @SubscribeEvent
    public static void started(ServerStartedEvent event) throws IOException {
        String path = System.getProperty("florafauna_frenchies.playfulTest.root");
        if (path == null) return;
        root = Path.of(path).toRealPath();
        require(event.getServer().getWorldPath(LevelResource.ROOT).toRealPath()
                .equals(root.resolve("server/world")), "Wrong disposable server world");
        phase = Integer.parseInt(Files.readString(root.resolve("phase")).trim());
        Files.writeString(root.resolve("ready-" + phase), "ready");
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
        ServerPlayer visitor = server.getPlayerList().getPlayerByName("FFVisitor");
        if (owner == null || visitor == null || !owner.connection.hasClientLoaded()
                || !visitor.connection.hasClientLoaded()) return;
        try {
            ServerLevel level = owner.level();
            long now = level.getGameTime();
            if (since != 0 && now - since > 260) throw new AssertionError("Timed out in " + step);
            FrenchieEntity dog = (FrenchieEntity) level.getEntity(DOG);
            switch (step) {
                case JOIN -> {
                    require(!owner.getUUID().equals(visitor.getUUID()), "Observers share a player identity");
                    server.setDifficulty(Difficulty.PEACEFUL, true);
                    server.clockManager().moveToTimeMarker(level.dimensionType().defaultClock().orElseThrow(),
                            ClockTimeMarkers.NOON);
                    preparePlayers(owner, visitor);
                    if (phase == 2) {
                        Files.deleteIfExists(root.resolve("Visitor/screenshots/visitor-reload-upright-stick.png"));
                        require(dog != null, "Saved Frenchie did not reload");
                        stageMouthPortrait(owner, visitor, dog);
                        cue(visitor, REJOIN);
                        advance(Step.REJOIN, now);
                        break;
                    }
                    floor(level, Blocks.GRASS_BLOCK);
                    dog = FrenchieRegistry.FRENCHIE.get().create(level, EntitySpawnReason.COMMAND);
                    require(dog != null, "Could not create Frenchie");
                    dog.setUUID(DOG);
                    dog.setPersistenceRequired();
                    dog.tame(owner);
                    dog.standUp();
                    dog.snapTo(Vec3.atBottomCenterOf(YARD), 0, 0);
                    require(level.addFreshEntity(dog), "Could not add Frenchie");
                    owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
                    owner.teleportTo(level, 0.5, 200, 2.5, Set.of(), 180, 0, true);
                    cue(owner, TRICK);
                    advance(Step.TRICK, now);
                }
                case TRICK -> {
                    require(dog != null, "Trick dog disappeared");
                    if (dog.trickStage() != 0 || !frames(TRICK_FRAMES)) break;
                    require(dog.onGround() && !dog.isOrderedToSit(), "Trick left dog stranded or sitting");
                    dog.getNavigation().stop();
                    dog.snapTo(Vec3.atBottomCenterOf(YARD), 0, 0);
                    owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    owner.teleportTo(level, 2.0, 200, 0.5, Set.of(), -90, 0, true);
                    // The owner's position points the dog east; the observer sees the kick in profile.
                    visitor.teleportTo(level, 0.5, 199.7, -2.1, Set.of(), 0, 0, true);
                    cue(owner, BELLY);
                    advance(Step.BELLY, now);
                }
                case BELLY -> {
                    if (dog != null && dog.bellyStage() == 2 && frame("belly-up")) {
                        cue(owner, RUB);
                        advance(Step.RUB, now);
                    }
                }
                case RUB -> {
                    if (dog == null || dog.bellyStage() != 3 || !frames(BELLY_FRAMES)) break;
                    require(!dog.isOrderedToSit(), "Belly rub toggled sit");
                    owner.teleportTo(level, 5.5, 200, 5.5, Set.of(), 180, 0, true);
                    advance(Step.BELLY_EXIT, now);
                }
                case BELLY_EXIT -> {
                    if (dog == null || dog.bellyStage() != 0) break;
                    startSwim(level, dog);
                    advance(Step.SWIM, now);
                }
                case SWIM -> {
                    require(dog != null, "Swimming dog disappeared");
                    if (now - since < 45) {
                        if (!dog.isInWater()) dog.snapTo(Vec3.atBottomCenterOf(YARD), 0, 0);
                        break;
                    }
                    for (BlockPos p : BlockPos.betweenClosed(YARD.offset(-5, 0, -5), YARD.offset(5, 0, 5)))
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    for (BlockPos p : BlockPos.betweenClosed(YARD.offset(-5, 0, -5), YARD.offset(5, 0, 5)))
                        require(level.getFluidState(p).isEmpty(), "Pool clearance left flowing water");
                    dog.snapTo(Vec3.atBottomCenterOf(YARD), 0, 0);
                    dog.setDeltaMovement(Vec3.ZERO);
                    dog.getNavigation().stop();
                    advance(Step.DRY, now);
                }
                case DRY -> {
                    if (dog != null && dog.weatherStage() == 1 && !forcedRoll) {
                        // The ordinary swim and shake occurred naturally. Select the optional
                        // grass branch only for deterministic visual coverage of its poses.
                        forceSafeGrassBranch(dog);
                        forcedRoll = true;
                    }
                    if (dog == null || dog.weatherStage() != 0 || !frames(DRY_FRAMES)) break;
                    require(level.getBlockState(YARD.below()).is(Blocks.GRASS_BLOCK), "Drying changed grass");
                    dog.snapTo(Vec3.atBottomCenterOf(YARD), 0, 0);
                    var snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
                    for (BlockPos p : BlockPos.betweenClosed(YARD.offset(-2, 0, -2), YARD.offset(2, 0, 2)))
                        level.setBlockAndUpdate(p, snow);
                    dog.snapTo(new Vec3(0.5, 200.125, 0.5), 0, 0);
                    advance(Step.SNOW_INVITE, now);
                }
                case SNOW_INVITE -> {
                    require(dog != null, "Snow dog disappeared");
                    if (now - since < 5 || !dog.onGround() || !dog.getNavigation().isDone()) break;
                    require(snowCandidate(dog) != null, "No safe shallow-snow candidate after settling");
                    var ball = EntityType.SNOWBALL.create(level, EntitySpawnReason.COMMAND);
                    require(ball != null, "Could not create snowball interest");
                    Vec3 impact = new Vec3(1.5, 200.125, 0.5);
                    NeoForgeGameplayEvents.impact(new net.neoforged.neoforge.event.entity.ProjectileImpactEvent(
                            ball, new BlockHitResult(impact, Direction.UP, BlockPos.containing(impact), false)));
                    advance(Step.SNOW, now);
                }
                case SNOW -> {
                    if (dog == null || dog.weatherStage() != 0 || !frames(SNOW_FRAMES)) break;
                    require(level.getBlockState(YARD).is(Blocks.SNOW), "Snow play changed layers");
                    for (BlockPos p : BlockPos.betweenClosed(YARD.offset(-2, 0, -2), YARD.offset(2, 0, 2)))
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    floor(level, Blocks.DIRT);
                    dog.snapTo(Vec3.atBottomCenterOf(YARD), 0, 0);
                    owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
                    owner.teleportTo(level, 4.5, 200, 3.5, Set.of(), 180, 0, true);
                    for (int x = 3; x <= 6; x++) for (int z = -4; z <= 4; z++)
                        level.setBlockAndUpdate(new BlockPos(x, 200, z), Blocks.DIRT.defaultBlockState());
                    attempts = 0;
                    advance(Step.DIG, now);
                }
                case DIG -> {
                    require(dog != null, "Digging dog disappeared");
                    if (dog.digStage() == 0 && attempts < 36) {
                        int x = 3 + attempts / 9;
                        int z = -4 + attempts % 9;
                        owner.gameMode.destroyBlock(new BlockPos(x, 200, z));
                        attempts++;
                    }
                    if (dog.digStage() != 0 || !frame("dig-paws")) break;
                    require(dog.getMouthItem().is(Items.STICK) && dog.getOffhandItem().isEmpty(),
                            "Reusable stick was not a virtual mouth prop");
                    require(level.getBlockState(YARD.below()).is(Blocks.DIRT), "Frenchie changed digging floor");
                    require(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            dog.getBoundingBox().inflate(8), item -> item.getItem().is(Items.STICK)).isEmpty(),
                            "Frenchie dropped a collectible stick");
                    stageMouthPortrait(owner, visitor, dog);
                    cue(visitor, STICK);
                    advance(Step.DIG_STICK, now);
                }
                case DIG_STICK -> {
                    require(dog != null && dog.getMouthItem().is(Items.STICK), "Staged portrait lost toy stick");
                    require(visitor.hasLineOfSight(dog), "Visitor cannot see the mouth-stick portrait");
                    if (frame("dig-stick")) {
                        dog.setNoAi(false);
                        finish(owner, visitor, "PASS");
                    }
                }
                case REJOIN -> {
                    if (dog == null) break;
                    require(visitor.hasLineOfSight(dog), "Visitor cannot see the reloaded stick portrait");
                    require(dog.getMouthItem().is(Items.STICK) && dog.getOffhandItem().isEmpty(),
                            "Reload lost or duplicated the virtual stick");
                    require(dog.trickStage() == 0 && dog.bellyStage() == 0
                            && dog.weatherStage() == 0 && dog.digStage() == 0,
                            "Reload retained an unfinished pose");
                    if (frame("reload-upright-stick")) {
                        dog.setNoAi(false);
                        finish(owner, visitor, "PASS");
                    }
                }
                case EXIT -> { }
            }
        } catch (AssertionError | RuntimeException e) {
            LogUtils.getLogger().error("Playful fixture failed in {}", step, e);
            finish(owner, visitor, "FAIL " + step + ": " + e);
        }
    }

    private static void startSwim(ServerLevel level, FrenchieEntity dog) {
        dog.snapTo(Vec3.atBottomCenterOf(YARD), 0, 0);
        for (BlockPos p : BlockPos.betweenClosed(YARD.offset(-2, 0, -2), YARD.offset(2, 0, 2)))
            level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
    }

    private static void floor(ServerLevel level, net.minecraft.world.level.block.Block block) {
        for (BlockPos p : BlockPos.betweenClosed(YARD.offset(-10, -1, -10), YARD.offset(10, -1, 10)))
            level.setBlockAndUpdate(p, block.defaultBlockState());
    }

    private static void preparePlayers(ServerPlayer owner, ServerPlayer visitor) {
        owner.setGameMode(GameType.SURVIVAL);
        visitor.setGameMode(GameType.SPECTATOR);
        owner.getInventory().clearContent();
        owner.teleportTo(owner.level(), 0.5, 200, 2.5, Set.of(), 180, 0, true);
        visitor.teleportTo(visitor.level(), -2.2, 199.7, -1.4, Set.of(), -50, 0, true);
    }

    private static void stageMouthPortrait(ServerPlayer owner, ServerPlayer visitor, FrenchieEntity dog) {
        // Keep the real excavation untouched; use the open yard for an unobstructed stick close-up.
        dog.getNavigation().stop();
        dog.setDeltaMovement(Vec3.ZERO);
        dog.snapTo(Vec3.atBottomCenterOf(YARD), 0, 0);
        dog.yBodyRot = 0;
        dog.setYHeadRot(0);
        dog.setNoAi(true);
        owner.teleportTo(owner.level(), 6.5, 200, -6.5, Set.of(), 0, 0, true);
        visitor.teleportTo(visitor.level(), 0.5, 199.7, 3.1, Set.of(), 180, 0, true);
    }

    private static void cue(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal(text)); }

    private static boolean frame(String stem) {
        return Files.isRegularFile(root.resolve("Visitor/screenshots/visitor-" + stem + ".png"));
    }

    private static boolean frames(String[] stems) {
        for (String stem : stems) if (!frame(stem)) return false;
        return true;
    }

    private static void advance(Step next, long now) {
        step = next;
        since = now;
        LogUtils.getLogger().info("Playful fixture reached {}", next);
    }

    private static void finish(ServerPlayer owner, ServerPlayer visitor, String result) throws IOException {
        Files.writeString(root.resolve("phase-" + phase + ".result"), result);
        cue(owner, DONE);
        cue(visitor, DONE);
        step = Step.EXIT;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void forceSafeGrassBranch(FrenchieEntity dog) {
        try {
            Field goalField = FrenchieEntity.class.getDeclaredField("weatherPlay");
            goalField.setAccessible(true);
            Object goal = goalField.get(dog);
            Method safeSpot = goal.getClass().getDeclaredMethod("grassSpot");
            safeSpot.setAccessible(true);
            Vec3 candidate = (Vec3) safeSpot.invoke(goal);
            require(candidate != null, "No real safe grass spot for drying-roll review");
            Field targetField = goal.getClass().getDeclaredField("target");
            targetField.setAccessible(true);
            targetField.set(goal, candidate);
            LogUtils.getLogger().info("Visual fixture selected optional safe grass spot {}", candidate);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot select optional drying branch", e);
        }
    }

    private static Object snowCandidate(FrenchieEntity dog) {
        try {
            Field goalField = FrenchieEntity.class.getDeclaredField("weatherPlay");
            goalField.setAccessible(true);
            Object goal = goalField.get(dog);
            Method spot = goal.getClass().getDeclaredMethod("snowSpotNear", Vec3.class);
            spot.setAccessible(true);
            return spot.invoke(goal, new Vec3(1.5, 200.125, 0.5));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot inspect shallow-snow candidate", e);
        }
    }

}
