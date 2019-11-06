package com.optum.ocr.config;

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

    public static boolean PROD;
    public static String[] DynamicDirs;
    public static String RuntimeFolder;
    public static Environment ENV;
    public static FileObjectExtractor sfileObjectExtractor;
    public static String WebDriver;

    public static ApplicationContext applicationContext;

    @Autowired
    ApplicationContext applicationContextTmp;
    @Autowired
    private FileObjectExtractor fileObjectExtractor;

    @Value("${prod}")
    private boolean prod;
    @Value("${project.dynamic.dirs}")
    private String[] dynamicDirs;
    @Value("${web.driver}")
    private String webDriver;

    @Autowired
    private Environment env;

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        applicationContext = applicationContextTmp;

        PROD = prod;
        DynamicDirs = dynamicDirs;
        sfileObjectExtractor = fileObjectExtractor;
        ENV = env;
        WebDriver = webDriver;
        FileObjectExtractor.initAllGroovy();

        Logger.getGlobal().log(Level.INFO, "hello world, I have just started up");
    }

}
