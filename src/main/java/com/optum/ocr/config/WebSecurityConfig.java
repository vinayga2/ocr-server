package com.optum.ocr.config;

import com.optum.ocr.security.JwtAuthenticationEntryPoint;
import com.optum.ocr.security.JwtConfigurer;
import com.optum.ocr.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Value("${ldap.host}")
    private String ldapHost;

    @Value("${ldap.port}")
    private String ldapPort;

    @Value("${ldap.domain}")
    private String ldapBaseDn;

    @Value("${ldap.domainPatterns}")
    private String ldapDomainPatterns;

//    @Value("${ldap.domain}")
//    private String ldapDomain;

    @Value("${ldap.searchBase}")
    private String ldapSearchBase;

    @Value("${ldap.searchFilter}")
    private String ldapSearchFilter;

    @Value("${ldap.passwordAttribute}")
    private String ldapPasswordAttribute;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Override
    public void configure(WebSecurity web) throws Exception {
        web
                .ignoring()
                .antMatchers(
                        "/resources/**",
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/dist/**",
                        "/plugins/**",
                        "/index",
                        "/login",
                        "/swag**",
                        "/webjars/**",
                        "/error**",
                        "/logout**",
                        "/swagger-resources/**",
                        "/api-docs**",
                        "/images/**",
                        "/api/auth/**",
                        "/api/ocrFiles/**"
                );
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .httpBasic().disable()
                .cors().and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.POST,"/api/auth/signin").permitAll()
                .anyRequest().authenticated()
                .and()
                .apply(new JwtConfigurer(jwtTokenProvider));
//        http
//                .csrf()
//                .disable()
//                .cors()
//                .disable()
//                .exceptionHandling()
//                .authenticationEntryPoint(unauthorizedHandler)
//                .and()
//                .authorizeRequests()
//                .antMatchers("/api/auth/**").permitAll()
//                .anyRequest().authenticated()
//                .and()
//                .apply(new JwtConfigurer(jwtTokenProvider));
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        //Good to use BcryptEncoder for spring 5.0
        auth
                .ldapAuthentication()
                .userDnPatterns(ldapDomainPatterns)
                .groupSearchBase(ldapSearchBase)
//                .groupSearchFilter(ldapSearchFilter)
                .contextSource(contextSource())
                .passwordCompare()
                .passwordEncoder(new LdapShaPasswordEncoder())
                .passwordAttribute(ldapPasswordAttribute);
    }

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        return new DefaultSpringSecurityContextSource(Collections.singletonList(ldapHost+":"+ldapPort), ldapBaseDn);
    }

}