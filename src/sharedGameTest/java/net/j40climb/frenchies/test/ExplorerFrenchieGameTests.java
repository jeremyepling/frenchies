package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.common.entity.frenchie.travel.ExplorerOutfit;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.Set;

final class ExplorerFrenchieGameTests {
    private static final InteractionHand HAND = InteractionHand.MAIN_HAND;
    private static final int DIGGING = 2;

    static void maps(GameTestHelper h) {
        var owner = player(h, 4.5, 5.5);
        var visitor = player(h, 6.5, 5.5);
        var dog = h.spawnWithNoFreeWill(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        dog.tame(owner);
        var visitorDog = h.spawnWithNoFreeWill(FrenchieRegistry.FRENCHIE.get(), 7, 1, 5);
        visitorDog.tame(visitor);

        BlockPos marker = h.absolutePos(new BlockPos(8, 1, 6));
        ItemStack ordinary = createMap(h, marker);
        clickMap(h, dog, owner, ordinary);
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE, "A plain filled map started guidance");
        clickMap(h, dog, owner, new ItemStack(Items.FILLED_MAP));
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE, "A filled map with missing world data started guidance");

        var unrelatedMarker = createMap(h, marker);
        MapItemSavedData.addTargetDecoration(unrelatedMarker, marker, "+", MapDecorationTypes.RED_MARKER);
        clickMap(h, dog, owner, unrelatedMarker);
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE, "An ordinary map pin started guidance");

        ItemStack otherDimension = map(h, marker, MapDecorationTypes.RED_X);
        h.getLevel().setMapData(otherDimension.get(DataComponents.MAP_ID),
                MapItemSavedData.createFresh(marker.getX(), marker.getZ(), (byte) 2, true, true, Level.NETHER));
        clickMap(h, dog, owner, otherDimension);
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE, "A map from another dimension started guidance");

        ItemStack pirate = map(h, marker, MapDecorationTypes.RED_X);
        clickMap(h, dog, visitor, pirate);
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE, "A visitor started a map job on someone else's dog");
        clickMap(h, visitorDog, visitor, pirate);
        h.assertTrue(visitorDog.explorerOutfit() == ExplorerOutfit.PIRATE, "Visitor could not guide their own dog");

        ItemStack first = map(h, marker, MapDecorationTypes.TRIAL_CHAMBERS);
        ItemStack second = map(h, marker.east(20), MapDecorationTypes.TRIAL_CHAMBERS);
        clickMap(h, dog, owner, first);
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.EXPLORER, "Trial chambers did not equip the explorer outfit");
        clickMap(h, dog, owner, second);
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.EXPLORER, "A different map with the same icon canceled guidance");
        clickMap(h, dog, owner, second.copy());
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE, "A copy of the supplied map did not cancel guidance");

        var genericTypes = List.of(MapDecorationTypes.WOODLAND_MANSION, MapDecorationTypes.OCEAN_MONUMENT,
                MapDecorationTypes.DESERT_VILLAGE, MapDecorationTypes.PLAINS_VILLAGE, MapDecorationTypes.SAVANNA_VILLAGE,
                MapDecorationTypes.SNOWY_VILLAGE, MapDecorationTypes.TAIGA_VILLAGE, MapDecorationTypes.JUNGLE_TEMPLE,
                //? if >=26.3 {
                /*MapDecorationTypes.ABANDONED_CAMP, MapDecorationTypes.ANCIENT_CITY, MapDecorationTypes.DESERT_PYRAMID,
                MapDecorationTypes.MINESHAFT, MapDecorationTypes.OCEAN_RUIN_WARM,
                *///?}
                MapDecorationTypes.SWAMP_HUT);
        for (var type : genericTypes) {
            var explorer = map(h, marker, type);
            clickMap(h, dog, owner, explorer);
            h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.EXPLORER, "Structure marker rejected: " + type);
            clickMap(h, dog, owner, explorer);
            h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE, "Structure trail did not cancel: " + type);
        }

        clickMap(h, dog, owner, first);
        clickMap(h, dog, owner, pirate);
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.PIRATE, "Buried treasure did not switch to the pirate outfit");
        clickMap(h, dog, owner, ordinary);
        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.PIRATE, "An unsupported map interrupted the active map");
        var restored = CompanionTravelTests.reload(h, dog);
        h.assertTrue(restored.isOwnedBy(owner) && restored.explorerOutfit() == ExplorerOutfit.PIRATE,
                "Frenchie save/load lost the active map or owner");
        clickMap(h, restored, owner, pirate.copy());
        h.assertTrue(restored.explorerOutfit() == ExplorerOutfit.NONE, "Frenchie save/load lost the map identity");
        h.assertTrue(visitorDog.explorerOutfit() == ExplorerOutfit.PIRATE, "The owner's map changes affected another Frenchie");

        owner.setLastDeathLocation(Optional.of(GlobalPos.of(h.getLevel().dimension(), marker)));
        for (var compass : List.of(Items.COMPASS, Items.RECOVERY_COMPASS)) {
            owner.setItemInHand(HAND, new ItemStack(compass));
            GameTestPlayers.interact(owner, restored, HAND);
            h.assertTrue(restored.isDeathTracking() && restored.explorerOutfit() == ExplorerOutfit.NONE,
                    "The compass did not start its own death trail: " + compass);
            h.assertTrue(owner.getInventory().countItem(compass) == 1, "The compass was consumed or duplicated");
            owner.setItemInHand(HAND, ItemStack.EMPTY);
            GameTestPlayers.interact(owner, restored, HAND);
            h.assertTrue(!restored.isDeathTracking() && restored.isOrderedToSit(), "The existing empty-hand cancel order failed");
        }
        h.succeed();
    }

    static void pacing(GameTestHelper h) {
        var owner = player(h, 1.5, 5.5);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        dog.tame(owner);
        var farMap = map(h, h.absolutePos(new BlockPos(60, 1, 5)), MapDecorationTypes.PLAINS_VILLAGE);
        BlockPos pillar = new BlockPos(10, 1, 5);
        fill(h, pillar, pillar.above(2), Blocks.OBSIDIAN);
        var arrivalMap = map(h, h.absolutePos(pillar), MapDecorationTypes.TRIAL_CHAMBERS);
        var lastPosition = new Vec3[]{dog.position()};
        h.onEachTick(() -> {
            h.assertTrue(lastPosition[0].distanceToSqr(dog.position()) < 4, "Frenchie teleported while leading on foot");
            lastPosition[0] = dog.position();
        });

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie never settled on open ground"))
                .thenExecute(() -> clickMap(h, dog, owner, farMap))
                .thenWaitUntil(() -> h.assertTrue(localX(h, dog.blockPosition()) >= 8
                                && dog.getNavigation().isDone(),
                        "Frenchie did not establish a lead and wait; lead=" + dog.distanceTo(owner)
                                + ", path stopped=" + dog.getNavigation().isDone()))
                .thenExecuteFor(20, () -> h.assertTrue(dog.distanceToSqr(owner) >= 49 && dog.distanceToSqr(owner) <= 121,
                        "Frenchie did not hold roughly an eight-block lead on open ground"))
                .thenExecute(() -> {
                    owner.snapTo(h.absoluteVec(new Vec3(7.5, 1, 5.5)), 0, 0);
                    clickMap(h, dog, owner, arrivalMap);
                })
                .thenWaitUntil(() -> h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE && dog.celebrationStage() != 0,
                        "Approaching the marked pillar did not finish guidance with a celebration"))
                .thenWaitUntil(() -> h.assertTrue(dog.celebrationStage() == 0, "Arrival celebration never ended"))
                .thenExecuteFor(25, () -> h.assertTrue(dog.celebrationStage() == 0 && dog.distanceToSqr(owner) < 64,
                        "Frenchie repeated its celebration or left the arrival area"))
                .thenSucceed();
    }

    static void route(GameTestHelper h) {
        fill(h, new BlockPos(1, 4, 3), new BlockPos(4, 4, 7), Blocks.STONE);
        fill(h, new BlockPos(1, 5, 3), new BlockPos(9, 5, 7), Blocks.AIR);
        fill(h, new BlockPos(1, 0, 3), new BlockPos(9, 0, 7), Blocks.STONE);
        var owner = player(h, 2.5, 5.5);
        owner.snapTo(h.absoluteVec(new Vec3(2.5, 5, 5.5)), 0, 0);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 5, 5);
        dog.tame(owner);
        double highGround = h.absolutePos(new BlockPos(3, 5, 5)).getY();
        BlockPos marker = h.absolutePos(new BlockPos(1500, 1, 5));
        ItemStack map = map(h, marker, MapDecorationTypes.WOODLAND_MANSION);

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Frenchie never settled on the cliff platform"))
                .thenExecute(() -> {
                    h.assertTrue(!h.getLevel().hasChunkAt(marker), "Remote marker should start outside loaded terrain");
                    clickMap(h, dog, owner, map);
                })
                .thenExecuteFor(125, () -> {
                    h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.EXPLORER, "The cliff canceled guidance");
                    h.assertTrue(dog.getY() >= highGround && dog.distanceToSqr(owner) < 25,
                            "Frenchie left its owner or jumped from a high ledge");
                })
                .thenExecute(() -> {
                    // Three one-block steps let both companions descend without the four-block drop.
                    fill(h, new BlockPos(5, 3, 4), new BlockPos(5, 3, 6), Blocks.STONE);
                    fill(h, new BlockPos(6, 2, 4), new BlockPos(6, 2, 6), Blocks.STONE);
                    fill(h, new BlockPos(7, 1, 4), new BlockPos(7, 1, 6), Blocks.STONE);
                    clearHeadroomOnSteps(h);
                })
                .thenWaitUntil(() -> h.assertTrue(localX(h, dog.blockPosition()) >= 5 && dog.getY() < highGround,
                        "Frenchie did not try the newly built steps; dog x=" + localX(h, dog.blockPosition())))
                .thenExecute(() -> owner.snapTo(h.absoluteVec(new Vec3(5.5, 4, 5.5)), 0, 0))
                .thenWaitUntil(() -> h.assertTrue(localX(h, dog.blockPosition()) >= 7 && dog.getY() < highGround,
                        "Frenchie did not resume across the new stairs; dog x=" + localX(h, dog.blockPosition())))
                .thenExecuteFor(40, () -> {
                    h.assertTrue(dog.distanceToSqr(owner) <= 144, "Frenchie exceeded a 12-block lead");
                    h.assertTrue(!h.getLevel().hasChunkAt(marker), "Map guidance loaded the distant destination");
                })
                .thenSucceed();
    }

    static void travel(GameTestHelper h) {
        var home = h.getLevel();
        var nether = home.getServer().getLevel(Level.NETHER);
        h.assertTrue(nether != null, "Missing test Nether");
        var owner = player(h, 4.5, 5.5);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        dog.tame(owner);
        var id = dog.getUUID();
        var sameDimension = ChunkPos.containing(owner.blockPosition().east(96)).getMiddleBlockPosition(owner.getBlockY() + 14);
        var otherDimension = new BlockPos(88, 150, 72);
        var homeChunk = prepareLanding(h, home, sameDimension);
        var netherChunk = prepareLanding(h, nether, otherDimension);
        var map = map(h, sameDimension.east(32), MapDecorationTypes.JUNGLE_TEMPLE);

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(home.areEntitiesActuallyLoadedAndTicking(homeChunk)
                        && nether.areEntitiesActuallyLoadedAndTicking(netherChunk) && dog.onGround(),
                        "Travel fixture endpoints did not finish loading"))
                .thenExecute(() -> {
                    clickMap(h, dog, owner, map);
                    h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.EXPLORER, "Travel fixture did not start guidance");
                })
                .thenExecuteAfter(6, () -> owner.snapTo(Vec3.atBottomCenterOf(sameDimension), 0, 0))
                .thenWaitUntil(() -> h.assertTrue(home.getEntity(id) instanceof FrenchieEntity returned
                                && returned != dog && returned.distanceToSqr(owner) < 36,
                        "Existing catch-up did not bring the original Frenchie to its owner"))
                .thenExecute(() -> {
                    var returned = (FrenchieEntity) home.getEntity(id);
                    h.assertTrue(returned.isOwnedBy(owner) && returned.explorerOutfit() == ExplorerOutfit.EXPLORER,
                            "Same-dimension catch-up lost the owner or active map");
                    clickMap(h, returned, owner, map.copy());
                    h.assertTrue(returned.explorerOutfit() == ExplorerOutfit.NONE, "Catch-up lost the active map identity");
                    clickMap(h, returned, owner, map);
                })
                .thenExecuteAfter(6, () -> owner.teleportTo(nether, otherDimension.getX() + .5, otherDimension.getY(),
                        otherDimension.getZ() + .5, Set.of(), 0, 0, true))
                .thenWaitUntil(() -> h.assertTrue(nether.getEntity(id) instanceof FrenchieEntity returned
                                && returned.distanceToSqr(owner) < 36,
                        "Frenchie did not rejoin its owner across dimensions"))
                .thenExecute(() -> {
                    var returned = (FrenchieEntity) nether.getEntity(id);
                    h.assertTrue(home.getEntity(id) == null && returned.isOwnedBy(owner)
                                    && returned.explorerOutfit() == ExplorerOutfit.NONE && !returned.hasCompanionJob(),
                            "Cross-dimension catch-up duplicated the dog or kept guidance to the old dimension");
                    clickMap(h, returned, owner, map);
                    h.assertTrue(returned.explorerOutfit() == ExplorerOutfit.NONE,
                            "The previous dimension's map restarted guidance after crossing");
                    returned.discard();
                })
                .thenSucceed();
    }

    static void treasure(GameTestHelper h, boolean verified) {
        fill(h, new BlockPos(3, 2, 3), new BlockPos(9, 2, 7), Blocks.STONE);
        fill(h, new BlockPos(3, 3, 3), new BlockPos(9, 4, 7), Blocks.AIR);
        BlockPos buried = new BlockPos(8, 1, 5);
        h.setBlock(buried, Blocks.CHEST);
        h.setBlock(buried.above(), Blocks.SANDSTONE);
        var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(buried));
        h.assertTrue(chest != null, "Treasure fixture did not create a chest");
        if (verified) chest.setLootTable(BuiltInLootTables.BURIED_TREASURE, 89);
        else chest.setItem(0, new ItemStack(Items.IRON_INGOT));

        var owner = player(h, 4.5, 5.5);
        owner.snapTo(h.absoluteVec(new Vec3(4.5, 3, 5.5)), 0, 0);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 3, 5);
        dog.tame(owner);
        var map = map(h, h.absolutePos(buried.above(50)), MapDecorationTypes.RED_X);
        Vec3 aboveChest = Vec3.atBottomCenterOf(h.absolutePos(buried.above(2)));
        var sequence = h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Treasure fixture Frenchie did not settle"))
                .thenExecute(() -> {
                    clickMap(h, dog, owner, map);
                    h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.PIRATE, "Treasure map did not activate pirate guidance");
                });
        if (verified) {
            sequence.thenWaitUntil(() -> assertDigging(h, dog, aboveChest))
                    .thenWaitUntil(() -> h.assertTrue(dog.digStage() != DIGGING, "Frenchie never paused treasure digging"))
                    .thenWaitUntil(() -> assertDigging(h, dog, aboveChest))
                    .thenExecute(() -> {
                        h.assertTrue(BuiltInLootTables.BURIED_TREASURE.equals(chest.getLootTable())
                                        && chest.getLootTableSeed() == 89,
                                "Digging unpacked or changed the chest's loot table");
                        assertTreasureUntouched(h, buried);
                        owner.snapTo(aboveChest.add(-2, 0, 0), 0, 0);
                        clickMap(h, dog, owner, map.copy());
                        h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.NONE, "Repeat map click did not cancel digging guidance");
                    })
                    .thenWaitUntil(() -> h.assertTrue(dog.digStage() != DIGGING, "Canceling left the Frenchie digging"))
                    .thenSucceed();
        } else {
            sequence.thenExecuteFor(130, () -> h.assertTrue(dog.digStage() != DIGGING,
                            "Frenchie marked an unrelated chest as exact treasure"))
                    .thenExecute(() -> {
                        assertTreasureUntouched(h, buried);
                        h.assertTrue(ItemStack.matches(chest.getItem(0), new ItemStack(Items.IRON_INGOT)),
                                "The ordinary chest's original stack changed");
                    })
                    .thenSucceed();
        }
    }

    static void enderman(GameTestHelper h) {
        fill(h, new BlockPos(3, 1, 4), new BlockPos(7, 3, 6), Blocks.AIR);
        fill(h, new BlockPos(3, 4, 4), new BlockPos(7, 4, 6), Blocks.STONE);
        fill(h, new BlockPos(5, 1, 4), new BlockPos(5, 3, 6), Blocks.STONE);
        var owner = player(h, 4.5, 4.5);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 4, 1, 5);
        dog.tame(owner);
        dog.sitDown();
        var enderman = h.spawnWithNoFreeWill(EntityType.ENDERMAN, 6, 1, 5);

        h.startSequence()
                .thenExecuteFor(30, () -> h.assertTrue(!dog.isAvoidingEnderman(), "Frenchie noticed an Enderman through a wall"))
                .thenExecute(() -> fill(h, new BlockPos(5, 1, 4), new BlockPos(5, 3, 6), Blocks.AIR))
                .thenWaitUntil(() -> h.assertTrue(dog.isAvoidingEnderman(), "Visible Enderman did not trigger the head overlay"))
                .thenExecuteFor(22, () -> {
                    h.assertTrue(dog.isAvoidingEnderman() && dog.isOrderedToSit(), "Reaction flickered or interrupted the sit order");
                    h.assertTrue(enderman.getTarget() == null && dog.getTarget() == null, "The visual reaction changed combat targets");
                })
                .thenExecute(enderman::discard)
                .thenWaitUntil(() -> h.assertTrue(!dog.isAvoidingEnderman(), "Frenchie kept lowering its head after the Enderman left"))
                .thenSucceed();
    }

    private static void assertDigging(GameTestHelper h, FrenchieEntity dog, Vec3 aboveChest) {
        h.assertTrue(dog.digStage() == DIGGING && dog.position().distanceToSqr(aboveChest) < 4,
                "Frenchie did not dig directly above or beside the buried chest; offset=" + dog.position().subtract(aboveChest)
                        + ", dig stage=" + dog.digStage() + ", outfit=" + dog.explorerOutfit());
    }

    private static void assertTreasureUntouched(GameTestHelper h, BlockPos buried) {
        h.assertTrue(h.getBlockState(buried).is(Blocks.CHEST) && h.getBlockState(buried.above()).is(Blocks.SANDSTONE),
                "Cosmetic digging changed the chest or its cover block");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(buried)).inflate(4)).isEmpty(),
                "Cosmetic digging created loose items");
    }

    private static void clearHeadroomOnSteps(GameTestHelper h) {
        fill(h, new BlockPos(5, 4, 4), new BlockPos(9, 4, 6), Blocks.AIR);
        fill(h, new BlockPos(6, 3, 4), new BlockPos(9, 3, 6), Blocks.AIR);
        fill(h, new BlockPos(7, 2, 4), new BlockPos(9, 2, 6), Blocks.AIR);
        fill(h, new BlockPos(8, 1, 4), new BlockPos(9, 1, 6), Blocks.AIR);
    }

    private static void fill(GameTestHelper h, BlockPos from, BlockPos to, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) h.setBlock(pos, block);
    }

    private static int localX(GameTestHelper h, BlockPos position) {
        BlockPos offset = position.subtract(h.absolutePos(BlockPos.ZERO));
        Direction east = h.getAbsoluteDirection(Direction.EAST);
        return offset.getX() * east.getStepX() + offset.getZ() * east.getStepZ();
    }

    private static ChunkPos prepareLanding(GameTestHelper h, ServerLevel level, BlockPos center) {
        var chunk = ChunkPos.containing(center);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.getChunk(chunk.x() + dx, chunk.z() + dz);
        }
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, chunk, 2);
        onFinish(h, () -> level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, chunk, 2));
        for (var pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, -1, 3))) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        return chunk;
    }

    private static ItemStack createMap(GameTestHelper h, BlockPos marker) {
        return MapItem.create(h.getLevel(), marker.getX(), marker.getZ(), (byte) 2, true, true);
    }

    private static ItemStack map(GameTestHelper h, BlockPos marker, Holder<MapDecorationType> type) {
        ItemStack map = createMap(h, marker);
        MapItemSavedData.addTargetDecoration(map, marker, "+", type);
        return map;
    }

    private static void clickMap(GameTestHelper h, FrenchieEntity dog, ServerPlayer owner, ItemStack map) {
        ItemStack expected = map.copy();
        owner.setItemInHand(HAND, map);
        GameTestPlayers.interact(owner, dog, HAND);
        h.assertTrue(ItemStack.matches(owner.getMainHandItem(), expected) && owner.getInventory().countItem(Items.FILLED_MAP) == 1,
                "Frenchie changed, consumed, or duplicated the held map");
    }

    private static ServerPlayer player(GameTestHelper h, double x, double z) {
        var player = CompanionTravelTests.player(h, x, z);
        onFinish(h, () -> CompanionTravelTests.disconnect(player));
        return player;
    }

    private static void onFinish(GameTestHelper h, Runnable cleanup) {
        h.testInfo.addListener(new GameTestListener() {
            @Override public void testStructureLoaded(GameTestInfo info) {}
            @Override public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {}
            @Override public void testPassed(GameTestInfo info, GameTestRunner runner) { cleanup.run(); }
            @Override public void testFailed(GameTestInfo info, GameTestRunner runner) { cleanup.run(); }
        });
    }
}
