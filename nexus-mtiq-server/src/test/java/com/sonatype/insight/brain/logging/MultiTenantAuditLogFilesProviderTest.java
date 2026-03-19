/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.logging;

import java.time.LocalDate;

import com.sonatype.insight.brain.audit.MultiTenantAuditLogFilesProvider;
import com.sonatype.insight.error.exception.InternalServerException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MultiTenantAuditLogFilesProviderTest
{
  private String savedAuditLogBasePath;

  @Before
  public void setUp() {
    savedAuditLogBasePath = MultiTenantAuditLogAppenderFactory.getAuditLogBasePathForTesting();
    MultiTenantAuditLogAppenderFactory.setAuditLogBasePathForTesting(null);
  }

  @After
  public void tearDown() {
    MultiTenantAuditLogAppenderFactory.setAuditLogBasePathForTesting(savedAuditLogBasePath);
  }

  @Test
  public void testGetAuditLogFiles_WhenThereIsNotAuditLogPath() {
    MultiTenantAuditLogFilesProvider multiTenantAuditLogFilesProvider = new MultiTenantAuditLogFilesProvider();

    assertThatThrownBy(() -> multiTenantAuditLogFilesProvider.getAuditLogFiles(LocalDate.MIN, LocalDate.MAX))
        .isInstanceOf(InternalServerException.class)
        .hasMessage("Cannot get the audit log path.");
  }
}
