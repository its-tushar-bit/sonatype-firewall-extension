/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.time.Duration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class DropwizardConnectorConfiguration
{
  @Bean
  DropwizardConnectorSettings dropwizardConnectorSettings(Environment environment) {
    return DropwizardConnectorSettings.from(environment);
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  WebServerFactoryCustomizer<JettyServletWebServerFactory> dropwizardApplicationConnectorCustomizer(
      DropwizardConnectorSettings connectorSettings)
  {
    return factory -> factory.addServerCustomizers(
        server -> applyIdleTimeout(server, connectorSettings.applicationIdleTimeout()));
  }

  static void applyIdleTimeout(Server server, Duration idleTimeout) {
    if (idleTimeout == null) {
      return;
    }

    long idleTimeoutMillis = idleTimeout.toMillis();
    for (Connector connector : server.getConnectors()) {
      if (connector instanceof ServerConnector serverConnector) {
        serverConnector.setIdleTimeout(idleTimeoutMillis);
      }
    }
  }
}
