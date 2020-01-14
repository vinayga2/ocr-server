package com.optum.ocr.api;

import javax.validation.Valid;

import com.optum.ocr.payload.ApiResponse;
import com.optum.ocr.payload.JwtAuthenticationResponse;
import com.optum.ocr.payload.LoginRequest;
import com.optum.ocr.payload.ValidateTokenRequest;
import com.optum.ocr.security.JwtTokenProvider;
import com.optum.ocr.util.MessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        if(loginRequest.getUsername().isEmpty() || loginRequest.getPassword().isEmpty()) {
            return new ResponseEntity(new ApiResponse(false, MessageConstants.USERNAME_OR_PASSWORD_INVALID),
                    HttpStatus.BAD_REQUEST);
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        String jwt = tokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
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