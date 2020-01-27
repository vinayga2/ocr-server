package com.optum.ocr.api;

import com.optum.ocr.service.SecureService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@Controller
@RequestMapping("/api/secure")
@Api(value = "secure", description = "The Secure API", tags = {"Secure"})
public class SecureController {
    @Autowired
    SecureService service;

    @GetMapping("/register/{msId}")
    public ResponseEntity<String> register(@PathVariable("msId") String msId) throws IllegalAccessException, IOException, InstantiationException {
        String str = service.register(msId);
        return new ResponseEntity(str, HttpStatus.OK);
    }

    @GetMapping("/addLoginHistory/{msId}")
    public ResponseEntity<String> addLoginHistory(@PathVariable("msId") String msId) throws IllegalAccessException, IOException, InstantiationException {
        String str = service.addLoginHistory(msId);
        return new ResponseEntity(str, HttpStatus.OK);
    }

    @GetMapping("/downloadInactive")
    public ResponseEntity<?> downloadInactive() throws IllegalAccessException, IOException, InstantiationException {
        byte[] bytes = service.downloadInactiveAccount();

        Resource resource = new ByteArrayResource(bytes);
        String contentType = "application/csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=InactiveAccount.csv")
                .body(resource);
    }
}