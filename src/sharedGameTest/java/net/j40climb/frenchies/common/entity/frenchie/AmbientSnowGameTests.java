package net.j40climb.frenchies.common.entity.frenchie;

import net.j40climb.frenchies.setup.FrenchieRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

/** Bounded ambient selection check without waiting for an arbitrary random scan tick. */
public final class AmbientSnowGameTests {
    private AmbientSnowGameTests() {}

    public static void diveWithoutSnowball(GameTestHelper h) {
        var owner = h.makeMockPlayer(GameType.SURVIVAL);
        owner.snapTo(h.absoluteVec(new Vec3(2.5, 1, 8.5)), 0, 0);
        var dog = h.spawn(FrenchieRegistry.FRENCHIE.get(), 2, 1, 5);
        dog.tame(owner);
        dog.standUp();
        var snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
        for (int x = 3; x <= 6; ++x) for (int z = 4; z <= 6; ++z)
            h.setBlock(new BlockPos(x, 1, z), snow);
        float health = dog.getHealth();
        var reached = new boolean[3];
        var goal = weatherGoal(dog);

        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(dog.onGround(), "Ambient snow dog never settled"))
                .thenExecute(() -> {
                    dog.getNavigation().stop();
                    boolean selected = false;
                    for (int attempt = 0; attempt < 256; ++attempt) {
                        resetScan(goal);
                        if (goal.canUse()) {
                            selected = true;
                            break;
                        }
                    }
                    h.assertTrue(selected, "Ambient scan did not select reachable shallow snow");
                    goal.start();
                    h.assertTrue(dog.weatherStage() == WeatherPlayGoal.SNOW_APPROACH,
                            "Ambient selection did not begin snow approach");
                })
                .thenExecuteFor(130, () -> {
                    if (dog.weatherStage() != WeatherPlayGoal.NONE) {
                        h.assertTrue(goal.canContinueToUse(), "Ambient snow play became unsafe or stuck");
                        goal.tick();
                    }
                    if (dog.weatherStage() == WeatherPlayGoal.SNOW_DIVE) reached[0] = true;
                    if (dog.weatherStage() == WeatherPlayGoal.SNOW_PLOW) reached[1] = true;
                    if (dog.weatherStage() == WeatherPlayGoal.SNOW_POP) reached[2] = true;
                })
                .thenExecute(() -> {
                    h.assertTrue(reached[0] && reached[1] && reached[2]
                                    && dog.weatherStage() == WeatherPlayGoal.NONE,
                            "Ambient snow did not dive, plow, pop, and finish upright");
                    h.assertTrue(dog.getHealth() == health && !dog.isInWater(),
                            "Ambient snow play harmed or immersed the dog");
                    for (int x = 3; x <= 6; ++x) for (int z = 4; z <= 6; ++z)
                        h.assertTrue(h.getBlockState(new BlockPos(x, 1, z)).equals(snow),
                                "Ambient snow play modified a layer");
                }).thenSucceed();
    }

    private static WeatherPlayGoal weatherGoal(FrenchieEntity dog) {
        try {
            Field field = FrenchieEntity.class.getDeclaredField("weatherPlay");
            field.setAccessible(true);
            return (WeatherPlayGoal) field.get(dog);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not inspect weather goal for ambient fixture", e);
        }
    }

    private static void resetScan(WeatherPlayGoal goal) {
        try {
            Field field = WeatherPlayGoal.class.getDeclaredField("nextSnowScan");
            field.setAccessible(true);
            field.setLong(goal, 0);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not retry ambient snow scan", e);
        }
    }
}
