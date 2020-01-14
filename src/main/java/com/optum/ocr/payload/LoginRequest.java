package com.optum.ocr.payload;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}