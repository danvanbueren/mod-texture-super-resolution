package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.LabelComponent;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * A label whose text is re-evaluated from a supplier every frame, used to
 * surface live values (e.g. pipeline counters) inside a {@link RichWindow}.
 */
public class LiveLabelComponent extends LabelComponent {

    private final Supplier<Component> textSupplier;

    public LiveLabelComponent(Supplier<Component> textSupplier) {
        super(textSupplier.get());
        this.textSupplier = textSupplier;
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        this.text(textSupplier.get());
    }
}
