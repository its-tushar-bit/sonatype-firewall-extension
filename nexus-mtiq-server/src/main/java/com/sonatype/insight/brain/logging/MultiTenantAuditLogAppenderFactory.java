/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.spring.config.DropwizardAppenderFactory;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.error.exception.InternalServerException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating multi-tenant audit log appenders.
 *
 * <p>
 * This class provides utility methods for audit log file paths and creates
 * a SiftingAppender that separates audit logs by tenant.
 * </p>
 *
 * <p>
 * Converted from Dropwizard AbstractAppenderFactory to pure Logback/Spring Boot.
 * The appender is configured via Spring Boot's logging system.
 * </p>
 */
public class MultiTenantAuditLogAppenderFactory
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantAuditLogAppenderFactory.class);

  private static final String DISCRIMINATOR_KEY = "tenant";

  private static final String DISCRIMINATOR_DEFAULT_VALUE = "undefined";

  private static final String AUDIT_LOG_NAME = "audit.log";

  private static final String AUDIT_LOG_PATH = "%s/%s/log";

  private static final String AUDIT_LOG_FILENAME = AUDIT_LOG_PATH + "/" + AUDIT_LOG_NAME;

  /**
   * The %%d is escaped so that results in a '%d' to define the rollover specifier (daily) and the timezone specifier
   * (UTC).
   * The '.gz' suffix automatically enables compression on rolled logs.
   * See the logback docs on
   * <a href="https://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy">TimeBasedRollingPolicy</a>
   */
  private static final String AUDIT_ARCHIVE_LOG_FILENAME = AUDIT_LOG_PATH + "/audit-%%d{yyyy-MM-dd, UTC}.log.gz";

  private static final DateTimeFormatter AUDIT_ARCHIVE_LOG_FORMATTER =
      DateTimeFormatter.ofPattern("'audit-'yyyy-MM-dd'.log.gz'");

  private static String auditLogBasePath;

  private MultiTenantAuditLogAppenderFactory() {
    // Utility class - not instantiable
  }

  /**
   * Set the base path for audit logs. Called during application startup.
   */
  public static void setAuditLogBasePath(final String path) {
    MultiTenantAuditLogAppenderFactory.auditLogBasePath = path;
  }

  public static String getAuditLogFileName(String tenantSlug) {
    return String.format(AUDIT_LOG_FILENAME, auditLogBasePath, tenantSlug);
  }

  @VisibleForTesting
  static String getAuditLogBasePathForTesting() {
    return auditLogBasePath;
  }

  @VisibleForTesting
  static void setAuditLogBasePathForTesting(String path) {
    MultiTenantAuditLogAppenderFactory.auditLogBasePath = path;
  }

  /**
   * Create a SiftingAppender for multi-tenant audit logging.
   * This should be called during Logback configuration.
   */
  public static Appender<ILoggingEvent> createAppender(final LoggerContext loggerContext, final Object layoutConfig) {
    final SiftingAppender siftingAppender = new SiftingAppender();
    siftingAppender.setName("audit-log-sift-appender");
    siftingAppender.setContext(loggerContext);

    MDCBasedDiscriminator mdcBasedDiscriminator = new MDCBasedDiscriminator();
    mdcBasedDiscriminator.setKey(DISCRIMINATOR_KEY);
    mdcBasedDiscriminator.setDefaultValue(DISCRIMINATOR_DEFAULT_VALUE);
    mdcBasedDiscriminator.start();
    siftingAppender.setDiscriminator(mdcBasedDiscriminator);

    siftingAppender.setAppenderFactory((innerContext, discriminatingValue) -> {
      final RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
      rollingFileAppender.setName("audit-log-rolling-file-appender");
      rollingFileAppender.setContext(innerContext);

      String auditLogFile = getAuditLogFileName(discriminatingValue);
      log("auditLogFile=" + auditLogFile);

      rollingFileAppender.setFile(auditLogFile);
      rollingFileAppender.setBufferSize(new FileSize(FileAppender.DEFAULT_BUFFER_SIZE));

      LayoutWrappingEncoder<ILoggingEvent> layoutEncoder =
          DropwizardAppenderFactory.createEncoderWithLayout((LoggerContext) innerContext, "%msg%n", layoutConfig);
      layoutEncoder.start();
      rollingFileAppender.setEncoder(layoutEncoder);

      final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
      final TimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent> triggeringPolicy =
          new DefaultTimeBasedFileNamingAndTriggeringPolicy<>();
      triggeringPolicy.setContext(innerContext);
      triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);
      rollingFileAppender.setTriggeringPolicy(triggeringPolicy);

      rollingPolicy.setContext(innerContext);

      String auditArchiveLogFilePattern =
          String.format(AUDIT_ARCHIVE_LOG_FILENAME, auditLogBasePath, discriminatingValue);
      log("auditArchiveLogFilePattern=" + auditArchiveLogFilePattern);

      rollingPolicy.setFileNamePattern(auditArchiveLogFilePattern);

      // This property will asynchronously delete older files based on the rollover period set: yyyy-MM-dd (daily).
      // That means the retention period of archive log files is 30 days.
      rollingPolicy.setMaxHistory(30);

      rollingFileAppender.setRollingPolicy(rollingPolicy);

      rollingPolicy.setParent(rollingFileAppender);
      rollingPolicy.start();

      rollingFileAppender.start();

      return rollingFileAppender;
    });
    siftingAppender.start();

    return siftingAppender;
  }

  public static List<File> getAuditLogFiles(final LocalDate startUtcDate, final LocalDate endUtcDate) {
    String auditLogParentFolder = getAuditLogParentFolder();
    if (auditLogParentFolder == null || auditLogParentFolder.contains("null") ||
        auditLogParentFolder.contains("notused"))
    {
      throw new InternalServerException("Cannot get the audit log path.");
    }

    try (Stream<Path> stream = Files.list(Paths.get(auditLogParentFolder))) {
      return stream
          .filter(path -> filterForAuditLogs(path, startUtcDate, endUtcDate))
          .map(Path::toFile)
          .sorted()
          .collect(Collectors.toList());
    }
    catch (IOException e) {
      throw new UncheckedIOException("Error listing audit log files: " + e.getMessage(), e);
    }
  }

  private static boolean filterForAuditLogs(final Path path, final LocalDate startUtcDate, final LocalDate endUtcDate) {
    if (Files.isDirectory(path)) {
      return false;
    }

    String fileName = path.getFileName().toString();
    if (fileName.equals(AUDIT_LOG_NAME) && endUtcDate.isEqual(LocalDate.now())) {
      return true;
    }

    try {
      LocalDate fileDate = LocalDate.parse(fileName, AUDIT_ARCHIVE_LOG_FORMATTER);
      if (fileDate.isBefore(startUtcDate) || fileDate.isAfter(endUtcDate)) {
        return false;
      }
    }
    catch (DateTimeParseException e) {
      return false;
    }

    return true;
  }

  private static String getAuditLogParentFolder() {
    String currentLogFilename = getAuditLogFileName(TenantThreadLocal.getTenant().tenantSlug);
    Path parent = Paths.get(currentLogFilename).getParent();
    if (parent == null) {
      return ".";
    }

    return parent.toString();
  }

  private static void log(String message) {
    // Since logging may not be initialized yet, we cannot write to the console.
    System.out.println(MultiTenantAuditLogAppenderFactory.class.getSimpleName() + ": " + message);
    log.debug(message);
  }
}
