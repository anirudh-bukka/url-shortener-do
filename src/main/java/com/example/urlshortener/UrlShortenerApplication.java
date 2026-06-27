package com.example.urlshortener;

import com.example.urlshortener.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Application entrypoint. Boots the embedded web server, wires the Spring
 * context, enables declarative caching and binds typed configuration.
 */
@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(AppProperties.class)
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
