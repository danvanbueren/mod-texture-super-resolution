package me.danvb10.mtsr.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Loads and atomically persists the MTSR JSON configuration file. */
public final class MtsrConfigStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;

    /** Creates a store for the supplied configuration file path. */
    public MtsrConfigStore(Path configFile) {
        this.configFile = configFile;
    }

    /** Loads the configuration, creating a default file when necessary. */
    public MtsrConfig load() {
        if (!Files.exists(configFile)) {
            MtsrConfig defaults = MtsrConfig.defaults();
            try {
                save(defaults);
            } catch (IOException e) {
                LOGGER.warn("Failed to create default configuration {}", configFile, e);
            }
            return defaults;
        }
        try {
            MtsrConfig config = GSON.fromJson(Files.readString(configFile), MtsrConfig.class);
            if (config == null) {
                throw new IOException("Configuration JSON was empty");
            }
            return config.validate();
        } catch (Exception e) {
            LOGGER.warn("Failed to load configuration {}, using defaults", configFile, e);
            return MtsrConfig.defaults();
        }
    }

    /** Saves a validated configuration using a temporary file and atomic move. */
    public void save(MtsrConfig config) throws IOException {
        config.validate();
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(config));
        try {
            Files.move(temporary, configFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.move(temporary, configFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
