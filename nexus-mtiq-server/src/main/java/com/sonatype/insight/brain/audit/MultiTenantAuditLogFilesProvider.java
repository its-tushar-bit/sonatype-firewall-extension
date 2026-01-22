/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import jakarta.annotation.Priority;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.logging.MultiTenantAuditLogAppenderFactory;

import ru.vyarus.dropwizard.guice.module.installer.order.Order;

@Named
@Singleton
@Priority(MultiTenantAuditLogFilesProvider.PRIORITY)
@Order(Integer.MAX_VALUE - MultiTenantAuditLogFilesProvider.PRIORITY)
public class MultiTenantAuditLogFilesProvider
    implements AuditLogFilesProvider
{
  public static final int PRIORITY = 1;

  @Override
  public List<File> getAuditLogFiles(final LocalDate startUtcDate, final LocalDate endUtcDate) {
    return MultiTenantAuditLogAppenderFactory.getAuditLogFiles(startUtcDate, endUtcDate);
  }
}
