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
                System.out.println("Time /s Threads CPUs   Free KBs  Total KBs    Max KBs  %CPU User %CPU Total %CPU / CPU LoopTime/s");
            }
            for (int i = 0; i < CPUhog.ITERSPERTITLE; i++) {
                Thread[] threads;
                int nThreads;
                do {
                    threads = new Thread[tg.activeCount() * 2];
                    nThreads = tg.enumerate(threads);
                } while (nThreads > threads.length);

                if (CPUhog.generateLogging) {
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

                    System.out.println(String.format("%7.3f %7d %4d %10d %10d %10d %10.3f %10.3f %10.3f %10.3e",
                            (newSysTime - startTime) / 1.0E9,
                            tg.activeCount(),
                            rt.availableProcessors(),
                            rt.freeMemory() / K,
                            rt.totalMemory() / K,
                            rt.maxMemory() / K,
                            100. * (newTotalUserTime - totalUserTime) / (newSysTime - sysTime),
                            100. * (newTotalCPUTime - totalCPUTime) / (newSysTime - sysTime),
                            100. * (newTotalCPUTime - totalCPUTime) / (newSysTime - sysTime) / rt.availableProcessors(),
                            sumLoopTime / 1e9 / nLoadThreads));

                    totalUserTime = newTotalUserTime;
                    totalCPUTime = newTotalCPUTime;
                    sysTime = newSysTime;
                }

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
