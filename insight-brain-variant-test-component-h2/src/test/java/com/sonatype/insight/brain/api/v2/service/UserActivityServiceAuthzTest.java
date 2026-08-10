/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiActivityEventDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiUserActivityFilterOptionsDTO;
import com.sonatype.insight.brain.audit.AuditLogFilesProvider;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class UserActivityServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private UserActivityService userActivityService;

  @Mock
  private AuditLogFilesProvider auditLogFilesProvider;

  @Mock
  private ClusterLockManager clusterLockManager;

  @Mock
  private ShutdownHandler shutdownHandler;

  @Test
  public void testGetUserActivitySummary_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class,
        () -> userActivityService.getUserActivitySummary("2024-02-04", "2024-02-08", null, 100, 0));
  }

  @Test
  public void testGetUserActivitySummary_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class,
        () -> userActivityService.getUserActivitySummary("2024-02-04", "2024-02-08", null, 100, 0));
  }

  @Test
  public void testGetUserActivitySummary_Authorized() {
    when(auditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(Collections.emptyList());
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.ACCESS_AUDIT_LOG);

    userActivityService.getUserActivitySummary("2024-02-04", "2024-02-08", null, 100, 0);

    verify(auditLogFilesProvider, times(1)).getAuditLogFiles(any(), any());
  }

  @Test
  public void testGetUserActivityDetail_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class,
        () -> userActivityService.getUserActivityDetail("2024-02-04", "2024-02-08", "test.user", 100, 0, null, null,
            null));
  }

  @Test
  public void testGetUserActivityDetail_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class,
        () -> userActivityService.getUserActivityDetail("2024-02-04", "2024-02-08", "test.user", 100, 0, null, null,
            null));
  }

  @Test
  public void testGetUserActivityDetail_Authorized() {
    when(auditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(Collections.emptyList());
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.ACCESS_AUDIT_LOG);

    userActivityService.getUserActivityDetail("2024-02-04", "2024-02-08", "test.user", 100, 0, null, null, null);

    verify(auditLogFilesProvider, times(1)).getAuditLogFiles(any(), any());
  }

  @Test
  public void testStreamAllUserActivitiesForExport_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class, () -> {
      try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
          "2024-02-04", "2024-02-08", null, null, null, Set.of(), Set.of(), Set.of()))
      {
        stream.count();
      }
    });
  }

  @Test
  public void testStreamAllUserActivitiesForExport_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class, () -> {
      try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
          "2024-02-04", "2024-02-08", null, null, null, Set.of(), Set.of(), Set.of()))
      {
        stream.count();
      }
    });
  }

  @Test
  public void testStreamAllUserActivitiesForExport_Authorized() {
    when(auditLogFilesProvider.getAuditLogFiles(any(), any())).thenReturn(Collections.emptyList());
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.ACCESS_AUDIT_LOG);

    try (Stream<ApiActivityEventDTO> stream = userActivityService.streamAllUserActivitiesForExport(
        "2024-02-04", "2024-02-08", null, null, null, Set.of(), Set.of(), Set.of()))
    {
      stream.count();
    }

    verify(auditLogFilesProvider, times(1)).getAuditLogFiles(any(), any());
  }

  @Test
  public void testGetFilterOptions_Unauthenticated() {
    Assertions.assertThrows(UnauthenticatedException.class,
        () -> userActivityService.getFilterOptions());
  }

  @Test
  public void testGetFilterOptions_Unauthorized() {
    login();
    Assertions.assertThrows(UnauthorizedException.class,
        () -> userActivityService.getFilterOptions());
  }

  @Test
  public void testGetFilterOptions_Authorized() {
    grantPermission(Organization.ROOT_ORGANIZATION_ID, Permission.ACCESS_AUDIT_LOG);

    ApiUserActivityFilterOptionsDTO result = userActivityService.getFilterOptions();

    // Verify the method executes successfully and returns expected data
    assertThat(result).isNotNull();
    assertThat(result.domains).isNotNull().isNotEmpty();
    assertThat(result.activityTypes).isNotNull().isNotEmpty();
    assertThat(result.errorTypes).isNotNull().isNotEmpty();
  }
}
