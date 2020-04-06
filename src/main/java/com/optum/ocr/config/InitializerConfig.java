package com.optum.ocr.config;

import com.optum.ocr.util.AbstractImageReader;
import com.optum.ocr.util.FileObjectExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
public class InitializerConfig {

    List<Class> allClasses = new ArrayList<>();

    public static boolean WithSecurity;
    public static boolean PROD;
    public static String[] DynamicDirs;
    public static String RuntimeFolder;
    public static Environment ENV;
    public static FileObjectExtractor sfileObjectExtractor;
    public static String WebDriver;
    public static String SecureFolder;
    public static String SecureFile20;

    public static String SecureEcgServer;
    public static String SecureEcgFolder;
    public static String SecureEcgUsername;
    public static String SecureEcgPassword;
    public static String OcrFolder;
    public static String TesseractFolder;
    public static FileObjectExtractor FileObjectExtractor;

    public static ApplicationContext applicationContext;

    @Autowired
    ApplicationContext applicationContextTmp;
    @Autowired
    private FileObjectExtractor fileObjectExtractor;

    @Value("${with.security}")
    private boolean withSecurity;

    @Value("${prod}")
    private boolean prod;
    @Value("${project.dynamic.dirs}")
    private String[] dynamicDirs;
    @Value("${web.driver}")
    private String webDriver;

    @Value("${secure.file.20}")
    private String secureFile20;
    @Value("${secure.inactive.folder}")
    private String secureFolder;

    @Value("${secure.ecg.server}")
    private String secureEcgServer;
    @Value("${secure.ecg.folder}")
    private String secureEcgFolder;
    @Value("${secure.ecg.username}")
    private String secureEcgUsername;
    @Value("${secure.ecg.password}")
    private String secureEcgPassword;

    @Value("${ocr.folder}")
    private String ocrFolder;
    @Value("${tesseract.data}")
    private String tesseractFolder;

    @Autowired
    private Environment env;

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        applicationContext = applicationContextTmp;

        WithSecurity = withSecurity;
        PROD = prod;
        DynamicDirs = dynamicDirs;
        sfileObjectExtractor = fileObjectExtractor;
        ENV = env;
        WebDriver = webDriver;
        SecureFolder = secureFolder;
        SecureFile20 = secureFile20;

        SecureEcgServer = secureEcgServer;
        SecureEcgFolder = secureEcgFolder;
        SecureEcgUsername = secureEcgUsername;
        SecureEcgPassword = secureEcgPassword;
        OcrFolder = ocrFolder;
        TesseractFolder = tesseractFolder;
        FileObjectExtractor = fileObjectExtractor;

        FileObjectExtractor.initAllGroovy();
        initialize();
        Logger.getGlobal().log(Level.INFO, "hello world, I have just started up");
    }

    public void initialize() throws IllegalAccessException, IOException, InstantiationException {
        InitializerConfig initializerConfig = (InitializerConfig) fileObjectExtractor.getGroovyObject("GInitializer.groovy");
        initializerConfig.initialize();
    }
}
