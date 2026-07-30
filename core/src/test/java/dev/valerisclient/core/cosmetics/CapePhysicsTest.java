package dev.valerisclient.core.cosmetics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapePhysicsTest {

    @AfterEach
    void tearDown() {
        CapePhysics.clearAll();
    }

    @Test
    void fallingFlaresCapeMoreThanRising() {
        var falling = CapePhysics.computeBoost(
                new CapePhysics.Motion(0.1f, -0.4f, false, false, 10f), 1f);
        var rising = CapePhysics.computeBoost(
                new CapePhysics.Motion(0.1f, 0.4f, false, false, 10f), 1f);
        assertTrue(falling.flap() > rising.flap());
    }

    @Test
    void sprintBoostsLeanOverWalk() {
        var sprint = CapePhysics.computeBoost(
                new CapePhysics.Motion(0.28f, 0f, true, true, 5f), 1f);
        var walk = CapePhysics.computeBoost(
                new CapePhysics.Motion(0.28f, 0f, true, false, 5f), 1f);
        assertTrue(sprint.lean() > walk.lean());
    }

    @Test
    void zeroIntensityIsNoOp() {
        CapePhysics.Pose pose = CapePhysics.enhance(
                10f, 40f, 5f,
                new CapePhysics.Motion(0.5f, -0.3f, false, true, 20f),
                0f);
        assertEquals(10f, pose.flap(), 0.001f);
        assertEquals(40f, pose.lean(), 0.001f);
        assertEquals(5f, pose.lean2(), 0.001f);
    }

    @Test
    void smoothApproachesTarget() {
        UUID id = UUID.randomUUID();
        var motion = new CapePhysics.Motion(0.3f, -0.35f, false, true, 0f);
        CapePhysics.Pose direct = CapePhysics.enhance(5f, 20f, 0f, motion, 1f);
        CapePhysics.Pose smoothed = CapePhysics.smooth(id, 5f, 20f, 0f, motion, 1f);
        for (int i = 0; i < 24; i++) {
            smoothed = CapePhysics.smooth(id, 5f, 20f, 0f, motion, 1f);
        }
        assertEquals(direct.flap(), smoothed.flap(), 0.35f);
        assertEquals(direct.lean(), smoothed.lean(), 0.35f);
    }
}
