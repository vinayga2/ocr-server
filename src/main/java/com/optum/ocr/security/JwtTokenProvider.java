package com.optum.ocr.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Date;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;

    @Value("${ldap.domain}")
    private String ldapBaseDn;

    public String generateToken(Authentication authentication) {
        String userName = (String) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userName)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }

    public Authentication getAuthentication(String token) {
        AuthenticationCache authenticationCache = new AuthenticationCache();
        Authentication authentication = authenticationCache.getAuthentication(token);
        if (authentication == null) {
            String username = getUsername(token);
            LdapUserDetailsImpl.Essence essence = new LdapUserDetailsImpl.Essence();
            essence.setUsername(username);
            essence.setDn(ldapBaseDn);
            essence.addAuthority(new SimpleGrantedAuthority("USER"));
            UserDetails userDetails = essence.createUserDetails();
            authentication = new UsernamePasswordAuthenticationToken(userDetails, "", null);
            authenticationCache.store(token, authentication);
        }
        return authentication;
    }

    public String getUsername(String token) {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
    }

    private static class AuthenticationCache {
        static Cache<String, Authentication> myCache;

        private AuthenticationCache() {
            if (myCache==null) {
                CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
                CacheConfiguration<String, Authentication> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Authentication.class,
                        ResourcePoolsBuilder.heap(100))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(1)))
                        .build();
                cacheManager.init();

                myCache = cacheManager.createCache("AuthenticationCache", cacheConfiguration);
            }
        }

        private Authentication getAuthentication(String token) {
            Authentication authentication = myCache.get(token);
            return authentication;
        }

        private void store(String token, Authentication authentication) {
            myCache.put(token, authentication);
        }
    }
}