package com.optum.ocr.util;

import com.google.common.io.Files;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.Data;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractImageReader {
    protected boolean batchRunning = false;
    protected static com.itextpdf.text.Font myFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 10, com.itextpdf.text.Font.NORMAL, BaseColor.BLACK);
    public static com.itextpdf.text.Font boldFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 14, com.itextpdf.text.Font.BOLD, BaseColor.BLUE);
    protected static com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.TIMES_ROMAN, 12, com.itextpdf.text.Font.NORMAL);
    protected Map<String, List<Anchor>> anchorMap = new HashMap<>();
    protected int anchorIndex = 0;

    public abstract void runBatch(String fIn, String fOut, String fDone, String tesseractDate);

    public String doBatch(String fIn, String fOut, String fDone, String tesseractDate) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                batchRunning = true;
                try {
                    runBatch(fIn, fOut, fDone, tesseractDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                batchRunning = false;
            }
        });
        if (batchRunning == false) {
            t.start();
            return "Start Run Batch";
        } else {
            return "Run Batch Still Running";
        }
    }

    public List<RectString> readLines(Tesseract instance, MultipartFile file) throws IOException {
        return null;
    }

    public List<String> getAllFiles(String fOut) {
        File folderOut = new File(fOut);

        File[] listOfFiles = folderOut.listFiles();
        List<String> lst = Arrays.stream(listOfFiles).filter(file -> file.isDirectory() && !file.getName().startsWith("Archive")).map(file -> file.getName()).collect(Collectors.toList());
        return lst;
    }

    public OcrObj readFile(String fOut, String faxFile) throws IOException {
        File folderOut = new File(fOut);
        File faxFolder = new File(folderOut, faxFile);
        OcrObj ocrObj = new OcrObj();
        ocrObj.fileName = faxFile;

        File[] listOfFiles = faxFolder.listFiles();
        List<String> lst = Arrays.stream(listOfFiles).filter(file -> file.getName().startsWith("img-")).map(file -> file.getName()).collect(Collectors.toList());
        Collections.sort(lst);

        ocrObj.ocrPageObjs = new ArrayList<>();
        for (String pageName : lst) {
            try {
//                String hocrName = pageName.replaceAll("img", "hocr").replaceAll(".jpg", ".html");
//                String hocr = Utils.readFile(faxFolder, hocrName);
                OcrObj.OcrPageObj ocrPageObj = new OcrObj.OcrPageObj();
                ocrPageObj.pageNum = Integer.parseInt(pageName.replaceAll("img-", "").replaceAll(".jpg", ""));
                ocrPageObj.pageFile = pageName;
//                ocrPageObj.pageHocr = hocr;
                ocrObj.ocrPageObjs.add(ocrPageObj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ocrObj;
    }

    public byte[] getSearchablePdf(String fOut, String faxfile) throws IOException {
        File folderOut = new File(fOut, faxfile);
        byte[] bytes = Utils.readFileBytes(folderOut, "Searchable-" + faxfile);
        return bytes;
    }

    public void uploadPdfImage(String fIn, String folderOut, String folderDone, String tesseractFolder, MultipartFile file) throws IOException {
        File faxFile = new File(fIn, file.getOriginalFilename());
        byte[] bytes = file.getBytes();
        Files.write(bytes, faxFile);
    }

    public abstract void archiveFile(String folderOut, String file);

    public abstract List<String> viewFaxOnQueue(String folderIn);

    public abstract String createSearchable(String folderOut, String file);

    public static class RectString {
        String str;
        Rectangle rect;

        public RectString(String str, Rectangle rect) {
            this.str = str;
            this.rect = rect;
        }
    }

    public byte[] getPageImage(String fOut, String faxFile, String page) throws IOException {
        File folderOut = new File(fOut);
        File faxFolder = new File(folderOut, faxFile);
        File imageFile = new File(faxFolder, page);

        BufferedImage bImage = ImageIO.read(imageFile);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(bImage, "jpg", bos);
        byte[] data = bos.toByteArray();
        return data;
    }

    public String getEndString(Node node) {
        boolean b = node.hasChildNodes();
        while (b) {
            node = node.getFirstChild();
            b = node.hasChildNodes();
        }
        String str = node.getNodeValue();
        return str;
    }

    public void addSearchablePage(com.itextpdf.text.Document document, String hocr, RenderedImage image, com.itextpdf.text.Font font, int pageNum) throws ParserConfigurationException, SAXException, DocumentException, IOException {
        document.newPage();

        String xml = hocr.substring(hocr.indexOf('\n') + 1);
        InputStream stream = new ByteArrayInputStream(xml.getBytes());

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(stream);
        doc.getDocumentElement().normalize();

        List<Node> nodeList = getOcrLines(doc);
        for (int i = 0; i < nodeList.size(); i++) {
            Element element = (Element) nodeList.get(i);
            String[] title = element.getAttribute("title").split(" ");
            float x = Float.parseFloat(title[1]);
            float y = Float.parseFloat(title[2]);

            String lineText = getLineText(element);
            Logger.getGlobal().log(Level.INFO, "Content added == " + lineText + " at [" + x + ":" + y + "]");

            Paragraph paragraph = new Paragraph(lineText, font);
            paragraph.setIndentationLeft(x);
            document.add(paragraph);
        }
    }

    public String getLineText(Node ocrLine) {
        StringBuilder sb = new StringBuilder();
        NodeList nodeList = ((Element) ocrLine).getElementsByTagName("span");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String className = element.getAttribute("class");
            if ("ocrx_word".equalsIgnoreCase(className)) {
//                this needs some computation for font size
                String str = getEndString(element);
                sb.append(str).append(" ");
            }
        }
        return sb.toString();
    }

    public List<Node> getOcrLines(Document doc) {
        List<Node> nodes = new ArrayList<>();
        NodeList nodeList = doc.getElementsByTagName("span");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String className = element.getAttribute("class");
            if ("ocr_line".equalsIgnoreCase(className)) {
                nodes.add(element);
            }
        }
        return nodes;
    }

    public List<ImageIndex> getImageIndex(PDDocument document) throws IOException {
        List<RenderedImage> images = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            images.addAll(OcrUtility.getImagesFromResources(page.getResources()));
        }
        List<ImageIndex> tmp = new ArrayList<>();
        for (int i = 1; i <= images.size(); i++) {
            ImageIndex ind = new ImageIndex();
            ind.image = images.get(i - 1);
            ind.imageIndex = 1000 + i;
            tmp.add(ind);
        }

        List<ImageIndex> lst = new ArrayList<>();
        while (tmp.size() > 0) {
            int middle = tmp.size() / 2;
            ImageIndex top = tmp.remove(middle);
            lst.add(top);
        }
        if (tmp.size() > 0) {
            lst.addAll(tmp);
        }
        return lst;
    }

    public void createSearchablePdf(String fOut, File faxFile, List<ImageIndex> images) throws FileNotFoundException, DocumentException {
        File folderOut = new File(fOut);
        File tmp = new File(folderOut, faxFile.getName());

        File searchFile = new File(tmp, "Searchable-" + faxFile.getName());
        searchFile.delete();
        com.itextpdf.text.Document searchablePdf = new com.itextpdf.text.Document(new com.itextpdf.text.Rectangle(images.get(0).image.getWidth(), images.get(0).image.getHeight()));
        PdfWriter.getInstance(searchablePdf, new FileOutputStream(searchFile));
        searchablePdf.open();
        images.stream().forEach(ind -> {
            int index = ind.imageIndex;
            RenderedImage img = ind.image;
            String fileHocr = "hocr-" + index + ".html";
            try {
                String hocr = Utils.readFile(tmp, fileHocr);
                addSearchablePage(searchablePdf, hocr, img, myFont, index);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        });
        searchablePdf.close();
    }

    public byte[] extractSearchablePdf(Tesseract instance, MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();

        PDDocument document = PDDocument.load(bytes);
        Map<Integer, PageObj> pages = OcrUtility.getImagesFromPDF(document);
        document.close();

        PDDocument doc = new PDDocument();
        pages.entrySet().parallelStream().forEach(node -> {
            PageObj page = node.getValue();
//            RenderedImage bImage = (RenderedImage) OcrAlignImage.getAlignedImage(PlanarImage.wrapRenderedImage(page.imageIn).getAsBufferedImage(), node.getKey());
//            page.imageOut = bImage;

            Tesseract1 tmp = new Tesseract1();
            tmp.setDatapath("C:/Work/tmp/testdata/tessdata_best");
            try {
                page.setText(tmp.doOCR((BufferedImage) page.getImageIn()));
            } catch (TesseractException e) {
                e.printStackTrace();
            }
        });
        pages.entrySet().stream().forEach(node -> {
            PageObj page = node.getValue();
            OcrUtility.processToSearchablePdf(doc, page);
        });

        byte[] retBytes = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            doc.save(out);
            retBytes = out.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            doc.close();
            out.close();
        }
        return retBytes;
    }

    public Anchor createTarget(String str, int index) {
        Anchor anchorTarget = new Anchor(str+" ", boldFont);
        anchorTarget.setName("link"+index);
        anchorTarget.setReference("#link10000000");
        return anchorTarget;
    }

    @Data
    public static class ImageIndex {
        public int imageIndex;
        public RenderedImage image;
        public File imgFile;
        public String fileHocr;
    }
}
