package dynamic.groovy

import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfWriter
import com.optum.ocr.config.InitializerConfig
import com.optum.ocr.util.*
import dynamic.groovy.mbm.ArchivePdf
import dynamic.groovy.mbm.PageHighlighter
import net.sourceforge.tess4j.Tesseract1
import org.apache.commons.io.FileUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.core.io.ByteArrayResource
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile
import org.w3c.dom.*
import org.xml.sax.SAXException

import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.nio.file.Files
import java.util.List
import java.util.concurrent.ForkJoinPool
import java.util.logging.Level
import java.util.logging.Logger
import com.optum.ocr.service.*

import java.util.stream.Collectors

class MBMFaxReader extends AbstractImageReader {
    int wordCounter = 0;
    static Graphics graphics;

    @Override
    void processPdfImage(String companyCode, String ocrFolder, String tesseractFolder, File faxFile) throws IOException {
        Thread thread = new Thread(new Runnable() {
            @Override
            void run() {
                runSingle(companyCode, ocrFolder, tesseractFolder, faxFile);
            }
        });
        thread.start();
    }

    @Override
    void uploadPdfImage(String companyCode, String ocrFolder, String tesseractFolder, MultipartFile file) throws IOException {
        super.uploadPdfImage(companyCode, ocrFolder, tesseractFolder, file);

        File fIn = getFolder(companyCode, ocrFolder, "in");
        File faxFile = new File(fIn, file.getOriginalFilename());
        Thread thread = new Thread(new Runnable() {
            @Override
            void run() {
                runSingle(companyCode, ocrFolder, tesseractFolder, faxFile);
            }
        });
        thread.start();
    }

    void runSingle(String companyCode, String ocrFolder, String tesseractFolder, File faxFile) {
        Benchmark benchmark = new Benchmark(this.getClass());
        benchmark.start("RUN OCR BATCH");

        byte[] bytes = faxFile.getBytes();
        PDDocument document = PDDocument.load(bytes);
        List<ImageIndex> images = getImageIndex(document);
        document.close();

        File folderOut = getFolder(companyCode, ocrFolder, "out");
        String notif = "<li>    Converting "+faxFile.name+" with "+images.size()+" pages.</li>";
        PushService.broadcast("OCR", notif);
        File tmp = new File(folderOut, faxFile.getName());
        tmp.mkdir();

        Logger.getGlobal().log(Level.INFO, "Init Tesseract List");
        Logger.getGlobal().log(Level.INFO, "Extracting "+faxFile.getName());
        images.parallelStream().forEach({ind ->
            try {
                ind.image = preProcess( (BufferedImage) ind.image);
                String fileImage = "img-"+ind.imageIndex+".jpg";
                File fTmp = new File(tmp, fileImage);
                ImageIO.write(ind.image, "jpg", fTmp);
                ind.imgFile = fTmp;

                if (InitializerConfig.UseTesseractService) {
                    runTesseractService(ind, tesseractFolder, tmp);
                }
                else {
                    runTesseract(ind, tesseractFolder, tmp);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        })

        File folderDone = getFolder(companyCode, ocrFolder, "done");
        File outFile = new File(folderDone, faxFile.getName());
        Files.deleteIfExists(outFile.toPath());
        Files.move(faxFile.toPath(), outFile.toPath());
        createSearchable(companyCode, ocrFolder, faxFile.getName());

        File finalFolderTmp = new File(folderOut, faxFile.getName());
        File sourceFile = new File(finalFolderTmp, "Searchable-" + faxFile.getName());
        File destFile = new File(finalFolderTmp, "Completed-" + faxFile.getName());
        FileUtils.copyFile(sourceFile, destFile);

        benchmark.log();
    }

    void runSingleOld(String companyCode, String ocrFolder, String tesseractFolder, File faxFile) {
        Benchmark benchmark = new Benchmark(this.getClass());
        benchmark.start("RUN OCR BATCH");

        byte[] bytes = faxFile.getBytes();
        PDDocument document = PDDocument.load(bytes);
        List<ImageIndex> images = getImageIndex(document);
        document.close();

        File folderOut = getFolder(companyCode, ocrFolder, "out");
        String notif = "<li>    Converting "+faxFile.name+" with "+images.size()+" pages.</li>";
        PushService.broadcast("OCR", notif);
        File tmp = new File(folderOut, faxFile.getName());
        tmp.mkdir();

        Logger.getGlobal().log(Level.INFO, "Init Tesseract List");
        Logger.getGlobal().log(Level.INFO, "Extracting "+faxFile.getName());
        images.parallelStream().forEach({ind ->
            try {
                ind.image = preProcess( (BufferedImage) ind.image);
                String fileImage = "img-"+ind.imageIndex+".jpg";
                File fTmp = new File(tmp, fileImage);
                ImageIO.write(ind.image, "jpg", fTmp);
                ind.imgFile = fTmp;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        })

        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println("CPU Cores == "+processors);
        int parallelism = (int) (processors * 0.70);
        ForkJoinPool forkJoinPool = new ForkJoinPool(parallelism);
        if (images.size() > parallelism) {
            for (int k=0; k<3; k++) {
                int sizePass = parallelism * (k+1);
                System.out.println("sizePass == "+sizePass);
                List<ImageIndex> lst = new ArrayList<>();
                for (int i=0; i<sizePass; i++) {
                    if (!images.isEmpty()) {
                        lst.add(images.remove(0));
                    }
                }
                forkJoinPool.submit { any ->
                    lst.stream().parallel().forEach({ ind ->
                        if (InitializerConfig.UseTesseractService) {
                            runTesseractService(ind, tesseractFolder, tmp);
                        }
                        else {
                            runTesseract(ind, tesseractFolder, tmp);
                        }
                    })
                }.join();
            }
        }

        forkJoinPool.submit { any ->
            images.stream().parallel().forEach({ ind ->
                if (InitializerConfig.UseTesseractService) {
                    runTesseractService(ind, tesseractFolder, tmp);
                }
                else {
                    runTesseract(ind, tesseractFolder, tmp);
                }
            });
        }.join();

        File folderDone = getFolder(companyCode, ocrFolder, "done");
        File outFile = new File(folderDone, faxFile.getName());
        Files.deleteIfExists(outFile.toPath());
        Files.move(faxFile.toPath(), outFile.toPath());
        createSearchable(companyCode, ocrFolder, faxFile.getName());

        File finalFolderTmp = new File(folderOut, faxFile.getName());
        File sourceFile = new File(finalFolderTmp, "Searchable-" + faxFile.getName());
        File destFile = new File(finalFolderTmp, "Completed-" + faxFile.getName());
        FileUtils.copyFile(sourceFile, destFile);

        benchmark.log();
    }

    BufferedImage preProcess(BufferedImage bufferedImage) {
        AbstractImageReader imageReader = (AbstractImageReader) new FileObjectExtractor().getGroovyObject("PreProcessImage.groovy");
        BufferedImage bImage = imageReader.preProcess(bufferedImage);
        return bImage;
    }

    void runTesseractService(ImageIndex ind, String tesseractFolder, File tmp) {
        Benchmark benchmark = new Benchmark(this.getClass());
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();

        String filename = "img-"+ind.imageIndex+".jpg";
        benchmark.start("RUN TESSERACT SERVICE for "+filename);
        File imgFile = new File(tmp, filename);
        byte[] fileContents = imgFile.getBytes();
        ByteArrayResource contentsAsResource = new ByteArrayResource(fileContents) {
            @Override
            public String getFilename() {
                return filename; // Filename has to be returned in order to be able to post.
            }
        };

        map.add("name", filename);
        map.add("filename", filename);
        map.add("file", contentsAsResource);

        // Now you can send your file along.
        String result = restTemplate.postForObject(InitializerConfig.TesseractServiceUrl, map, String.class);

        File hocrFile = new File(tmp, "hocr-"+ind.imageIndex+".html");
        Utils.writeToFile(hocrFile, result);
        benchmark.log();
    }

    void runTesseract(ImageIndex ind, String tesseractFolder, File tmp) {
        Benchmark benchmark = new Benchmark(this.getClass());
        ObjectPool.ObjectWithIndex tess = Tesseract1Pool.getInstance().checkOut();
        try {
            String fileHocr = "hocr-"+ind.imageIndex+".html";
            File fTmpHocr = new File(tmp, fileHocr);
            benchmark.start("RUN TESSERACT for "+fileHocr);
            Tesseract1 tesseract = (Tesseract1) tess.obj;

            String hocr = tesseract.doOCR(ind.imgFile);
            Utils.writeToFile(fTmpHocr, hocr);
            ind.fileHocr = fileHocr;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            Tesseract1Pool.getInstance().checkIn(tess);
        }
        benchmark.log();
    }

    @Override
    void archiveFile(String companyCode, String ocrFolder, String file) {
        File folderOut = getFolder(companyCode, ocrFolder, "out");

        ArchivePdf archivePdf = new ArchivePdf();
        archivePdf.archiveFile(folderOut.getAbsolutePath(), file);
    }

    @Override
    List<String> viewFaxOnQueue(String companyCode, String ocrFolder) {
        File folderIn = getFolder(companyCode, ocrFolder, "in");
        String[] arr = folderIn.list();
        List<String> lst = Arrays.asList(arr);
        return lst;
    }

    @Override
    String createSearchable(String companyCode, String ocrFolder, String faxFile) {
        wordCounter = 0;
        File folderOut = getFolder(companyCode, ocrFolder, "out");
        File folder = new File(folderOut, faxFile);
        File completedFile = new File(folder, "Completed-" + faxFile);
        boolean completedFileExists = completedFile.exists();
        if (!completedFileExists) {
            String[] fImages = folder.list(new FilenameFilter() {
                @Override
                boolean accept(File file, String s) {
                    return s.startsWith("img-");
                }
            });
            Arrays.sort(fImages);
            List<ImageIndex> images = new ArrayList<>();
            Arrays.asList(fImages).stream().forEach({ imgFile ->
                String index = imgFile.replaceAll("img-", "").replaceAll(".jpg", "");
                String hocrFile = "hocr-" + index + ".html";
                Logger.getGlobal().log(Level.INFO, imgFile);
                ImageIndex ind = new ImageIndex();
                ind.imgFile = new File(folder, imgFile);
                ind.imageIndex = Integer.parseInt(index);
                ind.image = Utils.toBufferedImage(ind.imgFile);
                ind.fileHocr = hocrFile;
                images.add(ind);
            });
            createSearchablePdf(companyCode, ocrFolder, faxFile, images);
            System.out.println("wordCounter == "+wordCounter)
        }
    }

    void createSearchablePdf(String companyCode, String ocrFolder, String faxFilename, List<ImageIndex> images) throws FileNotFoundException, DocumentException {
        anchorMap = new HashMap<>();
        anchorIndex = 0;
        File folderOut = getFolder(companyCode, ocrFolder, "out");
        File tmp = new File(folderOut, faxFilename);

        File searchFile = new File(tmp, "Searchable-" + faxFilename);
        searchFile.delete();
        com.itextpdf.text.Document searchablePdf = new com.itextpdf.text.Document(new com.itextpdf.text.Rectangle(images.get(0).image.getWidth(), images.get(0).image.getHeight()));
        PdfWriter writer = PdfWriter.getInstance(searchablePdf, new FileOutputStream(searchFile));
        searchablePdf.open();

        PageHighlighter highlighter = new PageHighlighter(companyCode, faxFilename);
        images.stream().forEach({ind ->
            if (ind.fileHocr != null) {
                int index = ind.imageIndex;
                RenderedImage img = ind.image;
                if (graphics == null) {
                    graphics = ((BufferedImage) img).createGraphics();
                }
                String fileHocr = ind.fileHocr;
                try {
                    File hocrFile = new File(tmp, fileHocr);
                    if (hocrFile.exists()) {
                        String hocr = Utils.readFile(tmp, fileHocr);
                        addSearchablePage(writer, searchablePdf, hocr, img, myFont, index, highlighter);
                    }
                    else {
                        addBackGroundPage(writer, searchablePdf, img, index);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        highlighter.addLinkPage(searchablePdf);
        highlighter.addLinkImagePage(searchablePdf);
        searchablePdf.close();
    }

    void addBackGroundPage(PdfWriter writer, com.itextpdf.text.Document document, RenderedImage image, int pageNum) {
        Rectangle rec = new Rectangle(image.getWidth() * 1.2, image.getHeight());
        document.setPageSize(rec);
        document.newPage();
        addBackground(writer, image, rec);

        Paragraph paragraph = new Paragraph();
        Anchor anchor = createTarget("This page is not yet extracted.", anchorIndex++, 12);
        paragraph.add(anchor);

        document.add(paragraph);
    }

    void addSearchablePage(PdfWriter writer, com.itextpdf.text.Document document, String hocr, RenderedImage image, Font font, int pageNum, PageHighlighter highlighter) throws ParserConfigurationException, SAXException, DocumentException, IOException {
        Rectangle rec = new Rectangle(image.getWidth() * 1.2, image.getHeight());
        document.setPageSize(rec);
        document.newPage();
        addBackground(writer, image, rec);

        String xml = hocr.substring(hocr.indexOf('\n')+1);
        InputStream stream = new ByteArrayInputStream(xml.getBytes());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(stream);
        doc.getDocumentElement().normalize();

        List<Node> nodeList = getOcrLines(doc);
        for (int i=0; i<nodeList.size(); i++) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) nodeList.get(i);
            String[] title = element.getAttribute("title").split(" ");
            float x1 = Float.parseFloat(title[1]);

            Paragraph paragraph = new Paragraph();
            paragraph = getLineText(x1, element, paragraph, pageNum, image, highlighter);

            document.add(paragraph);
        }
    }

    void addBackground(PdfWriter writer, RenderedImage image, Rectangle rec) {
        PdfContentByte canvas = writer.getDirectContentUnder();
        Image img = Image.getInstance(Utils.toByteArray(image));

        Rectangle scaleRec = new Rectangle( (int)(rec.width * 0.5), rec.height );
        img.scaleToFit(scaleRec);
        img.setAbsolutePosition((int)(rec.width / 2), (int) (rec.height * 0.7) / 2);
        canvas.addImage(img);
    }

    Paragraph getLineText(float indentX, Node ocrLine, Paragraph paragraph, int pageNum, RenderedImage image, PageHighlighter highlighter) {
        NodeList nodeList = ((org.w3c.dom.Element)ocrLine).getElementsByTagName("span");

        String useSpaces = "                                                                                                                                                                                                                                                                                                                            ";
        int spaceCount = countSpaces(0, indentX);
        Phrase startSpaces = new Phrase(useSpaces.substring(0, spaceCount), normalFont);
        paragraph.add(startSpaces);

        float oldPositionX = indentX;
        String oldStr = "";
        for (int i=0; i<nodeList.getLength(); i++){
            org.w3c.dom.Element element = (org.w3c.dom.Element) nodeList.item(i);
            String className = element.getAttribute("class");
            if ("ocrx_word".equalsIgnoreCase(className)) {
                String str = getEndString(element);
                String[] title = element.getAttribute("title").split(" ");
                float positionX = Float.parseFloat(title[1]);
                float positionY1 = Float.parseFloat(title[2]);
                float positionY2 = Float.parseFloat(title[4].replaceAll(";", ""));
                int height = (int) (positionY2 - positionY1);
                if (height < 12) {
                    height = 12;
                }
                else if (height > 20) {
                    height = 20;
                }
                int confidence = Integer.parseInt(title[6]);

                spaceCount = countSpaces(oldStr, height, oldPositionX, positionX);
                oldPositionX = positionX;
                oldStr = str;

                if (highlighter.existInHighlight(str, pageNum)) {
                    anchorIndex++;
                    highlighter.createAnchorForGlossary(str, anchorIndex, pageNum);
                    highlighter.createAnchorImageForGlossary(str, anchorIndex, pageNum, element, image);
                    Anchor anchor = createTarget(" "+str, anchorIndex, height);
                    paragraph.add(anchor);
                }
                else {
                    if (confidence < 30) {
                        Phrase normal = new Phrase(useSpaces.substring(0, spaceCount)+str, normalMidConfidenceFont);
                        paragraph.add(normal);
                    } else {
                        Phrase normal = new Phrase(useSpaces.substring(0, spaceCount)+str, normalFont);
                        paragraph.add(normal);
                        wordCounter++;
                    }
                }
            }
        }
        return paragraph;
    }

    int countSpaces(String str, int fontHeight, float oldPositionX, float offsetX) {
        int spaces = -1;
        if (offsetX <= oldPositionX) {
            spaces = 1;
        }
        else {
            int countPixel = str.length() * fontHeight;
            int newOffset = (int) (oldPositionX + countPixel);
            int remainingPixels = (int) (offsetX - newOffset);
            spaces = (int) (remainingPixels / fontHeight);
        }
        return spaces >= 1?spaces:1;
    }

    int countSpaces(float oldPositionX, float offsetX) {
        int spaces = (int) ((offsetX - oldPositionX) / 7);
        return spaces;
    }
}
