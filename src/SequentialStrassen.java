import java.text.DecimalFormat;
import java.util.Random;
import javax.swing.JOptionPane;

public class SequentialStrassen {

    public static void main(String[] args) {
        //GUI
        String input = JOptionPane.showInputDialog(null, "Vnesite začetno velikost matrike:");
        int matrixSize;

        try {
            matrixSize = Integer.parseInt(input);
            if (matrixSize <= 0) {
                JOptionPane.showMessageDialog(null, "Vnesite pozitivno celo število.", "Napaka", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Napaka: Vnesite veljavno številko.", "Napaka", JOptionPane.ERROR_MESSAGE);
            return;
        }

        while (true) {
            int[][] matrixA = generateRandomMatrix(matrixSize);
            int[][] matrixB = generateRandomMatrix(matrixSize);

            double totalElapsedTime = 0;
            boolean timeExceeded = false;

            for (int i = 0; i < 3; i++) {
                long startTime = System.nanoTime();
                strassenMultiply(matrixA, matrixB);
                long endTime = System.nanoTime();

                double elapsedTime = (endTime - startTime) / 1_000_000_000.0;
                if (elapsedTime > 600) {
                    JOptionPane.showMessageDialog(null, "Izvedba za velikost " + matrixSize + " presega 10 minut. Program se zaključi.", "Opozorilo", JOptionPane.WARNING_MESSAGE);
                    timeExceeded = true;
                    break;
                }
                totalElapsedTime += elapsedTime;
            }

            if (timeExceeded) break;

            DecimalFormat df = new DecimalFormat("0.0000");
            double avgTime = totalElapsedTime / 3;
            System.out.println("Velikost matrike: " + matrixSize + ", Povprečni čas: " + df.format(avgTime) + " sekund");

            matrixSize += 500;
        }
    }

    private static int[][] generateRandomMatrix(int size) {
        Random random = new Random();
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                matrix[i][j] = random.nextInt(100);
        return matrix;
    }

    private static void strassenMultiply(int[][] A, int[][] B) {
        int n = A.length;
        if (n == 1) {
            return;
        }

        int newSize = n / 2;
        int[][] A11 = new int[newSize][newSize];
        int[][] A12 = new int[newSize][newSize];
        int[][] A21 = new int[newSize][newSize];
        int[][] A22 = new int[newSize][newSize];
        int[][] B11 = new int[newSize][newSize];
        int[][] B12 = new int[newSize][newSize];
        int[][] B21 = new int[newSize][newSize];
        int[][] B22 = new int[newSize][newSize];

        splitMatrix(A, A11, 0, 0);
        splitMatrix(A, A12, 0, newSize);
        splitMatrix(A, A21, newSize, 0);
        splitMatrix(A, A22, newSize, newSize);
        splitMatrix(B, B11, 0, 0);
        splitMatrix(B, B12, 0, newSize);
        splitMatrix(B, B21, newSize, 0);
        splitMatrix(B, B22, newSize, newSize);

        int[][] M1 = multiply(add(A11, A22), add(B11, B22));
        int[][] M2 = multiply(add(A21, A22), B11);
        int[][] M3 = multiply(A11, subtract(B12, B22));
        int[][] M4 = multiply(A22, subtract(B21, B11));
        int[][] M5 = multiply(add(A11, A12), B22);
        int[][] M6 = multiply(subtract(A21, A11), add(B11, B12));
        int[][] M7 = multiply(subtract(A12, A22), add(B21, B22));

        int[][] C = new int[n][n];
        combineMatrix(C, add(subtract(add(M1, M4), M5), M7), 0, 0);
        combineMatrix(C, add(M3, M5), 0, newSize);
        combineMatrix(C, add(M2, M4), newSize, 0);
        combineMatrix(C, add(subtract(add(M1, M3), M2), M6), newSize, newSize);

    }

    private static void splitMatrix(int[][] parent, int[][] child, int row, int col) {
        for (int i = 0; i < child.length; i++) {
            System.arraycopy(parent[row + i], col, child[i], 0, child.length);
        }
    }

    private static void combineMatrix(int[][] parent, int[][] child, int row, int col) {
        for (int i = 0; i < child.length; i++) {
            System.arraycopy(child[i], 0, parent[row + i], col, child.length);
        }
    }

    private static int[][] add(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                C[i][j] = A[i][j] + B[i][j];
        return C;
    }

    private static int[][] subtract(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                C[i][j] = A[i][j] - B[i][j];
        return C;
    }

    private static int[][] multiply(int[][] A, int[][] B) {
        int n = A.length;
        int[][] C = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                for (int k = 0; k < n; k++)
                    C[i][j] += A[i][k] * B[k][j];
        return C;
    }
}
