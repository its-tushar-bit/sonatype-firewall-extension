/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Loads Dropwizard config.yml files and exposes them as Spring properties.
 * This allows customer config files to work unchanged with Spring Boot.
 */
public class DropwizardConfigLoader
{
  public static final String MTIQ_AUDIT_LOG_APPENDER_TYPE = "mtiq-audit-log";

  private static final String LEGACY_CONFIG_MESSAGE =
      "\n================================================================================================================="
          + "\nYour configuration file contains properties that are only compatible with Nexus IQ Server version 1.42"
          + " and lower."
          + "\nUpdate your configuration file to be compatible with this version of Nexus IQ Server."
          + "\nRefer to our configuration update guide at:"
          + "\nhttps://links.sonatype.com/products/nxiq/doc/updating-your-configuration"
          + "\n=================================================================================================================";

  private static final Logger log = LoggerFactory.getLogger(DropwizardConfigLoader.class);

  private final DropwizardConfigSourceReader configSourceReader;

  public DropwizardConfigLoader() {
    this(new DropwizardConfigSourceReader());
  }

  DropwizardConfigLoader(DropwizardConfigSourceReader configSourceReader) {
    this.configSourceReader = configSourceReader;
  }

  /**
   * Load config.yml and add properties to Spring Environment.
   */
  public void loadConfig(File configFile, ConfigurableEnvironment environment) throws IOException {
    if (!configFile.exists()) {
      throw new IllegalArgumentException("Config file not found: " + configFile.getAbsolutePath());
    }

    Map<String, Object> configMap = configSourceReader.readConfigMap(configFile);
    if (configMap == null) {
      throw new IllegalStateException(
          "Config file is empty or contains no valid YAML mapping: " + configFile.getAbsolutePath());
    }

    rejectLegacyConfig(configMap);

    applyDropwizardSystemPropertyOverrides(configMap);

    Map<String, Object> flatProperties = new HashMap<>();
    translateServerSection(configMap, flatProperties);
    translateLoggingSection(configMap, flatProperties);
    flatProperties.putAll(flattenMap(configMap, ""));

    MutablePropertySources propertySources = environment.getPropertySources();
    propertySources.addFirst(new MapPropertySource("dropwizardConfig", flatProperties));
  }

  /**
   * Rejects config files written for the Dropwizard 0.6.2 format used by Nexus IQ Server 1.42 and earlier, where
   * the server lived under {@code http} and logging appenders under {@code logging.console/file/syslog}. These keys
   * do not exist in any supported config, so their presence means the file predates the supported format. We fail
   * with upgrade guidance instead of silently ignoring the unrecognized sections.
   */
  private void rejectLegacyConfig(Map<String, Object> configMap) {
    if (configMap.containsKey("http") || hasLegacyLoggingKey(configMap)) {
      throw new IllegalStateException(LEGACY_CONFIG_MESSAGE);
    }
  }

  private boolean hasLegacyLoggingKey(Map<String, Object> configMap) {
    if (configMap.get("logging") instanceof Map<?, ?> logging) {
      return logging.containsKey("console") || logging.containsKey("file") || logging.containsKey("syslog");
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public static void applyDropwizardSystemPropertyOverrides(Map<String, Object> configMap) {
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith("dw.")) {
        setNestedValue(configMap, key.substring(3), entry.getValue().toString());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void setNestedValue(Map<String, Object> map, String path, Object value) {
    String[] segments = path.split("\\.");
    Map<String, Object> current = map;
    for (int i = 0; i < segments.length - 1; i++) {
      String segment = segments[i];
      int arrayIndex = parseArrayIndex(segment);
      if (arrayIndex >= 0) {
        String arrayKey = segment.substring(0, segment.indexOf('['));
        Object list = current.get(arrayKey);
        if (list instanceof List<?> l && arrayIndex < l.size() && l.get(arrayIndex) instanceof Map) {
          current = (Map<String, Object>) l.get(arrayIndex);
        }
        else {
          return;
        }
      }
      else {
        Object next = current.get(segment);
        if (next instanceof Map) {
          current = (Map<String, Object>) next;
        }
        else {
          Map<String, Object> newMap = new HashMap<>();
          current.put(segment, newMap);
          current = newMap;
        }
      }
    }
    String lastSegment = segments[segments.length - 1];
    int arrayIndex = parseArrayIndex(lastSegment);
    if (arrayIndex >= 0) {
      String arrayKey = lastSegment.substring(0, lastSegment.indexOf('['));
      Object list = current.get(arrayKey);
      if (list instanceof List<?>) {
        ((List<Object>) list).set(arrayIndex, value);
      }
    }
    else {
      current.put(lastSegment, value);
    }
  }

  private static int parseArrayIndex(String segment) {
    int start = segment.indexOf('[');
    int end = segment.indexOf(']');
    if (start >= 0 && end > start) {
      try {
        return Integer.parseInt(segment.substring(start + 1, end));
      }
      catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }

  private void translateServerSection(Map<String, Object> configMap, Map<String, Object> flatProperties) {
    Object serverValue = configMap.get("server");
    if (!(serverValue instanceof Map)) {
      return;
    }

    DropwizardServerConfig server = configSourceReader.convertValueStrict(serverValue, DropwizardServerConfig.class);
    DropwizardConfigCompat.warnOnDeprecatedFields(server, "server");

    validateConnectorCount(server.applicationConnectors, "applicationConnectors");
    validateConnectorCount(server.adminConnectors, "adminConnectors");

    putIfNotNull(server.applicationContextPath, "server.servlet.context-path", flatProperties);
    putIfNotNull(server.adminContextPath, "management.server.base-path", flatProperties);
    putIfNotNull(server.maxThreads, "server.jetty.threads.max", flatProperties);
    putIfNotNull(server.minThreads, "server.jetty.threads.min", flatProperties);

    if (Boolean.TRUE.equals(server.enableVirtualThreads) || Boolean.TRUE.equals(server.enableAdminVirtualThreads)) {
      flatProperties.put("spring.threads.virtual.enabled", true);
    }

    translateConnector(server.applicationConnectors, "applicationConnectors", "server.port", "server.address",
        "server.ssl", DropwizardConnectorSettings.APPLICATION_IDLE_TIMEOUT_PROPERTY, flatProperties);
    translateConnector(server.adminConnectors, "adminConnectors", "management.server.port",
        "management.server.address", "management.server.ssl", DropwizardConnectorSettings.ADMIN_IDLE_TIMEOUT_PROPERTY,
        flatProperties);
    translateGzipConfig(server.gzip, flatProperties);
    translateShutdownGracePeriod(server.shutdownGracePeriod, flatProperties);
  }

  private void translateShutdownGracePeriod(String shutdownGracePeriod, Map<String, Object> flatProperties) {
    if (shutdownGracePeriod == null) {
      return;
    }
    try {
      java.time.Duration duration = DropwizardDurationParser.parse(shutdownGracePeriod);
      flatProperties.put("spring.lifecycle.timeout-per-shutdown-phase", duration.getSeconds() + "s");
      flatProperties.put("server.shutdown", "graceful");
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException("Invalid server.shutdownGracePeriod value: " + shutdownGracePeriod, e);
    }
  }

  private void translateGzipConfig(DropwizardGzipConfig gzip, Map<String, Object> flatProperties) {
    if (gzip == null) {
      return;
    }
    DropwizardConfigCompat.warnOnDeprecatedFields(gzip, "server.gzip");

    if (Boolean.FALSE.equals(gzip.enabled)) {
      flatProperties.put("server.compression.enabled", false);
      return;
    }
    flatProperties.put("server.compression.enabled", true);
    putIfNotNull(gzip.minimumEntitySize, "server.compression.min-response-size", flatProperties);
    if (gzip.compressedMimeTypes != null) {
      flatProperties.put("server.compression.mime-types", String.join(",", gzip.compressedMimeTypes));
    }
  }

  private void translateConnector(
      List<DropwizardConnectorConfig> connectors,
      String connectorListName,
      String portProperty,
      String addressProperty,
      String sslPrefix,
      String idleTimeoutProperty,
      Map<String, Object> flatProperties)
  {
    if (connectors == null || connectors.isEmpty()) {
      return;
    }

    DropwizardConnectorConfig connector = connectors.get(0);
    DropwizardConfigCompat.warnOnDeprecatedFields(connector, "server." + connectorListName);

    putIfNotNull(connector.port, portProperty, flatProperties);
    putIfNotNull(connector.bindHost, addressProperty, flatProperties);

    if (connector.idleTimeout != null) {
      flatProperties.put(idleTimeoutProperty,
          DropwizardConnectorSettings.parseIdleTimeout(connector.idleTimeout, connectorListName));
    }

    if (connector.type != null && "https".equalsIgnoreCase(connector.type.trim())) {
      flatProperties.put(sslPrefix + ".enabled", true);
      putIfNotNull(connector.keyStorePath, sslPrefix + ".key-store", flatProperties);
      putIfNotNull(connector.keyStorePassword, sslPrefix + ".key-store-password", flatProperties);
      putIfNotNull(connector.keyStoreType, sslPrefix + ".key-store-type", flatProperties);
      putIfNotNull(connector.trustStorePath, sslPrefix + ".trust-store", flatProperties);
      putIfNotNull(connector.trustStorePassword, sslPrefix + ".trust-store-password", flatProperties);
      putIfNotNull(connector.trustStoreType, sslPrefix + ".trust-store-type", flatProperties);
      putIfNotNull(connector.certAlias, sslPrefix + ".certificate.alias", flatProperties);
      putIfNotNull(connector.keyManagerPassword, sslPrefix + ".key-password", flatProperties);

      if (Boolean.TRUE.equals(connector.needClientAuth)) {
        flatProperties.put(sslPrefix + ".client-auth", "need");
      }
      else if (Boolean.TRUE.equals(connector.wantClientAuth)) {
        flatProperties.put(sslPrefix + ".client-auth", "want");
      }

      putIfNotNull(connector.protocol, sslPrefix + ".protocol", flatProperties);
    }
  }

  private void putIfNotNull(Object value, String propertyName, Map<String, Object> flatProperties) {
    if (value != null) {
      flatProperties.put(propertyName, value);
    }
  }

  private void translateLoggingSection(Map<String, Object> configMap, Map<String, Object> springProps) {
    Map<String, Object> logging = asMap(configMap.get("logging"));
    if (!logging.isEmpty()) {
      DropwizardLoggingConfig loggingConfig =
          configSourceReader.convertValueStrict(logging, DropwizardLoggingConfig.class);
      DropwizardConfigCompat.warnOnDeprecatedFields(loggingConfig, "logging");
    }
    Object rootLevel = logging.get("level");
    if (isScalarValue(rootLevel)) {
      springProps.put("logging.level.root", rootLevel);
    }
    asMap(logging.get("loggers")).forEach((name, value) -> {
      if (isScalarValue(value)) {
        springProps.put("logging.level." + name, value);
      }
      else if (value instanceof Map) {
        Object level = asMap(value).get("level");
        if (isScalarValue(level)) {
          springProps.put("logging.level." + name, level);
        }
      }
    });
    translateMultiTenantAuditLogging(logging, springProps);
  }

  private void translateMultiTenantAuditLogging(Map<String, Object> logging, Map<String, Object> springProps) {
    Map<String, Object> auditLogger = asMap(asMap(logging.get("loggers")).get("com.sonatype.insight.audit"));
    Object appendersValue = auditLogger.get("appenders");
    if (!(appendersValue instanceof List<?> appenders)) {
      return;
    }

    appenders.stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .filter(appender -> MTIQ_AUDIT_LOG_APPENDER_TYPE.equals(appender.get("type")))
        .map(appender -> appender.get("auditLogBasePath"))
        .filter(this::isScalarValue)
        .findFirst()
        .ifPresent(path -> springProps.put("auditLogBasePath", path));
  }

  private void validateConnectorCount(List<DropwizardConnectorConfig> connectors, String propertyName) {
    if (connectors != null && connectors.size() > 1) {
      throw new IllegalStateException(
          "Dropwizard-to-Spring compatibility: multiple connectors are not supported for " + propertyName
              + "; refusing startup instead of silently ignoring extra connectors.");
    }
  }

  private boolean isScalarValue(Object value) {
    return value != null && !(value instanceof Map) && !(value instanceof List);
  }

  private Map<String, Object> asMap(Object value) {
    if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) value;
      return map;
    }
    return Collections.emptyMap();
  }

  private Map<String, Object> flattenMap(Map<String, Object> source, String prefix) {
    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) value;
        result.putAll(flattenMap(nested, key));
      }
      else if (value instanceof List<?> list) {
        result.put(key, list);
        for (int i = 0; i < list.size(); i++) {
          Object element = list.get(i);
          String indexedKey = key + "[" + i + "]";
          if (element instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) element;
            result.putAll(flattenMap(nestedMap, indexedKey));
          }
          else {
            result.put(indexedKey, element);
          }
        }
      }
      else {
        result.put(key, value);
      }
    }
    return result;
  }
}
