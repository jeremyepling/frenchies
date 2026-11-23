package net.j40climb.frenchies.common.entity.frenchie;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/** Loaded once from the loader's configuration directory at startup. */
public final class CompanionSettings {
    private static final Properties options = new Properties();

    private CompanionSettings() {}

    public static void load(Path directory) {
        var file = directory.resolve("florafauna_frenchies.properties");
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(directory);
                Files.writeString(file, """
                        # Frenchie companion behaviors. Restart the server or game after editing.
                        automaticAdventureOutfits=true
                        miningHelmetDynamicLight=true
                        weatherReactions=true
                        thunderFear=true
                        snowAccumulation=true
                        """, StandardOpenOption.CREATE_NEW);
            }
            try (var reader = Files.newBufferedReader(file)) {
                options.load(reader);
            }
        } catch (IOException | IllegalArgumentException ex) {
            options.clear();
            LogUtils.getLogger().warn("Could not load Frenchie settings from {}. Using defaults.", file, ex);
        }
    }

    private static boolean enabled(String key) {
        return !"false".equalsIgnoreCase(options.getProperty(key, "true").trim());
    }

    public static boolean automaticAdventureOutfits() { return enabled("automaticAdventureOutfits"); }
    public static boolean miningHelmetDynamicLight() { return enabled("miningHelmetDynamicLight"); }
    public static boolean weatherReactions() { return enabled("weatherReactions"); }
    public static boolean snowAccumulation() {
        return enabled("snowAccumulation") && (options.containsKey("snowAccumulation") || enabled("visibleSnow"));
    }
    public static boolean thunderFear() { return enabled("thunderFear"); }
}
