/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for static resources.
 * Replaces Dropwizard assets bundle.
 */
@Configuration
public class WebConfiguration
    implements WebMvcConfigurer
{

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Serve static assets from classpath
    registry.addResourceHandler("/static/**")
        .addResourceLocations("classpath:/static/");

    // Serve frontend assets
    registry.addResourceHandler("/assets/**")
        .addResourceLocations("classpath:/assets/");

    // Favicon and root
    registry.addResourceHandler("/favicon.ico")
        .addResourceLocations("classpath:/favicon.ico");
  }

  /**
   * Configure content negotiation to include charset for text-based media types.
   * This ensures consistency with Dropwizard's AssetBundle behavior which added
   * charset=UTF-8 to text-based content types.
   */
  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    // Configure text-based media types with UTF-8 charset for static resources
    configurer
        .parameterName("mediaType")
        .ignoreAcceptHeader(false)
        .useRegisteredExtensionsOnly(false)
        .defaultContentType(MediaType.APPLICATION_JSON)
        .mediaType("html", new MediaType("text", "html", StandardCharsets.UTF_8))
        .mediaType("css", new MediaType("text", "css", StandardCharsets.UTF_8))
        .mediaType("js", new MediaType("text", "javascript", StandardCharsets.UTF_8))
        .mediaType("woff", new MediaType("font", "woff"))
        .mediaType("json", MediaType.APPLICATION_JSON);
  }
}
