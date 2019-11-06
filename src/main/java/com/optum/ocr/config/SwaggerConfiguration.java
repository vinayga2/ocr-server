package com.optum.ocr.config;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
@EnableSwagger2
@ComponentScan(basePackages = {"com.optum.ocr.api"})
public class SwaggerConfiguration {
    private final Logger log = LoggerFactory.getLogger(SwaggerConfiguration.class);

    protected Contact contact = new Contact(
            "Matyas Albert-Nagy",
            "https://justrocket.de",
            "matyas@justrocket.de");

    protected List<VendorExtension> vext = new ArrayList<>();
    protected ApiInfo apiInfo = new ApiInfo(
            "Backend API",
            "This is the best stuff since sliced bread - API",
            "6.6.6",
            "https://justrocket.de",
            contact,
            "MIT",
            "https://justrocket.de",
            vext);

    @Bean
    public Docket myapi() {
        log.debug("Starting Swagger");

        Docket docket = getDocket("optum").select()
                .apis(RequestHandlerSelectors.basePackage("com.optum.ocr.api"))
                .paths(PathSelectors.any())
                .build()
                .pathMapping("/");

        return docket;
    }

    protected Docket getDocket(String groupName) {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName(groupName)
                .apiInfo(apiInfo)
                .pathMapping("/")
                .apiInfo(ApiInfo.DEFAULT)
                .forCodeGeneration(true)
                .genericModelSubstitutes(ResponseEntity.class)
                .ignoredParameterTypes(java.time.LocalDate.class)
                .directModelSubstitute(java.time.LocalDate.class, java.time.LocalDate.class)
                .directModelSubstitute(java.time.ZonedDateTime.class, Date.class)
                .directModelSubstitute(java.time.LocalDateTime.class, Date.class)
                .useDefaultResponseMessages(false);
        return docket;
    }
}