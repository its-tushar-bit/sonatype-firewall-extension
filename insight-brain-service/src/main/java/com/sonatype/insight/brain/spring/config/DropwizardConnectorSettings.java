/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

record DropwizardConnectorSettings(
    Duration applicationIdleTimeout,
    Duration adminIdleTimeout,
    List<AdditionalConnector> additionalApplicationConnectors,
    List<AdditionalConnector> additionalAdminConnectors)
{

  static final String APPLICATION_IDLE_TIMEOUT_PROPERTY =
      "sonatype.dropwizard.application-connector.idle-timeout";

  static final String ADMIN_IDLE_TIMEOUT_PROPERTY =
      "sonatype.dropwizard.admin-connector.idle-timeout";

  static final String ADDITIONAL_APPLICATION_CONNECTORS_PREFIX =
      "sonatype.dropwizard.application-connector.additional";

  static final String ADDITIONAL_ADMIN_CONNECTORS_PREFIX =
      "sonatype.dropwizard.admin-connector.additional";

  static DropwizardConnectorSettings from(Environment environment) {
    Binder binder = Binder.get(environment);
    return new DropwizardConnectorSettings(
        environment.getProperty(APPLICATION_IDLE_TIMEOUT_PROPERTY, Duration.class),
        environment.getProperty(ADMIN_IDLE_TIMEOUT_PROPERTY, Duration.class),
        bindAdditionalConnectors(binder, ADDITIONAL_APPLICATION_CONNECTORS_PREFIX),
        bindAdditionalConnectors(binder, ADDITIONAL_ADMIN_CONNECTORS_PREFIX));
  }

  private static List<AdditionalConnector> bindAdditionalConnectors(Binder binder, String prefix) {
    return binder.bind(prefix, Bindable.listOf(AdditionalConnector.class)).orElseGet(List::of);
  }

  static void requireSupportedConnectorType(String type, String connectorDescription) {
    if (type == null) {
      return;
    }
    String normalized = type.trim();
    if (!"http".equalsIgnoreCase(normalized) && !"https".equalsIgnoreCase(normalized)) {
      throw new IllegalStateException(
          "Unsupported connector type '" + type + "' for " + connectorDescription + "; expected 'http' or 'https'");
    }
  }

  static boolean isHttpsType(String type) {
    return type != null && "https".equalsIgnoreCase(type.trim());
  }

  static void requireKeyStoreForHttps(String type, Integer port, AdditionalConnector.Ssl ssl) {
    if (isHttpsType(type) && (ssl == null || ssl.keyStore() == null)) {
      throw new IllegalStateException(
          "No keyStorePath configured for the https additional connector on port " + port
              + "; an https connector cannot serve traffic without a key store");
    }
  }

  record AdditionalConnector(String type, Integer port, String bindHost, Duration idleTimeout, Ssl ssl)
  {
    AdditionalConnector {
      requireSupportedConnectorType(type, "an additional connector");
      requireKeyStoreForHttps(type, port, ssl);
    }

    boolean isHttps() {
      return isHttpsType(type);
    }

    record Ssl(
        String keyStore,
        String keyStorePassword,
        String keyStoreType,
        String trustStore,
        String trustStorePassword,
        String trustStoreType,
        String certificateAlias,
        String keyPassword,
        String clientAuth,
        String protocol)
    {
    }
  }

  static Duration parseIdleTimeout(Object value, String connectorListName) {
    if (value == null) {
      return null;
    }
    try {
      return DropwizardDurationParser.parse(String.valueOf(value));
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Unsupported legacy connector idleTimeout for " + connectorListName + ": " + value, e);
    }
  }
}
