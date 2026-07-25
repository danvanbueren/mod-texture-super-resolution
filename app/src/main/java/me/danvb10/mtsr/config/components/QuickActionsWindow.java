package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.core.Insets;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.upscale.UpscaleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import static me.danvb10.mtsr.config.components.RichWindowTypes.QUICK_ACTIONS_WINDOW;

public class QuickActionsWindow extends AbstractRichWindow<QuickActionsWindow> {

    public QuickActionsWindow(ConfigScreen parent) {
        super(parent, QUICK_ACTIONS_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Quick Actions";
    }

    @Override
    protected void populateContent(RichWindow window) {
        UpscaleManager manager = ClientEntrypoint.upscaleManager();
        if (manager == null) {
            window.child(UIComponents.label(
                    Component.literal("Upscale pipeline not initialized")
                            .withStyle(ChatFormatting.GRAY)));
            return;
        }

        LabelComponent resultLabel = UIComponents.label(Component.empty());

        window.child(
                UIComponents.button(Component.literal("Clear upscale cache"), button -> {
                    int removed = manager.cache().clear();
                    resultLabel.text(Component
                            .literal("Removed " + removed + " cached texture" + (removed == 1 ? "" : "s"))
                            .withStyle(ChatFormatting.GRAY));
                    requestScreenRefresh();
                }).margins(Insets.bottom(4)));

        window.child(
                UIComponents.button(Component.literal("Clear cache & reload textures"), button -> {
                    int removed = manager.cache().clear();
                    Minecraft client = Minecraft.getInstance();
                    if (client != null) {
                        client.reloadResourcePacks();
                    }
                    resultLabel.text(Component
                            .literal("Removed " + removed + " entries; reloading textures")
                            .withStyle(ChatFormatting.GRAY));
                    requestScreenRefresh();
                }).margins(Insets.bottom(4)));

        window.child(resultLabel);
    }
}
