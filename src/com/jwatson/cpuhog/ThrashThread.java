package com.jwatson.cpuhog;

import com.jwatson.fastmatrix.Matrix;

class ThrashThread implements Runnable {

    public void run() {
        while (true) {
            double[][] aData = new double[CPUhog.mSize][CPUhog.mSize];
            double[][] bData = new double[CPUhog.mSize][CPUhog.mSize];
            for (int r = 0; r < CPUhog.mSize; r++) {
                for (int c = 0; c < CPUhog.mSize; c++) {
                    aData[r][c] = Math.random();
                    bData[r][c] = Math.random();
                }
                Matrix a = new Matrix(aData);
                Matrix b = new Matrix(bData);
                for (int i = 0; i < CPUhog.NLOOPS; i++) {
                    Matrix c = a.postMultiply(b);
                    a = b;
                    b = c;
                }
            }
        }
    }
}
