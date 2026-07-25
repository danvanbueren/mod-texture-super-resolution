package me.danvb10.mtsr;

import me.danvb10.mtsr.config.MtsrConfig;
import me.danvb10.mtsr.config.MtsrConfigStore;
import me.danvb10.mtsr.upscale.TextureReloadHook;
import me.danvb10.mtsr.upscale.UpscaleManager;
import me.danvb10.mtsr.upscale.model.ModelDownloader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.ClientModInitializer;

import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public class ClientEntrypoint implements ClientModInitializer {
	public static final String MOD_ID = "mtsr";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static UpscaleManager upscaleManager;
	private static MtsrConfig config = MtsrConfig.defaults();

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static UpscaleManager upscaleManager() {
		return upscaleManager;
	}

	/** Returns the loaded persistent configuration. */
	public static MtsrConfig config() {
		return config;
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Client");
		Path gameDir = FabricLoader.getInstance().getGameDir();
		config = new MtsrConfigStore(gameDir.resolve("config/mtsr/config.json")).load();
		upscaleManager = UpscaleManager.create(gameDir, config);
		new ModelDownloader(gameDir.resolve("config/mtsr/models")).downloadIfMissing();
		TextureReloadHook.register(upscaleManager);
	}
}
