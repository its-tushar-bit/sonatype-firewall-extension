/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.policy.violation.AbstractPolicyViolationLogger;
import com.sonatype.insight.brain.service.InsightConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.sonatype.insight.brain.testsupport.TempFolder;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

public class DefaultLogLevelConfigurationTest
{
  @RegisterExtension
  public TempFolder tempFolder = new TempFolder();

  private final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

  private final List<String> cleanupLoggerNames = new ArrayList<>();

  @AfterEach
  public void tearDown() {
    for (String name : cleanupLoggerNames) {
      Logger logger = loggerContext.getLogger(name);
      logger.detachAndStopAllAppenders();
      logger.setLevel(null);
      logger.setAdditive(true);
    }
  }

  @Test
  public void testDefaultLogLevels_appliedWhenNotConfigured() throws IOException {
    initializeDefaults("sonatypeWork: ./work\n");

    assertLoggerLevel("org.jooq.tools", Level.WARN);
    assertLoggerLevel("org.jooq.Constants", Level.OFF);
    assertLoggerLevel("com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueProducer", Level.INFO);
    assertLoggerLevel("com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConsumer", Level.INFO);
    assertLoggerLevel("org.glassfish.jersey.internal.inject.Providers", Level.ERROR);
    assertLoggerLevel("org.spdx.core.SimpleUriValue", Level.ERROR);
    assertLoggerLevel("org.spdx.library.model.SimpleUriValue", Level.ERROR);
  }

  @Test
  public void testDefaultLogLevels_preservedWhenAlreadySet() throws IOException {
    Logger jooqLogger = trackLogger("org.jooq.tools");
    jooqLogger.setLevel(Level.DEBUG);

    initializeDefaults("sonatypeWork: ./work\n");

    assertThat(jooqLogger.getLevel()).isEqualTo(Level.DEBUG);
  }

  @Test
  public void testDefaultAuditLogger_createdWhenNotUserConfigured() throws IOException {
    initializeDefaults("sonatypeWork: ./work\n");

    Logger auditLogger = trackLogger(AuditRecorder.BASE_LOGGER_NAME);
    assertThat(auditLogger.isAdditive()).isFalse();
    assertThat(findFileAppender(auditLogger)).isNotNull();
    assertThat(findFileAppender(auditLogger).getFile()).isEqualTo("./log/audit.log");
  }

  @Test
  public void testDefaultAuditLogger_skippedWhenUserConfigured() throws IOException {
    initializeDefaults(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"com.sonatype.insight.audit\":",
        "      appenders:",
        "        - type: console",
        ""));

    Logger auditLogger = trackLogger(AuditRecorder.BASE_LOGGER_NAME);
    assertThat(findFileAppender(auditLogger)).isNull();
  }

  @Test
  public void testDefaultPolicyViolationLogger_offWhenNotUserConfigured() throws IOException {
    initializeDefaults("sonatypeWork: ./work\n");

    Logger pvLogger = trackLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    assertThat(pvLogger.getLevel()).isEqualTo(Level.OFF);
    assertThat(pvLogger.isAdditive()).isFalse();
  }

  @Test
  public void testDefaultPolicyViolationLogger_skippedWhenUserConfigured() throws IOException {
    initializeDefaults(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"com.sonatype.insight.policy.violation\":",
        "      appenders:",
        "        - type: console",
        ""));

    Logger pvLogger = trackLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    assertThat(pvLogger.getLevel()).isNull();
  }

  private void initializeDefaults(String yaml) throws IOException {
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), yaml);

    DropwizardConfigSourceReader reader = new DropwizardConfigSourceReader();
    Map<String, Object> configMap = reader.readConfigMap(configFile);
    InsightConfig insightConfig = reader.convertValue(configMap, InsightConfig.class);

    trackLogger(AuditRecorder.BASE_LOGGER_NAME);
    trackLogger(AbstractPolicyViolationLogger.POLICY_VIOLATION_LOGGER_NAME);
    trackLogger("org.jooq.tools");
    trackLogger("org.jooq.Constants");
    trackLogger("com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueProducer");
    trackLogger("com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConsumer");
    trackLogger("org.glassfish.jersey.internal.inject.Providers");
    trackLogger("org.spdx.core.SimpleUriValue");
    trackLogger("org.spdx.library.model.SimpleUriValue");

    DefaultLogLevelConfiguration config = new DefaultLogLevelConfiguration();
    config.defaultLogLevelInitializer(insightConfig).afterSingletonsInstantiated();
  }

  private Logger trackLogger(String name) {
    cleanupLoggerNames.add(name);
    return loggerContext.getLogger(name);
  }

  private void assertLoggerLevel(String name, Level expected) {
    Logger logger = trackLogger(name);
    assertThat(logger.getLevel()).isEqualTo(expected);
  }

  private FileAppender<?> findFileAppender(Logger logger) {
    Iterator<Appender<ILoggingEvent>> iter = logger.iteratorForAppenders();
    while (iter.hasNext()) {
      Appender<ILoggingEvent> appender = iter.next();
      if (appender instanceof ch.qos.logback.classic.AsyncAppender asyncAppender) {
        Iterator<Appender<ILoggingEvent>> innerIter = asyncAppender.iteratorForAppenders();
        while (innerIter.hasNext()) {
          Appender<ILoggingEvent> inner = innerIter.next();
          if (inner instanceof FileAppender<?> fileAppender) {
            return fileAppender;
          }
        }
      }
      if (appender instanceof FileAppender<?> fileAppender) {
        return fileAppender;
      }
    }
    return null;
  }
}
