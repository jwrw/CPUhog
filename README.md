# CPUhog
*Hog the CPU*

 The CPUhog provides a simple way to load a multiprocessor / multithread machine
 to the specified degree.
 
 The application is written in Java so runs unmodified on a variety of platforms.
 
 It is self-contained and requires no additional special libraries.
 
 The system load is created using a number of Java threads. To fully load any
 environment the application requires that the Java Virtual Machine (JVM)
 used to run the application spreads the load across the available system
 processors / cores / CPU threads.
 
 The load uses a mixture of integer and double floating point arithmetic
 which provides a good load on many platforms.  However some CPU architectures
 with restricted availability of floating point hardware may find their integer
 components not fully utilised.  (A future version could do something about this).
 
 The application can also print out a great deal of diagnostic information
 about the system, the JVM and the environment. This information is largely
 derived from the java.lang.management 'MXBean' family of components.  A more
 comprehensive description of the statistics generated may be found there.
 
 Usage:
 ```
 java -jar CPUhog <options>
 ```
 The options can be specified in any order and later ones override earlier ones.
 Available options are:

|Option|Description| 
|-|-|
|`-t nnn`|Start load in `nnn` threads (default 10). Typically the main program runs in the initial thread and it starts a monitoring thread as well as the specified number of load threads.  You may see additional threads created by the JVM for system use. |
|`-s nnn`|The number of coefficients in the vector used during the convolution. The signal vector that the coefficients are applied against is a fixed multiple in size of the coefficient vector.  So doubling the number coefficients will nearly increase the load execution time by 4.
|`-a`|Permit the application to adjust the vector size automatically. Initially this will reduce the size when out of memory errors start to occur.  During adjustment the load may fluctuate.  Currently the size is not adjusted upwards so the -d option can be used to set an upper value.
|`-w nnn`|The amount of time (ms) to wait between log line outputs.|
|`-sn` |  No statistics.|
|`-sa` |  All statistics|
|`-sc` |  Compilation information|
|`-so` |  Operating system information.  This is the only |section output by default.
 |`-sr` |  Runtime information (includes all java system |properties)
|`-st` |  Thread information|
|`-sm` |  Memory information|
|`-sp` |  Memory pool information|
|`-c nnn`|The target percentage of total CPU to use (integer - default 100). A delay within each load thread will be adjusted to bring the aggregate load on the system to the specified percentage. The granularity that the application can achieve will be determined by the size of matrix and the speed of CPU.  This also relies on the JVM / OS to spread the total load evenly (although this may be what you are testing!) |
|`-q` |  Quiet - Suppress logging information.|
| | |
