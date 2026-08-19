/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.guide.mcp.McpServletProvider;
import com.sonatype.insight.brain.guide.mcp.policy.McpPolicyAnnotator;
import com.sonatype.insight.brain.guide.core.SearchApiClient;

import jakarta.servlet.Servlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the MCP servlet at {@code /mcp} as a {@link ServletRegistrationBean} so Spring Boot honors
 * the exact {@code /mcp} mapping.
 *
 * <p>
 * Reachability also requires {@code mcp} to be in {@link SelectiveJerseyFilter}'s bypass set, so the
 * Jersey {@code /*} filter lets {@code /mcp} through to this servlet rather than handling it itself.
 * Access is gated by {@code McpLicenseFilter}.
 */
@Configuration
public class McpConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(McpConfiguration.class);

  @Bean
  public ServletRegistrationBean<Servlet> mcpServletRegistration(
      final CoreConfiguration.StaticInjectionInitializer staticInjectionInitializer,
      final McpServletProvider mcpServletProvider,
      final SearchApiClient searchApiClient,
      final McpPolicyAnnotator policyAnnotator)
  {
    // initialize() must run before getServlet(). Declaring the collaborators as bean parameters
    // (including StaticInjectionInitializer) preserves the original init ordering.
    mcpServletProvider.initialize(searchApiClient, policyAnnotator);

    ServletRegistrationBean<Servlet> registration =
        new ServletRegistrationBean<>(mcpServletProvider.getServlet(), "/mcp", "/mcp/*");
    registration.setName("mcp");
    registration.setLoadOnStartup(1);
    log.info("Registered MCP servlet at /mcp via ServletRegistrationBean");
    return registration;
  }
}
