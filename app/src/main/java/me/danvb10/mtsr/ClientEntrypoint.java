package me.danvb10.mtsr;

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

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static UpscaleManager upscaleManager() {
		return upscaleManager;
	}

	@Override
	public void onInitializeClient() {
		LOGGER.info("Initializing Client");
		Path gameDir = FabricLoader.getInstance().getGameDir();
		upscaleManager = UpscaleManager.create(gameDir);
		new ModelDownloader(gameDir.resolve("config/mtsr/models")).downloadIfMissing();
		TextureReloadHook.register(upscaleManager);
	}
}
