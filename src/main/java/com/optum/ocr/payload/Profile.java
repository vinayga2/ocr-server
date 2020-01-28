package com.optum.ocr.payload;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.List;

@Data
public class Profile {
    public String token;
    public String tokenType = "Bearer";

    public String fullName;
    public String firstName;
    public String lastName;
    public String email;
    public boolean hasAuthority;

    public String employeeId;
    public String memberOf;
    public List<GrantedAuthority> authorities = new ArrayList<>();
}
