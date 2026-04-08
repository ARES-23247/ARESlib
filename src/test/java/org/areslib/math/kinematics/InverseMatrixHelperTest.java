package org.areslib.math.kinematics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InverseMatrixHelper}. */
class InverseMatrixHelperTest {

  private static final double EPSILON = 1e-6;

  @Test
  @DisplayName("pseudoInverse of 3x3 identity-like produces transpose")
  void simpleMatrix() {
    // A simple 3x3 orthogonal-row matrix
    double[][] matrixA = {
      {1, 0, 0},
      {0, 1, 0},
      {0, 0, 1}
    };
    double[][] pinv = InverseMatrixHelper.pseudoInverse(matrixA);
    // For an orthogonal 3x3 matrix, pseudo-inverse = inverse = transpose
    assertEquals(3, pinv.length);
    assertEquals(3, pinv[0].length);
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        double expected = (i == j) ? 1.0 : 0.0;
        assertEquals(expected, pinv[i][j], EPSILON);
      }
    }
  }

  @Test
  @DisplayName("pseudoInverse * A ≈ identity for square-like system")
  void roundTripIdentity() {
    // 4x3 matrix (swerve-like)
    double[][] matrixA = {
      {1, 0, -0.3},
      {0, 1, 0.3},
      {1, 0, 0.3},
      {0, 1, -0.3}
    };
    double[][] pinv = InverseMatrixHelper.pseudoInverse(matrixA);
    // pinv is 3x4, matrixA is 4x3, pinv * matrixA should ≈ 3x3 identity
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        double sum = 0;
        for (int k = 0; k < 4; k++) {
          sum += pinv[i][k] * matrixA[k][j];
        }
        double expected = (i == j) ? 1.0 : 0.0;
        assertEquals(
            expected, sum, EPSILON, "pinv*matrixA[" + i + "][" + j + "] should be " + expected);
      }
    }
  }

  @Test
  @DisplayName("pseudoInverse throws on singular matrix")
  void singularThrows() {
    // Two identical rows → singular ata
    double[][] matrixA = {
      {1, 2, 3},
      {1, 2, 3}
    };
    assertThrows(IllegalArgumentException.class, () -> InverseMatrixHelper.pseudoInverse(matrixA));
  }

  @Test
  @DisplayName("pseudoInverse throws on too few rows")
  void tooFewRows() {
    double[][] matrixA = {{1, 0, 0}};
    assertThrows(IllegalArgumentException.class, () -> InverseMatrixHelper.pseudoInverse(matrixA));
  }

  @Test
  @DisplayName("pseudoInverse output dimensions are correct")
  void outputDimensions() {
    double[][] matrixA = {
      {1, 0, -0.5},
      {0, 1, 0.5},
      {1, 0, 0.5},
      {0, 1, -0.5},
      {1, 1, 0.0},
      {1, -1, 0.0}
    };
    double[][] pinv = InverseMatrixHelper.pseudoInverse(matrixA);
    assertEquals(3, pinv.length); // rows = cols of matrixA
    assertEquals(6, pinv[0].length); // cols = rows of matrixA
  }
}
