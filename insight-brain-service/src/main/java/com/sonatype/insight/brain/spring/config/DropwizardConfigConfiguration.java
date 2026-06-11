/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.service.InsightConfig;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Spring configuration for loading Dropwizard-style config.yml files.
 * <p>
 * This configuration creates the {@link InsightConfig} bean from the Dropwizard YAML.
 * Server connector ports are translated earlier by {@link DropwizardConfigLoader} into
 * Spring's {@code server.port} and {@code management.server.port} properties.
 */
@Configuration
public class DropwizardConfigConfiguration
{

  private static final Logger log = LoggerFactory.getLogger(DropwizardConfigConfiguration.class);

  private final DropwizardConfigSourceReader configSourceReader = new DropwizardConfigSourceReader();

  /**
   * Ensures property placeholders like ${...} are resolved in @Value annotations.
   * This is needed for Spring Boot 4.x compatibility.
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  public DropwizardWebSettings dropwizardWebSettings(
      @Value("${config.file:config.yml}") String configPath,
      @Value("${config.file.implicitDefault:false}") boolean implicitDefaultConfigFile) throws IOException
  {
    return loadDropwizardWebSettings(configPath, implicitDefaultConfigFile);
  }

  DropwizardWebSettings dropwizardWebSettings(String configPath) throws IOException {
    return loadDropwizardWebSettings(configPath, false);
  }

  /**
   * Creates the InsightConfig bean by loading the Dropwizard YAML file.
   * <p>
   * The config file path is resolved from:
   * 1. Spring property "config.file" (set by the application listener)
   * 2. Default path "config.yml"
   * <p>
   * If the implicit default config file is missing, this bean falls back to {@link InsightConfig}'s object defaults.
   * Missing explicitly configured paths still fail fast. Port compatibility is handled separately by
   * {@link DropwizardConfigLoader} when a real Dropwizard config file is provided at startup.
   */
  @Bean
  @Primary
  public InsightConfig insightConfig(
      @Value("${config.file:config.yml}") String configPath,
      @Value("${config.class:com.sonatype.insight.brain.service.InsightConfig}") String configClassName,
      @Value("${config.file.implicitDefault:false}") boolean implicitDefaultConfigFile) throws IOException
  {
    return loadInsightConfig(configPath, configClassName, implicitDefaultConfigFile);
  }

  InsightConfig insightConfig(String configPath, String configClassName) throws IOException {
    return loadInsightConfig(configPath, configClassName, false);
  }

  private InsightConfig loadInsightConfig(
      String configPath,
      String configClassName,
      boolean implicitDefaultConfigFile) throws IOException
  {
    Class<? extends InsightConfig> configClass = resolveConfigClass(configClassName);

    File configFile = new File(configPath);
    if (!configFile.exists()) {
      if (implicitDefaultConfigFile) {
        log.info("Implicit default config file {} not found, using {} defaults",
            configFile.getAbsolutePath(), configClass.getSimpleName());
        return instantiateConfigClass(configClass);
      }
      throw new IllegalStateException(
          "Config file not found: " + configFile.getAbsolutePath() +
              ". Provide a valid config file path or ensure the file exists.");
    }

    log.info("Loading configuration from {}", configFile.getAbsolutePath());

    // Read as Map first to strip logging section before deserialization
    // This avoids polymorphic type resolution issues with Dropwizard logging appenders
    Map<String, Object> configMap = configSourceReader.readConfigMap(configFile);
    if (configMap == null) {
      throw new IllegalStateException(
          "Config file is empty or contains no valid YAML mapping: " + configFile.getAbsolutePath());
    }

    DropwizardConfigLoader.applyDropwizardSystemPropertyOverrides(configMap);

    for (String key : List.of("database", "mainDatabase", "locksDatabase")) {
      resolveDbUrlPrecedence(configMap, key);
    }

    InsightConfig config = configSourceReader.convertValueStrict(configMap, configClass);
    DropwizardConfigCompat.warnOnDeprecatedFields(config, "config.yml");

    DropwizardServerConfig serverConfig = config.getServer();
    DropwizardLoggingConfig loggingConfig = config.getLogging();
    DropwizardWebConfig webConfig = config.getWeb();

    if (serverConfig != null) {
      validateConnectorCount(serverConfig.applicationConnectors, "applicationConnectors");
      validateConnectorCount(serverConfig.adminConnectors, "adminConnectors");
      applyConnectorTypes(config, serverConfig);
      applyConnectorPorts(config, serverConfig);
    }
    if (webConfig != null) {
      applyHstsConfig(config, webConfig);
      applyFrameOptionsConfig(config, webConfig);
    }
    applyConfiguredLogFiles(config, loggingConfig, serverConfig);

    // If no HDS URL is configured, use a default
    if (config.getHdsUrl() == null || config.getHdsUrl().isEmpty()) {
      log.warn("HDS URL not configured, some features may not work");
    }

    return config;
  }

  @SuppressWarnings("unchecked")
  private Class<? extends InsightConfig> resolveConfigClass(String configClassName) {
    try {
      Class<?> configClass = Class.forName(configClassName);
      if (!InsightConfig.class.isAssignableFrom(configClass)) {
        throw new IllegalStateException("Configured class " + configClassName + " is not an InsightConfig");
      }
      return (Class<? extends InsightConfig>) configClass;
    }
    catch (ClassNotFoundException e) {
      throw new IllegalStateException("Unable to resolve config class " + configClassName, e);
    }
  }

  private InsightConfig instantiateConfigClass(Class<? extends InsightConfig> configClass) {
    try {
      return configClass.getDeclaredConstructor().newInstance();
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Unable to instantiate config class " + configClass.getName(), e);
    }
  }

  private DropwizardWebSettings loadDropwizardWebSettings(
      String configPath,
      boolean implicitDefaultConfigFile) throws IOException
  {
    File configFile = new File(configPath);
    if (!configFile.exists()) {
      if (implicitDefaultConfigFile) {
        return DropwizardWebSettings.empty();
      }
      throw new IllegalStateException(
          "Config file not found: " + configFile.getAbsolutePath() +
              ". Provide a valid config file path or ensure the file exists.");
    }

    Map<String, Object> configMap = configSourceReader.readConfigMap(configFile);
    if (configMap == null) {
      return DropwizardWebSettings.empty();
    }
    Object webValue = configMap.get("web");
    if (webValue == null) {
      return DropwizardWebSettings.empty();
    }
    DropwizardWebConfig webConfig = configSourceReader.convertValueStrict(webValue, DropwizardWebConfig.class);
    return parseDropwizardWebSettings(webConfig);
  }

  private DropwizardWebSettings parseDropwizardWebSettings(DropwizardWebConfig webConfig) {
    if (webConfig == null) {
      return DropwizardWebSettings.empty();
    }

    String uriPath = webConfig.uriPath != null ? webConfig.uriPath : "/";
    Map<String, String> headers = new LinkedHashMap<>();
    headers.putAll(parseContentTypeOptionsHeaders(webConfig.contentTypeOptions));
    headers.putAll(parseXssProtectionHeaders(webConfig.xssProtection));
    headers.putAll(parseCspHeaders(webConfig.csp));
    if (webConfig.headers != null) {
      headers.putAll(webConfig.headers);
    }

    DropwizardWebSettings.CorsSettings corsSettings = parseCorsSettings(webConfig.cors);
    return DropwizardWebSettings.of(uriPath, headers, corsSettings);
  }

  private Map<String, String> parseContentTypeOptionsHeaders(DropwizardWebConfig.ContentTypeOptions cto) {
    if (cto == null || !Boolean.TRUE.equals(cto.enabled)) {
      return Map.of();
    }
    return Map.of(LegacyWebHeaderFilter.X_CONTENT_TYPE_OPTIONS, "nosniff");
  }

  private Map<String, String> parseXssProtectionHeaders(DropwizardWebConfig.XssProtection xss) {
    if (xss == null || !Boolean.TRUE.equals(xss.enabled)) {
      return Map.of();
    }
    boolean on = xss.on == null || xss.on;
    boolean block = xss.block == null || xss.block;
    String headerValue = on ? "1" : "0";
    if (on && block) {
      headerValue += "; mode=block";
    }
    return Map.of(LegacyWebHeaderFilter.X_XSS_PROTECTION, headerValue);
  }

  private Map<String, String> parseCspHeaders(DropwizardWebConfig.Csp csp) {
    if (csp == null || !Boolean.TRUE.equals(csp.enabled)) {
      return Map.of();
    }
    if (isBlank(csp.policy) && isBlank(csp.reportOnlyPolicy)) {
      throw new IllegalStateException(
          "Invalid legacy web.csp configuration: either 'policy' or 'reportOnlyPolicy' must be defined when "
              + "web.csp.enabled is true");
    }
    Map<String, String> headers = new LinkedHashMap<>();
    if (!isBlank(csp.policy)) {
      headers.put(LegacyWebHeaderFilter.CONTENT_SECURITY_POLICY, csp.policy);
    }
    if (!isBlank(csp.reportOnlyPolicy)) {
      headers.put(LegacyWebHeaderFilter.CONTENT_SECURITY_POLICY_REPORT_ONLY, csp.reportOnlyPolicy);
    }
    return headers;
  }

  private DropwizardWebSettings.CorsSettings parseCorsSettings(DropwizardWebConfig.Cors cors) {
    if (cors == null) {
      return null;
    }
    DropwizardWebSettings.CorsSettings defaults = DropwizardWebSettings.CorsSettings.defaults();
    return DropwizardWebSettings.CorsSettings.of(
        parseStringList(cors.allowedOrigins, defaults.getAllowedOrigins(), "web.cors.allowedOrigins"),
        parseStringList(cors.allowedTimingOrigins, defaults.getAllowedTimingOrigins(),
            "web.cors.allowedTimingOrigins"),
        parseStringList(cors.allowedMethods, defaults.getAllowedMethods(), "web.cors.allowedMethods"),
        parseStringList(cors.allowedHeaders, defaults.getAllowedHeaders(), "web.cors.allowedHeaders"),
        parseDuration(cors.preflightMaxAge, defaults.getPreflightMaxAge(), "web.cors.preflightMaxAge"),
        cors.allowCredentials != null ? cors.allowCredentials : defaults.isAllowCredentials(),
        parseStringList(cors.exposedHeaders, defaults.getExposedHeaders(), "web.cors.exposedHeaders"),
        cors.chainPreflight != null ? cors.chainPreflight : defaults.isChainPreflight());
  }

  private List<String> parseStringList(Object value, List<String> defaultValue, String key) {
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof String stringValue) {
      return splitStringList(stringValue);
    }
    if (value instanceof List<?> listValue) {
      List<String> values = new ArrayList<>();
      for (Object entry : listValue) {
        if (!(entry instanceof String stringEntry)) {
          throw new IllegalStateException("Invalid legacy " + key + " configuration: expected a list of strings");
        }
        if (!stringEntry.trim().isEmpty()) {
          values.add(stringEntry.trim());
        }
      }
      return values;
    }
    throw new IllegalStateException("Invalid legacy " + key + " configuration: expected a string or list of strings");
  }

  private List<String> splitStringList(String value) {
    List<String> values = new ArrayList<>();
    for (String entry : value.split(",")) {
      String trimmedEntry = entry.trim();
      if (!trimmedEntry.isEmpty()) {
        values.add(trimmedEntry);
      }
    }
    return values;
  }

  private Duration parseDuration(Object value, Duration defaultValue, String key) {
    if (value == null) {
      return defaultValue;
    }
    try {
      return DropwizardDurationParser.parse(String.valueOf(value));
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException("Invalid legacy " + key + " configuration: expected a Dropwizard duration", e);
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void applyHstsConfig(InsightConfig config, DropwizardWebConfig webConfig) {
    DropwizardWebConfig.Hsts hsts = webConfig.hsts;
    if (hsts == null) {
      return;
    }

    InsightConfig.HstsConfig hstsConfig = config.getHstsConfig();

    if (hsts.enabled != null) {
      hstsConfig.setEnabled(hsts.enabled);
    }
    if (hsts.maxAge != null) {
      hstsConfig.setMaxAgeSeconds(parseDurationToSeconds(String.valueOf(hsts.maxAge)));
    }
    if (hsts.includeSubDomains != null) {
      hstsConfig.setIncludeSubDomains(hsts.includeSubDomains);
    }
    if (hsts.preload != null) {
      hstsConfig.setPreload(hsts.preload);
    }
  }

  private void applyFrameOptionsConfig(InsightConfig config, DropwizardWebConfig webConfig) {
    DropwizardWebConfig.FrameOptions fo = webConfig.frameOptions;
    if (fo == null) {
      return;
    }

    InsightConfig.FrameOptionsConfig frameOptionsConfig = config.getFrameOptionsConfig();

    if (fo.enabled != null) {
      frameOptionsConfig.setEnabled(fo.enabled);
    }

    if (fo.option != null) {
      try {
        String normalizedOption = fo.option.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        frameOptionsConfig.setOption(InsightConfig.FrameOptionsConfig.FrameOption.valueOf(normalizedOption));
      }
      catch (IllegalArgumentException e) {
        log.warn("Ignoring unsupported web.frame-options.option '{}' as it is no longer supported.",
            fo.option);
      }
    }

    if (fo.origin != null && !fo.origin.trim().isEmpty()) {
      String trimmedOrigin = fo.origin.trim();
      if (trimmedOrigin.contains("\r") || trimmedOrigin.contains("\n")) {
        throw new IllegalStateException("web.frame-options.origin must not contain CR or LF characters");
      }
      frameOptionsConfig.setOrigin(trimmedOrigin);
    }

    if (frameOptionsConfig.getOption() == InsightConfig.FrameOptionsConfig.FrameOption.ALLOW_FROM
        && (frameOptionsConfig.getOrigin() == null || frameOptionsConfig.getOrigin().isBlank()))
    {
      log.warn("Ignoring invalid legacy web.frame-options configuration because ALLOW_FROM requires origin.");
      frameOptionsConfig.setOption(InsightConfig.FrameOptionsConfig.FrameOption.DENY);
    }
  }

  /**
   * Parse Dropwizard duration strings like "365 days", "24 hours", "30 minutes" into seconds.
   */
  static long parseDurationToSeconds(String duration) {
    try {
      return DropwizardDurationParser.parse(duration).getSeconds();
    }
    catch (IllegalArgumentException e) {
      throw new IllegalStateException("Invalid web.hsts.maxAge value '" + duration + "'", e);
    }
  }

  private void validateConnectorCount(List<DropwizardConnectorConfig> connectors, String propertyName) {
    if (connectors != null && connectors.size() > 1) {
      throw new IllegalStateException(
          "Dropwizard-to-Spring compatibility: multiple connectors are not supported for " + propertyName
              + "; refusing startup instead of silently ignoring extra connectors.");
    }
  }

  private void applyConnectorPorts(InsightConfig config, DropwizardServerConfig serverConfig) {
    String ports = extractConnectorField(serverConfig.applicationConnectors, connector -> connector.port);
    if (ports != null) {
      config.setApplicationConnectorPorts(ports);
    }
  }

  private void applyConnectorTypes(InsightConfig config, DropwizardServerConfig serverConfig) {
    String appTypes = extractConnectorField(serverConfig.applicationConnectors, connector -> connector.type);
    if (appTypes != null) {
      config.setApplicationConnectorTypes(appTypes);
    }
    String adminTypes = extractConnectorField(serverConfig.adminConnectors, connector -> connector.type);
    if (adminTypes != null) {
      config.setAdminConnectorTypes(adminTypes);
    }
  }

  private String extractConnectorField(
      List<DropwizardConnectorConfig> connectors,
      Function<DropwizardConnectorConfig, Object> accessor)
  {
    if (connectors == null || connectors.isEmpty()) {
      return null;
    }
    return connectors.stream()
        .map(accessor)
        .filter(value -> value != null)
        .map(String::valueOf)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .reduce((left, right) -> left + "," + right)
        .orElse(null);
  }

  private void applyConfiguredLogFiles(
      InsightConfig config,
      DropwizardLoggingConfig loggingConfig,
      DropwizardServerConfig serverConfig)
  {
    if (loggingConfig != null) {
      config.setServerLogFilename(extractFirstFilename(loggingConfig.appenders));
      config.setAuditLogFilename(extractLoggerFilename(loggingConfig, "com.sonatype.insight.audit"));
      config.setPolicyViolationLogFilename(
          extractLoggerFilename(loggingConfig, "com.sonatype.insight.policy.violation"));
    }
    if (serverConfig != null && serverConfig.requestLog != null) {
      config.setRequestLogFilename(extractFirstFilename(serverConfig.requestLog.appenders));
    }
  }

  @SuppressWarnings("unchecked")
  private String extractLoggerFilename(DropwizardLoggingConfig loggingConfig, String loggerName) {
    if (loggingConfig.loggers == null) {
      return null;
    }
    Object loggerValue = loggingConfig.loggers.get(loggerName);
    if (!(loggerValue instanceof Map<?, ?> loggerMap)) {
      return null;
    }
    return extractFirstFilename(((Map<String, Object>) loggerMap).get("appenders"));
  }

  private String extractFirstFilename(Object appendersValue) {
    if (!(appendersValue instanceof List<?> appenders)) {
      return null;
    }
    return appenders.stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(appender -> appender.get("currentLogFilename"))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .findFirst()
        .orElse(null);
  }

  @SuppressWarnings("unchecked")
  private void resolveDbUrlPrecedence(Map<String, Object> configMap, String key) {
    Object value = configMap.get(key);
    if (value instanceof Map) {
      Map<String, Object> dbMap = (Map<String, Object>) value;
      Object url = dbMap.get("url");
      if (url instanceof String && !((String) url).isBlank()) {
        dbMap.remove("hostname");
        dbMap.remove("port");
        dbMap.remove("name");
        dbMap.remove("parameters");
      }
    }
  }
}
