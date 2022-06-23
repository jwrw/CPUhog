/*
 * CPUhog.java
 *
 * The CPUhog provides a simple way to load a multipocessor / multithread machine to the speified
 * degree.
 *
 * The aplication is written in Java so runs unmodified on a variety of platforms.
 *
 * It is self-contained and requires no additional special libraries.
 *
 * The system load is created using a number of Java threads. To fully load any environment the
 * application requires that the Java Virtual Machine (JVM) used to run the application spreads the
 * load across the available system processors / cores / CPU threads.
 *
 * The load uses a mixture of integer and double floating point arithmetic which provides a good
 * load on many platforms. However some CPU architectures with restricted availabilty of floating
 * point hardware may find their integer components not fully utilised. (A future version could do
 * something about this).
 *
 * The application can also print out a great deal of diagnosic information about the system, the
 * JVM and the environment. This information is largely derived from the java.lang.management
 * 'MXBean' family of components. A more comprehensive description of the statistics generated may
 * be found there.
 *
 * Usage: java -jar CPUhog <options>
 *
 * The options can be specified in any order and later ones override earlier ones. Available options
 * are -t nnn Start load nnn threads (default 10). Typically the main program runs in the initial
 * thread and it starts a monitoring thread as well as the specified number of load threads. You may
 * see additional threads created by the JVM for system use.
 *
 * -s nnn The number of coefficients in the vector used during the convolution. The signal vector
 * that the coefficients are applied against is a fixed multiple in size of the coefficient vector.
 * So doubling the number coefficients will nearly increase the load execution time by 4.
 *
 * -a Permit the application to adjust the vector size automatically. Initially this will reduce the
 * size when out of memory errors start to occur. During adjustment the load may fluctuate.
 * Currently the size is not adjusted upwards so the -d option can be used to set an upper value.
 *
 * -w nnn The amount of time (ms) to wait between log line outputs.
 *
 * -sn No statistics. -sa All statistics -sc Compilation information -so Operating system
 * information. This is the only section output by default. -sr Runtime information (includes all
 * java system properties) -st Thread information -sm Memory information -sp Memory pool information
 *
 * -c nnn The target percentage of total CPU to use (integer - default 100). A delay within each
 * load thread will be adjusted to bring the aggregate load on the system to the specified
 * percentage. The granularity that the application can achieve will be determined by the size of
 * matrix and the speed of CPU. This also relies on the JVM / OS to spread the total load evenly
 * (although this may be what you are testing!)
 *
 * -q Supress logging information.
 *
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The main class of the CPUhog application. Reads the arguments, starts a monitoring thread and
 * then starts the requested number of load generating threads.
 *
 * @author jim
 */
@SuppressWarnings("checkstyle:staticvariablename")
public final class CPUhog {

    /** Number of times to perform the main load loop. */
    public static final int NLOOPS = 25;
    /**
     * Number of lines of monitor output between re-printing of the column titles.
     */
    public static final int ITERSPERTITLE = 20;
    /** Text for "not supported". */
    public static final String NOT_SUPPORTED = "<not supported>";
    /**
     * Don't adjust the wait time in the load loop too quickly. Only this proportion of the desired
     * wait time is averaged with the current wait time. i.e. 1 = no damping; 0 = so damped there is
     * no change
     */
    public static final double LOADWAITDAMPING = 0.3;
    /**
     * The automatic adjustment will adjust the load complexity try to allow the load loop to run a
     * number of times within a single monitoring period. The adjustment calculations aim to make
     * this number between _LO and _HI number of load loops in a single log period
     */
    public static final double LOADRUNSPERLOG_LO = 1.7;
    /**
     * The automatic adjustment will adjust the load complexity try to allow the load loop to run a
     * number of times within a single monitoring period. The adjustment calculations aim to make
     * this number between _LO and _HI number of load loops in a single log period.
     */
    public static final double LOADRUNSPERLOG_HI = 3.;
    /**
     * The automatic adjustment will adjust the load complexity try to allow the load loop to run a
     * this number of times within a single monitoring period.
     */
    public static final double LOADRUNSPERLOG_TARGET = 2.;
    /**
     * The signal vector is a number of times larger than the coefficient vector This factor
     * determines how much bigger.
     */
    public static final int SIGNAL_FACTOR = 100;
    private static final int PERCENT_100 = 0;

    /** Number of load threads the application will create. */
    private static int nThreads = 10;
    /**
     * the size of the load. The bigger this value, the greater amount of time the main load part of
     * the load loop will take.
     */
    private static volatile int loadSize = 1000;
    /** Time to wait between each monitor output. */
    private static long monitorWait_ms = 2000;
    /** Can the load automatically adjust its size. */
    private static boolean autoSizeAdjustmentAllowed;
    /** Show stats of compiled java. */
    private static boolean showCompilationStats;
    /** Show stats of host O/S. */
    private static boolean showOSStats = true;
    /** Show java runtime stats. */
    private static boolean showRuntimeStats;
    /** Show thread information. */
    private static boolean showThreadStats;
    /** Show outline memory stats. */
    private static boolean showMemoryStats;
    /** Show memory pool info. */
    private static boolean showPoolStats;
    /** Do loggin output (i.e. don't be quiet). */
    private static boolean generateLogging = true;

    /** Target CPU percentage hogging to aim for. */
    private static int targetCPUpercent = 100;
    /** Keep track of the threads running the load tasks. */
    private static ArrayList<ThrashThread> loadThreads;

    /** A thread to monitor the worker threads. */
    private static Thread monitorThread;
    /**
     * The wait time in the load loop cannot be set by the user but is adjusted by the application
     * to fix the load at the requested percentage. All load threads use the same wait time. Total
     * loop time is loadExecute_ns/1e9 + loadWaitTime_ms/1e3
     */
    private static int loadWaitTime_ms;

    private CPUhog() {}

    public static long getMonitorWait_ms() {
        return monitorWait_ms;
    }

    public static int getLoadSize() {
        return loadSize;
    }

    public static boolean isAutoSizeAdjustmentAllowed() {
        return autoSizeAdjustmentAllowed;
    }

    public static void setAutoSizeAdjustmentAllowed(boolean autoSizeAdjustmentAllowed) {
        CPUhog.autoSizeAdjustmentAllowed = autoSizeAdjustmentAllowed;
    }

    public static void setLoadSize(int loadSize) {
        CPUhog.loadSize = loadSize;
    }

    public static ArrayList<ThrashThread> getLoadThreads() {
        return loadThreads;
    }

    public static boolean isGenerateLogging() {
        return generateLogging;
    }

    @SuppressWarnings("checkstyle:parametername")
    public static void setLoadWaitTime_ms(int loadWaitTime_ms) {
        CPUhog.loadWaitTime_ms = loadWaitTime_ms;
    }

    public static int getLoadWaitTime_ms() {
        return loadWaitTime_ms;
    }

    /**
     * Main entry point for the application.
     *
     * @param args the command line arguments
     */
    @SuppressWarnings("checkstyle:modifiedcontrolvariable")
    public static void main(String[] args) {
        // dumpVersionInfo();

        try {
            for (int i = 0; i < args.length; i++) {

                if (args[i].equals("-t")) {
                    i++;
                    nThreads = Integer.parseInt(args[i]);
                    if (nThreads < 1) {
                        throw new IllegalArgumentException("must have number of threads >= 1");
                    }
                } else if (args[i].equals("-s")) {
                    i++;
                    loadSize = Integer.parseInt(args[i]);
                    if (loadSize < 1) {
                        throw new IllegalArgumentException(
                                "must have coefficients vector size >= 1");
                    }
                } else if (args[i].equals("-a")) {
                    setAutoSizeAdjustmentAllowed(true);
                } else if (args[i].equals("-w")) {
                    i++;
                    monitorWait_ms = Long.parseLong(args[i]);
                    if (loadSize < 1) {
                        throw new IllegalArgumentException("must have wait >= 0 ms");
                    }
                } else if (args[i].equals("-sn")) {
                    showCompilationStats = false;
                    showOSStats = false;
                    showRuntimeStats = false;
                    showThreadStats = false;
                    showMemoryStats = false;
                    showPoolStats = false;
                } else if (args[i].equals("-sa")) {
                    showCompilationStats = true;
                    showOSStats = true;
                    showRuntimeStats = true;
                    showThreadStats = true;
                    showMemoryStats = true;
                    showPoolStats = true;
                } else if (args[i].equals("-sc")) {
                    showCompilationStats = true;
                } else if (args[i].equals("-so")) {
                    showOSStats = true;
                } else if (args[i].equals("-sr")) {
                    showRuntimeStats = true;
                } else if (args[i].equals("-st")) {
                    showThreadStats = true;
                } else if (args[i].equals("-sm")) {
                    showMemoryStats = true;
                } else if (args[i].equals("-sp")) {
                    showPoolStats = true;
                } else if (args[i].equals("-c")) {
                    i++;
                    targetCPUpercent = Integer.parseInt(args[i]);
                    if (targetCPUpercent > PERCENT_100 || targetCPUpercent < 0) {
                        throw new IllegalArgumentException("must have 0 <= CPU <= 100");
                    }
                } else if (args[i].equals("-q")) {
                    generateLogging = false;
                } else {
                    throw new IllegalArgumentException("Bad command line arguments");
                }
            }

        } catch (IllegalArgumentException e) {
            usage();
            System.exit(-1);
        }

        dumpSystemInformation();

        System.out.println("Hogging the CPU with " + nThreads + " java threads " + "doing "
                + loadSize + " element convolution.");
        System.out.print("Monitoring displayed every " + monitorWait_ms
                + "ms with target CPU usage of " + targetCPUpercent + "%");
        if (isAutoSizeAdjustmentAllowed()) {
            System.out.println(" and automtic load size adjustment");
        } else {
            System.out.println(". Load size is constant.");
        }

        monitorThread = new Thread(new MonitorThread(targetCPUpercent));
        loadThreads = new ArrayList<ThrashThread>(nThreads);

        monitorThread.start();

        for (int i = 0; i < nThreads; i++) {
            ThrashThread t = new ThrashThread();
            loadThreads.add(t);
            t.setPriority(t.getPriority() - 1); // minimise system killing ability?
            t.setDaemon(false); // slower exit - but everything is forced killed
            t.start();
        }

        System.out.println();
        System.out.println("All threads started.");
    }

    /**
     * Output version information.
     */
    // TODO Can't find a good way to do this yet
    // private static void dumpVersionInfo() {
    // Package p = CPUhog.class.getPackage();
    // System.out.println("${application.title}" + " by " + "${application.vendor}" + ". Version "
    // + p.getImplementationVersion());

    // }

    private static void usage() {
        System.err.println(" Usage:\n" + " java -jar CPUhog <options>\n" + "\n"
                + " The options can be specified in any order and later ones override earlier ones.\n"
                + " Available options are\n"
                + " -t nnn   Start load nnn threads (default 10).  Typically the main program\n"
                + "          runs in the initial thread and it starts a monitoring thread as\n"
                + "          well as the specified number of load threads.  You may see additional\n"
                + "          threads created by the JVM for system use.\n" + "\n"
                + " -s nnn   The size of the vector used during the convolution (default 100).\n"
                + "          Note that doubling the number coefficients will \n"
                + "          appoximately increase the load execution time by 4.\n" + "\n"
                + " -a       Permit the application to adjust the vector size automatically.\n"
                + "          Initially this will reduce the size when out of memory errors start to\n"
                + "          occur.  During adjustment the load may fluctuate.  Currently the\n"
                + "          size is not adjusted upwards so the -d option can be used to set an\n"
                + "          upper value.\n" + "\n"
                + " -w nnn   The amount of time (ms) to wait between log line outputs.\n" + "\n"
                + " -sn      No statistics.\n" + " -sa      All statistics\n"
                + " -sc      Compilation information\n"
                + " -so      Operating system information.  This is the only section output by default.\n"
                + " -sr      Runtime information (includes all java system properties)\n"
                + " -st      Thread information\n" + " -sm      Memory information\n"
                + " -sp      Memory pool information\n" + "\n"
                + " -c nnn   The target percentage of total CPU to use (integer - default 100).\n"
                + "          A delay within each load thread will be adjusted to bring the aggregate\n"
                + "          load on the system to the specified percentage. The granularity that\n"
                + "          the application can achieve will be determined by the size of load\n"
                + "          and the speed of CPU.  This also relies on the JVM / OS to spread the\n"
                + "          total load evenly (although this may be what you are testing!)\n"
                + "\n" + " -q       Supress logging information.\n" + "\n");
    }

    private static void dumpSystemInformation() {
        CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        List<MemoryManagerMXBean> memoryManagerMXBeans =
                ManagementFactory.getMemoryManagerMXBeans();
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

        if (showCompilationStats) {
            System.out.println("Compliation Information");

            System.out.println("JIT compiler name: " + compilationMXBean.getName());
            System.out.println("Total JIT compile time: "
                    + (compilationMXBean.isCompilationTimeMonitoringSupported()
                            ? compilationMXBean.getTotalCompilationTime()
                            : NOT_SUPPORTED));
            System.out.println();
        }

        if (showOSStats) {
            System.out.println("Operating System Information");
            System.out.println("OS name: " + operatingSystemMXBean.getName());
            System.out.println("OS version: " + operatingSystemMXBean.getVersion());
            System.out.println("Architecture: " + operatingSystemMXBean.getArch());
            System.out.println(
                    "Available processors: " + operatingSystemMXBean.getAvailableProcessors());
            System.out.println();
        }

        if (showRuntimeStats) {
            System.out.println("Runtime Information");
            System.out.println("---system properties---");
            Map<String, String> sortedProps =
                    new TreeMap<String, String>(runtimeMXBean.getSystemProperties());
            for (String key : sortedProps.keySet()) {
                System.out.println(">" + key + ": " + sortedProps.get(key));
            }

            System.out.println("---end of system properties---");
            System.out.println("JVM uptime/ms: " + runtimeMXBean.getUptime());
            System.out.println();
        }

        if (showThreadStats) {
            System.out.println("Thread Information");
            System.out.println("Thread count: " + threadMXBean.getThreadCount());
            System.out.println(
                    "Thread CPU time supported: " + threadMXBean.isThreadCpuTimeSupported());
            System.out.println("Thread CPU time enabled: " + threadMXBean.isThreadCpuTimeEnabled());
            System.out.println();
        }

        if (showMemoryStats) {
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
        }

        if (showPoolStats) {
            for (MemoryPoolMXBean mpmxb : memoryPoolMXBeans) {
                System.out.println("Memory Pool Information (Name: " + mpmxb.getName() + ")");
                System.out.println("Peak usage: " + mpmxb.getPeakUsage());
                System.out.println("Type: " + mpmxb.getType());
                System.out.println("Usage: " + mpmxb.getUsage());
                System.out.println("Usage threshold: "
                        + (mpmxb.isUsageThresholdSupported() ? mpmxb.getUsageThreshold()
                                : NOT_SUPPORTED));
                System.out.println();
            }
        }
    }
}
