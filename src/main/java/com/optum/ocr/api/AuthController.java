package com.optum.ocr.api;

import javax.validation.Valid;

import com.optum.ocr.config.OptumLDAP;
import com.optum.ocr.payload.*;
import com.optum.ocr.security.JwtTokenProvider;
import com.optum.ocr.util.MessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenProvider tokenProvider;

    @Autowired
    OptumLDAP optumLDAP;

    @Value("${ldap.groups}")
    private String[] ldapGroups;

    @Value("${ldap.groups}")
    private String ldapGroupStr;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) throws Exception {
        if(loginRequest.getUsername().isEmpty() || loginRequest.getPassword().isEmpty()) {
            return new ResponseEntity(new ApiResponse(false, MessageConstants.USERNAME_OR_PASSWORD_INVALID),
                    HttpStatus.BAD_REQUEST);
        }
        Profile profile = optumLDAP.userAuthority(loginRequest, ldapGroups);
        if (!profile.hasAuthority) {
            throw new RuntimeException("Access Denied,you must be part of the following group/s ["+ldapGroupStr+"]");
        }
        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword(),
                profile.getAuthorities());

//        String jwt = tokenProvider.generateToken(result);
//        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
        profile.token = tokenProvider.generateToken(result);
        return ResponseEntity.ok(profile);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @PostMapping("/validatetoken")
    public ResponseEntity<?> getTokenByCredentials(@Valid @RequestBody ValidateTokenRequest validateToken) {
        String username = null;
        String jwt =validateToken.getToken();
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            username = tokenProvider.getUsernameFromJWT(jwt);
            //If required we can have one more check here to load the user from LDAP server
            return ResponseEntity.ok(new ApiResponse(Boolean.TRUE,MessageConstants.VALID_TOKEN + username));
        }else {
            return new ResponseEntity(new ApiResponse(false, MessageConstants.INVALID_TOKEN),
                    HttpStatus.BAD_REQUEST);
        }

    }
}