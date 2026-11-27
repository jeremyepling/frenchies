package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.AdventureOutfit;
import net.j40climb.frenchies.common.entity.frenchie.travel.ExplorerOutfit;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.Vec3;

import static net.j40climb.frenchies.test.CompanionTravelTests.disconnect;
import static net.j40climb.frenchies.test.CompanionTravelTests.player;

final class AdventureOutfitGameTests {
    static void snow(GameTestHelper h) {
        var level = h.getLevel();
        var from = h.absolutePos(new BlockPos(0, 0, 0)).below(3);
        var to = h.absolutePos(new BlockPos(10, 5, 10));
        var original = level.getBiome(h.absolutePos(new BlockPos(5, 1, 5)));
        var snowy = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.SNOWY_PLAINS);
        h.assertTrue(FillBiomeCommand.fill(level, from, to, snowy).left().orElse(0) > 0,
                "Could not make the yard cold enough for snowfall");
        h.testInfo.addListener(new GameTestListener() {
            @Override public void testStructureLoaded(GameTestInfo info) {}
            @Override public void testAddedForRerun(GameTestInfo originalTest, GameTestInfo copy, GameTestRunner runner) {}
            @Override public void testPassed(GameTestInfo info, GameTestRunner runner) { restore(); }
            @Override public void testFailed(GameTestInfo info, GameTestRunner runner) { restore(); }

            private void restore() { FillBiomeCommand.fill(level, from, to, original); }
        });

        addShelter(h, 7);
        var owner = player(h, 5.5, 5.5);
        var exposed = h.spawn(FrenchieRegistry.FRENCHIE.get(), 3, 1, 5);
        var sheltered = h.spawn(FrenchieRegistry.FRENCHIE.get(), 7, 1, 5);
        exposed.tame(owner);
        sheltered.tame(owner);
        exposed.sitDown();
        sheltered.sitDown();

        h.startSequence()
                .thenWaitUntil(() -> {
                    var exposedEyes = BlockPos.containing(exposed.getEyePosition());
                    var shelteredEyes = BlockPos.containing(sheltered.getEyePosition());
                    h.assertTrue(level.precipitationAt(exposedEyes) == Precipitation.SNOW
                                    && level.precipitationAt(shelteredEyes) == Precipitation.NONE,
                            "Snowfall fixture did not expose one Frenchie and shelter the other");
                })
                .thenExecuteFor(100, () -> h.assertTrue(exposed.snowLayer() <= 2 && sheltered.snowLayer() == 0,
                        "Coat accumulated too quickly or accumulated under shelter"))
                .thenWaitUntil(() -> h.assertTrue(exposed.snowLayer() == 5 && exposed.adventureOutfit() == AdventureOutfit.WINTER,
                        "Exposed Frenchie did not build up snow and equip its boots and scarf"))
                .thenExecute(() -> addShelter(h, 3))
                .thenExecuteFor(250, () -> h.assertTrue(exposed.snowLayer() > 0 && sheltered.snowLayer() == 0,
                        "Snow vanished abruptly or accumulated under shelter"))
                .thenWaitUntil(() -> h.assertTrue(exposed.snowLayer() == 0 && exposed.adventureOutfit() == AdventureOutfit.WINTER,
                        "Coat did not melt normally while the cold-weather outfit remained equipped"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    private static void addShelter(GameTestHelper h, int x) {
        for (BlockPos pos : BlockPos.betweenClosed(x - 1, 3, 4, x + 1, 3, 6)) h.setBlock(pos, Blocks.STONE);
    }

    static void mining(GameTestHelper h) {
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 7, 5, 8)) {
            if (pos.getY() == 5 || pos.getX() == 1 || pos.getX() == 7 || pos.getZ() == 1 || pos.getZ() == 8) {
                h.setBlock(pos, Blocks.STONE);
            }
        }
        var leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (BlockPos pos : BlockPos.betweenClosed(8, 5, 3, 10, 5, 7)) h.setBlock(pos, leaves);
        var owner = player(h, 8.5, 5.5);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 9, 1, 5);
        dog.tame(owner);
        dog.standUp();
        var cave = h.absolutePos(new BlockPos(4, 1, 5));

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(h.getLevel().getBrightness(LightLayer.SKY, cave) == 0,
                        "Roof and walls never blocked skylight in the cave fixture"))
                .thenExecuteFor(50, () -> h.assertTrue(dog.adventureOutfit() != AdventureOutfit.MINING
                        && dog.miningLightLevel() == 0, "The lamp turned on under a canopy"))
                .thenExecute(() -> {
                    move(h, owner, 3.5, 1, 5.5);
                    move(h, dog, 4.5, 1, 5.5);
                })
                .thenExecuteFor(20, () -> h.assertTrue(dog.adventureOutfit() != AdventureOutfit.MINING
                        && dog.miningLightLevel() == 0, "A brief visit to darkness equipped the mining lamp"))
                .thenWaitUntil(() -> h.assertTrue(dog.adventureOutfit() == AdventureOutfit.MINING,
                        "Sustained cave darkness did not equip the mining helmet"))
                .thenExecuteFor(30, () -> h.assertTrue(dog.miningLightLevel() == 11,
                        "The active helmet did not publish a steady level 11 light source"))
                .thenExecute(() -> {
                    move(h, owner, 8.5, 1, 5.5);
                    move(h, dog, 9.5, 1, 5.5);
                })
                .thenExecuteFor(20, () -> h.assertTrue(dog.adventureOutfit() == AdventureOutfit.MINING
                        && dog.miningLightLevel() == 11, "The lamp changed immediately upon leaving the cave"))
                .thenWaitUntil(() -> h.assertTrue(dog.adventureOutfit() == AdventureOutfit.NONE
                        && dog.miningLightLevel() == 0, "The mining lamp stayed active in daylight"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    static void scuba(GameTestHelper h) {
        buildScubaPool(h);
        var owner = player(h, 9.5, 5.5);
        move(h, owner, 8.25, 5.25, 5.5);
        owner.setNoGravity(true);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 5, 5);
        dog.tame(owner);
        dog.standUp();
        var health = dog.getHealth();
        var lastPosition = new Vec3[]{dog.position()};
        h.onEachTick(() -> {
            // The embedded player has no client movement packet to run its normal fluid tick.
            owner.doTick();
            h.assertTrue(dog.isAlive() && dog.position().distanceToSqr(lastPosition[0]) < 4,
                    "The dog needed a rescue or teleport to reach its diving owner");
            lastPosition[0] = dog.position();
        });

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.isInWater(), "Pool did not immerse the dog"))
                .thenExecuteFor(12, () -> h.assertTrue(dog.adventureOutfit() != AdventureOutfit.SCUBA,
                        "A surface swim equipped scuba gear"))
                .thenExecute(() -> move(h, owner, 8.25, 1.05, 5.5))
                .thenWaitUntil(() -> h.assertTrue(dog.adventureOutfit() == AdventureOutfit.SCUBA,
                        "A deep owner dive did not equip scuba gear"))
                .thenWaitUntil(() -> h.assertTrue(dog.isEyeInFluid(FluidTags.WATER) && dog.distanceToSqr(owner) < 8,
                        "Frenchie never followed its owner down and across the pool"))
                .thenExecuteFor(320, () -> {
                    owner.setAirSupply(owner.getMaxAirSupply());
                    h.assertTrue(dog.adventureOutfit() == AdventureOutfit.SCUBA && dog.isEyeInFluid(FluidTags.WATER)
                                    && dog.distanceToSqr(owner) < 10 && dog.getAirSupply() == dog.getMaxAirSupply()
                                    && dog.getHealth() == health,
                            "Frenchie surfaced, lost air, or left the diving owner");
                })
                .thenExecute(() -> move(h, owner, 9.5, 6, 5.5))
                .thenExecuteFor(5, () -> h.assertTrue(dog.adventureOutfit() == AdventureOutfit.SCUBA,
                        "Scuba gear switched off without a surface delay"))
                .thenWaitUntil(() -> h.assertTrue(dog.adventureOutfit() != AdventureOutfit.SCUBA
                                && !dog.isEyeInFluid(FluidTags.WATER),
                        "Frenchie failed to resume normal surface swimming"))
                .thenExecute(() -> {
                    var marker = h.absolutePos(new BlockPos(60, 6, 5));
                    var map = MapItem.create(h.getLevel(), marker.getX(), marker.getZ(), (byte) 2, true, true);
                    MapItemSavedData.addTargetDecoration(map, marker, "+", MapDecorationTypes.RED_X);
                    owner.setItemInHand(InteractionHand.MAIN_HAND, map);
                    GameTestPlayers.interact(owner, dog, InteractionHand.MAIN_HAND);
                    h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.PIRATE, "The treasure map did not start guidance");
                    h.assertTrue(!dog.adventureOutfit().hasHeadwear(), "The pirate hat should be available at the surface");
                    move(h, owner, 8.25, 1.05, 5.5);
                })
                .thenWaitUntil(() -> h.assertTrue(dog.adventureOutfit() == AdventureOutfit.SCUBA,
                        "Explorer headwear prevented scuba gear on a second dive"))
                .thenExecute(() -> {
                    h.assertTrue(dog.adventureOutfit().hasHeadwear() && dog.explorerOutfit() == ExplorerOutfit.PIRATE,
                            "Scuba should replace the pirate hat while keeping the explorer state");
                    move(h, owner, 9.5, 6, 5.5);
                })
                .thenWaitUntil(() -> h.assertTrue(!dog.adventureOutfit().hasHeadwear(),
                        "The scuba helmet continued to cover explorer headwear after surfacing"))
                .thenExecute(() -> h.assertTrue(dog.explorerOutfit() == ExplorerOutfit.PIRATE,
                        "Changing environmental outfits canceled the explorer appearance"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    static void scubaFromShore(GameTestHelper h) {
        buildScubaPool(h);
        // A submerged shelf gives the Frenchie a walkable step down from the dry dock.
        for (BlockPos pos : BlockPos.betweenClosed(2, 4, 3, 2, 4, 7)) h.setBlock(pos, Blocks.STONE);
        var owner = player(h, 1.5, 6.5);
        move(h, owner, 1.5, 6, 6.5);
        owner.setNoGravity(true);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 1, 6, 5);
        dog.tame(owner);
        dog.standUp();
        var lastPosition = new Vec3[]{dog.position()};
        h.onEachTick(() -> {
            owner.doTick();
            h.assertTrue(dog.isAlive() && dog.position().distanceToSqr(lastPosition[0]) < 4,
                    "The dog needed a rescue or teleport to enter the pool");
            lastPosition[0] = dog.position();
        });

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround() && !dog.isInWater(), "Dog did not start on the dry dock"))
                .thenExecute(() -> move(h, owner, 8.25, 1.05, 5.5))
                .thenWaitUntil(() -> h.assertTrue(dog.isInWater() && dog.adventureOutfit() == AdventureOutfit.SCUBA,
                        "Frenchie stayed on the dock after its nearby owner dove"))
                .thenWaitUntil(() -> h.assertTrue(dog.isEyeInFluid(FluidTags.WATER) && dog.distanceToSqr(owner) < 8,
                        "Frenchie entered from shore but did not dive and reach its owner"))
                .thenExecute(() -> disconnect(owner)).thenSucceed();
    }

    private static void buildScubaPool(GameTestHelper h) {
        for (BlockPos pos : BlockPos.betweenClosed(1, 1, 1, 9, 5, 9)) {
            boolean bank = pos.getX() == 1 || pos.getX() == 9 || pos.getZ() == 1 || pos.getZ() == 9;
            boolean ruin = pos.getX() == 5 && pos.getZ() >= 4 && pos.getZ() <= 6;
            h.setBlock(pos, bank || ruin ? Blocks.STONE : Blocks.WATER);
        }
    }

    static void thunder(GameTestHelper h) {
        var owner = player(h, 9.5, 5.5);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        dog.tame(owner);
        dog.standUp();
        var whine = dog.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT)
                .getOrThrow(WolfSoundVariants.PUGLIN).value().adultSounds().whineSound().value();
        var whimper = GameTestEffects.sounds(owner, whine, owner.position(), 144);
        var stay = new Vec3[1];
        Runnable checkStay = () -> h.assertTrue(dog.isFrightened() && dog.isOrderedToSit()
                        && dog.position().distanceToSqr(stay[0]) < 0.25,
                "Thunder did not sustain fear or broke an explicit stay");
        h.assertTrue(dog.distanceToSqr(owner) > 36, "Fixture must start outside normal nearby idle range");

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog has not reached the test floor"))
                .thenExecute(() -> strike(h))
                .thenWaitUntil(() -> h.assertTrue(dog.isFrightened(), "Nearby thunder did not frighten the dog"))
                .thenWaitUntil(() -> h.assertTrue(whimper.count() > 0, "Owner heard no whimper after thunder"))
                .thenWaitUntil(() -> h.assertTrue(dog.distanceToSqr(owner) <= 9,
                        "Frightened Frenchie did not approach its owner"))
                .thenExecute(() -> {
                    owner.interactOn(dog, InteractionHand.MAIN_HAND, Vec3.ZERO);
                    h.assertTrue(dog.isOrderedToSit(), "Frightened Frenchie ignored the owner's sit command");
                })
                .thenExecuteAfter(8, () -> {
                    stay[0] = dog.position();
                    move(h, owner, 2.5, 1, 5.5);
                })
                .thenExecuteFor(80, () -> {
                    checkStay.run();
                    h.assertTrue(whimper.count() == 1, "The Frenchie whined repeatedly soon after one strike");
                })
                .thenExecute(whimper::close)
                .thenExecuteFor(120, checkStay)
                .thenExecute(() -> strike(h))
                .thenExecuteFor(200, checkStay)
                .thenExecute(() -> strike(h))
                .thenExecuteFor(210, checkStay)
                .thenWaitUntil(() -> h.assertTrue(!dog.isFrightened(), "Frenchie stayed frightened after the last thunder"))
                .thenExecute(() -> {
                    h.assertTrue(dog.isOrderedToSit(), "Recovering from thunder cleared the sit order");
                    disconnect(owner);
                }).thenSucceed();
    }

    private static void strike(GameTestHelper h) {
        var lightning = EntityType.LIGHTNING_BOLT.create(h.getLevel(), EntitySpawnReason.COMMAND);
        if (lightning == null) throw new IllegalStateException("Could not create lightning for the thunder fixture");
        lightning.setVisualOnly(true);
        lightning.setPos(h.absoluteVec(new Vec3(5.5, 2, 5.5)));
        h.assertTrue(h.getLevel().addFreshEntity(lightning), "Could not spawn thunder fixture lightning");
    }

    private static void move(GameTestHelper h, Entity entity, double x, double y, double z) {
        entity.snapTo(h.absoluteVec(new Vec3(x, y, z)), 0, 0);
        entity.setDeltaMovement(Vec3.ZERO);
    }
}
