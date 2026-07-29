package dev.stellarclient.core.state;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandShaderStateTest {

    @AfterEach
    void tearDown() {
        HandShaderState.reset();
    }

    @Test
    void mixesTintWithIntensity() {
        HandShaderState.setActive(true);
        HandShaderState.setArgb(0xFFFF0000);
        HandShaderState.setIntensity(1f);
        assertEquals(1f, HandShaderState.red(), 0.001f);
        assertEquals(0f, HandShaderState.green(), 0.001f);
        assertEquals(0f, HandShaderState.blue(), 0.001f);

        HandShaderState.setIntensity(0f);
        assertEquals(1f, HandShaderState.red(), 0.001f);
        assertEquals(1f, HandShaderState.green(), 0.001f);
        assertEquals(1f, HandShaderState.blue(), 0.001f);
    }

    @Test
    void resetClearsActive() {
        HandShaderState.setActive(true);
        HandShaderState.reset();
        assertFalse(HandShaderState.active());
        assertTrue(HandShaderState.intensity() > 0f);
    }
}
