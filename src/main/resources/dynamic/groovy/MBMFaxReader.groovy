package dynamic.groovy

import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfWriter
import com.lowagie.text.pdf.ArabicLigaturizer
import com.optum.ocr.util.*
import dynamic.groovy.mbm.ArchivePdf
import dynamic.groovy.mbm.PageHighlighter
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.Tesseract1
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.web.multipart.MultipartFile
import org.w3c.dom.*
import org.xml.sax.SAXException

import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.nio.file.Files
import java.util.List
import java.util.logging.Level
import java.util.logging.Logger
import com.optum.ocr.service.*

import java.util.stream.IntStream;

class MBMFaxReader extends AbstractImageReader {
    static List<Tesseract1> tesseract1List = new ArrayList<>();

    @Override
    void uploadPdfImage(String fIn, String folderOut, String folderDone, String tesseractFolder, MultipartFile file) throws IOException {
        super.uploadPdfImage(fIn, folderOut, folderDone, tesseractFolder, file);
        File faxFile = new File(fIn, file.getOriginalFilename());
        Thread thread = new Thread(new Runnable() {
            @Override
            void run() {
                runSingle(folderOut, folderDone, tesseractFolder, faxFile);
            }
        });
        thread.start();
    }

    void runSingle(String folderOut, String folderDone, String tesseractFolder, File faxFile) {
        Benchmark benchmark = new Benchmark(this.getClass());
        benchmark.start("RUN OCR BATCH");

        byte[] bytes = faxFile.getBytes();
        PDDocument document = PDDocument.load(bytes);
        List<ImageIndex> images = getImageIndex(document);
        document.close();

        String notif = "<li>    Converting "+faxFile.name+" with "+images.size()+" pages.</li>";
        PushService.broadcast("OCR", notif);
        File tmp = new File(folderOut, faxFile.getName());
        tmp.mkdir();

        Logger.getGlobal().log(Level.INFO, "Init Tesseract List");
        if (tesseract1List.isEmpty()) {
            IntStream.range(1, 20).forEach({
                Tesseract1 tesseract = new Tesseract1();
                tesseract.setHocr(true);
                tesseract.setDatapath(tesseractFolder);
                tesseract1List.add(tesseract);
            });
        }
        images.parallelStream().forEach({ind ->
            try {
//                ind.image = OcrAlignImage.getAlignedImage( (BufferedImage) ind.image);
                String fileImage = "img-"+ind.imageIndex+".jpg";
                File fTmp = new File(tmp, fileImage);
                ImageIO.write(ind.image, "jpg", fTmp);
                ind.imgFile = fTmp;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        })
        images.stream().forEach({ind ->
            try {
                String fileHocr = "hocr-"+ind.imageIndex+".html";
                File fTmpHocr = new File(tmp, fileHocr);
                Logger.getGlobal().log(Level.INFO, "Tesseract "+fileHocr);

                Tesseract1 tesseract = tesseract1List.remove(0);
                String hocr = tesseract.doOCR(ind.imgFile);
                Utils.writeToFile(fTmpHocr, hocr);
                ind.fileHocr = fileHocr;

                tesseract1List.add(tesseract);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        })
        File outFile = new File(folderDone, faxFile.getName());
        Files.deleteIfExists(outFile.toPath());
        Files.move(faxFile.toPath(), outFile.toPath());
        benchmark.log();
    }

    void runBatch(String fIn, String folderOut, String folderDone, String tesseractFolder) {
        Benchmark benchmark = new Benchmark(this.getClass());
        benchmark.start("RUN OCR BATCH");

        File folderIn = new File(fIn);

        String str = "";
        java.util.List<File> listOfFiles = Arrays.asList(folderIn.listFiles()).stream().filter({
            file ->
                boolean b = file.name.startsWith("NRS") || file.name.startsWith("KRS") || file.name.startsWith("OHUM");
                if (b) {
                    str += "<br>"+file.name;
                }
                return b;
        }).collect();
        if (!str.isEmpty()) {
            str = "<li>Batch Started for the following files ["+PushService.notificationTime()+"] .... "+str+"</li>";
            PushService.broadcast("OCR", str);
        }
        listOfFiles.stream().forEach({faxFile ->
            runSingle(folderOut, folderDone, tesseractFolder, faxFile);
        })
        Logger.getGlobal().log(Level.INFO, "########################################PDF Run Batch Complete########################################")
        Logger.getGlobal().log(Level.INFO, "PDF Count == "+listOfFiles.size());
        benchmark.log();
        Logger.getGlobal().log(Level.INFO, "Check timer");
        PushService.broadcast("OCR", "Conversion Complete.");
    }

    void archiveFile(String folderOut, String file) {
        ArchivePdf archivePdf = new ArchivePdf();
        archivePdf.archiveFile(folderOut, file);
    }

    @Override
    List<String> viewFaxOnQueue(String fIn) {
        File folderIn = new File(fIn);
        String[] arr = folderIn.list();
        List<String> lst = Arrays.asList(arr);
        return lst;
    }

    @Override
    String createSearchable(String folderOut, String faxFile) {
        File folder = new File(folderOut, faxFile);
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
        createSearchablePdf(folderOut, faxFile, images);
    }

    void createSearchablePdf(String fOut, String faxFilename, List<ImageIndex> images) throws FileNotFoundException, DocumentException {
        anchorMap = new HashMap<>();
        anchorIndex = 0;
        File folderOut = new File(fOut);
        File tmp = new File(folderOut, faxFilename);

        File searchFile = new File(tmp, "Searchable-" + faxFilename);
        searchFile.delete();
        com.itextpdf.text.Document searchablePdf = new com.itextpdf.text.Document(new com.itextpdf.text.Rectangle(images.get(0).image.getWidth(), images.get(0).image.getHeight()));
        PdfWriter writer = PdfWriter.getInstance(searchablePdf, new FileOutputStream(searchFile));
        searchablePdf.open();

        PageHighlighter highlighter = new PageHighlighter();
        highlighter.init(faxFilename);
        images.stream().forEach({ind ->
            if (ind.fileHocr != null) {
                int index = ind.imageIndex;
                RenderedImage img = ind.image;
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
        Rectangle rec = new Rectangle(image.getWidth() * 2, image.getHeight());
        document.setPageSize(rec);
        document.newPage();
        addBackground(writer, image, rec);

        Paragraph paragraph = new Paragraph();
        Anchor anchor = createTarget("This page is not yet extracted.", anchorIndex++);
        paragraph.add(anchor);

        document.add(paragraph);
    }

    void addSearchablePage(PdfWriter writer, com.itextpdf.text.Document document, String hocr, RenderedImage image, Font font, int pageNum, PageHighlighter highlighter) throws ParserConfigurationException, SAXException, DocumentException, IOException {
        Rectangle rec = new Rectangle(image.getWidth() * 2, image.getHeight());
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
            float x = Float.parseFloat(title[1]);
            if (x >= 500) {
                x = 300;
            }
            float y = Float.parseFloat(title[2]);

            Paragraph paragraph = new Paragraph();
            paragraph = getLineText(element, paragraph, pageNum, image, highlighter);

            paragraph.setIndentationLeft(x);
            document.add(paragraph);
        }
    }

    void addBackground(PdfWriter writer, RenderedImage image, Rectangle rec) {
        PdfContentByte canvas = writer.getDirectContentUnder();
        Image img = Image.getInstance(Utils.toByteArray(image));
        img.scaleToFit(rec);
        img.setAbsolutePosition((int)(rec.width / 2), 0);
        canvas.addImage(img);
    }

    Paragraph getLineText(Node ocrLine, Paragraph paragraph, int pageNum, RenderedImage image, PageHighlighter highlighter) {
        NodeList nodeList = ((org.w3c.dom.Element)ocrLine).getElementsByTagName("span");
        for (int i=0; i<nodeList.getLength(); i++){
            org.w3c.dom.Element element = (org.w3c.dom.Element) nodeList.item(i);
            String className = element.getAttribute("class");
            if ("ocrx_word".equalsIgnoreCase(className)) {
                String str = getEndString(element);
                if (highlighter.existInHighlight(str, pageNum)) {
                    anchorIndex++;
                    highlighter.createAnchorForGlossary(str, anchorIndex, pageNum);
                    highlighter.createAnchorImageForGlossary(str, anchorIndex, pageNum, element, image);
                    Anchor anchor = createTarget(str, anchorIndex);
                    paragraph.add(anchor);
                }
                else {
                    Phrase normal = new Phrase(str+" ", normalFont);
                    paragraph.add(normal);
                }
            }
        }
        return paragraph;
    }

}
