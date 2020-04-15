package com.optum.ocr.util;

import net.sourceforge.tess4j.Tesseract1;

public class Tesseract1Pool extends ObjectPool {
    static Tesseract1Pool tessPool = null;

    private String tesseractFolder;

    public static Tesseract1Pool getInstance() {
        return tessPool;
    }

    public static void initTessPool(String tesseractFolder, int poolCount) {
        if (tessPool == null) {
            Tesseract1Pool tess = new Tesseract1Pool(tesseractFolder, poolCount);
            tessPool = tess;
        }
    }

    private Tesseract1Pool(String tesseractFolder, int poolCount) {
        this.tesseractFolder = tesseractFolder;
        this.init(poolCount);
    }

    @Override
    protected ObjectWithIndex create(int index) {
        Benchmark benchmark = new Benchmark(this.getClass());
        benchmark.start("POOL CREATE TESSERACT INSTANCE");
        Tesseract1 tesseract = new Tesseract1();
        tesseract.setHocr(true);
        tesseract.setDatapath(tesseractFolder);
        ObjectWithIndex tesseractWithIndex = new ObjectWithIndex();
        tesseractWithIndex.index = index;
        tesseractWithIndex.obj = tesseract;
        benchmark.log();
        return tesseractWithIndex;
    }

    public synchronized ObjectWithIndex checkOut() {
        ObjectWithIndex t = super.checkOut();
        if (t == null) {
            t = create(pool.size());
        }
        return t;
    }

}