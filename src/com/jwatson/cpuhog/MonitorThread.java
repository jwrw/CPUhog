package com.jwatson.cpuhog;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

class MonitorThread implements Runnable {

    final static int K = 1024;
    static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public void run() {
        System.out.println();
        Runtime rt = Runtime.getRuntime();
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null) {
            tg = tg.getParent();
        }

        long startTime = System.nanoTime();
        Object obj = new Object();
        long totalUserTime = 0;
        long totalCPUTime = 0;
        long sysTime = 0;
        while (true) {
            if (CPUhog.generateLogging) {
                System.out.println("Time /s Threads CPUs   Free KBs  Total KBs    Max KBs  %CPU User %CPU Total %CPU / CPU ExeTime/ms Ld Wait/ms vSize");
            }
            for (int i = 0; i < CPUhog.ITERSPERTITLE; i++) {
                // Get the threads in the current thread group into an array
                // May take a few goes to get a big enough array if
                // the number is very rapidly increasing
                Thread[] threads;
                int nThreads;
                do {
                    threads = new Thread[tg.activeCount() * 2];
                    nThreads = tg.enumerate(threads);
                } while (nThreads > threads.length);


                // interrogate all the threads in the thread group to
                // determine the total amount of CPU time they have all used
                long newTotalUserTime = 0;
                long newTotalCPUTime = 0;
                long newSysTime = System.nanoTime();
                for (int iThread = 0; iThread < nThreads; iThread++) {
                    long tid = threads[iThread].getId();
                    newTotalUserTime += threadMXBean.getThreadUserTime(tid);
                    newTotalCPUTime += threadMXBean.getThreadCpuTime(tid);
                }
                newSysTime = (newSysTime + System.nanoTime()) / 2;

                // interrogate only the load threads to
                // get an average time to perform the load within
                // the main thread loop
                long sumExecuteTime = 0;
                int nLoadThreads = 0;
                for (ThrashThread t : CPUhog.loadThreads) {
                    if (t.getLoadExecuteTime_ns() > 0) {
                        sumExecuteTime += t.getLoadExecuteTime_ns();
                        nLoadThreads++;
                    }
                }
                if (nLoadThreads == 0) {
                    sumExecuteTime = -1;
                    nLoadThreads = 1;
                }
                // Vector size may be adjusted if the load is running too
                // fast or too slow.
                // The target is to have the load run LOADRUNSPERLOG_TARGET times
                // within the logging time
                // i.e. N * (load + load_wait) = logging_time
                // 
                // In practice the size is adjusted to bring the number of iterations
                // achieved between ..._LO and ..._HI limits
                double timeDelta_ns = newSysTime - sysTime;
                double percentUserTime = 100. * (newTotalUserTime - totalUserTime) / timeDelta_ns;
                double percentCPUTime = 100. * (newTotalCPUTime - totalCPUTime) / timeDelta_ns;
                double perProcessorPercentCPU = percentCPUTime / rt.availableProcessors();
                double aveLoadExecuteTime_ns = sumExecuteTime / nLoadThreads;

                if (CPUhog.autoSizeAdjustmentAllowed && sumExecuteTime > 0) {
                    // for this loop time work out how many times this theoretically ought
                    // to run if the loop wait time were perfect
                    double runTimesPerLog =
                            (CPUhog.monitorWait_ms / 1000.) * (CPUhog.targetCPUpercent / 100.) /
                            (aveLoadExecuteTime_ns / 1.e9);
                    // Now adjust - about halfway to the correct value if outside the
                    // HI/LO tolerance - otherwise just tweak a bit
                    if (runTimesPerLog < CPUhog.LOADRUNSPERLOG_LO) {
                        CPUhog.loadSize = adjustedLoadSize(runTimesPerLog, 0.5);
                    } else if (runTimesPerLog > CPUhog.LOADRUNSPERLOG_HI) {
                        CPUhog.loadSize = adjustedLoadSize(runTimesPerLog, 0.5);
                    } else {
                          CPUhog.loadSize = adjustedLoadSize(runTimesPerLog, 0.01);
                    }
                }

                // Adjust the wait time used in the load threads to give
                // the target CPU load
                if (CPUhog.targetCPUpercent < 100) {
                    double theoreticLoadWaitTime_ns = aveLoadExecuteTime_ns * (100. - CPUhog.targetCPUpercent) / CPUhog.targetCPUpercent;
                    double correctedLoadWaitTime_ns = 
                            (aveLoadExecuteTime_ns+CPUhog.loadWaitTime_ms*1.0e6) * perProcessorPercentCPU / CPUhog.targetCPUpercent - aveLoadExecuteTime_ns;
 //                   CPUhog.loadWaitTime_ms =
 //                           (int) (CPUhog.loadWaitTime_ms * (1.0 - CPUhog.LOADWAITDAMPING) +
 //                           (theoreticalLoadWaitTime_ns / 1.0e6) * CPUhog.LOADWAITDAMPING);
                   CPUhog.loadWaitTime_ms =
                            (int) (CPUhog.loadWaitTime_ms * (1.0 - CPUhog.LOADWAITDAMPING) +
                            (correctedLoadWaitTime_ns / 1.0e6) * CPUhog.LOADWAITDAMPING);
                }

                if (CPUhog.generateLogging) {
                    System.out.println(String.format("%7.3f %7d %4d %10d %10d %10d %10.3f %10.3f %10.3f %10.3f %10d %5d",
                            (newSysTime - startTime) / 1.0E9,
                            tg.activeCount(),
                            rt.availableProcessors(),
                            rt.freeMemory() / K,
                            rt.totalMemory() / K,
                            rt.maxMemory() / K,
                            percentUserTime,
                            percentCPUTime,
                            perProcessorPercentCPU,
                            aveLoadExecuteTime_ns / 1e6,
                            CPUhog.loadWaitTime_ms,
                            CPUhog.loadSize));
                }

                totalUserTime = newTotalUserTime;
                totalCPUTime = newTotalCPUTime;
                sysTime = newSysTime;

                if (CPUhog.monitorWait_ms > 0) {
                    synchronized (obj) {
                        try {
                            obj.wait(CPUhog.monitorWait_ms);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }
    }

    /**
     * Return a new load sizing based on a theoretic estimate of load
     * required to achieve target loop time.
     * The estimate is allowed to be as small as necessary - however
     * the upper bound is limited to twice the current value.
     * @param factor Value of 1.0 returns the estimate; value 0.0 returns the
     * original loadSizing value.
     * @return The factored and limited estimate
     */
    private int adjustedLoadSize(double currentLoadRunsPerLog, double factor) {
        double scale = currentLoadRunsPerLog / CPUhog.LOADRUNSPERLOG_TARGET;
        double estimate = CPUhog.loadSize * scale * scale;

        if (estimate > CPUhog.loadSize) {
            estimate = Math.min(estimate, CPUhog.loadSize * 2.);
        }
        int newLoad = (int) (estimate * factor + CPUhog.loadSize * (1. - factor));
        return newLoad;
    }
}
