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
                System.out.println("Time /s Threads CPUs   Free KBs  Total KBs    Max KBs  %CPU User %CPU Total %CPU / CPU LoopTime/s Ld Wait/ms mSize");
            }
            for (int i = 0; i < CPUhog.ITERSPERTITLE; i++) {
                Thread[] threads;
                int nThreads;
                do {
                    threads = new Thread[tg.activeCount() * 2];
                    nThreads = tg.enumerate(threads);
                } while (nThreads > threads.length);


                long newTotalUserTime = 0;
                long newTotalCPUTime = 0;
                long newSysTime = System.nanoTime();
                for (int iThread = 0; iThread < nThreads; iThread++) {
                    long tid = threads[iThread].getId();
                    newTotalUserTime += threadMXBean.getThreadUserTime(tid);
                    newTotalCPUTime += threadMXBean.getThreadCpuTime(tid);
                }
                newSysTime = (newSysTime + System.nanoTime()) / 2;

                long sumLoopTime = 0;
                int nLoadThreads = 0;
                for (ThrashThread t : CPUhog.loadThreads) {
                    if (t.getLoopTime_ns() > 0) {
                        sumLoopTime += t.getLoopTime_ns();
                        nLoadThreads++;
                    }
                }
                if (nLoadThreads == 0) {
                    sumLoopTime = -1;
                    nLoadThreads = 1;
                }

                double timeDelta_ns = newSysTime - sysTime;
                double percentUserTime = 100. * (newTotalUserTime - totalUserTime) / timeDelta_ns;
                double percentCPUTime = 100. * (newTotalCPUTime - totalCPUTime) / timeDelta_ns;
                double perProcessorPercentCPU = percentCPUTime / rt.availableProcessors();
                double aveLoadLoopTime_ns = sumLoopTime / nLoadThreads;

                if (CPUhog.targetCPUpercent < 100) {
                    double newLoadWaitTime_ns = aveLoadLoopTime_ns * (100. - CPUhog.targetCPUpercent) / CPUhog.targetCPUpercent;
                    CPUhog.loadWait_ms =
                            (int) (CPUhog.loadWait_ms * (1 - CPUhog.LOADWAITDAMPING) +
                            newLoadWaitTime_ns * CPUhog.LOADWAITDAMPING / 1000000.);
                    if (CPUhog.autoDimensioningAllowed) {
                        if (newLoadWaitTime_ns < 10000000.) {
                            CPUhog.mSize++;
                        } else if (newLoadWaitTime_ns > 16000000000L || newLoadWaitTime_ns / 1000000L > CPUhog.monitorWait_ms) {
                            if (CPUhog.mSize > 1) {
                                CPUhog.mSize--;
                            }
                        }
                    }
                }

                if (CPUhog.generateLogging) {
                    System.out.println(String.format("%7.3f %7d %4d %10d %10d %10d %10.3f %10.3f %10.3f %10.3e %10d %5d",
                            (newSysTime - startTime) / 1.0E9,
                            tg.activeCount(),
                            rt.availableProcessors(),
                            rt.freeMemory() / K,
                            rt.totalMemory() / K,
                            rt.maxMemory() / K,
                            percentUserTime,
                            percentCPUTime,
                            perProcessorPercentCPU,
                            aveLoadLoopTime_ns / 1e9,
                            CPUhog.loadWait_ms,
                            CPUhog.mSize));
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
}
