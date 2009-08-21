/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jwatson.cpuhog;

import com.jwatson.fastmatrix.Matrix;

/**
 * The main class of the CPUhog application.
 * Reads the arguments, starts a monitoring thread and then starts
 * the requested number of load generating threads
 *
 * @author jim
 */
public class CPUhog {

    final static int MSIZE = 100;
    final static int NLOOPS = 100;
    final static int NTHREADS = 100;
    final static long MONITORWAIT_MS = 1000;
    final static int ITERSPERTITLE = 20;

    static int nThreads;
    static int mSize;
    static long monitorWait_ms;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        nThreads = NTHREADS;
        mSize = MSIZE;
        monitorWait_ms = MONITORWAIT_MS;

        try {
        if (args.length > 0) {
            nThreads = Integer.parseInt(args[0]);
        }
        
        if (args.length > 1) {
            mSize = Integer.parseInt(args[1]);
        }
        
        if (args.length >2) {
            monitorWait_ms = Long.parseLong(args[2]);
        }
        
        if (args.length >3) throw new Exception();

        } catch (Exception e) {
            usage();
            System.exit(-1);
        }



        System.out.println("Hogging all the CPU with " + nThreads + " java threads\n" +
                "doing " + mSize + "x" + mSize + " matrix arithmetic.");

        (new MonitorThread()).start();

        for (int i = 0; i < nThreads; i++) {
            Thread t = new ThrashThread();
            t.setPriority(t.getPriority()-1);   // minimise system killing ability?
            t.setDaemon(true);                  // faster exit?
            t.start();
        }

        System.out.println();
        System.out.println("All threads started.");
    }

    private static void usage() {
        System.err.println(
                "\nUsage: java -jar CPUhog.jar <n threads> <matrix sz> <log delay>\n" +
                "\n" +
                "\tArguments are optional, but the preceeding ones must be entered\n" +
                "\t<n threads>\t 100\tNumber of CPU-loading threads to start (there will usually be 2 additional 'system' ones)\n" +
                "\t<matrix sz>\t 100\tSize of matrices to use - so you can hog memory too!\n" +
                "\t<log delay>\t1000\tNumber of ms to wait between log output lines (won't be exact)\n\n");

    }

    static class MonitorThread extends Thread {

        @Override
        public void run() {
            System.out.println();
            Runtime rt = Runtime.getRuntime();
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            long t0 = System.nanoTime();

            Object obj = new Object();
            while (true) {
                System.out.println("Time /s Threads CPUs   Free mem  Total mem   Max mem");

                for (int i = 0; i < ITERSPERTITLE; i++) {
                    System.out.println(
                            String.format("%7.3f %7d %4d %10d %10d %10d",
                            (System.nanoTime()-t0)/1e9,
                            tg.activeCount(),
                            rt.availableProcessors(),
                            rt.freeMemory(),
                            rt.totalMemory(),
                            rt.maxMemory()));

                    synchronized (obj) {
                        try {
                            obj.wait(monitorWait_ms);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }
    }

    static class ThrashThread extends Thread {

        @Override
        public void run() {
            while (true) {
                double aData[][] = new double[mSize][mSize];
                double bData[][] = new double[mSize][mSize];

                for (int r = 0; r < mSize; r++) {
                    for (int c = 0; c < mSize; c++) {
                        aData[r][c] = Math.random();
                        bData[r][c] = Math.random();
                    }

                    Matrix a = new Matrix(aData);
                    Matrix b = new Matrix(bData);

                    for (int i = 0; i < NLOOPS; i++) {
                        Matrix c = a.postMultiply(b);
                        a = b;
                        b = c;
                    }
                }
            }
        }
    }
}
