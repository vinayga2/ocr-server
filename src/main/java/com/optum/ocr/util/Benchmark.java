package com.optum.ocr.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Benchmark {
    Logger logger = Logger.getGlobal();

    Class cls;
    String methodName;
    long time;

    public Benchmark(Class cls) {
        this.cls = cls;
    }

    public void start(String methodName) {
        this.methodName = methodName;
        this.time = System.nanoTime();
    }

    public void log() {
        long elapseTime = System.nanoTime() - this.time;
        logger.log(Level.INFO, cls.getSimpleName() + "-" + this.methodName + " elapse nano time == " + elapseTime + " milli == " + (elapseTime / 1000000));
    }
}
