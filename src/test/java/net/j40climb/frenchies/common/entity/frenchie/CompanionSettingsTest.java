package net.j40climb.frenchies.common.entity.frenchie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CompanionSettingsTest {
    @Test
    void eachDocumentedOptionCanBeDisabledIndependently(@TempDir Path config) throws IOException {
        var switches = Map.<String, BooleanSupplier>of(
                "weatherReactions", CompanionSettings::weatherReactions,
                "visibleSnow", CompanionSettings::visibleSnow,
                "thunderFear", CompanionSettings::thunderFear,
                "automaticHerding", CompanionSettings::automaticHerding,
                "fallReactions", CompanionSettings::fallReactions,
                "ambientCompanionship", CompanionSettings::ambientCompanionship,
                "sideSleeping", CompanionSettings::sideSleeping,
                "lavaReactions", CompanionSettings::lavaReactions,
                "stickRetrieval", CompanionSettings::stickRetrieval);
        CompanionSettings.load(config);
        var file = config.resolve("frenchies.properties");
        var generated = Files.readString(file);
        try {
            for (var key : switches.keySet()) {
                assertTrue(generated.contains(key + "=true"), key);
                Files.writeString(file, key + "=false");
                CompanionSettings.load(config);
                switches.forEach((other, reading) -> assertEquals(!key.equals(other), reading.getAsBoolean(), other));
            }
        } finally {
            Files.writeString(file, generated);
            CompanionSettings.load(config);
        }
    }
}
