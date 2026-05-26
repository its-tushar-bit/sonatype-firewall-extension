/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.sonatype.insight.brain.service.InsightConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class DropwizardLoggingAppenderConfiguration
{
  private static final org.slf4j.Logger log =
      LoggerFactory.getLogger(DropwizardLoggingAppenderConfiguration.class);

  private final DropwizardConfigSourceReader configSourceReader = new DropwizardConfigSourceReader();

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SmartInitializingSingleton dropwizardLoggingAppenderInitializer(final InsightConfig insightConfig) {
    return () -> {
      DropwizardLoggingConfig loggingConfig = insightConfig.getDropwizardLoggingConfig();
      if (loggingConfig == null) {
        return;
      }
      LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

      if (loggingConfig.appenders != null) {
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAndStopAllAppenders();
        processAppenders(context, Logger.ROOT_LOGGER_NAME, loggingConfig.appenders);
      }

      applyLevel(context.getLogger(Logger.ROOT_LOGGER_NAME), loggingConfig.level);

      if (loggingConfig.loggers != null) {
        for (Map.Entry<String, Object> entry : loggingConfig.loggers.entrySet()) {
          String loggerName = entry.getKey();
          Object value = entry.getValue();

          if (value instanceof String levelString) {
            applyLevel(context.getLogger(loggerName), levelString);
            continue;
          }

          if (value instanceof Boolean boolValue) {
            applyLevel(context.getLogger(loggerName), boolValue.toString());
            continue;
          }

          if (!(value instanceof Map<?, ?> rawLoggerConfig)) {
            continue;
          }
          @SuppressWarnings("unchecked")
          Map<String, Object> loggerConfig = (Map<String, Object>) rawLoggerConfig;
          Logger logger = context.getLogger(loggerName);

          applyLevel(logger, loggerConfig.get("level"));

          Object additiveValue = loggerConfig.get("additive");
          if (additiveValue instanceof Boolean additive) {
            logger.setAdditive(additive);
          }

          Object loggerAppendersValue = loggerConfig.get("appenders");
          if (loggerAppendersValue instanceof List<?> loggerAppenders) {
            logger.detachAndStopAllAppenders();
            if (additiveValue == null) {
              logger.setAdditive(false);
            }
            processAppenders(context, loggerName, loggerAppenders);
          }
        }
      }
    };
  }

  private void applyLevel(Logger logger, Object levelValue) {
    if (levelValue == null) {
      return;
    }
    Level level = Level.toLevel(String.valueOf(levelValue), null);
    if (level != null) {
      logger.setLevel(level);
    }
  }

  @SuppressWarnings("unchecked")
  private void processAppenders(final LoggerContext context, final String loggerName, final List<?> appenders) {
    Logger logger = context.getLogger(loggerName);
    List<Runnable> deferredWarnings = new ArrayList<>();
    for (Object appenderEntry : appenders) {
      if (!(appenderEntry instanceof Map<?, ?>)) {
        continue;
      }
      String type = String.valueOf(((Map<String, Object>) appenderEntry).getOrDefault("type", ""));
      Appender<ILoggingEvent> appender = createAppender(context, loggerName, type, appenderEntry, deferredWarnings);
      if (appender != null) {
        logger.addAppender(appender);
      }
    }
    deferredWarnings.forEach(Runnable::run);
  }

  private Appender<ILoggingEvent> createAppender(
      LoggerContext context,
      String loggerName,
      String type,
      Object rawConfig,
      List<Runnable> deferredWarnings)
  {
    String appenderName = loggerName + "." + type + ".appender";
    String context2 = "logging appender for '" + loggerName + "'";

    DropwizardAppenderConfig config;
    Appender<ILoggingEvent> appender = switch (type) {
      case "console" -> {
        DropwizardAppenderConfig.Console typedConfig =
            configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Console.class);
        deferredWarnings.add(() -> DropwizardConfigCompat.warnOnDeprecatedFields(typedConfig, context2));
        config = typedConfig;
        yield createConsoleAppender(context, appenderName, typedConfig);
      }
      case "file" -> {
        DropwizardAppenderConfig.File typedConfig =
            configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.File.class);
        deferredWarnings.add(() -> DropwizardConfigCompat.warnOnDeprecatedFields(typedConfig, context2));
        config = typedConfig;
        yield createFileAppender(context, appenderName, loggerName, typedConfig);
      }
      case "syslog" -> {
        DropwizardAppenderConfig.Syslog typedConfig =
            configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Syslog.class);
        deferredWarnings.add(() -> DropwizardConfigCompat.warnOnDeprecatedFields(typedConfig, context2));
        config = typedConfig;
        yield createSyslogAppender(context, appenderName, typedConfig);
      }
      case "tcp" -> {
        DropwizardAppenderConfig.Tcp typedConfig =
            configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Tcp.class);
        deferredWarnings.add(() -> DropwizardConfigCompat.warnOnDeprecatedFields(typedConfig, context2));
        config = typedConfig;
        yield createTcpAppender(context, appenderName, typedConfig);
      }
      case "tls" -> {
        DropwizardAppenderConfig.Tls typedConfig =
            configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Tls.class);
        deferredWarnings.add(() -> DropwizardConfigCompat.warnOnDeprecatedFields(typedConfig, context2));
        config = typedConfig;
        yield createTlsAppender(context, appenderName, typedConfig);
      }
      case "udp" -> {
        DropwizardAppenderConfig.Udp typedConfig =
            configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Udp.class);
        deferredWarnings.add(() -> DropwizardConfigCompat.warnOnDeprecatedFields(typedConfig, context2));
        config = typedConfig;
        deferredWarnings.add(() -> log.warn(
            "UDP logging appender for logger '{}' is no longer supported and will be ignored",
            loggerName));
        yield null;
      }
      default -> {
        deferredWarnings.add(() -> log.warn(
            "Unrecognized Dropwizard logging appender type '{}' for logger '{}' will be ignored",
            type, loggerName));
        config = null;
        yield null;
      }
    };

    if (appender != null && config != null) {
      DropwizardAppenderFactory.applyThresholdFilter(appender, config.threshold);
      int queueSize = config.queueSize != null ? config.queueSize : DropwizardAppenderFactory.DEFAULT_QUEUE_SIZE;
      int discardingThreshold = config.discardingThreshold != null
          ? config.discardingThreshold
          : DropwizardAppenderFactory.DEFAULT_DISCARDING_THRESHOLD;
      boolean neverBlock = Boolean.TRUE.equals(config.neverBlock);
      appender = DropwizardAppenderFactory.wrapAsync(context, appender, queueSize, discardingThreshold, neverBlock);
    }
    return appender;
  }

  private Appender<ILoggingEvent> createConsoleAppender(
      LoggerContext context,
      String name,
      DropwizardAppenderConfig.Console config)
  {
    return DropwizardAppenderFactory.createConsoleAppender(context, name, config.logFormat, config.target,
        config.layout);
  }

  private Appender<ILoggingEvent> createFileAppender(
      LoggerContext context,
      String name,
      String loggerName,
      DropwizardAppenderConfig.File config)
  {
    if (config.currentLogFilename == null || config.currentLogFilename.isBlank()) {
      log.warn("File appender for logger '{}' is missing 'currentLogFilename' and will be skipped", loggerName);
      return null;
    }
    boolean archive = !Boolean.FALSE.equals(config.archive);
    return DropwizardAppenderFactory.createFileAppender(context, name, config.currentLogFilename,
        config.archivedLogFilenamePattern, config.archivedFileCount, config.logFormat, archive, config.layout);
  }

  private Appender<ILoggingEvent> createSyslogAppender(
      LoggerContext context,
      String name,
      DropwizardAppenderConfig.Syslog config)
  {
    int port = config.port != null ? config.port : 514;
    return DropwizardAppenderFactory.createSyslogAppender(
        context, name, config.host, port, config.facility, config.logFormat, config.stackTracePrefix);
  }

  private Appender<ILoggingEvent> createTcpAppender(
      LoggerContext context,
      String name,
      DropwizardAppenderConfig.Tcp config)
  {
    int port = config.port != null ? config.port : 4560;
    Duration connectionTimeout = parseDuration(config.connectionTimeout);
    boolean includeCallerData = Boolean.TRUE.equals(config.includeCallerData);
    return DropwizardAppenderFactory.createTcpAppender(
        context, name, config.host, port, connectionTimeout, includeCallerData);
  }

  private Appender<ILoggingEvent> createTlsAppender(
      LoggerContext context,
      String name,
      DropwizardAppenderConfig.Tls config)
  {
    int port = config.port != null ? config.port : 4560;
    boolean includeCallerData = Boolean.TRUE.equals(config.includeCallerData);
    return DropwizardAppenderFactory.createTlsAppender(context, name, config.host, port, includeCallerData);
  }

  private Duration parseDuration(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return DropwizardDurationParser.parse(value);
    }
    catch (IllegalArgumentException e) {
      log.warn("Could not parse duration '{}': {}", value, e.getMessage());
      return null;
    }
  }

}
