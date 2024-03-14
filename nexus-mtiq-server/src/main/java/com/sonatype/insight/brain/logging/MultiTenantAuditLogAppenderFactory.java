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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.AbstractAppenderFactory;
import io.dropwizard.logging.async.AsyncAppenderFactory;
import io.dropwizard.logging.filter.LevelFilterFactory;
import io.dropwizard.logging.layout.LayoutFactory;
import io.dropwizard.util.DataSize;
import io.dropwizard.validation.MinDataSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonTypeName("mtiq-audit-log")
public class MultiTenantAuditLogAppenderFactory
    extends AbstractAppenderFactory<ILoggingEvent>
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantAuditLogAppenderFactory.class);

  private static final String DISCRIMINATOR_KEY = "tenant";

  private static final String DISCRIMINATOR_DEFAULT_VALUE = "undefined";

  // The path for audit logs is configured in the Dropwizard config yaml file.
  // There is a separate dir for each tenant.
  // The first '%s' for the audit logs path and the second '%s' is for the tenant.
  // For ex, if the audit logs path is configured to sonatype-work/clm-cluster:
  // sonatype-work/clm-cluster/global/log
  // sonatype-work/clm-cluster/tenant-1/log
  private static final String AUDIT_LOG_PATH = "%s/%s/log";

  private static final String AUDIT_LOG_FILENAME = AUDIT_LOG_PATH + "/audit.log";

  /**
   * The %%d is escaped so that results in a '%d' to define the rollover specifier (daily) and the timezone specifier
   * (UTC).
   * The '.gz' suffix automatically enables compression on rolled logs.
   * See the logback docs on
   * <a href="https://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy">TimeBasedRollingPolicy</a>
   */
  private static final String AUDIT_ARCHIVE_LOG_FILENAME = AUDIT_LOG_PATH + "/audit-%%d{yyyy-MM-dd, UTC}.log.gz";

  @JsonProperty
  private static String auditLogBasePath;

  @MinDataSize(1)
  private final DataSize bufferSize = DataSize.bytes(FileAppender.DEFAULT_BUFFER_SIZE);

  public static String getAuditLogFileName(String tenantSlug) {
    return String.format(AUDIT_LOG_FILENAME, auditLogBasePath, tenantSlug);
  }

  /**
   * This method is used by Dropwizard when it loads this class during logging initialization.
   * It reads the the param value from the auditLogBasePath property in the config yaml file.
   */
  public void setAuditLogBasePath(final String auditLogBasePath) {
    MultiTenantAuditLogAppenderFactory.auditLogBasePath = auditLogBasePath;
  }

  @Override
  public Appender<ILoggingEvent> build(
      final LoggerContext loggerContext,
      final String unused,
      final LayoutFactory<ILoggingEvent> layoutFactory,
      final LevelFilterFactory<ILoggingEvent> levelFilterFactory,
      final AsyncAppenderFactory<ILoggingEvent> asyncAppenderFactory)
  {
    final SiftingAppender siftingAppender = new SiftingAppender();
    siftingAppender.setName("audit-log-sift-appender");
    siftingAppender.setContext(loggerContext);

    MDCBasedDiscriminator mdcBasedDiscriminator = new MDCBasedDiscriminator();
    mdcBasedDiscriminator.setKey(DISCRIMINATOR_KEY);
    mdcBasedDiscriminator.setDefaultValue(DISCRIMINATOR_DEFAULT_VALUE);
    mdcBasedDiscriminator.start();
    siftingAppender.setDiscriminator(mdcBasedDiscriminator);

    final LayoutWrappingEncoder<ILoggingEvent> layoutEncoder = new LayoutWrappingEncoder<>();
    layoutEncoder.setLayout(buildLayout(loggerContext, layoutFactory));

    log("auditLogBasePath=" + auditLogBasePath);

    siftingAppender.setAppenderFactory((innerContext, discriminatingValue) -> {
      final RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<>();
      rollingFileAppender.setName("audit-log-rolling-file-appender");
      rollingFileAppender.setContext(innerContext);

      String auditLogFile = getAuditLogFileName(discriminatingValue);
      log("auditLogFile=" + auditLogFile);

      rollingFileAppender.setFile(auditLogFile);
      rollingFileAppender.setBufferSize(new FileSize(bufferSize.toBytes()));
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

    return wrapAsync(siftingAppender, asyncAppenderFactory);
  }

  private void log(String message) {
    // Since logging may not be initialized yet, we cannot write to the console.
    System.out.println(getClass().getSimpleName() + ": " + message);
    log.debug(message);
  }
}
