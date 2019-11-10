package com.optum.ocr.util;

import java.util.List;

public class OcrObj {
    public String fileName;
//    public String fileHocr;
    public List<OcrPageObj> ocrPageObjs;

    public static class OcrPageObj {
        public int pageNum;
        public String pageFile;
//        public String pageHocr;
    }
}
