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
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.service.InsightConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
public class DropwizardLoggingAppenderConfiguration
{
  public interface CustomAppenderFactory
  {
    /**
     * The appender type this factory handles (e.g. "mtiq-audit-log").
     */
    String supportedType();

    /**
     * Create the appender. Only called when {@code type} matches {@link #supportedType()}.
     * The framework applies {@code threshold}, {@code queueSize}, {@code discardingThreshold}, and {@code neverBlock}
     * from the raw config, and wraps the appender in {@code AsyncAppender}. Do not add your own async wrapping.
     */
    Appender<ILoggingEvent> create(LoggerContext context, Object rawConfig);
  }

  private static final org.slf4j.Logger log =
      LoggerFactory.getLogger(DropwizardLoggingAppenderConfiguration.class);

  private static final String INDEPENDENT_JSON_LOG_FORMAT = "%message%n";

  private static final Set<String> INDEPENDENT_JSON_LOGGERS = Set.of(
      AuditRecorder.BASE_LOGGER_NAME,
      AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);

  private final DropwizardConfigSourceReader configSourceReader = new DropwizardConfigSourceReader();

  private final ObjectProvider<CustomAppenderFactory> customAppenderFactories;

  DropwizardLoggingAppenderConfiguration(ObjectProvider<CustomAppenderFactory> customAppenderFactories) {
    this.customAppenderFactories = customAppenderFactories;
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SmartInitializingSingleton dropwizardLoggingAppenderInitializer(final InsightConfig insightConfig) {
    return () -> {
      DropwizardLoggingConfig loggingConfig = insightConfig.getLogging();
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
    // A bare YAML 'OFF'/'ON' arrives as a Boolean; toLevel maps false -> OFF and true -> ALL like pre-Spring.
    Level level = DropwizardAppenderFactory.toLevel(levelValue, null);
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
    String warnContext = "logging appender for '" + loggerName + "'";

    // Shared strict parsing with the request-logging path. A null result means an unrecognized built-in type, which
    // may still be handled by a custom appender factory (e.g. mtiq-audit-log).
    DropwizardAppenderConfig config = DropwizardAppenderFactory.convertConfig(configSourceReader, type, rawConfig);

    if (config == null) {
      Appender<ILoggingEvent> customAppender = tryCreateCustomAppender(context, type, rawConfig);
      if (customAppender == null) {
        deferredWarnings.add(() -> log.warn(
            "Unrecognized Dropwizard logging appender type '{}' for logger '{}' will be ignored",
            type, loggerName));
        return null;
      }
      DropwizardAppenderConfig customConfig =
          configSourceReader.convertValue(rawConfig, DropwizardAppenderConfig.class);
      DropwizardAppenderFactory.applyThresholdFilter(customAppender, customConfig.threshold);
      return DropwizardAppenderFactory.wrapAsync(
          context, customAppender, DropwizardAppenderFactory.asyncSettings(customConfig));
    }

    deferredWarnings.add(() -> DropwizardConfigCompat.warnOnDeprecatedFields(config, warnContext));

    if (config instanceof DropwizardAppenderConfig.Udp) {
      deferredWarnings.add(() -> log.warn(
          "UDP logging appender for logger '{}' is not supported and will be ignored",
          loggerName));
      return null;
    }

    applyIndependentJsonLogFormat(loggerName, config);

    Appender<ILoggingEvent> appender = DropwizardAppenderFactory.createAppender(context, appenderName, type, config);
    if (appender == null) {
      return null;
    }
    DropwizardAppenderFactory.applyThresholdFilter(appender, config.threshold);
    return DropwizardAppenderFactory.wrapAsync(context, appender, DropwizardAppenderFactory.asyncSettings(config));
  }

  /**
   * Audit and policy-violation loggers write pre-formatted JSON as the log message, so their file/console/syslog
   * appenders must emit the raw message only. Without this, the standard pattern layout corrupts the JSON with a
   * level/timestamp/logger prefix. An explicit {@code logFormat} in the config is left untouched.
   *
   * <p>
   * For syslog appenders {@code logFormat} maps to {@link ch.qos.logback.classic.net.SyslogAppender}'s
   * suffix pattern (see {@code DropwizardAppenderFactory.createSyslogAppender}), so {@code %message%n} makes the
   * raw JSON the syslog suffix - the desired behaviour. TCP/TLS socket appenders are intentionally excluded: they
   * use logback's serialization protocol rather than a pattern layout, so {@code logFormat} has no effect there.
   *
   * <p>
   * Only the exact logger names are matched (not child loggers such as {@code com.sonatype.insight.audit.specific});
   * this reproduces pre-Spring behaviour, where {@code InsightConfigurationFactory} applied the override by exact-name
   * lookup of the audit/policy-violation logger nodes. Appenders are configured on the exact parent logger and child
   * loggers inherit them via additivity, so there are no child-logger appenders to format.
   */
  private void applyIndependentJsonLogFormat(String loggerName, DropwizardAppenderConfig config) {
    if (config instanceof DropwizardAppenderConfig.Tcp || config instanceof DropwizardAppenderConfig.Tls) {
      return;
    }
    if (config.logFormat == null && loggerName != null && INDEPENDENT_JSON_LOGGERS.contains(loggerName)) {
      // Intentional mutation of a per-call config object: convertConfig deserializes a fresh instance for each
      // appender, so this is not shared state.
      config.logFormat = INDEPENDENT_JSON_LOG_FORMAT;
    }
  }

  private Appender<ILoggingEvent> tryCreateCustomAppender(LoggerContext context, String type, Object rawConfig) {
    for (CustomAppenderFactory factory : customAppenderFactories.orderedStream().toList()) {
      if (!type.equals(factory.supportedType())) {
        continue;
      }
      try {
        return factory.create(context, rawConfig);
      }
      catch (RuntimeException e) {
        log.warn("CustomAppenderFactory {} threw an exception for appender type '{}': {}",
            factory.getClass().getSimpleName(), type, e.getMessage(), e);
      }
    }
    return null;
  }
}
