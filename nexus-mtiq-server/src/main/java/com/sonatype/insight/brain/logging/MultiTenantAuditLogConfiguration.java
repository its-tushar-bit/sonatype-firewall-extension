/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import static com.sonatype.insight.brain.spring.config.DropwizardConfigLoader.MTIQ_AUDIT_LOG_APPENDER_TYPE;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.sonatype.insight.brain.spring.config.DropwizardLoggingAppenderConfiguration.CustomAppenderFactory;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiTenantAuditLogConfiguration
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantAuditLogConfiguration.class);

  @Bean
  CustomAppenderFactory mtiqAuditLogAppenderFactory() {
    return new CustomAppenderFactory()
    {
      @Override
      public String supportedType() {
        return MTIQ_AUDIT_LOG_APPENDER_TYPE;
      }

      @Override
      public Appender<ILoggingEvent> create(LoggerContext context, Object rawConfig) {
        if (!(rawConfig instanceof Map<?, ?> rawMap)) {
          return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) rawMap;
        Object basePath = config.get("auditLogBasePath");
        if (basePath == null) {
          log.warn("mtiq-audit-log appender is missing 'auditLogBasePath' and will be skipped");
          return null;
        }
        MultiTenantAuditLogAppenderFactory.setAuditLogBasePath(basePath.toString());
        return MultiTenantAuditLogAppenderFactory.createAppender(context, config.get("layout"));
      }
    };
  }
}
