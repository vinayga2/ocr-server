package com.optum.ocr.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    public static double round(double value) {
        double roundOff = Math.round(value * 100.0) / 100.0;
        return roundOff;
    }

    //    for lists
    public static <T, U> List<U> convertList(List<T> from, Function<T, U> func) {
        return from.stream().map(func).collect(Collectors.toList());
    }

    //    for arrays
    public static <T, U> U[] convertArray(T[] from,
                                          Function<T, U> func,
                                          IntFunction<U[]> generator) {
        return Arrays.stream(from).map(func).toArray(generator);
    }

    public static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    public static String getLabelFromCamelCase(String camel) {
        String lbl = Utils.splitCamelCase(camel);
        if (lbl.length() > 0) {
            lbl = Character.toUpperCase(lbl.charAt(0)) + lbl.substring(1);
        }
        return lbl;
    }

    public static String toClassName(String lbl) {
        return Character.toUpperCase(lbl.charAt(0)) + lbl.substring(1);
    }

    public static String getGenericType(Field column) {
        ParameterizedType stringListType = (ParameterizedType) column.getGenericType();
        Class<?> cls = (Class<?>) stringListType.getActualTypeArguments()[0];
        return cls.getSimpleName();
    }

    public static String smallFirstLetter(String lbl) {
        return Character.toLowerCase(lbl.charAt(0)) + lbl.substring(1);
    }

    public static String getPossibleLabel(String label, String name) {
        String ret = "";
        if (label == null || label.isEmpty()) {
            ret = getLabelFromCamelCase(name);
        } else {
            ret = label;
        }
        return ret;
    }

    public static String getInitials(String name) {
        String retval = "";
        if (name.contains(" ")) {
            Pattern p = Pattern.compile("((^| )[A-Za-z])");
            Matcher m = p.matcher(name);
            String initials = "";
            while (m.find()) {
                initials += m.group().trim();
            }
            retval = initials;
        }
        else {
            if (name.length() > 4) {
                retval = name.substring(0, 2);
            }
            else {
                retval = name;
            }
        }
        if (retval.length() < 3) {
            retval = retval + "D";
        }
        return retval.toUpperCase();
    }

    public static String getGenericGroup(Class cls) {
        String packageName = cls.getPackageName();
        String[] arr = packageName.split("\\.");
        return arr[arr.length-1].toUpperCase();
    }

    public static LocalDate toDate(String s) {
        return LocalDate.parse(s);
    }

    public static byte[] readFileBytes(File folderOut, String filename) throws IOException {
        File f = new File(folderOut, filename);
        byte[] data = Files.readAllBytes(f.toPath());
        return data;
    }

    public static String readFile(File folderOut, String filename) throws IOException {
        File f = new File(folderOut, filename);
        String data = new String(Files.readAllBytes(f.toPath()));
        return data;
    }

    public static void writeToFile(File file, String str) throws IOException {
        Files.write(file.toPath(), str.getBytes());
    }

    public static BufferedImage toBufferedImage(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        return img;
    }

    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bimage;
    }

    public static String escapeForSql(String str) {
        String s = null;
        if (str.contains("'")) {
            s = str.replaceAll("\'", "''");
        }
        else {
            s = str;
        }
        return s;
    }

    public static byte[] toByteArray(RenderedImage img) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos );
        baos.flush();
        byte[] imageInByte = baos.toByteArray();
        baos.close();
        return imageInByte;
    }
}
