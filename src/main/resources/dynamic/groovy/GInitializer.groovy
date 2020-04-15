package dynamic.groovy

import com.optum.ocr.config.InitializerConfig
import com.optum.ocr.util.AbstractImageReader
import com.optum.ocr.util.Tesseract1Pool
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.springframework.web.multipart.MultipartFile

class GInitializer extends InitializerConfig {
    void initialize() throws IllegalAccessException, IOException, InstantiationException {
        initAllPendingFiles();
    }

    void initAllPendingFiles() {
        File rootFolder = new File(InitializerConfig.OcrFolder);
        Collection<File> files = FileUtils.listFilesAndDirs(rootFolder, new WildcardFileFilter("*"), new WildcardFileFilter("*"));
        for (File file:files) {
            if (file.isDirectory() && file.getName().equals("out")) {
                File[] pdfFiles = file.listFiles();
                for (File pdf:pdfFiles) {
                    if (!pdf.getName().startsWith("Archived-")) {
                        File completedFile = new File(pdf, "Completed-"+pdf.getName());
                        if (!completedFile.exists()) {
                            String outPath = pdf.getAbsolutePath();
                            String inPath = outPath.replaceAll("out", "in");
                            File inFile = new File(inPath);
                            System.out.println("outPath = "+outPath);
                            System.out.println("inPath = "+inPath);
                            System.out.println("Run for "+pdf.getName());

                            String companyCode = null;
                            if (outPath.contains("/")) {
                                String[] arr = outPath.split("/");
                                System.out.println("arr "+arr);
                                companyCode = arr[arr.length-3];
                            }
                            else {
                                String[] arr = outPath.split("\\\\");
                                System.out.println("arr "+arr);
                                companyCode = arr[arr.length-3];
                            }
                            System.out.println("companyCode "+companyCode);

                            AbstractImageReader imageReader = (AbstractImageReader) InitializerConfig.FileObjectExtractor.getGroovyObject("MBMFaxReader.groovy");
                            imageReader.processPdfImage(companyCode, InitializerConfig.OcrFolder, InitializerConfig.TesseractFolder, inFile);
                        }
                    }
                }
            }
        }
    }
}
