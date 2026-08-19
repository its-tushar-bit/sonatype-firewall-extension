/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import com.sonatype.insight.brain.logging.MultiTenantAuditLogAppenderFactory;
import org.springframework.context.annotation.Primary;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

@Named
@Singleton
@Primary
public class MultiTenantAuditLogFilesProvider
    implements AuditLogFilesProvider
{

  @Override
  public List<File> getAuditLogFiles(final LocalDate startUtcDate, final LocalDate endUtcDate) {
    return MultiTenantAuditLogAppenderFactory.getAuditLogFiles(startUtcDate, endUtcDate);
  }
}
