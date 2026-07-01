import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import javax.swing.JOptionPane;

public class SequentialStrassen {

    private static final int THRESHOLD = 128;
    private static final int PARALLEL_THRESHOLD = 512;
    private static final String DATOTEKA_REZULTATOV = "results.csv";

    public static void main(String[] args) {

        String nacinIzvedbe = pridobiNacinIzvedbe(args);
        if (nacinIzvedbe == null) {
            return;
        }

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

        if (nacinIzvedbe.equals("distributed")) {
            int stProcesov = pridobiSteviloProcesov();
            long osnovniSeed = narediSeed(matrixSize);

            zazeniPorazdeljeno(matrixSize, osnovniSeed, stProcesov);
            return;
        }

        pripraviCsvDatoteko();

        int stJeder = Runtime.getRuntime().availableProcessors();
        ForkJoinPool bazenNiti = null;

        if (izvajaParalelno(nacinIzvedbe)) {
            bazenNiti = new ForkJoinPool(stJeder);
        }

        System.out.println("Način izvedbe: " + nacinIzvedbe);
        System.out.println("Število procesorskih jeder: " + stJeder);

        while (true) {
            long osnovniSeed = narediSeed(matrixSize);

            int[][] matrixA = generateRandomMatrix(matrixSize, osnovniSeed);
            int[][] matrixB = generateRandomMatrix(matrixSize, osnovniSeed + 1);

            Double avgTime = null;
            Double avgParallelTime = null;
            boolean timeExceeded = false;

            if (izvajaZaporedno(nacinIzvedbe)) {
                double totalElapsedTime = 0;

                for (int i = 0; i < 3; i++) {
                    long startTime = System.nanoTime();
                    int[][] result = strassenMultiply(matrixA, matrixB);
                    long endTime = System.nanoTime();

                    double elapsedTime = (endTime - startTime) / 1_000_000_000.0;

                    if (elapsedTime > 600) {
                        JOptionPane.showMessageDialog(null, "Zaporedna izvedba za velikost " + matrixSize + " presega 10 minut. Program se zaključi.", "Opozorilo", JOptionPane.WARNING_MESSAGE);
                        timeExceeded = true;
                        break;
                    }

                    totalElapsedTime += elapsedTime;
                }

                if (timeExceeded) break;

                avgTime = totalElapsedTime / 3;
            }

            if (izvajaParalelno(nacinIzvedbe)) {
                double totalParallelTime = 0;

                for (int i = 0; i < 3; i++) {
                    long startTime = System.nanoTime();
                    int[][] result = parallelStrassenMultiply(matrixA, matrixB, bazenNiti);
                    long endTime = System.nanoTime();

                    double elapsedTime = (endTime - startTime) / 1_000_000_000.0;

                    if (elapsedTime > 600) {
                        JOptionPane.showMessageDialog(null, "Paralelna izvedba za velikost " + matrixSize + " presega 10 minut. Program se zaključi.", "Opozorilo", JOptionPane.WARNING_MESSAGE);
                        timeExceeded = true;
                        break;
                    }

                    totalParallelTime += elapsedTime;
                }

                if (timeExceeded) break;

                avgParallelTime = totalParallelTime / 3;
            }

            DecimalFormat df = new DecimalFormat("0.0000");

            String zaporedniIzpis = avgTime == null ? "-" : df.format(avgTime) + " sekund";
            String paralelniIzpis = avgParallelTime == null ? "-" : df.format(avgParallelTime) + " sekund";

            System.out.println("Velikost matrike: " + matrixSize
                    + ", Zaporedni čas: " + zaporedniIzpis
                    + ", Paralelni čas: " + paralelniIzpis);

            zapisiCsvVrstico(matrixSize, avgTime, avgParallelTime);

            matrixSize += 500;
        }

        if (bazenNiti != null) {
            bazenNiti.shutdown();
        }
    }

    private static String pridobiNacinIzvedbe(String[] args) {
        if (args.length == 0) {
            return "both";
        }

        String nacin = args[0].toLowerCase();

        if (nacin.equals("sequential") || nacin.equals("parallel") || nacin.equals("both") || nacin.equals("distributed")) {
            return nacin;
        }

        JOptionPane.showMessageDialog(null, "Neveljaven način izvedbe. Uporabi: sequential, parallel, both ali distributed.", "Napaka", JOptionPane.ERROR_MESSAGE);
        return null;
    }

    private static boolean izvajaZaporedno(String nacin) {
        return nacin.equals("sequential") || nacin.equals("both");
    }

    private static boolean izvajaParalelno(String nacin) {
        return nacin.equals("parallel") || nacin.equals("both");
    }

    private static int pridobiSteviloProcesov() {
        String input = JOptionPane.showInputDialog(null, "Vnesite število procesov za porazdeljeno izvedbo:");

        try {
            int stProcesov = Integer.parseInt(input);
            if (stProcesov < 2) {
                return 2;
            }
            return stProcesov;
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    private static void zazeniPorazdeljeno(int velikost, long osnovniSeed, int stProcesov) {
        String mpjHome = System.getenv("MPJ_HOME");

        if (mpjHome == null || mpjHome.isEmpty()) {
            System.out.println("MPJ_HOME ni nastavljen.");
            return;
        }

        String mpjrun = mpjHome + "\\bin\\mpjrun.bat";
        String mpjJar = mpjHome + "\\lib\\mpj.jar";

        // tukaj samo zaženem MPI program, pravo računanje dodam kasneje
        ProcessBuilder pb = new ProcessBuilder(
                mpjrun,
                "-np", Integer.toString(stProcesov),
                "-cp", ".;src;" + mpjJar,
                "StrassenMPI",
                Integer.toString(velikost),
                Long.toString(osnovniSeed)
        );

        pb.inheritIO();

        try {
            Process proces = pb.start();
            int izhodnaKoda = proces.waitFor();
            System.out.println("MPI proces zaključen s kodo: " + izhodnaKoda);
        } catch (IOException | InterruptedException e) {
            System.out.println("Napaka pri zagonu porazdeljene izvedbe.");
        }
    }

    private static long narediSeed(int velikost) {
        return 123456789L + velikost * 31L;
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

    private static int[][] parallelStrassenMultiply(int[][] A, int[][] B, ForkJoinPool bazenNiti) {
        return bazenNiti.invoke(new ParalelnaNaloga(A, B));
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

    private static void pripraviCsvDatoteko() {
        try (PrintWriter izpis = new PrintWriter(new FileWriter(DATOTEKA_REZULTATOV))) {
            izpis.println("size,sequential,parallel,distributed");
        } catch (IOException e) {
            System.out.println("Napaka pri pripravi CSV datoteke.");
        }
    }

    private static void zapisiCsvVrstico(int velikost, Double casZaporedno, Double casParalelno) {
        try (PrintWriter izpis = new PrintWriter(new FileWriter(DATOTEKA_REZULTATOV, true))) {
            String zaporedniCas = casZaporedno == null ? "" : String.format(Locale.US, "%.4f", casZaporedno);
            String paralelniCas = casParalelno == null ? "" : String.format(Locale.US, "%.4f", casParalelno);

            izpis.println(velikost + "," + zaporedniCas + "," + paralelniCas + ",");
        } catch (IOException e) {
            System.out.println("Napaka pri zapisovanju rezultatov.");
        }
    }

    private static class ParalelnaNaloga extends RecursiveTask<int[][]> {

        private final int[][] prva;
        private final int[][] druga;

        ParalelnaNaloga(int[][] prva, int[][] druga) {
            this.prva = prva;
            this.druga = druga;
        }

        @Override
        protected int[][] compute() {
            int n = prva.length;

            if (n <= PARALLEL_THRESHOLD) {
                return strassenMultiply(prva, druga);
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

            splitMatrix(prva, A11, 0, 0);
            splitMatrix(prva, A12, 0, newSize);
            splitMatrix(prva, A21, newSize, 0);
            splitMatrix(prva, A22, newSize, newSize);

            splitMatrix(druga, B11, 0, 0);
            splitMatrix(druga, B12, 0, newSize);
            splitMatrix(druga, B21, newSize, 0);
            splitMatrix(druga, B22, newSize, newSize);

            ParalelnaNaloga naloga1 = new ParalelnaNaloga(add(A11, A22), add(B11, B22));
            ParalelnaNaloga naloga2 = new ParalelnaNaloga(add(A21, A22), B11);
            ParalelnaNaloga naloga3 = new ParalelnaNaloga(A11, subtract(B12, B22));
            ParalelnaNaloga naloga4 = new ParalelnaNaloga(A22, subtract(B21, B11));
            ParalelnaNaloga naloga5 = new ParalelnaNaloga(add(A11, A12), B22);
            ParalelnaNaloga naloga6 = new ParalelnaNaloga(subtract(A21, A11), add(B11, B12));
            ParalelnaNaloga naloga7 = new ParalelnaNaloga(subtract(A12, A22), add(B21, B22));

            naloga2.fork();
            naloga3.fork();
            naloga4.fork();
            naloga5.fork();
            naloga6.fork();
            naloga7.fork();

            int[][] M1 = naloga1.compute();
            int[][] M2 = naloga2.join();
            int[][] M3 = naloga3.join();
            int[][] M4 = naloga4.join();
            int[][] M5 = naloga5.join();
            int[][] M6 = naloga6.join();
            int[][] M7 = naloga7.join();

            int[][] C = new int[n][n];

            combineMatrix(C, add(subtract(add(M1, M4), M5), M7), 0, 0);
            combineMatrix(C, add(M3, M5), 0, newSize);
            combineMatrix(C, add(M2, M4), newSize, 0);
            combineMatrix(C, add(subtract(add(M1, M3), M2), M6), newSize, newSize);

            return C;
        }
    }
}