package com.optum.ocr.payload;

import lombok.Data;

@Data
public class JwtAuthenticationResponse {
    private String token;
    private String tokenType = "Bearer";

    public JwtAuthenticationResponse(String accessToken) {
        this.token = accessToken;
    }
}