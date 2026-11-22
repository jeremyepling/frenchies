package net.j40climb.frenchies.common.entity.frenchie;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/** Loaded at startup. Companion behavior choices are made on the logical server. */
public final class CompanionSettings {
    private static Properties options = new Properties();

    private CompanionSettings() {}

    public static void load(Path directory) {
        var file = directory.resolve("florafauna_frenchies.properties");
        try {
            if (Files.notExists(file)) {
                Files.createDirectories(directory);
                Files.writeString(file, """
                        # Frenchie companion behaviors. Restart the server or game after editing.
                        weatherReactions=true
                        visibleSnow=true
                        thunderFear=true
                        automaticHerding=true
                        fallReactions=true
                        ambientCompanionship=true
                        sideSleeping=true
                        lavaReactions=true
                        stickRetrieval=true
                        """, StandardOpenOption.CREATE_NEW);
            }
            var loaded = new Properties();
            try (var reader = Files.newBufferedReader(file)) {
                loaded.load(reader);
            }
            options = loaded;
        } catch (IOException | IllegalArgumentException ex) {
            options = new Properties();
            LogUtils.getLogger().warn("Could not load Frenchie settings from {}. Using defaults.", file, ex);
        }
    }

    private static boolean enabled(String key) {
        return !"false".equalsIgnoreCase(options.getProperty(key, "true").trim());
    }

    public static boolean weatherReactions() { return enabled("weatherReactions"); }
    public static boolean visibleSnow() { return enabled("visibleSnow"); }
    public static boolean thunderFear() { return enabled("thunderFear"); }
    public static boolean automaticHerding() { return enabled("automaticHerding"); }
    public static boolean fallReactions() { return enabled("fallReactions"); }
    public static boolean ambientCompanionship() { return enabled("ambientCompanionship"); }
    public static boolean sideSleeping() { return enabled("sideSleeping"); }
    public static boolean lavaReactions() { return enabled("lavaReactions"); }
    public static boolean stickRetrieval() { return enabled("stickRetrieval"); }
}
