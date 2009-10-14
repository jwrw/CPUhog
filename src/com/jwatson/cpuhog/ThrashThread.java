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
     * It uses two double vecors and avoids object creation
     * and destruction (and garbage collection).
     * Memory footprint should remain fairly static during execution.
     *
     * The coefs are scaled to ensure they average 1.0
     * The signal vector is re-randomised periodically to stop it getting too big or small
     *
     * If the required loadSize is changed then new vectors are created
     * 
     */
    @Override
    public final void run() {
        while (true) {
            double[] coefs = new double[CPUhog.loadSize];
            double[] signal = new double[CPUhog.loadSize * CPUhog.SIGNAL_FACTOR];

            fillCoefs(coefs);

sizeChanged:
            while (true) {
                randomiseSignal(signal);

                for (int i = 0; i < 100; i++) {
                    long t0 = System.nanoTime();

                    convolve(coefs, signal);

                    loopTime_ns = (System.nanoTime() - t0);

                    if (coefs.length != CPUhog.loadSize) {
                        break sizeChanged;
                    }

                    if (CPUhog.loadWait_ms > 0) {
                        synchronized (this) {
                            try {
                                this.wait(CPUhog.loadWait_ms);
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                }
            }
        }
    }

    private void fillCoefs(double[] coefs) {
        double sum = 0;
        for (int i = 0; i < coefs.length; i++) {
            sum += coefs[i] = Math.random();
        }
        double scale = coefs.length / sum;
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] *= scale;
        }
    }

    private void randomiseSignal(double[] signal) {
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
    }

    /**
     * This function represents the load - it is this routine that
     * is timed to give the 'load time' or 'loop time'
     *
     * @param coefs
     * @param signal
     */
    private void convolve(double[] coefs, double[] signal) {
        for (int off = 0; off < signal.length - coefs.length; off++) {
            double sum = 0;
            for (int i = 0; i < coefs.length; i++) {
                sum += signal[i + off] * coefs[i];
            }
            signal[off] = sum;
        }



    }
}
