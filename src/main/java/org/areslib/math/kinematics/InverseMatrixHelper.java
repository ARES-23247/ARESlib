package org.areslib.math.kinematics;

/**
 * Utility for performing standard matrix operations specifically tailored for drivetrain
 * kinematics, like Cramer's Rule inversions.
 */
public class InverseMatrixHelper {

  /**
   * Computes the pseudo-inverse of a generic inverse kinematics matrix `A`. Applies `(A^T A)^-1 *
   * A^T` to reliably project overdetermined wheel vectors into 3-DOF robot pose vectors (vx, vy,
   * omega).
   *
   * @param inverseKinematics The parsed Nx3 matrix mapping chassis vectors to wheel vectors.
   * @return The parsed 3xN forward kinematics pseudo-inverse matrix.
   */
  public static double[][] pseudoInverse(double[][] inverseKinematics) {
    int rows = inverseKinematics.length;
    if (rows < 2) {
      throw new IllegalArgumentException("Matrix must have at least 2 rows for pseudo-inversion");
    }

    // Compute A^T A
    double[][] ata = new double[3][3];
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        double sum = 0;
        for (int i = 0; i < rows; i++) {
          sum += inverseKinematics[i][row] * inverseKinematics[i][col];
        }
        ata[row][col] = sum;
      }
    }

    // Invert ata (3x3 matrix) using Cramer's Rule
    double det =
        ata[0][0] * (ata[1][1] * ata[2][2] - ata[1][2] * ata[2][1])
            - ata[0][1] * (ata[1][0] * ata[2][2] - ata[1][2] * ata[2][0])
            + ata[0][2] * (ata[1][0] * ata[2][1] - ata[1][1] * ata[2][0]);

    if (Math.abs(det) < 1E-9) {
      throw new IllegalArgumentException("Kinematics matrix is singular. Check module locations.");
    }

    double[][] ataInv = new double[3][3];
    ataInv[0][0] = (ata[1][1] * ata[2][2] - ata[1][2] * ata[2][1]) / det;
    ataInv[0][1] = (ata[0][2] * ata[2][1] - ata[0][1] * ata[2][2]) / det;
    ataInv[0][2] = (ata[0][1] * ata[1][2] - ata[0][2] * ata[1][1]) / det;
    ataInv[1][0] = (ata[1][2] * ata[2][0] - ata[1][0] * ata[2][2]) / det;
    ataInv[1][1] = (ata[0][0] * ata[2][2] - ata[0][2] * ata[2][0]) / det;
    ataInv[1][2] = (ata[0][2] * ata[1][0] - ata[0][0] * ata[1][2]) / det;
    ataInv[2][0] = (ata[1][0] * ata[2][1] - ata[1][1] * ata[2][0]) / det;
    ataInv[2][1] = (ata[0][1] * ata[2][0] - ata[0][0] * ata[2][1]) / det;
    ataInv[2][2] = (ata[0][0] * ata[1][1] - ata[0][1] * ata[1][0]) / det;

    // Multiply (A^T A)^-1 * A^T
    double[][] forwardKinematics = new double[3][rows];
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < rows; col++) {
        double sum = 0;
        for (int i = 0; i < 3; i++) {
          sum += ataInv[row][i] * inverseKinematics[col][i]; // Transpose baked in
        }
        forwardKinematics[row][col] = sum;
      }
    }
    return forwardKinematics;
  }
}
