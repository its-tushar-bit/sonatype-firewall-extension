/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.mcp.McpServletProvider;
import com.sonatype.insight.brain.mcp.policy.PolicyAnnotator;
import com.sonatype.insight.brain.mcp.search.SearchApiClient;

import jakarta.servlet.ServletRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the optional MCP servlet when the GUIDE_MCP feature is enabled.
 */
@Configuration
public class McpConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(McpConfiguration.class);

  @Bean
  public ServletContextInitializer mcpServletInitializer(
      final CoreConfiguration.StaticInjectionInitializer staticInjectionInitializer,
      final ObjectProvider<McpServletProvider> mcpServletProviderProvider,
      final ObjectProvider<SearchApiClient> searchApiClientProvider,
      final ObjectProvider<PolicyAnnotator> policyAnnotatorProvider)
  {
    return servletContext -> {
      McpServletProvider mcpServletProvider = mcpServletProviderProvider.getIfAvailable();
      if (mcpServletProvider == null) {
        throw new IllegalStateException("GUIDE_MCP is enabled but McpServletProvider is unavailable");
      }

      mcpServletProvider.initialize(searchApiClientProvider.getObject(), policyAnnotatorProvider.getObject());
      ServletRegistration.Dynamic registration = servletContext.addServlet("mcp", mcpServletProvider.getServlet());
      if (registration == null) {
        log.info("MCP servlet already registered, skipping duplicate registration.");
        return;
      }
      registration.addMapping("/mcp", "/mcp/*");
      registration.setLoadOnStartup(1);
      log.info("Registered MCP servlet at /mcp");
    };
  }
}
