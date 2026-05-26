/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.telemetry.UserTelemetryRequestLoggingFilter;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.json.AccessJsonLayoutBaseFactory;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

@Configuration
public class RequestLoggingConfiguration
{
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(RequestLoggingConfiguration.class);

  static final String DEFAULT_REQUEST_LOG_TIME_ZONE = "UTC";

  static final String DEFAULT_REQUEST_LOG_FORMAT =
      "%{client}a - %u [%{dd/MMM/yyyy:HH:mm:ss Z|__TIME_ZONE__}t] \"%r\" %s %O %D \"%{User-Agent}i\"";

  private static final String LEGACY_REQUEST_LOG_FORMAT =
      "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent}\"";

  private static final String REQUEST_LOGGER_NAME_PREFIX = "com.sonatype.insight.requestlog.";

  @Bean
  UserTelemetryRequestLoggingFilter userTelemetryRequestLoggingFilter() {
    return new UserTelemetryRequestLoggingFilter();
  }

  @Bean
  WebServerFactoryCustomizer<JettyServletWebServerFactory> requestLoggingCustomizer(
      final InsightConfig insightConfig,
      final UserTelemetryRequestLoggingFilter userTelemetryRequestLoggingFilter,
      final Environment environment)
  {
    return factory -> {
      Object appendersValue = getDropwizardConfigProperty(environment, "server.requestLog.appenders");
      if (!(appendersValue instanceof List<?> appenders) || appenders.isEmpty()) {
        return;
      }

      Map<String, Object> accessJsonLayout = findAccessJsonLayout(appenders);
      if (accessJsonLayout != null) {
        factory.addServerCustomizers(server -> server.setRequestLog(
            createAccessJsonRequestLog(accessJsonLayout, userTelemetryRequestLoggingFilter)));
        return;
      }

      RequestLogSettings requestLogSettings = requestLogSettings(environment, appenders);
      String requestLogFilename = insightConfig.getRequestLogFilename();
      if (!requestLogSettings.enabled() || requestLogFilename == null || requestLogFilename.isBlank()) {
        return;
      }

      factory.addServerCustomizers(server -> server.setRequestLog(requestLog(
          requestLogWriter(requestLogFilename, requestLogSettings),
          userTelemetryRequestLoggingFilter,
          requestLogSettings)));
    };
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> findAccessJsonLayout(List<?> appenders) {
    for (Object appender : appenders) {
      if (!(appender instanceof Map<?, ?> appenderMap)) {
        continue;
      }
      Object layout = ((Map<String, Object>) appenderMap).get("layout");
      if (layout instanceof Map<?, ?> layoutMap
          && "access-json".equals(((Map<String, Object>) layoutMap).get("type")))
      {
        return (Map<String, Object>) layoutMap;
      }
    }
    return null;
  }

  private RequestLog createAccessJsonRequestLog(
      Map<String, Object> layoutMap,
      UserTelemetryRequestLoggingFilter telemetryFilter)
  {
    log.info("Configuring access-json request log layout");
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    AccessJsonLayoutBaseFactory factory =
        Jackson.newObjectMapper().convertValue(layoutMap, AccessJsonLayoutBaseFactory.class);
    Layout<IAccessEvent> layout = factory.build(loggerContext, TimeZone.getTimeZone("UTC"));
    layout.setContext(loggerContext);
    layout.start();

    LayoutWrappingEncoder<IAccessEvent> encoder = new LayoutWrappingEncoder<>();
    encoder.setContext(loggerContext);
    encoder.setLayout(layout);
    encoder.start();

    ConsoleAppender<IAccessEvent> consoleAppender = new ConsoleAppender<>();
    consoleAppender.setName("access-json-console");
    consoleAppender.setContext(loggerContext);
    consoleAppender.setEncoder(encoder);
    consoleAppender.start();

    RequestLogImpl requestLog = new RequestLogImpl();
    requestLog.setQuiet(true);
    requestLog.addAppender(consoleAppender);
    requestLog.start();

    return (request, response) -> {
      if (!telemetryFilter.shouldSkip(Request.getPathInContext(request))) {
        requestLog.log(request, response);
      }
    };
  }

  RequestLog.Writer requestLogWriter(final String requestLogFilename, final RequestLogSettings requestLogSettings) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    String loggerName = REQUEST_LOGGER_NAME_PREFIX + Integer.toHexString(requestLogFilename.hashCode());
    Logger logger = loggerContext.getLogger(loggerName);

    logger.detachAndStopAllAppenders();
    logger.setAdditive(false);
    logger.setLevel(Level.INFO);
    logger.addAppender(requestLogAppender(loggerContext, loggerName, requestLogFilename, requestLogSettings));

    return new LogbackRequestLogWriter(logger);
  }

  Appender<ILoggingEvent> requestLogAppender(
      final LoggerContext loggerContext,
      final String loggerName,
      final String requestLogFilename,
      final RequestLogSettings requestLogSettings)
  {
    String archivePattern = resolveArchiveFileNamePattern(requestLogFilename, requestLogSettings);
    return DropwizardAppenderFactory.createFileAppender(
        loggerContext,
        loggerName + ".appender",
        requestLogFilename,
        archivePattern,
        requestLogSettings.retainDays(),
        "%msg%n",
        requestLogSettings.archiveEnabled());
  }

  CustomRequestLog requestLog(
      final RequestLog.Writer requestLogWriter,
      final UserTelemetryRequestLoggingFilter userTelemetryRequestLoggingFilter,
      final RequestLogSettings requestLogSettings)
  {
    CustomRequestLog requestLog = new CustomRequestLog(requestLogWriter, requestLogSettings.format());
    requestLog.setFilter(
        (request, response) -> !userTelemetryRequestLoggingFilter.shouldSkip(Request.getPathInContext(request)));
    return requestLog;
  }

  RequestLogSettings requestLogSettings(final Environment environment) {
    Object appendersValue = getDropwizardConfigProperty(environment, "server.requestLog.appenders");
    if (!(appendersValue instanceof List<?> appenders)) {
      return RequestLogSettings.disabled();
    }
    return requestLogSettings(environment, appenders);
  }

  RequestLogSettings requestLogSettings(final Environment environment, List<?> appenders) {
    Map<String, Object> fileAppender = appenders.stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(this::asMap)
        .filter(appender -> "file".equals(stringProperty(appender.get("type"))))
        .findFirst()
        .orElse(Map.of());

    String requestLogType = stringProperty(getDropwizardConfigProperty(environment, "server.requestLog.type"));
    if (requestLogType != null && !requestLogType.isBlank() && !"classic".equalsIgnoreCase(requestLogType)) {
      return RequestLogSettings.disabled();
    }

    boolean unsupportedAppenderShape = appenders.stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(this::asMap)
        .anyMatch(appender -> !isSupportedRequestLogAppender(appender));
    if (unsupportedAppenderShape) {
      log.warn("Request log appenders with unsupported configuration (e.g. filterFactories) are not supported"
          + " and are no longer supported. Request logging is disabled."
          + " Remove unsupported keys from server.requestLog.appenders to re-enable request logging.");
      return RequestLogSettings.disabled();
    }
    if (fileAppender.isEmpty() || isOff(fileAppender.get("threshold"))) {
      return RequestLogSettings.disabled();
    }

    String timeZone = stringProperty(getDropwizardConfigProperty(environment, "server.requestLog.timeZone"));
    if (timeZone == null || timeZone.isBlank()) {
      timeZone = DEFAULT_REQUEST_LOG_TIME_ZONE;
    }

    String configuredFormat = stringProperty(fileAppender.get("logFormat"));
    Integer retainDays = integerProperty(fileAppender.get("archivedFileCount"));
    boolean archiveEnabled = !Boolean.FALSE.equals(booleanProperty(fileAppender.get("archive")));
    String archivedLogFilenamePattern = stringProperty(fileAppender.get("archivedLogFilenamePattern"));

    return new RequestLogSettings(
        true,
        toJettyRequestLogFormat(configuredFormat, timeZone),
        timeZone,
        retainDays,
        archiveEnabled,
        archivedLogFilenamePattern);
  }

  private Object getDropwizardConfigProperty(final Environment environment, final String propertyName) {
    if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
      return null;
    }

    for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
      if ("dropwizardConfig".equals(propertySource.getName())) {
        return propertySource.getProperty(propertyName);
      }
    }
    return null;
  }

  private String toJettyRequestLogFormat(final String configuredFormat, final String timeZone) {
    String legacyOrDefaultFormat = configuredFormat;
    if (legacyOrDefaultFormat == null || legacyOrDefaultFormat.isBlank()) {
      legacyOrDefaultFormat = LEGACY_REQUEST_LOG_FORMAT;
    }

    if (LEGACY_REQUEST_LOG_FORMAT.equals(legacyOrDefaultFormat)) {
      return DEFAULT_REQUEST_LOG_FORMAT.replace("__TIME_ZONE__", timeZone);
    }

    return legacyOrDefaultFormat
        .replace("%header{User-Agent}", "%{User-Agent}i")
        .replace("%clientHost", "%{client}a")
        .replace("%requestURL", "%r")
        .replace("%statusCode", "%s")
        .replace("%elapsedTime", "%D")
        .replace("%bytesSent", "%O")
        .replace("%date", "%{dd/MMM/yyyy:HH:mm:ss Z|" + timeZone + "}t")
        .replace("%user", "%u")
        .replaceAll("%l(?![a-zA-Z])", "-");
  }

  private String resolveArchiveFileNamePattern(
      final String requestLogFilename,
      final RequestLogSettings requestLogSettings)
  {
    if (requestLogSettings.archivedLogFilenamePattern() != null
        && !requestLogSettings.archivedLogFilenamePattern().isBlank())
    {
      return requestLogSettings.archivedLogFilenamePattern();
    }
    return DropwizardAppenderFactory.deriveArchivePattern(requestLogFilename);
  }

  private boolean isSupportedRequestLogAppender(final Map<String, Object> appender) {
    String type = stringProperty(appender.get("type"));
    Object filterFactories = appender.get("filterFactories");
    return ("console".equals(type) || "file".equals(type)) && filterFactories == null;
  }

  private boolean isOff(final Object thresholdValue) {
    String threshold = stringProperty(thresholdValue);
    return threshold != null && "OFF".equalsIgnoreCase(threshold);
  }

  private String stringProperty(final Object value) {
    return value instanceof String ? (String) value : null;
  }

  private Integer integerProperty(final Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value instanceof String stringValue && !stringValue.isBlank()) {
      try {
        return Integer.parseInt(stringValue);
      }
      catch (NumberFormatException e) {
        throw new IllegalStateException("Invalid integer value '" + stringValue + "'", e);
      }
    }
    return null;
  }

  private Boolean booleanProperty(final Object value) {
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    if (value instanceof String stringValue && !stringValue.isBlank()) {
      return Boolean.parseBoolean(stringValue);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(final Map<?, ?> value) {
    return (Map<String, Object>) value;
  }

  private record LogbackRequestLogWriter(Logger logger)
      implements RequestLog.Writer
  {
    @Override
    public void write(final String requestEntry) {
      logger.info(requestEntry);
    }
  }

  record RequestLogSettings(
      boolean enabled,
      String format,
      String timeZone,
      Integer retainDays,
      boolean archiveEnabled,
      String archivedLogFilenamePattern)
  {
    static RequestLogSettings disabled() {
      return new RequestLogSettings(false, null, DEFAULT_REQUEST_LOG_TIME_ZONE, null, false, null);
    }
  }
}
