package net.j40climb.frenchies.common.entity.frenchie;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A server samples precipitation twice a second. Only the discrete coat layer is synchronized. */
final class WeatherExposure {
    static final int SAMPLE_TICKS = 10;
    private static final int FULL = 120;
    private int snow;
    private int rain;
    private long nextSnowShake;
    private long nextRainShake;

    void sample(Precipitation precipitation, boolean warm, long now, RandomSource random) {
        if (precipitation == Precipitation.SNOW && snow == 0 && now >= nextSnowShake)
            nextSnowShake = now + snowCooldown(random);
        if (precipitation == Precipitation.RAIN && rain == 0 && now >= nextRainShake)
            nextRainShake = now + rainCooldown(random);
        snow = Math.clamp(snow + (warm ? -10 : precipitation == Precipitation.SNOW ? 3 : -2), 0, FULL);
        rain = Math.clamp(rain + (precipitation == Precipitation.RAIN ? 3 : -2), 0, FULL);
    }

    int snowLayer() {
        return Math.round(5F * snow / FULL);
    }

    boolean snowy() {
        return snow > 0;
    }

    Precipitation readyToShake(long now) {
        if (snow >= FULL / 4 && now >= nextSnowShake) return Precipitation.SNOW;
        if (rain >= FULL / 3 && now >= nextRainShake) return Precipitation.RAIN;
        return Precipitation.NONE;
    }

    void shake(long now, RandomSource random) {
        snow = rain = 0;
        nextSnowShake = now + snowCooldown(random);
        nextRainShake = now + rainCooldown(random);
    }

    private static int snowCooldown(RandomSource random) {
        return 600 + random.nextInt(1201);
    }

    private static int rainCooldown(RandomSource random) {
        return 900 + random.nextInt(1501);
    }

    void save(ValueOutput output) {
        output.putInt("SnowExposure", snow);
        output.putInt("RainExposure", rain);
        output.putLong("NextSnowShake", nextSnowShake);
        output.putLong("NextRainShake", nextRainShake);
    }

    void load(ValueInput input) {
        snow = Math.clamp(input.getIntOr("SnowExposure", 0), 0, FULL);
        rain = Math.clamp(input.getIntOr("RainExposure", 0), 0, FULL);
        nextSnowShake = input.getLongOr("NextSnowShake", 0L);
        nextRainShake = input.getLongOr("NextRainShake", 0L);
    }
}
