package net.j40climb.frenchies.test;

import net.j40climb.frenchies.common.entity.frenchie.FrenchieEntity;
import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
//? if <26.3 {
import net.minecraft.world.entity.EntityType;
//?} else {
/*import net.minecraft.world.entity.EntityTypes;
*///?}
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.Vec3;

final class WeatherPlayTests {
    static void snowball(GameTestHelper h) {
        var owner = CompanionTravelTests.player(h, 8.5, 8.5);
        FrenchieEntity dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        dog.tame(owner);
        dog.standUp();
        var snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
        for (int x = 3; x <= 5; ++x) for (int z = 4; z <= 6; ++z) h.setBlock(new BlockPos(x, 1, z), snow);
        Vec3 landing = h.absoluteVec(new Vec3(4.5, 1.125, 5.5));
        float health = dog.getHealth();
        h.startSequence().thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Dog has not landed"))
                .thenExecute(() -> {
                    var snowball = EntityType.SNOWBALL.create(h.getLevel(), EntitySpawnReason.COMMAND);
                    h.assertTrue(snowball != null, "Could not create snowball fixture");
                    snowball.setPos(landing.add(0, 1, 0));
                    snowball.setDeltaMovement(0, -0.7, 0);
                    h.assertTrue(h.getLevel().addFreshEntity(snowball), "Could not launch the snowball");
                })
                .thenWaitUntil(() -> h.assertTrue(dog.weatherStage() == 6, "Snowball did not start a safe approach"))
                .thenWaitUntil(() -> h.assertTrue(dog.weatherStage() == 7, "Snow approach did not become a dive"))
                .thenWaitUntil(() -> h.assertTrue(dog.weatherStage() == 8 || dog.weatherStage() == 9,
                        "Snow dive did not plow or pop"))
                .thenWaitUntil(() -> h.assertTrue(dog.weatherStage() == 0, "Snow sequence did not finish"))
                .thenExecute(() -> {
                    h.assertTrue(dog.getHealth() == health && !dog.isInWater(),
                            "Snow play harmed or immersed the dog");
                    for (int x = 3; x <= 5; ++x) for (int z = 4; z <= 6; ++z)
                        h.assertTrue(h.getBlockState(new BlockPos(x, 1, z)).equals(snow), "Snow was modified");
                    CompanionTravelTests.disconnect(owner);
                }).thenSucceed();
    }

    static void swim(GameTestHelper h) {
        var owner = CompanionTravelTests.player(h, 8.5, 8.5);
        FrenchieEntity dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 4, 1, 5);
        dog.tame(owner);
        dog.standUp();
        for (int x = 3; x <= 5; ++x) for (int z = 4; z <= 6; ++z)
            h.setBlock(new BlockPos(x, 1, z), Blocks.WATER);
        h.setBlock(new BlockPos(6, 0, 5), Blocks.GRASS_BLOCK);
        float health = dog.getHealth();
        h.startSequence().thenExecuteAfter(45, () -> {
                    h.assertTrue(dog.isInWater(), "Fixture failed to immerse the dog");
                    for (int x = 3; x <= 5; ++x) for (int z = 4; z <= 6; ++z)
                        h.setBlock(new BlockPos(x, 1, z), Blocks.AIR);
                }).thenWaitUntil(() -> h.assertTrue(dog.weatherStage() == 1, "Genuine swim did not trigger shake"))
                .thenWaitUntil(() -> h.assertTrue(dog.weatherStage() == 0 || dog.weatherStage() >= 2,
                        "Shake never finished"))
                .thenExecute(() -> {
                    h.assertTrue(dog.getHealth() == health, "Drying harmed the dog");
                    h.assertTrue(h.getBlockState(new BlockPos(6, 0, 5)).is(Blocks.GRASS_BLOCK), "Drying changed grass");
                    CompanionTravelTests.disconnect(owner);
                }).thenSucceed();
    }
}
