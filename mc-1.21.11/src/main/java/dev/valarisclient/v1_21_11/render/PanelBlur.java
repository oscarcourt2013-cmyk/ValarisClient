package dev.valarisclient.v1_21_11.render;

import dev.valarisclient.core.gui.BlurBackdrop;
import net.minecraft.client.Minecraft;

/** Clears blur post-effects when premium GUI screens close. */
public final class PanelBlur {

    private PanelBlur() {
    }

    public static void end(Minecraft minecraft) {
        BlurBackdrop.setActive(false);
        if (minecraft != null && minecraft.gameRenderer != null) {
            minecraft.gameRenderer.clearPostEffect();
        }
    }
}
