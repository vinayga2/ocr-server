package dynamic.groovy.mbm

import com.itextpdf.text.Anchor
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import org.w3c.dom.NamedNodeMap

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage

class PageHighlighter {
    private File faxFile;
    protected Map<String, List<PageNode>> anchorMapNode = new HashMap<>();
    protected Map<String, List<Anchor>> anchorMap = new HashMap<>();
    protected TextForHighlight textForHighlight;

    PageHighlighter() {
    }

    void init(File faxFile) {
        this.faxFile = faxFile;
        textForHighlight = new TextForHighlight();
        textForHighlight.init(faxFile);
    }

    void addLinkPage(com.itextpdf.text.Document document) throws DocumentException {
        document.newPage();
        Anchor anchorTarget = new Anchor("Glossary Page", AbstractImageReader.boldFont);
        anchorTarget.setName("link10000000");
        Paragraph glossary = new Paragraph();
        glossary.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        glossary.add(anchorTarget);
        document.add(glossary);

        textForHighlight.highlight.stream().forEach({txt ->
            List<Anchor> anchors = anchorMap.get(txt.trim().toUpperCase());
            if (anchors != null) {
                Paragraph paragraph = new Paragraph();
                Phrase bold = new Phrase(txt+" ", AbstractImageReader.boldFont);
                paragraph.add(bold);
                paragraph.add(" Page ");

                anchors.stream().forEach({anchor ->
                    paragraph.add(anchor);
                    paragraph.add(" ");
                });
                try {
                    document.add(paragraph);
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void addLinkImagePage(com.itextpdf.text.Document document) throws DocumentException {
        document.newPage();
        Anchor anchorTarget = new Anchor("Glossary Image Page", AbstractImageReader.boldFont);
        anchorTarget.setName("link10000001");
        Paragraph glossary = new Paragraph();
        glossary.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        glossary.add(anchorTarget);
        document.add(glossary);

        textForHighlight.highlight.stream().forEach({txt ->
            List<PageNode> anchors = anchorMapNode.get(txt.trim().toUpperCase());
            if (anchors != null) {
                Paragraph paragraph = new Paragraph();
                Phrase bold = new Phrase(txt+" ", AbstractImageReader.boldFont);
                paragraph.add(bold);
                paragraph.add(" Page ");

                anchors.stream().forEach({anchor ->
                    paragraph.add(anchor.anchor);
                    paragraph.add(" ");
                });
                try {
                    document.add(paragraph);
                } catch (DocumentException e) {
                    e.printStackTrace();
                }
                anchors.stream().forEach({pageNode ->
                    try {
                        byte[] portion = getImagePortion(pageNode);
                        Image image = Image.getInstance(portion);
                        document.add(image);
                    } catch(Exception e) {
                        System.out.println(e.getMessage()+" looking for "+txt);
                    }
                });
            }
        });
    }

    byte[] getImagePortion(PageNode pageNode) {
        RenderedImage image = pageNode.image;
        NamedNodeMap map = pageNode.node.getAttributes();
        String box = map.getNamedItem("title").getNodeValue();
        String[] arr = box.split("[ ;]");
        int x0 = Integer.parseInt(arr[1]);
        int y0 = Integer.parseInt(arr[2]);

        int newX = x0;
        int newY = y0;
        int width = (int) (image.width / 2);
        int height = 75;
        if (x0+100 < image.width) {
            newX = x0 - 50;
        }
        if (y0+100 < image.height) {
            newY = y0 - 20;
        }
        if (newX + width > image.width) {
            width = image.width - newX - 5;
        }
        if (newY + height > image.height) {
            height = image.height - newY - 5;
        }
        BufferedImage portion = ((BufferedImage)image).getSubimage(newX, newY, width, height);
        portion = addPadding(portion);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(portion, "jpg", baos);
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();

        return imageInByte;
    }

    BufferedImage addPadding(BufferedImage image) {
        int h = 15;
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight() + h, image.getType());

        Graphics g = newImage.getGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, image.getWidth() + 2*h, image.getHeight() + 2*h);
        g.drawImage(image, h, 0, null);
        g.dispose();

        return newImage;
    }

    void createAnchorForGlossary(String str, int index, int pageNum) {
        List<Anchor> anchors = anchorMap.get(str.trim().toUpperCase());
        if (anchors == null) {
            anchors = new ArrayList<>();
            anchorMap.put(str.trim().toUpperCase(), anchors);
        }
        String anchorName = str.trim().toUpperCase()+"pg"+pageNum;
        boolean pageLinkExist = anchors.stream().anyMatch({anc -> anchorName.equalsIgnoreCase(anc.getName())});
        if (!pageLinkExist) {
            String anchorText = (pageNum-1000)+", ";
            Anchor anchor = new Anchor(anchorText);
            anchor.setName(anchorName);
            anchor.setReference("#link"+index);

            anchors.add(anchor);
        }
    }

    void createAnchorImageForGlossary(String str, int index, int pageNum, org.w3c.dom.Element element, RenderedImage image) {
        List<PageNode> anchorNodes = anchorMapNode.get(str.trim().toUpperCase());
        if (anchorNodes == null) {
            anchorNodes = new ArrayList<>();
            anchorMapNode.put(str.trim().toUpperCase(), anchorNodes);
        }
        String anchorName = str.trim().toUpperCase()+"pg"+pageNum;
        String anchorText = (pageNum-1000)+", ";
        Anchor anchor = new Anchor(anchorText);
        anchor.setName(anchorName);
        anchor.setReference("#link"+index);

        PageNode pageNode = new PageNode(pageNum, anchor, element, image);
        anchorNodes.add(pageNode);
    }

    boolean existInHighlight(String str, int pageNum) {
        return textForHighlight.existInHighlight(str, pageNum);
    }
}
