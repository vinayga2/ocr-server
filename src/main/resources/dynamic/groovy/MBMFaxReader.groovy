package dynamic.groovy

import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.optum.ocr.util.*
import dynamic.groovy.mbm.ArchivePdf
import dynamic.groovy.mbm.PageHighlighter
import net.sourceforge.tess4j.Tesseract
import org.apache.pdfbox.pdmodel.PDDocument
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

class MBMFaxReader extends AbstractImageReader {

    void runBatch(String fIn, String fOut, String fDone, String tesseractDate) {
        Benchmark benchmark = new Benchmark(this.getClass());
        benchmark.start("RUN OCR BATCH");

        File folderIn = new File(fIn);
        File folderOut = new File(fOut);
        File folderDone = new File(fDone);

        java.util.List<File> listOfFiles = Arrays.asList(folderIn.listFiles());
        listOfFiles.stream().forEach({faxFile ->
            byte[] bytes = faxFile.getBytes();
            PDDocument document = PDDocument.load(bytes);
            java.util.List<ImageIndex> images = getImageIndex(document);
            document.close();

            File tmp = new File(folderOut, faxFile.getName());
            tmp.mkdir();

            images.parallelStream().forEach({ind ->
                try {
                    Logger.getGlobal().log(Level.INFO, "Aligning/Writing image "+faxFile.getName()+" - "+ind.imageIndex);
                    ind.image = OcrAlignImage.getAlignedImage( (BufferedImage) ind.image);

                    String fileImage = "img-"+ind.imageIndex+".jpg";
                    File fTmp = new File(tmp, fileImage);
                    ImageIO.write(ind.image, "jpg", fTmp);
                    ind.imgFile = fTmp;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            })
            images.parallelStream().forEach({ind ->
                if (ind.imgFile!=null) {
                    String fileHocr = "hocr-"+ind.imageIndex+".html";
                    Logger.getGlobal().log(Level.INFO, "Tesseract "+fileHocr);

                    Tesseract tesseract1 = new Tesseract();
                    tesseract1.setHocr(true);
                    tesseract1.setDatapath(tesseractDate);
                    String hocr = tesseract1.doOCR(ind.imgFile);
                    File fTmpHocr = new File(tmp, fileHocr);
                    Utils.writeToFile(fTmpHocr, hocr);
                    ind.fileHocr = fileHocr;
                }
            })
            try {
                createSearchablePdf(fOut, faxFile, images);
            } catch(Exception e) {
                e.printStackTrace();
            }
            File outFile = new File(folderDone, faxFile.getName());
            Files.deleteIfExists(outFile.toPath());
            Files.move(faxFile.toPath(), outFile.toPath());
        })
        Logger.getGlobal().log(Level.INFO, "########################################PDF Run Batch Complete########################################")
        Logger.getGlobal().log(Level.INFO, "PDF Count == "+listOfFiles.size());
        benchmark.log();
        Logger.getGlobal().log(Level.INFO, "Check timer");
    }

    void archiveFile(String folderOut, String file) {
        ArchivePdf archivePdf = new ArchivePdf();
        archivePdf.archiveFile(folderOut, file);
    }

    void createSearchablePdf(String fOut, File faxFile, java.util.List<ImageIndex> images) throws FileNotFoundException, DocumentException {
        anchorMap = new HashMap<>();
        anchorIndex = 0;
        File folderOut = new File(fOut);
        File tmp = new File(folderOut, faxFile.getName());

        File searchFile = new File(tmp, "Searchable-" + faxFile.getName());
        searchFile.delete();
        com.itextpdf.text.Document searchablePdf = new com.itextpdf.text.Document(new com.itextpdf.text.Rectangle(images.get(0).image.getWidth(), images.get(0).image.getHeight()));
        PdfWriter.getInstance(searchablePdf, new FileOutputStream(searchFile));
        searchablePdf.open();

        PageHighlighter highlighter = new PageHighlighter();
        highlighter.init(faxFile);
        images.stream().forEach({ind ->
            if (ind.fileHocr != null) {
                int index = ind.imageIndex;
                RenderedImage img = ind.image;
                String fileHocr = ind.fileHocr;
                try {
                    String hocr = Utils.readFile(tmp, fileHocr);
                    addSearchablePage(searchablePdf, hocr, img, myFont, index, highlighter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        highlighter.addLinkPage(searchablePdf);
        highlighter.addLinkImagePage(searchablePdf);
        searchablePdf.close();
    }

    void addSearchablePage(com.itextpdf.text.Document document, String hocr, RenderedImage image, Font font, int pageNum, PageHighlighter highlighter) throws ParserConfigurationException, SAXException, DocumentException, IOException {
        document.setPageSize(new Rectangle(image.getHeight(), image.getWidth()));
        document.newPage();

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
