package com.optum.ocr.service;

import com.optum.ocr.util.FileObjectExtractor;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
}
