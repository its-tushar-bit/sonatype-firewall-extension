/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import io.dropwizard.metrics.servlets.PingServlet;
import jakarta.servlet.http.HttpServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PingServletConfiguration
{
  @Bean
  public ServletRegistrationBean<HttpServlet> appPingServlet() {
    return new ServletRegistrationBean<>(new PingServlet(), "/ping");
  }
}
