package com.optum.ocr.payload;

import lombok.Data;

@Data
public class Profile {
    public String token;
    public String tokenType = "Bearer";

    public String fullName;
    public String email;
    public boolean hasAuthority;
}
