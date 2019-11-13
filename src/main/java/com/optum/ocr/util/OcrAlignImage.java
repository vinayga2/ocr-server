package com.optum.ocr.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OcrAlignImage {

    public static BufferedImage getAlignedImage(BufferedImage img) {
//        first pass
        List<Double> lst = getDegreeSteps(0d, 1d, 4);
        DegreeImage degreeImage = getBestAligned(lst, img);
        displayBestPass("First pass", degreeImage);

//        second pass
        lst = getDegreeSteps(degreeImage.degree, 0.1, 3);
        degreeImage = getBestAligned(lst, degreeImage.bufferedImage);
        displayBestPass("Second pass", degreeImage);

//        third pass
        lst = getDegreeSteps(degreeImage.degree, 0.01, 2);
        degreeImage = getBestAligned(lst, degreeImage.bufferedImage);
        displayBestPass("Third pass", degreeImage);
//        return (BufferedImage) cleanMargin(degreeImage.bufferedImage);
        return degreeImage.bufferedImage;
    }

    static RenderedImage cleanMargin(RenderedImage orig) {
        int height = orig.getHeight();
        for (int i=0; i<150; i++) {
            OcrUtility.removeBlackLine((BufferedImage) orig, i);
        }
        for (int i=height-150; i<height; i++) {
            OcrUtility.removeBlackLine((BufferedImage) orig, i);
        }

        int width = orig.getWidth();
        for (int i=0; i<100; i++) {
            OcrUtility.removeBlackVerticalLine((BufferedImage) orig, i);
        }
        for (int i=width-100; i<width; i++) {
            OcrUtility.removeBlackVerticalLine((BufferedImage) orig, i);
        }
        return orig;
    }

    static void displayBestPass(String prefix, DegreeImage degreeImage) {
        int highestWhite = degreeImage.highestWhite;
        double bestDegree = degreeImage.degree;
//        Logger.getGlobal().log(Level.DEBUG, "^^^^^^^^^"+prefix+" best degrees "+bestDegree+" with white count of "+highestWhite);
    }

    static DegreeImage getBestAligned(List<Double> lst, BufferedImage img) {
        int highestWhite = 0;
        double bestDegree = 0d;
        BufferedImage lastHighest = null;
        for (int i=0; i<lst.size(); i++) {
            Double degree = lst.get(i);
            BufferedImage myImg = rotateImageByDegrees(img, degree);
            int tmpCount = countWhiteLines(myImg);
            if (tmpCount > highestWhite) {
                highestWhite = tmpCount;
                lastHighest = myImg;
                bestDegree = degree;
            }
//            Logger.getGlobal().log(Level.INFO, "Using degrees "+degree+" with white count of "+tmpCount);
        }
        DegreeImage degreeImage = new DegreeImage();
        degreeImage.degree = bestDegree;
        degreeImage.bufferedImage = lastHighest;
        return degreeImage;
    }

    static class DegreeImage {
        Double degree;
        int highestWhite;
        BufferedImage bufferedImage;
    }

    static List<Double> getDegreeSteps(Double center, Double step, int stepCount) {
        List<Double> lst = new ArrayList<>();
        for (int i=0; i<=stepCount; i++) {
            Double left = center - i*step;
            Double right = center + i*step;
            lst.addAll(Arrays.asList(left, right));
        }
        lst = lst.stream()
                .distinct()
                .collect(Collectors.toList());
        return lst;
    }

    static int countWhiteLines(BufferedImage myImg) {
        int whiteCount = 0;
        int height = myImg.getHeight();
        for (int i=100; i<height-80; i++) {
            if (OcrUtility.areAllWhiteHorizontal(myImg, i)) {
                whiteCount++;
            }
        }
        return whiteCount;
    }

    static BufferedImage rotateImageByDegrees(BufferedImage src, double degrees) {
        int width = src.getWidth();
        int height = src.getHeight();
        double radians = Math.toRadians(degrees);

        BufferedImage dest = new BufferedImage(width, height, src.getType());

        Graphics2D graphics2D = dest.createGraphics();
        graphics2D.rotate(radians, 0, 0);
        graphics2D.drawRenderedImage(src, null);

        return dest;
    }
}
