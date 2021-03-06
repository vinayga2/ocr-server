package com.optum.ocr.service;

import com.optum.ocr.bean.LoginHistory;
import com.optum.ocr.payload.SecureFileTypeEnum;
import com.optum.ocr.util.FileObjectExtractor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SecureService {
    public String register(String msId) throws IllegalAccessException, IOException, InstantiationException {
        SecureService secureService = (SecureService) new FileObjectExtractor().getGroovyObject("GSecureService.groovy");
        return secureService.register(msId);
    }

    public byte[] downloadInactiveAccount() throws IllegalAccessException, IOException, InstantiationException {
        SecureService secureService = (SecureService) new FileObjectExtractor().getGroovyObject("GSecureService.groovy");
        return secureService.downloadInactiveAccount();
    }

    public String addLoginHistory(String msId) throws IllegalAccessException, IOException, InstantiationException {
        SecureService secureService = (SecureService) new FileObjectExtractor().getGroovyObject("GSecureService.groovy");
        return secureService.addLoginHistory(msId);
    }

    public List<LoginHistory> getAllRegistered() throws IllegalAccessException, IOException, InstantiationException {
        SecureService secureService = (SecureService) new FileObjectExtractor().getGroovyObject("GSecureService.groovy");
        return secureService.getAllRegistered();
    }

    public byte[] createSecureFile(SecureFileTypeEnum fileType) throws IllegalAccessException, IOException, InstantiationException {
        SecureService secureService = (SecureService) new FileObjectExtractor().getGroovyObject("GSecureService.groovy");
        return secureService.createSecureFile(fileType);
    }

    public void createInactiveFile() throws IllegalAccessException, IOException, InstantiationException {
        SecureService secureService = (SecureService) new FileObjectExtractor().getGroovyObject("GSecureService.groovy");
        secureService.createInactiveFile();
    }

    public void scheduleFileCreation() throws IllegalAccessException, IOException, InstantiationException {
        SecureService secureService = (SecureService) new FileObjectExtractor().getGroovyObject("GSecureService.groovy");
        secureService.scheduleFileCreation();
    }
}
