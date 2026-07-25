package me.danvb10.mtsr.upscale;

import me.danvb10.mtsr.ClientEntrypoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * Displays a client toast after background texture upscaling completes.
 * Minecraft state is accessed only from the client thread.
 */
@Environment(EnvType.CLIENT)
public final class UpscaleCompletionToast {

    private static final Component TITLE = Component.literal("MTSR");
    private static final Component MESSAGE = Component.literal(
            "Textures upscaled in background! Press F3 + T to load high-resolution textures.");

    private UpscaleCompletionToast() {
    }

    /** Registers the completion toast listener on the supplied manager. */
    public static void register(UpscaleManager manager) {
        manager.addBatchCompletionListener(UpscaleCompletionToast::show);
    }

    private static void show() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player == null || client.level == null) {
                return;
            }
            SystemToast.addOrUpdate(client.getToastManager(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION, TITLE, MESSAGE);
        });
    }
}
