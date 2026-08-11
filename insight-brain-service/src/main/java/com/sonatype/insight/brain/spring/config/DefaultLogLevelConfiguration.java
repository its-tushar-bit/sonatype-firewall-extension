/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.service.InsightConfig;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Applies default logging configuration, ported from
 * {@code InsightConfigurationFactory.setDefaultLogSettings()}.
 * All defaults use putIfAbsent semantics so user overrides in config.yml take precedence.
 */
@Configuration
public class DefaultLogLevelConfiguration
{
  private static final String AUDIT_LOG_FILENAME = "./log/audit.log";

  private static final String AUDIT_LOG_ARCHIVE_PATTERN = "./log/audit-%d.log.gz";

  private static final int AUDIT_LOG_ARCHIVE_COUNT = 50;

  private static final String INDEPENDENT_LOG_FORMAT = "%message%n";

  private static final Map<String, Level> DEFAULT_LOG_LEVELS = Map.of(
      "org.jooq.tools", Level.WARN,
      "org.jooq.Constants", Level.OFF,
      "com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueProducer", Level.INFO,
      "com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConsumer", Level.INFO,
      // https://github.com/eclipse-ee4j/jersey/issues/3700
      "org.glassfish.jersey.internal.inject.Providers", Level.ERROR,
      // The SPDX library logs a WARN for any externalRef whose referenceType is not one of its
      // registered types (e.g. category OTHER with type "repository"). This is cosmetic: the SBOM
      // is still parsed and evaluated correctly, no component/vulnerability data is dropped.
      // Suppress the WARN noise while keeping ERROR visible. The emitting class name differs across
      // java-spdx-library versions (org.spdx.core.SimpleUriValue in 2.x, org.spdx.library.model in
      // 1.x), so both are covered defensively. CLM-36307
      "org.spdx.core.SimpleUriValue", Level.ERROR,
      "org.spdx.library.model.SimpleUriValue", Level.ERROR);

  @Bean
  @Order(Ordered.LOWEST_PRECEDENCE)
  SmartInitializingSingleton defaultLogLevelInitializer(InsightConfig insightConfig) {
    return () -> {
      LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
      setDefaultLogLevels(context);
      setDefaultAuditLogSettings(context, insightConfig);
      setDefaultPolicyViolationLogSettings(context, insightConfig);
    };
  }

  private void setDefaultLogLevels(LoggerContext context) {
    DEFAULT_LOG_LEVELS.forEach((name, level) -> {
      Logger logger = context.getLogger(name);
      if (logger.getLevel() == null) {
        logger.setLevel(level);
      }
    });
  }

  private void setDefaultAuditLogSettings(LoggerContext context, InsightConfig insightConfig) {
    if (isUserConfigured(insightConfig, AuditRecorder.BASE_LOGGER_NAME)) {
      return;
    }
    Logger logger = context.getLogger(AuditRecorder.BASE_LOGGER_NAME);
    logger.setAdditive(false);

    Appender<ILoggingEvent> fileAppender = DropwizardAppenderFactory.createFileAppender(
        context,
        AuditRecorder.BASE_LOGGER_NAME + ".file",
        AUDIT_LOG_FILENAME,
        AUDIT_LOG_ARCHIVE_PATTERN,
        AUDIT_LOG_ARCHIVE_COUNT,
        INDEPENDENT_LOG_FORMAT,
        true);
    Appender<ILoggingEvent> asyncAppender = DropwizardAppenderFactory.wrapAsync(
        context, fileAppender,
        DropwizardAppenderFactory.DEFAULT_QUEUE_SIZE,
        0, false);
    logger.addAppender(asyncAppender);

    if (insightConfig.getAuditLogFilename() == null) {
      insightConfig.setAuditLogFilename(AUDIT_LOG_FILENAME);
    }
  }

  private void setDefaultPolicyViolationLogSettings(LoggerContext context, InsightConfig insightConfig) {
    if (isUserConfigured(insightConfig, AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME)) {
      return;
    }
    Logger logger = context.getLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    logger.setLevel(Level.OFF);
    logger.setAdditive(false);
  }

  private boolean isUserConfigured(InsightConfig insightConfig, String loggerName) {
    DropwizardLoggingConfig loggingConfig = insightConfig.getLogging();
    return loggingConfig != null
        && loggingConfig.loggers != null
        && loggingConfig.loggers.containsKey(loggerName);
  }
}
