package net.j40climb.frenchies.test;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

final class CompanionWeatherTests {
    private CompanionWeatherTests() {}

    static void snow(GameTestHelper h) {
        biome(h, Biomes.SNOWY_PLAINS);
        var owner = CompanionTravelTests.player(h, 5.5, 8.5);
        var exposed = CompanionWhistleTests.dog(h, owner, 3, 4);
        var heated = CompanionWhistleTests.dog(h, owner, 7, 4);
        var sheltered = CompanionWhistleTests.dog(h, owner, 5, 7);
        h.setBlock(new BlockPos(5, 3, 7), Blocks.STONE);
        var restored = new net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(exposed.snowLayer() > 0 && exposed.snowLayer() < 5,
                        "Exposed snowy biome never produced an intermediate coat layer"))
                .thenWaitUntil(() -> h.assertTrue(exposed.snowLayer() == 5 && heated.snowLayer() == 5,
                        "Exposed coats never filled"))
                .thenExecute(() -> {
                    h.assertTrue(sheltered.snowLayer() == 0, "Roof allowed snow to accumulate indoors");
                    restored[0] = CompanionTravelTests.reload(h, exposed);
                    h.assertTrue(restored[0].snowLayer() == 5, "Reload lost the accumulated snow");
                    h.setBlock(new BlockPos(3, 3, 4), Blocks.STONE);
                    h.setBlock(new BlockPos(7, 3, 4), Blocks.STONE);
                    h.setBlock(new BlockPos(8, 1, 4), Blocks.CAMPFIRE);
                })
                .thenExecuteAfter(20, () -> h.assertTrue(restored[0].snowLayer() > 0,
                        "Going under cover cleared the coat immediately"))
                .thenWaitUntil(() -> h.assertTrue(heated.snowLayer() == 0, "Campfire did not melt the snow"))
                .thenExecute(() -> h.assertTrue(restored[0].snowLayer() > 0 && restored[0].snowLayer() < 5,
                        "Unheated coat did not melt more slowly than the heated coat"))
                .thenWaitUntil(() -> h.assertTrue(restored[0].snowLayer() == 0, "Coat never melted under cover"))
                .thenExecute(() -> CompanionTravelTests.disconnect(owner)).thenSucceed();
    }

    static void rain(GameTestHelper h) {
        biome(h, Biomes.PLAINS);
        // An untamed dog can shake too. A short open pen keeps the observer in particle range.
        for (int i = 1; i <= 9; i++) {
            h.setBlock(new BlockPos(i, 1, 1), Blocks.OAK_FENCE);
            h.setBlock(new BlockPos(i, 1, 9), Blocks.OAK_FENCE);
            h.setBlock(new BlockPos(1, 1, i), Blocks.OAK_FENCE);
            h.setBlock(new BlockPos(9, 1, i), Blocks.OAK_FENCE);
        }
        var player = CompanionTravelTests.player(h, 1.5, 0.5);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 5, 1, 5);
        var splashes = GameTestEffects.particles(player, ParticleTypes.SPLASH, dog.position(), 100);
        h.startSequence()
                .thenExecuteFor(880, () -> h.assertTrue(dog.weatherStage() != 1 && splashes.count() == 0,
                        "Rain shake triggered before its minimum exposure cooldown"))
                .thenWaitUntil(() -> h.assertTrue(dog.weatherStage() == 1, "Rain-soaked companion never shook"))
                .thenWaitUntil(() -> h.assertTrue(splashes.count() > 0, "Rain shake sent no droplets to the observer"))
                .thenWaitUntil(() -> h.assertTrue(dog.weatherStage() == 0, "Rain shake never finished"))
                .thenExecuteFor(300, () -> h.assertTrue(dog.weatherStage() != 1 && splashes.count() == 1,
                        "Rain shake immediately repeated or kept emitting droplets"))
                .thenExecute(() -> {
                    splashes.close();
                    CompanionTravelTests.disconnect(player);
                }).thenSucceed();
    }

    private static void biome(GameTestHelper h, ResourceKey<Biome> key) {
        var level = h.getLevel();
        var low = h.absolutePos(new BlockPos(-4, -4, -4));
        var high = h.absolutePos(new BlockPos(14, 6, 14));
        var biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        var result = FillBiomeCommand.fill(level, low, high, biomes.getOrThrow(key));
        h.assertTrue(result.right().isEmpty() && level.getBiome(h.absolutePos(new BlockPos(5, 1, 5))).is(key),
                "Could not establish the precipitation biome for this test");
    }
}
