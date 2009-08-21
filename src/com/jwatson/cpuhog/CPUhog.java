/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jwatson.cpuhog;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.TreeMap;

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
    final static String NOT_SUPPORTED = "<not supported>";
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
        
            if (args.length > 3) {
                throw new Exception();
            }

        } catch (Exception e) {
            usage();
            System.exit(-1);
        }

        dumpSystemInformation();

        System.out.println("Hogging all the CPU with " + nThreads + " java threads\n" +
                "doing " + mSize + "x" + mSize + " matrix arithmetic.");

        (new Thread(new MonitorThread())).start();

        for (int i = 0; i < nThreads; i++) {
            Thread t = new Thread(new ThrashThread());
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

    private static void dumpSystemInformation() {
        CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        List<MemoryManagerMXBean> memoryManagerMXBeans = ManagementFactory.getMemoryManagerMXBeans();
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

        System.out.println("Compliation Information");
        System.out.println("JIT compiler name: " + compilationMXBean.getName());
        System.out.println("Total JIT compile time: " +
                (compilationMXBean.isCompilationTimeMonitoringSupported() ? compilationMXBean.getTotalCompilationTime() : NOT_SUPPORTED));
            System.out.println();

        System.out.println("Operating System Information");
        System.out.println("OS name: " + operatingSystemMXBean.getName());
        System.out.println("OS version: " + operatingSystemMXBean.getVersion());
        System.out.println("Architecture: " + operatingSystemMXBean.getArch());
        System.out.println("Available processors: " + operatingSystemMXBean.getAvailableProcessors());
        System.out.println();

        System.out.println("Runtime Information");
        System.out.println("---system properties---");
        TreeMap<String, String> sortedProps = new TreeMap<String, String>(runtimeMXBean.getSystemProperties());
        for (String key : sortedProps.keySet()) {
            System.out.println(">" + key + ": " + sortedProps.get(key));
                        }
        System.out.println("---end of system properties---");
        System.out.println("JVM uptime/ms: " + runtimeMXBean.getUptime());
        System.out.println();

        System.out.println("Thread Information");
        System.out.println("Thread count: " + threadMXBean.getThreadCount());
        System.out.println("Thread CPU time supported: " + threadMXBean.isThreadCpuTimeSupported());
        System.out.println("Thread CPU time enabled: " + threadMXBean.isThreadCpuTimeEnabled());
        System.out.println();

        System.out.println("Memory Information");
        System.out.println("Heap memory usage: " + memoryMXBean.getHeapMemoryUsage());
        System.out.println("Non-heap memory usage: " + memoryMXBean.getNonHeapMemoryUsage());
        System.out.println();

        for (MemoryManagerMXBean mmmxb : memoryManagerMXBeans) {
            System.out.println("Memory Manager Information (Name: " + mmmxb.getName() + ")");
            System.out.print("Managed pool names: ");
            for (String poolName : mmmxb.getMemoryPoolNames()) {
                System.out.print("'" + poolName + "' ");
                    }
            System.out.println();
            System.out.println();
        }

        for (MemoryPoolMXBean mpmxb : memoryPoolMXBeans) {
            System.out.println("Memory Pool Information (Name: " + mpmxb.getName() + ")");
            System.out.println("Peak usage: " + mpmxb.getPeakUsage());
            System.out.println("Type: " + mpmxb.getType());
            System.out.println("Usage: " + mpmxb.getUsage());
            System.out.println("Usage threshold: " + (mpmxb.isUsageThresholdSupported() ? mpmxb.getUsageThreshold() : NOT_SUPPORTED));
            System.out.println();
                    }
                }
            }
