package ooo.soloop.sacred_trees;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for Sacred Trees mod.
 * Loaded from {@code config/sacred_trees.json}.
 */
public class SacredTreesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("SacredTreesConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static SacredTreesConfig instance = new SacredTreesConfig();

    public int blocksPerTick = 100000;
    public int chunksPerTick = 200;
    public int chunkRadius = 16;

    public static int blocksPerTick() { return instance.blocksPerTick; }
    public static int chunksPerTick() { return instance.chunksPerTick; }
    public static int chunkRadius() { return instance.chunkRadius; }

    /** Load config from {@code configDir/sacred_trees.json}. Creates default if missing. */
    public static void load(Path configDir) {
        Path configFile = configDir.resolve("sacred_trees.json");
        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                instance = GSON.fromJson(json, SacredTreesConfig.class);
                LOGGER.info("Loaded config from {}", configFile);
            } catch (Exception e) {
                LOGGER.error("Failed to load config from {}, using defaults", configFile, e);
                instance = new SacredTreesConfig();
            }
        } else {
            // Write default config
            try {
                Files.createDirectories(configDir);
                String json = GSON.toJson(new SacredTreesConfig());
                Files.writeString(configFile, json);
                LOGGER.info("Created default config at {}", configFile);
            } catch (IOException e) {
                LOGGER.error("Failed to create default config at {}", configFile, e);
            }
        }
    }
}
