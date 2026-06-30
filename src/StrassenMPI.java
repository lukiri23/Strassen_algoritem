import mpi.*;

import java.util.Random;

public class StrassenMPI {

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
            System.out.println("Zagon porazdeljene izvedbe.");
            System.out.println("Stevilo procesov: " + stProcesov);
            System.out.println("Velikost matrike: " + velikost);
            System.out.println("Seed: " + osnovniSeed);

            // tukaj zaenkrat samo pripravim matrike, racunanje pride v naslednjem koraku
            int[][] matrixA = generateRandomMatrix(velikost, osnovniSeed);
            int[][] matrixB = generateRandomMatrix(velikost, osnovniSeed + 1);

            System.out.println("Matriki sta pripravljeni na glavnem procesu.");
            System.out.println("Kontrola A[0][0]: " + matrixA[0][0] + ", B[0][0]: " + matrixB[0][0]);
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
}