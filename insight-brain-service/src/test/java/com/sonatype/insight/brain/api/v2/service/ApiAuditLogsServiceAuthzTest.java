/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.audit.AuditLogFilesProvider;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiAuditLogsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiAuditLogsService apiAuditLogsService;

  @Mock
  private AuditLogFilesProvider auditLogFilesProvider;

  @Override
  public void configure(Binder binder) {
    binder.bind(AuditLogFilesProvider.class).toInstance(auditLogFilesProvider);
    super.configure(binder);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAuditLogs_Unauthenticated() {
    apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-08");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAuditLogs_Unauthorized() {
    login();
    apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-08");
  }

  @Test
  public void testGetAuditLogs_Authorized() {
    when(auditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(Collections.emptyList());
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.ACCESS_AUDIT_LOG);

    apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-08");

    verify(auditLogFilesProvider, times(1)).getAuditLogFiles(any(), any());
  }
}
