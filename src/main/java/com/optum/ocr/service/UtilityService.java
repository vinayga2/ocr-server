package com.optum.ocr.service;

import com.optum.ocr.util.AbstractImageReader;
import com.optum.ocr.util.AbstractPdf2Image;
import com.optum.ocr.util.FileObjectExtractor;
import com.optum.ocr.util.OcrObj;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class UtilityService {
    Tesseract instance = new Tesseract();

    @Autowired
    FileObjectExtractor fileObjectExtractor;

    public byte[] getPageImage(String folderOut, String faxfile, String page) throws InstantiationException, IllegalAccessException, IOException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        byte[] bytes = imageReader.getPageImage(folderOut, faxfile, page);
        return bytes;
    }

    public void ocrUpload(String folderIn, String folderOut, String folderDone, String tesseractFolder, MultipartFile file) throws InstantiationException, IllegalAccessException, IOException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        imageReader.uploadPdfImage(folderIn, folderOut, folderDone, tesseractFolder, file);
    }

    public byte[] ocr(MultipartFile file) throws InstantiationException, IllegalAccessException, IOException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        byte[] bytes = imageReader.extractSearchablePdf(instance, file);
        return bytes;
    }

    public String ocrBatch(String folderIn, String folderOut, String folderDone, String tesseractDate) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        String str = imageReader.doBatch(folderIn, folderOut, folderDone, tesseractDate);
        return str;
    }

    public List<String> ocrFiles(String folderOut) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        List<String> lst = imageReader.getAllFiles(folderOut);
        return lst;
    }

    public void archiveFile(String folderOut, String file) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        imageReader.archiveFile(folderOut, file);
    }

    public OcrObj readFile(String folderOut, String file) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        OcrObj ocrObj = imageReader.readFile(folderOut, file);
        return ocrObj;
    }

    public byte[] getSearchablePdf(String folderOut, String faxfile) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        byte[] bytes = imageReader.getSearchablePdf(folderOut, faxfile);
        return bytes;
    }

    public byte[] pdf2images(MultipartFile file) throws IllegalAccessException, IOException, InstantiationException {
        AbstractPdf2Image pdf2Image = (AbstractPdf2Image) fileObjectExtractor.getGroovyObject("Pdf2Images.groovy");
        byte[] bytes = pdf2Image.convert(file);
        return bytes;
    }

    public List<String> viewFaxOnQueue(String folderIn) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        List<String> lst = imageReader.viewFaxOnQueue(folderIn);
        return lst;
    }
}
