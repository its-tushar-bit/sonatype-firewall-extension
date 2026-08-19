/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.audit.AuditLogFilesProvider;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.Collections;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

@ComponentH2Test
public class ApiAuditLogsServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiAuditLogsService apiAuditLogsService;

  @Mock
  private AuditLogFilesProvider auditLogFilesProvider;

  @Test
  public void testGetAuditLogs_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-08"));
  }

  @Test
  public void testGetAuditLogs_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-08"));
  }

  @Test
  public void testGetAuditLogs_Authorized() {
    when(auditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(Collections.emptyList());
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.ACCESS_AUDIT_LOG);

    apiAuditLogsService.getAuditLogs("2024-02-04", "2024-02-08");

    verify(auditLogFilesProvider, times(1)).getAuditLogFiles(any(), any());
  }
}
