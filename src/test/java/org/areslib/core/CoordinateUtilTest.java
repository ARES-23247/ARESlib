package org.areslib.core;

import org.areslib.math.geometry.Pose2d;
import org.areslib.math.geometry.Rotation2d;
import org.areslib.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CoordinateUtil} — the single source of truth for all
 * WPILib ↔ Pedro ↔ Vision coordinate conversions.
 * <p>
 * These tests verify:
 * <ul>
 *   <li>Unit conversions (meters↔inches, mm↔inches, mm↔meters)</li>
 *   <li>Origin shifts (center↔bottom-left)</li>
 *   <li>Axis swaps (WPILib X-forward/Y-left → Pedro X-right/Y-forward)</li>
 *   <li>Round-trip identity: wpi→pedro→wpi == original</li>
 *   <li>Known landmark positions (field center, corners)</li>
 * </ul>
 */
class CoordinateUtilTest {

    private static final double EPSILON = 1e-6;

    // ==================== Unit Conversions ====================

    @Test
    void metersToInches_oneMeter() {
        // 1 meter = ~39.3701 inches
        assertEquals(1.0 / 0.0254, CoordinateUtil.metersToInches(1.0), EPSILON);
    }

    @Test
    void inchesToMeters_oneInch() {
        // 1 inch = 0.0254 meters
        assertEquals(0.0254, CoordinateUtil.inchesToMeters(1.0), EPSILON);
    }

    @Test
    void metersToInches_zero() {
        assertEquals(0.0, CoordinateUtil.metersToInches(0.0), EPSILON);
    }

    @Test
    void inchesToMeters_zero() {
        assertEquals(0.0, CoordinateUtil.inchesToMeters(0.0), EPSILON);
    }

    @Test
    void metersToInches_negative() {
        assertEquals(-39.37007874, CoordinateUtil.metersToInches(-1.0), 1e-4);
    }

    @Test
    void metersInchesRoundTrip() {
        // Converting meters→inches→meters should return the original value
        double original = 1.8288; // 6 feet in meters
        double roundTrip = CoordinateUtil.inchesToMeters(CoordinateUtil.metersToInches(original));
        assertEquals(original, roundTrip, EPSILON, "meters→inches→meters round trip failed");
    }

    @Test
    void mmToInches_25_4mm_is_one_inch() {
        assertEquals(1.0, CoordinateUtil.mmToInches(25.4), EPSILON);
    }

    @Test
    void mmToMeters_1000mm_is_one_meter() {
        assertEquals(1.0, CoordinateUtil.mmToMeters(1000.0), EPSILON);
    }

    @Test
    void mmToInches_zero() {
        assertEquals(0.0, CoordinateUtil.mmToInches(0.0), EPSILON);
    }

    @Test
    void mmToMeters_zero() {
        assertEquals(0.0, CoordinateUtil.mmToMeters(0.0), EPSILON);
    }

    // ==================== Origin Shifts ====================

    @Test
    void centerMetersToBottomLeftInches_fieldCenter() {
        // Field center in WPILib (0, 0) → Pedro (72, 72) for each axis
        assertEquals(72.0, CoordinateUtil.centerMetersToBottomLeftInches(0.0), EPSILON,
                "Field center (0m) should map to 72 inches in Pedro");
    }

    @Test
    void bottomLeftInchesToCenterMeters_fieldCenter() {
        // Pedro (72, 72) → WPILib (0, 0) for each axis
        assertEquals(0.0, CoordinateUtil.bottomLeftInchesToCenterMeters(72.0), EPSILON,
                "Pedro center (72in) should map to 0m in WPILib");
    }

    @Test
    void centerMetersToBottomLeftInches_bottomLeftCorner() {
        // WPILib (-1.8288, -1.8288) = the bottom-left corner in meters
        // Should map to (0, 0) in Pedro inches
        double cornerMeters = -72.0 * 0.0254; // -1.8288m
        assertEquals(0.0, CoordinateUtil.centerMetersToBottomLeftInches(cornerMeters), EPSILON,
                "Bottom-left corner should map to 0 inches");
    }

    @Test
    void centerMetersToBottomLeftInches_topRightCorner() {
        // WPILib (+1.8288, +1.8288) = the top-right corner
        // Should map to (144, 144) in Pedro inches
        double cornerMeters = 72.0 * 0.0254; // +1.8288m
        assertEquals(144.0, CoordinateUtil.centerMetersToBottomLeftInches(cornerMeters), EPSILON,
                "Top-right corner should map to 144 inches");
    }

    @Test
    void originShiftRoundTrip() {
        // center→bottomLeft→center should return original
        double original = 0.75; // meters
        double roundTrip = CoordinateUtil.bottomLeftInchesToCenterMeters(
                CoordinateUtil.centerMetersToBottomLeftInches(original));
        assertEquals(original, roundTrip, EPSILON, "Origin shift round trip failed");
    }

    @Test
    void centerToBottomLeft_knownValues() {
        // center meters → expected bottom-left inches
        double[][] cases = {
            { 0.0,  72.0   },    // center
            { 1.0, 111.370 },    // ~1m right of center
            {-1.0,  32.630 },    // ~1m left of center
        };
        for (double[] c : cases) {
            assertEquals(c[1], CoordinateUtil.centerMetersToBottomLeftInches(c[0]), 0.01,
                    "centerMeters=" + c[0]);
        }
    }

    // ==================== Full Pose Conversion (WPILib ↔ Pedro) ====================

    @Test
    void wpiToPedro_fieldCenter() {
        // WPILib origin (0, 0, 0) → Pedro (72, 72, 0)
        Pose2d wpiOrigin = new Pose2d(0, 0, new Rotation2d(0));
        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(wpiOrigin);

        assertEquals(72.0, pedro.getX(), EPSILON, "Pedro X at field center");
        assertEquals(72.0, pedro.getY(), EPSILON, "Pedro Y at field center");
        assertEquals(0.0, pedro.getHeading(), EPSILON, "Heading should be preserved");
    }

    @Test
    void pedroToWpi_fieldCenter() {
        // Pedro (72, 72, 0) → WPILib (0, 0, 0)
        com.pedropathing.geometry.Pose pedroCenter = new com.pedropathing.geometry.Pose(72.0, 72.0, 0);
        Pose2d wpi = CoordinateUtil.pedroToWpi(pedroCenter);

        assertEquals(0.0, wpi.getX(), EPSILON, "WPILib X at field center");
        assertEquals(0.0, wpi.getY(), EPSILON, "WPILib Y at field center");
        assertEquals(0.0, wpi.getRotation().getRadians(), EPSILON, "Heading should be preserved");
    }

    @Test
    void wpiToPedro_axisSwap_xForwardBecomesPedroYForward() {
        // WPILib: robot 1m forward (+X), no lateral, heading 0
        // In Pedro: X-right Y-forward, so +1m in WPILib-X should become +1m in Pedro-Y
        Pose2d wpi = new Pose2d(1.0, 0, new Rotation2d(0));
        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(wpi);

        // pedroY = centerMetersToBottomLeftInches(wpiX=1.0) = metersToInches(1.0) + 72
        double expectedPedroY = CoordinateUtil.metersToInches(1.0) + 72.0;
        assertEquals(expectedPedroY, pedro.getY(), EPSILON,
                "WPILib +X should map to larger Pedro Y (forward)");

        // pedroX stays at 72 because wpiY = 0
        assertEquals(72.0, pedro.getX(), EPSILON,
                "Pedro X should be field center when WPILib Y = 0");
    }

    @Test
    void wpiToPedro_axisSwap_yLeftBecomesPedroXNegative() {
        // WPILib: +1m left (+Y), no forward, heading 0
        // In Pedro: pedroX = centerMetersToBottomLeftInches(-wpiY) = centerMeters(-1.0)
        Pose2d wpi = new Pose2d(0, 1.0, new Rotation2d(0));
        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(wpi);

        // pedroX = centerMetersToBottomLeftInches(-1.0) = metersToInches(-1.0) + 72
        double expectedPedroX = CoordinateUtil.metersToInches(-1.0) + 72.0;
        assertEquals(expectedPedroX, pedro.getX(), EPSILON,
                "WPILib +Y (left) should decrease Pedro X");
        assertTrue(pedro.getX() < 72.0,
                "WPILib +Y (left) should result in Pedro X < 72 (left of center)");
    }

    @Test
    void wpiToPedro_headingPreserved() {
        double heading = Math.PI / 4.0; // 45 degrees
        Pose2d wpi = new Pose2d(0, 0, new Rotation2d(heading));
        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(wpi);

        assertEquals(heading, pedro.getHeading(), EPSILON,
                "Heading should be passed through unchanged");
    }

    @Test
    void wpiPedroRoundTrip_identity() {
        // The critical invariant: wpi→pedro→wpi must return the original pose
        Pose2d original = new Pose2d(1.5, -0.8, new Rotation2d(Math.PI / 3.0));

        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(original);
        Pose2d roundTrip = CoordinateUtil.pedroToWpi(pedro);

        assertEquals(original.getX(), roundTrip.getX(), EPSILON,
                "Round trip X failed");
        assertEquals(original.getY(), roundTrip.getY(), EPSILON,
                "Round trip Y failed");
        assertEquals(original.getRotation().getRadians(), roundTrip.getRotation().getRadians(), EPSILON,
                "Round trip heading failed");
    }

    @Test
    void pedroWpiRoundTrip_identity() {
        // Reverse direction: pedro→wpi→pedro
        com.pedropathing.geometry.Pose original = new com.pedropathing.geometry.Pose(24.0, 110.0, Math.PI / 6.0);

        Pose2d wpi = CoordinateUtil.pedroToWpi(original);
        com.pedropathing.geometry.Pose roundTrip = CoordinateUtil.wpiToPedro(wpi);

        assertEquals(original.getX(), roundTrip.getX(), EPSILON,
                "Reverse round trip X failed");
        assertEquals(original.getY(), roundTrip.getY(), EPSILON,
                "Reverse round trip Y failed");
        assertEquals(original.getHeading(), roundTrip.getHeading(), EPSILON,
                "Reverse round trip heading failed");
    }

    @Test
    void wpiPedroRoundTrip_negativeCoordinates() {
        // Test with negative WPILib coordinates (robot behind/right of center)
        Pose2d original = new Pose2d(-0.5, -1.2, new Rotation2d(-Math.PI / 2.0));

        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(original);
        Pose2d roundTrip = CoordinateUtil.pedroToWpi(pedro);

        assertEquals(original.getX(), roundTrip.getX(), EPSILON, "Negative X round trip");
        assertEquals(original.getY(), roundTrip.getY(), EPSILON, "Negative Y round trip");
        assertEquals(original.getRotation().getRadians(), roundTrip.getRotation().getRadians(), EPSILON,
                "Negative heading round trip");
    }

    // ==================== Translation-only Conversion ====================

    @Test
    void wpiToPedroPose_translationOnly() {
        Translation2d t = new Translation2d(1.0, -0.5);
        com.pedropathing.geometry.Pose result = CoordinateUtil.wpiToPedroPose(t);

        // pedroX = centerMetersToBottomLeftInches(-(-0.5)) = centerMetersToBottomLeftInches(0.5)
        double expectedPedroX = CoordinateUtil.centerMetersToBottomLeftInches(0.5);
        // pedroY = centerMetersToBottomLeftInches(1.0)
        double expectedPedroY = CoordinateUtil.centerMetersToBottomLeftInches(1.0);

        assertEquals(expectedPedroX, result.getX(), EPSILON, "Translation-only Pedro X");
        assertEquals(expectedPedroY, result.getY(), EPSILON, "Translation-only Pedro Y");
        assertEquals(0.0, result.getHeading(), EPSILON, "Translation-only heading should be 0");
    }

    // ==================== Vision → Pedro Conversion ====================

    @Test
    void visionCenterToPedro_fieldCenter() {
        // Vision center (0, 0) → Pedro (72, 72)
        com.pedropathing.geometry.Pose result = CoordinateUtil.visionCenterToPedro(0, 0, 0);
        assertEquals(72.0, result.getX(), EPSILON, "Vision center → Pedro X should be 72");
        assertEquals(72.0, result.getY(), EPSILON, "Vision center → Pedro Y should be 72");
        assertEquals(0.0, result.getHeading(), EPSILON, "Heading preserved");
    }

    @Test
    void visionCenterToPedro_noAxisSwap() {
        // Vision conversion does NOT swap axes (unlike wpiToPedro)
        // +X in vision → +X in Pedro (both are "right" when aligned)
        double xMeters = 0.5;
        double yMeters = 1.0;
        com.pedropathing.geometry.Pose result = CoordinateUtil.visionCenterToPedro(xMeters, yMeters, Math.PI / 4.0);

        double expectedX = CoordinateUtil.centerMetersToBottomLeftInches(xMeters);
        double expectedY = CoordinateUtil.centerMetersToBottomLeftInches(yMeters);

        assertEquals(expectedX, result.getX(), EPSILON, "Vision X should map directly (no axis swap)");
        assertEquals(expectedY, result.getY(), EPSILON, "Vision Y should map directly (no axis swap)");
        assertEquals(Math.PI / 4.0, result.getHeading(), EPSILON, "Heading should pass through");
    }

    // ==================== Edge Cases & Boundary Conditions ====================

    @Test
    void wpiToPedro_fieldCorner_bottomLeft() {
        // WPILib bottom-left corner: (-1.8288, -1.8288)
        // In Pedro space: should be near (144, 0) due to axis swap
        // pedroX = centerMetersToBottomLeftInches(-(-1.8288)) = centerMetersToBottomLeftInches(1.8288) = 72 + 72 = 144
        // pedroY = centerMetersToBottomLeftInches(-1.8288) = 72 - 72 = 0
        double cornerMeters = -72.0 * 0.0254; // -1.8288
        Pose2d wpi = new Pose2d(cornerMeters, cornerMeters, new Rotation2d(0));
        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(wpi);

        assertEquals(144.0, pedro.getX(), EPSILON,
                "WPILib -Y maps to Pedro +X (right side)");
        assertEquals(0.0, pedro.getY(), EPSILON,
                "WPILib -X maps to Pedro bottom");
    }

    @Test
    void wpiToPedro_fieldCorner_topRight() {
        // WPILib top-right corner: (+1.8288, +1.8288)
        // pedroX = centerMetersToBottomLeftInches(-1.8288) = 72 - 72 = 0
        // pedroY = centerMetersToBottomLeftInches(+1.8288) = 72 + 72 = 144
        double cornerMeters = 72.0 * 0.0254; // +1.8288
        Pose2d wpi = new Pose2d(cornerMeters, cornerMeters, new Rotation2d(0));
        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(wpi);

        assertEquals(0.0, pedro.getX(), EPSILON,
                "WPILib +Y maps to Pedro left side (X=0)");
        assertEquals(144.0, pedro.getY(), EPSILON,
                "WPILib +X maps to Pedro top (Y=144)");
    }

    @Test
    void fullFieldRoundTrip_allCorners() {
        // Test that all 4 corners survive a round trip
        double[][] corners = {
            {-1.8288, -1.8288},
            {-1.8288,  1.8288},
            { 1.8288, -1.8288},
            { 1.8288,  1.8288},
        };

        for (double[] corner : corners) {
            Pose2d original = new Pose2d(corner[0], corner[1], new Rotation2d(0));
            com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(original);
            Pose2d roundTrip = CoordinateUtil.pedroToWpi(pedro);

            assertEquals(original.getX(), roundTrip.getX(), EPSILON,
                    String.format("Corner (%.3f, %.3f) X failed", corner[0], corner[1]));
            assertEquals(original.getY(), roundTrip.getY(), EPSILON,
                    String.format("Corner (%.3f, %.3f) Y failed", corner[0], corner[1]));
        }
    }

    // ==================== Regression Tests ====================

    @Test
    void regression_inchesToMeters_not_divided_byFactor() {
        // Guard against the common mistake of dividing by 0.0254 instead of multiplying
        // 72 inches should be ~1.8288 meters, NOT 72/0.0254 = 2834.6
        double result = CoordinateUtil.inchesToMeters(72.0);
        assertTrue(result < 2.0 && result > 1.5,
                "72 inches should be ~1.83m, got " + result + " — likely dividing instead of multiplying");
        assertEquals(1.8288, result, EPSILON);
    }

    @Test
    void regression_metersToInches_not_multiplied_byInverse() {
        // Guard against the common mistake of multiplying by 0.0254 instead of dividing
        // 1 meter should be ~39.37 inches, NOT 0.0254
        double result = CoordinateUtil.metersToInches(1.0);
        assertTrue(result > 30.0 && result < 50.0,
                "1 meter should be ~39.37 inches, got " + result);
    }

    @Test
    void regression_originShift_uses_72_not_36() {
        // Guard against using 36 (half of 72) instead of 72 (half of 144)
        double atCenter = CoordinateUtil.centerMetersToBottomLeftInches(0.0);
        assertEquals(72.0, atCenter, EPSILON,
                "Center origin shift must use 72.0, not 36.0");
    }

    @Test
    void regression_axisSwap_notIdentity() {
        // Guard against forgetting the axis swap entirely
        // WPILib (1, 0) should NOT produce Pedro (X=something, Y=72)
        // It should produce Pedro (X=72, Y=something>72) because X-forward→Y-forward
        Pose2d wpi = new Pose2d(1.0, 0, new Rotation2d(0));
        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(wpi);

        // If someone removed the axis swap, pedroX would change instead of pedroY
        assertEquals(72.0, pedro.getX(), EPSILON,
                "With zero WPILib Y, Pedro X should be 72 (no lateral movement)");
        assertTrue(pedro.getY() > 72.0,
                "With positive WPILib X (forward), Pedro Y should be > 72");
    }

    @Test
    void regression_axisSwap_signCorrectness() {
        // WPILib +Y = left. In Pedro, left = -X direction (X decreasing).
        // So wpiY > 0 should produce pedroX < 72
        Pose2d wpi = new Pose2d(0, 0.5, new Rotation2d(0));
        com.pedropathing.geometry.Pose pedro = CoordinateUtil.wpiToPedro(wpi);

        assertTrue(pedro.getX() < 72.0,
                "WPILib +Y (left) should result in Pedro X < 72 (moved left)");
    }
}
