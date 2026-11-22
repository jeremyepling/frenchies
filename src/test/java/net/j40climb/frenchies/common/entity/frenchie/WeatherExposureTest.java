package net.j40climb.frenchies.common.entity.frenchie;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome.Precipitation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherExposureTest {
    private final RandomSource random = RandomSource.create(76L);
    private final WeatherExposure exposure = new WeatherExposure();

    private void sample(Precipitation precipitation, boolean warm, int fromTick, int untilTick) {
        for (int tick = fromTick; tick < untilTick; tick += WeatherExposure.SAMPLE_TICKS)
            exposure.sample(precipitation, warm, tick, random);
    }

    @Test
    void coatAccumulatesAndMeltsGradually() {
        sample(Precipitation.NONE, false, 0, 400);
        assertEquals(0, exposure.snowLayer());
        sample(Precipitation.SNOW, false, 400, 500);
        assertEquals(1, exposure.snowLayer());
        sample(Precipitation.SNOW, false, 500, 800);
        assertEquals(5, exposure.snowLayer());
        sample(Precipitation.NONE, false, 800, 810);
        assertEquals(5, exposure.snowLayer());
        sample(Precipitation.NONE, false, 810, 1100);
        assertEquals(3, exposure.snowLayer());
        sample(Precipitation.NONE, false, 1100, 1400);
        assertFalse(exposure.snowy());
    }

    @Test
    void heatMeltsAFullCoatInSixSeconds() {
        sample(Precipitation.SNOW, false, 0, 400);
        sample(Precipitation.NONE, true, 400, 460);
        assertEquals(3, exposure.snowLayer());
        sample(Precipitation.NONE, true, 460, 520);
        assertFalse(exposure.snowy());
    }

    @Test
    void shakeClearsCoatAndSnowAccumulatesAgainDuringCooldown() {
        sample(Precipitation.SNOW, false, 0, 400);
        assertEquals(Precipitation.NONE, exposure.readyToShake(599));
        assertEquals(Precipitation.SNOW, exposure.readyToShake(1800));
        exposure.shake(1800, random);
        assertEquals(0, exposure.snowLayer());
        sample(Precipitation.SNOW, false, 1800, 2200);
        assertEquals(5, exposure.snowLayer());
        assertEquals(Precipitation.NONE, exposure.readyToShake(2399));
        assertEquals(Precipitation.SNOW, exposure.readyToShake(3600));
    }

    @Test
    void wetDogCanShakeInShelterUntilItDries() {
        sample(Precipitation.RAIN, false, 0, 2400);
        assertEquals(0, exposure.snowLayer());
        assertEquals(Precipitation.NONE, exposure.readyToShake(899));
        assertEquals(Precipitation.RAIN, exposure.readyToShake(2400));
        sample(Precipitation.NONE, false, 2400, 2500);
        assertEquals(Precipitation.RAIN, exposure.readyToShake(2500));
        sample(Precipitation.NONE, false, 2500, 3000);
        assertEquals(Precipitation.NONE, exposure.readyToShake(3000));
        sample(Precipitation.RAIN, false, 3000, 3400);
        assertEquals(Precipitation.NONE, exposure.readyToShake(3899));
        assertEquals(Precipitation.RAIN, exposure.readyToShake(5400));
    }
}
