package com.optum.ocr.util;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OcrUtility {
    private static final PDFont FONT = PDType1Font.HELVETICA;
    private static final float FONT_SIZE = 12;
    private static final float LEADING = -1.5f * FONT_SIZE;

    public static boolean areAllWhiteVertical(BufferedImage source, int x) {
        int countNonWhite = 0;
        for (int y=100; y<source.getHeight()-100; y++) {
            if (!isWhite(source.getRGB(x, y))) {
                countNonWhite++;
            }
        }
        return countNonWhite < 10;
    }

    public static boolean areAllWhiteHorizontal(BufferedImage source, int y) {
        int countNonWhite = 0;
        for (int x=1; x<source.getWidth(); x++) {
            if (!isWhite(source.getRGB(x, y))) {
                countNonWhite++;
            }
        }
        return countNonWhite < 10;
    }

    public static boolean isWhite(int rgb) {
        boolean white = true;
        Color color = new Color(rgb);
        if (color.getRed() < 200 && color.getGreen() < 200 && color.getBlue() < 200) {
            white = false;
        }
        return white;
    }

    public static BufferedImage removeHorizontalBlack(BufferedImage bufferedImage) {
        int height = bufferedImage.getHeight();
        for (int y=0; y<height; y++) {
            if (areAllBlackHorizontal(bufferedImage, y)) {
                removeBlackLine(bufferedImage, y);
            }
        }
        return bufferedImage;
    }

    public static BufferedImage removeHorizontalBlack(BufferedImage bufferedImage, int leftMargin, int rightMargin) {
        int height = bufferedImage.getHeight();
        for (int y=0; y<height; y++) {
            if (areAllBlackHorizontal(bufferedImage, y, leftMargin, rightMargin)) {
                removeBlackLine(bufferedImage, y);
            }
        }
        return bufferedImage;
    }

    public static void removeBlackLine(BufferedImage source, int y) {
        for (int x=1; x<source.getWidth(); x++) {
            source.setRGB(x, y, Color.WHITE.getRGB());
        }
    }

    public static void removeBlackVerticalLine(BufferedImage source, int x) {
        for (int y=1; y<source.getHeight(); y++) {
            source.setRGB(x, y, Color.WHITE.getRGB());
        }
    }

    public static boolean areAllBlackHorizontal(BufferedImage source, int y, int leftMargin, int rightMargin) {
        int countNonBlack = 0;
        for (int x=leftMargin; x<rightMargin; x++) {
            if (!isBlack(source.getRGB(x, y))) {
                countNonBlack++;
            }
        }
        return countNonBlack < 10;
    }

    public static boolean areAllBlackHorizontal(BufferedImage source, int y) {
        int countNonBlack = 0;
        for (int x=1; x<source.getWidth(); x++) {
            if (!isBlack(source.getRGB(x, y))) {
                countNonBlack++;
            }
        }
        return countNonBlack < 10;
    }

    public static boolean isBlack(int rgb) {
        boolean black = true;
        Color color = new Color(rgb);
        if (color.getRed() > 230 && color.getGreen() > 230 && color.getBlue() > 230) {
            black = false;
        }
        return black;
    }

    public static int getTopMargin(BufferedImage bufferedImage) {
        int top = 0;
        int height = bufferedImage.getHeight();
        for (int y=1; y<height-1; y++) {
            top = y;
            if (!areAllWhiteHorizontal(bufferedImage, y)) {
                break;
            }
        }
        return top;
    }

    public static int getLeftMargin(BufferedImage bufferedImage) {
        int left = 0;
        int width = bufferedImage.getWidth();
        for (int x=1; x<width-1; x++) {
            left = x;
            if (!areAllWhiteVertical(bufferedImage, x)) {
                break;
            }
        }
        return left;
    }

    public static int getBottomMargin(BufferedImage bufferedImage) {
        int bottom = 0;
        int height = bufferedImage.getHeight();
        for (int y=height-1; y>1; y--) {
            bottom = y;
            if (!areAllWhiteHorizontal(bufferedImage, y)) {
                break;
            }
        }
        return bottom;
    }

    public static int getRightMargin(BufferedImage bufferedImage) {
        int right = 0;
        int width = bufferedImage.getWidth();
        for (int x=width-1; x>1; x--) {
            right = x;
            if (!areAllWhiteVertical(bufferedImage, x)) {
                break;
            }
        }
        return right;
    }

    public static Map<Integer, PageObj> getImagesFromPDF(PDDocument document) throws IOException {
        List<RenderedImage> images = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            images.addAll(getImagesFromResources(page.getResources()));
        }
        Map<Integer, PageObj> pages = new TreeMap<>();
        for (int i=0; i<images.size(); i++) {
            PageObj pageObj = new PageObj();
            pageObj.imageIn = images.get(i);
            pages.put(i, pageObj);
        }
        return pages;
    }

    public static List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
        List<RenderedImage> images = new ArrayList<>();
        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);
            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
            } else if (xObject instanceof PDImageXObject) {
                images.add(((PDImageXObject) xObject).getImage());
            }
        }
        return images;
    }

    public static void processToSearchablePdf(PDDocument doc, PageObj page) {
        BufferedImage bImg = (BufferedImage) page.imageOut;
        PDPage pdPage = new PDPage(new PDRectangle(bImg.getWidth(), bImg.getHeight()));
        doc.addPage(pdPage);

        try {
            PDPageContentStream backgroundContent = new PDPageContentStream(doc, pdPage, false, true);
            PDImageXObject pdImage = LosslessFactory.createFromImage(doc, (BufferedImage) bImg);
            backgroundContent.saveGraphicsState();
            backgroundContent.drawImage(pdImage, 0, 0);
            backgroundContent.restoreGraphicsState();
            backgroundContent.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger.getGlobal().log(Level.INFO, "Writing "+page.text);
        PDRectangle mediaBox = pdPage.getMediaBox();
        float marginY = 80;
        float marginX = 60;
        float width = mediaBox.getWidth() - 2 * marginX;
        float startX = mediaBox.getLowerLeftX() + marginX;
        float startY = mediaBox.getUpperRightY() - marginY;

        try {
            PDPageContentStream textContent = new PDPageContentStream(doc, pdPage, true, true, true);
            textContent.beginText();
            addParagraph(textContent, width, startX, startY, page.text, true);
            textContent.endText();
            textContent.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addParagraph(PDPageContentStream contentStream, float width, float sx, float sy, String text, boolean justify) throws IOException {
        List<String> lines = Arrays.asList(text.split("\\r?\\n"));
        contentStream.setFont(FONT, FONT_SIZE);
        contentStream.newLineAtOffset(sx, sy);
        for (String line: lines) {
            contentStream.showText(line);
            contentStream.newLineAtOffset(0, LEADING);
        }
    }

}
