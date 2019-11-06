package com.optum.ocr.util;

import com.optum.ocr.config.InitializerConfig;
import groovy.lang.GroovyClassLoader;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.optum.ocr.config.InitializerConfig.DynamicDirs;
import static com.optum.ocr.config.InitializerConfig.RuntimeFolder;

@Component
public class FileObjectExtractor {
    Logger logger = Logger.getGlobal();

    private static Map<String, FileObj> fileMap = new ConcurrentHashMap<>();
    private static GroovyClassLoader groovyClassLoader;

    public static void initAllGroovy() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                FileObjectExtractor extractor = new FileObjectExtractor();
                try {
                    getResourceList().stream().forEach(file -> {
                        if (file.getName().endsWith(".groovy")) {
                            try {
                                extractor.getGroovyObject(file.getName());
                            } catch (Exception e) {
//                                e.printStackTrace();
                                System.out.println("Exception on "+file.getName());
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private static class FileObj {
        String file;
        Long fileDate;
        String text;
        byte[] bytes;
        Object obj;
    }

    private static List<File> resourceList;

    public static void clearResourceList() {
        resourceList = null;
    }

    public static List<File> getResourceList() throws IOException {
        new FileObjectExtractor().initResourceList();
        return resourceList;
    }

    private void initResourceList() throws IOException {
        if (resourceList == null || resourceList.isEmpty()) {
            resourceList = new ArrayList<>();
            if (InitializerConfig.PROD) {
                ClassLoader classLoader = FileObjectExtractor.class.getClassLoader();
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);

                Resource[] resources = resolver.getResources("classpath:dynamic/**");
                for (Resource resource:resources) {
                    resourceList.add(resource.getFile());
                }
            }
            else {
                for (String dir: DynamicDirs) {
                    File f = new File(dir);
                    Collection<File> files = FileUtils.listFiles(f, null, true);
                    for (File file:files) {
                        resourceList.add(file);
                    }
                }
            }
        }
    }

    public File getFile(String filename) throws IOException {
        initResourceList();

        File retFile = null;
        for (File str:resourceList) {
            if (str.getName().equals(filename)) {
                retFile = str;
                break;
            }
        }

        if (retFile!=null) {
            logger.log(Level.FINEST, "Using file "+retFile.getAbsolutePath());
            return retFile;
        }
        else {
            if (!InitializerConfig.PROD) {
//                here we check if the file is in runtime (for non prod only)
                File f = new File(RuntimeFolder);
                Collection<File> files = FileUtils.listFiles(f, null, true);
                for (File str:files) {
                    if (str.getName().equals(filename)) {
                        retFile = str;
                        logger.log(Level.FINEST, "Using file "+retFile.getAbsolutePath());
                        return retFile;
                    }
                }
            }
            throw new RuntimeException("File "+filename+" not found.");
        }
    }

    public boolean isUpdated(String filename) throws IOException {
        FileObj fileObj = getFileObj(filename);

        Long stored = fileObj.fileDate;
        Long lastModified = getFile(filename).lastModified();
        boolean b = false;
        if (stored == null || stored.longValue() != lastModified.longValue()) {
            b = true;
        }
        return b;
    }

    public Object getGroovyObject(String tmp) throws IllegalAccessException, InstantiationException, IOException {
        String filename = null;
        if (tmp.endsWith(".groovy")) {
            filename = tmp;
        }
        else {
            filename = tmp + ".groovy";
        }
        FileObj fileObj = getFileObj(filename);

        if (isUpdated(filename) || fileObj == null || fileObj.obj == null) {
            String str = getPathContent(filename);
            if (groovyClassLoader==null) {
                groovyClassLoader = new GroovyClassLoader();
            }
            Object obj = groovyClassLoader.parseClass(str).newInstance();
            fileObj.obj = obj;
        }
        Object obj = fileObj.obj;
        return obj;
    }

    public BufferedImage getPathImage(String filename) throws IOException {
        byte[] bytes = getPathBytes(filename);
        InputStream in = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(in);
        return image;
    }

    public byte[] getPathBytes(String filename) throws IOException {
        FileObj fileObj = getFileObj(filename);

        byte[] allBytes = null;
        if (isUpdated(filename) || fileObj == null || fileObj.bytes == null) {
            File f = getFile(filename);
            logger.log(Level.FINER, "UPDATED FILE **** "+f.getAbsolutePath());
            allBytes = Files.readAllBytes(f.toPath());
            fileObj.bytes = allBytes;

            Long lastModified = f.lastModified();
            fileObj.fileDate = lastModified;
        }
        else {
            logger.log(Level.FINEST, "NOT UPDATED FILE **** "+filename);
            allBytes = fileObj.bytes;
        }
        return allBytes;
    }

    public String getPathContent(String filename) throws IOException {
        FileObj fileObj = getFileObj(filename);

        String str = null;
        if (isUpdated(filename) || fileObj == null || fileObj.text == null) {
            File f = getFile(filename);
            logger.log(Level.FINER, "UPDATED FILE **** "+f.getAbsolutePath());
            str = new String(Files.readAllBytes(f.toPath()));
            fileObj.text = str;

            Long lastModified = f.lastModified();
            fileObj.fileDate = lastModified;
        }
        else {
            logger.log(Level.FINEST, "NOT UPDATED FILE **** "+filename);
            str = fileObj.text;
        }
        return str;
    }

    FileObj getFileObj(String filename) {
        FileObj fileObj = fileMap.get(filename);
        if (fileObj == null) {
            fileObj = new FileObj();
            fileObj.file = filename;
            synchronized (fileMap) {
                fileMap.put(filename, fileObj);
            }
        }
        return fileObj;
    }
}
