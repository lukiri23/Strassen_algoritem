import mpi.*;

import java.util.Locale;
import java.util.Random;

public class StrassenMPI {

    private static final int THRESHOLD = 128;

    public static void main(String[] args) {
        MPI.Init(args);

        int mojRank = MPI.COMM_WORLD.Rank();
        int stProcesov = MPI.COMM_WORLD.Size();

        if (args.length < 2) {
            if (mojRank == 0) {
                System.out.println("Manjkajo argumenti. Uporaba: StrassenMPI velikost seed");
            }
            MPI.Finalize();
            return;
        }

        int velikost = Integer.parseInt(args[args.length - 2]);
        long osnovniSeed = Long.parseLong(args[args.length - 1]);

        if (mojRank == 0) {
            System.out.println("Stevilo procesov: " + stProcesov);
            System.out.println("Velikost matrike: " + velikost);

            int[][] matrixA = generateRandomMatrix(velikost, osnovniSeed);
            int[][] matrixB = generateRandomMatrix(velikost, osnovniSeed + 1);

            long zacetek = System.nanoTime();
            int[][] rezultat = strassenMultiply(matrixA, matrixB);
            long konec = System.nanoTime();

            double cas = (konec - zacetek) / 1_000_000_000.0;

            System.out.println("Cas izracuna na procesu 0: " + String.format(Locale.US, "%.4f", cas) + " sekund");
        } else {
            System.out.println("Proces " + mojRank + " je pripravljen in caka na delo.");
        }

        MPI.Finalize();
    }

    private static int[][] generateRandomMatrix(int size, long seed) {
        Random random = new Random(seed);
        int[][] matrix = new int[size][size];

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                matrix[i][j] = random.nextInt(100);

        return matrix;
    }

    private static int[][] strassenMultiply(int[][] A, int[][] B) {
        int n = A.length;

        if (n <= THRESHOLD) {
            return multiply(A, B);
        }

        int newSize = n / 2;
        if (n % 2 != 0) {
            newSize++;
        }

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

        int[][] M1 = strassenMultiply(add(A11, A22), add(B11, B22));
        int[][] M2 = strassenMultiply(add(A21, A22), B11);
        int[][] M3 = strassenMultiply(A11, subtract(B12, B22));
        int[][] M4 = strassenMultiply(A22, subtract(B21, B11));
        int[][] M5 = strassenMultiply(add(A11, A12), B22);
        int[][] M6 = strassenMultiply(subtract(A21, A11), add(B11, B12));
        int[][] M7 = strassenMultiply(subtract(A12, A22), add(B21, B22));

        int[][] C = new int[n][n];

        combineMatrix(C, add(subtract(add(M1, M4), M5), M7), 0, 0);
        combineMatrix(C, add(M3, M5), 0, newSize);
        combineMatrix(C, add(M2, M4), newSize, 0);
        combineMatrix(C, add(subtract(add(M1, M3), M2), M6), newSize, newSize);

        return C;
    }

    private static void splitMatrix(int[][] parent, int[][] child, int row, int col) {
        for (int i = 0; i < child.length; i++) {
            if (row + i < parent.length) {
                int copyLength = Math.min(child.length, parent.length - col);
                if (copyLength > 0) {
                    System.arraycopy(parent[row + i], col, child[i], 0, copyLength);
                }
            }
        }
    }

    private static void combineMatrix(int[][] parent, int[][] child, int row, int col) {
        for (int i = 0; i < child.length; i++) {
            if (row + i < parent.length) {
                int copyLength = Math.min(child.length, parent.length - col);
                if (copyLength > 0) {
                    System.arraycopy(child[i], 0, parent[row + i], col, copyLength);
                }
            }
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