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

    public byte[] getPageImage(String companyCode, String ocrFolder, String faxfile, String page) throws InstantiationException, IllegalAccessException, IOException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        byte[] bytes = imageReader.getPageImage(companyCode, ocrFolder, faxfile, page);
        return bytes;
    }

    public void ocrUpload(String companyCode, String ocrFolder, String tesseractFolder, MultipartFile file) throws InstantiationException, IllegalAccessException, IOException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        imageReader.uploadPdfImage(companyCode, ocrFolder, tesseractFolder, file);
    }

    public byte[] ocr(String companyCode, String ocrFolder, MultipartFile file) throws InstantiationException, IllegalAccessException, IOException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        byte[] bytes = imageReader.extractSearchablePdf(companyCode, ocrFolder, instance, file);
        return bytes;
    }

    public List<String> ocrFiles(String companyCode, String ocrFolder) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        List<String> lst = imageReader.getAllFiles(companyCode, ocrFolder);
        return lst;
    }

    public void archiveFile(String companyCode, String ocrFolder, String file) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        imageReader.archiveFile(companyCode, ocrFolder, file);
    }

    public OcrObj readFile(String companyCode, String ocrFolder, String file) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        OcrObj ocrObj = imageReader.readFile(companyCode, ocrFolder, file);
        return ocrObj;
    }

    public byte[] getSearchablePdf(String companyCode, String ocrFolder, String faxfile) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        byte[] bytes = imageReader.getSearchablePdf(companyCode, ocrFolder, faxfile);
        return bytes;
    }

    public byte[] pdf2images(MultipartFile file) throws IllegalAccessException, IOException, InstantiationException {
        AbstractPdf2Image pdf2Image = (AbstractPdf2Image) fileObjectExtractor.getGroovyObject("Pdf2Images.groovy");
        byte[] bytes = pdf2Image.convert(file);
        return bytes;
    }

    public List<String> viewFaxOnQueue(String companyCode, String ocrFolder) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        List<String> lst = imageReader.viewFaxOnQueue(companyCode, ocrFolder);
        return lst;
    }

    public String createSearchable(String companyCode, String ocrFolder, String file) throws IllegalAccessException, IOException, InstantiationException {
        AbstractImageReader imageReader = (AbstractImageReader) fileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
        String str = imageReader.createSearchable(companyCode, ocrFolder, file);
        return str;
    }
}
