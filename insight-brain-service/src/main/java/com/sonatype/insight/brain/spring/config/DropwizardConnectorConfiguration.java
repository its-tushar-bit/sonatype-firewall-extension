/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.spring.config.DropwizardConnectorSettings.AdditionalConnector;
import java.time.Duration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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
    return factory -> factory.addServerCustomizers(server -> {
      applyIdleTimeout(server, connectorSettings.applicationIdleTimeout());
      for (AdditionalConnector additionalConnector : connectorSettings.additionalApplicationConnectors()) {
        server.addConnector(
            buildAdditionalConnector(server, additionalConnector, connectorSettings.applicationIdleTimeout()));
      }
    });
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

  static ServerConnector buildAdditionalConnector(
      Server server,
      AdditionalConnector connector,
      Duration defaultIdleTimeout)
  {
    HttpConfiguration httpConfiguration = new HttpConfiguration();
    ServerConnector serverConnector;
    if (connector.isHttps()) {
      httpConfiguration.addCustomizer(new SecureRequestCustomizer());
      HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);
      SslConnectionFactory sslConnectionFactory =
          new SslConnectionFactory(buildSslContextFactory(connector.ssl()), httpConnectionFactory.getProtocol());
      serverConnector = new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
    }
    else {
      serverConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfiguration));
    }

    if (connector.port() != null) {
      serverConnector.setPort(connector.port());
    }
    if (connector.bindHost() != null) {
      serverConnector.setHost(connector.bindHost());
    }

    Duration idleTimeout = connector.idleTimeout() != null ? connector.idleTimeout() : defaultIdleTimeout;
    if (idleTimeout != null) {
      serverConnector.setIdleTimeout(idleTimeout.toMillis());
    }
    return serverConnector;
  }

  private static SslContextFactory.Server buildSslContextFactory(AdditionalConnector.Ssl ssl) {
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    if (ssl == null) {
      return sslContextFactory;
    }

    if (ssl.keyStore() != null) {
      sslContextFactory.setKeyStorePath(ssl.keyStore());
    }
    if (ssl.keyStorePassword() != null) {
      sslContextFactory.setKeyStorePassword(ssl.keyStorePassword());
    }
    if (ssl.keyStoreType() != null) {
      sslContextFactory.setKeyStoreType(ssl.keyStoreType());
    }
    if (ssl.trustStore() != null) {
      sslContextFactory.setTrustStorePath(ssl.trustStore());
    }
    if (ssl.trustStorePassword() != null) {
      sslContextFactory.setTrustStorePassword(ssl.trustStorePassword());
    }
    if (ssl.trustStoreType() != null) {
      sslContextFactory.setTrustStoreType(ssl.trustStoreType());
    }
    if (ssl.certificateAlias() != null) {
      sslContextFactory.setCertAlias(ssl.certificateAlias());
    }
    if (ssl.keyPassword() != null) {
      sslContextFactory.setKeyManagerPassword(ssl.keyPassword());
    }
    if (ssl.protocol() != null) {
      sslContextFactory.setProtocol(ssl.protocol());
    }
    if ("need".equalsIgnoreCase(ssl.clientAuth())) {
      sslContextFactory.setNeedClientAuth(true);
    }
    else if ("want".equalsIgnoreCase(ssl.clientAuth())) {
      sslContextFactory.setWantClientAuth(true);
    }
    return sslContextFactory;
  }
}
