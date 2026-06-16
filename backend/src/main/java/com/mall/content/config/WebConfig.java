package com.mall.content.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final MallProperties mallProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String patterns = mallProperties.getCors().getAllowedOriginPatterns();
        String[] origins = patterns.contains(",") ? patterns.split(",") : new String[]{patterns};
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
