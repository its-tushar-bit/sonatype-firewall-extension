/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
public class DropwizardManagementConnectorConfiguration
{
  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  WebServerFactoryCustomizer<JettyServletWebServerFactory> dropwizardAdminConnectorCustomizer(
      DropwizardConnectorSettings connectorSettings)
  {
    return factory -> factory.addServerCustomizers(
        server -> DropwizardConnectorConfiguration.applyIdleTimeout(server, connectorSettings.adminIdleTimeout()));
  }
}
