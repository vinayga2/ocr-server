package com.optum.ocr.api;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<String> getHome(@AuthenticationPrincipal UserDetails userDetails) throws Exception {
        String data = "Welcome to home page!";
        return new ResponseEntity(data, HttpStatus.OK);
    }

}