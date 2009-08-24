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
            double[][] a = new double[CPUhog.mSize][CPUhog.mSize];
            double[][] b = new double[CPUhog.mSize][CPUhog.mSize];
            double[][] result = new double[CPUhog.mSize][CPUhog.mSize];


            for (int r = 0; r < CPUhog.mSize; r++) {
                for (int c = 0; c < CPUhog.mSize; c++) {
                    a[r][c] = Math.random();
                    b[r][c] = Math.random();
                }
            }

            for (int i = 0; i < CPUhog.NLOOPS; i++) {
                long t0 = System.nanoTime();
                matrixMultiply(result, a, b);
                matrixCopy(a, result);
                loopTime_ns = System.nanoTime() - t0;
            }


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
