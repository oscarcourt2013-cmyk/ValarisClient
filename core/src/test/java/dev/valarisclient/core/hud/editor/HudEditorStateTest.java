package dev.valarisclient.core.hud.editor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudEditorStateTest {

    @AfterEach
    void tearDown() {
        HudEditorState.setActive(false);
    }

    @Test
    void defersNormalHudWhileEditorActive() {
        HudEditorState.setActive(true);
        assertTrue(HudEditorState.isActive());
        assertFalse(HudEditorState.isRenderingVanillaHud());
    }

    @Test
    void allowsVanillaHudPassDuringEditorRedraw() {
        HudEditorState.setActive(true);
        HudEditorState.runVanillaHudRender(() -> assertTrue(HudEditorState.isRenderingVanillaHud()));
        assertFalse(HudEditorState.isRenderingVanillaHud());
    }

    @Test
    void clearsVanillaPassWhenEditorCloses() {
        HudEditorState.setActive(true);
        HudEditorState.runVanillaHudRender(() -> {
        });
        HudEditorState.setActive(false);
        assertFalse(HudEditorState.isRenderingVanillaHud());
    }
}
