package com.jwatson.cpuhog;

/**
 * All the things the application does to load the system are in this class.
 * 
 * @author WatsonJ
 */
final public class ThrashThread extends Thread {

    private volatile long loopTime_ns = -1;

    public long getLoopTime_ns() {
        return loopTime_ns;
    }

    /**
     * This method creates a CPU load on the system.
     * It uses three double matrices and avoids object creation
     * and destruction (and garbage collection).
     * Memory footprint should remain fairly static during execution.
     */
    @Override
    public final void run() {
        while (true) {
            double[][] a = new double[CPUhog.loadSize][CPUhog.loadSize];
            double[][] b = new double[CPUhog.loadSize][CPUhog.loadSize];
            double[][] result = new double[CPUhog.loadSize][CPUhog.loadSize];

            do {
                long t0 = System.nanoTime();
                for (int r = 0; r < a.length; r++) {
                    for (int c = 0; c < a[0].length; c++) {
                        a[r][c] = Math.random();
                        b[r][c] = Math.random();
                    }
                }

                for (int i = 0; i < CPUhog.NLOOPS; i++) {
                    matrixMultiply(result, a, b);
                    matrixCopy(a, result);
                }
                loopTime_ns = (System.nanoTime() - t0);

                if (CPUhog.loadWait_ms > 0) {
                    synchronized (this) {
                        try {
                            this.wait(CPUhog.loadWait_ms);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            } while (a.length == CPUhog.loadSize);
        }
    }

    /**
     * Compute result = matrix a x matrix b
     * @param result
     * @param a
     * @param b
     */
    public final static void matrixMultiply(double result[][], double a[][], double b[][]) {
        double sum;
        for (int r = 0; r < result.length; r++) {
            for (int c = 0; c < result[0].length; c++) {
                sum = 0;
                for (int rc = 0; rc < a.length; rc++) {
                    sum += a[r][rc] * b[rc][c];
                }
                result[r][c] = sum;
            }
        }
    }

    /**
     * Copy maxtrix a to the result matrix
     * @param result
     * @param a
     */
    public final static void matrixCopy(double result[][], double a[][]) {
        for (int r = 0; r < result.length; r++) {
            for (int c = 0; c < result[0].length; c++) {
                result[r][c] = a[r][c];
            }
        }

    }
}
