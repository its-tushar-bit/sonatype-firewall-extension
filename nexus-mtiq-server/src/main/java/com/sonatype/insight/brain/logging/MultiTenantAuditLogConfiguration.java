/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.sonatype.insight.brain.audit.AuditRecorder;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for multi-tenant audit logging.
 *
 * <p>
 * Configures the audit log appender path and attaches it to the Logback context.
 * </p>
 */
@Configuration
public class MultiTenantAuditLogConfiguration
{
  @Bean
  public AuditLogPathInitializer auditLogPathInitializer(
      @Value("${auditLogBasePath:#{null}}") String auditLogBasePath)
  {
    return new AuditLogPathInitializer(auditLogBasePath);
  }

  /**
   * Initializes the audit log path and creates the appender on application ready.
   */
  public static class AuditLogPathInitializer
      implements ApplicationListener<ApplicationReadyEvent>
  {
    private final String auditLogBasePath;

    public AuditLogPathInitializer(String auditLogBasePath) {
      this.auditLogBasePath = auditLogBasePath;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
      if (auditLogBasePath != null && !auditLogBasePath.isEmpty()) {
        MultiTenantAuditLogAppenderFactory.setAuditLogBasePath(auditLogBasePath);

        // Create and attach the appender to the audit logger
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Appender<ILoggingEvent> appender = MultiTenantAuditLogAppenderFactory.createAppender(loggerContext);

        Logger auditLogger = loggerContext.getLogger(
            AuditRecorder.BASE_LOGGER_NAME);
        auditLogger.addAppender(appender);
        auditLogger.setAdditive(false); // Don't propagate to root logger
      }
    }
  }
}
