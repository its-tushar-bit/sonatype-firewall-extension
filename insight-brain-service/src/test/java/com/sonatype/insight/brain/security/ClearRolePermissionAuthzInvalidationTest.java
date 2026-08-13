/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;

public class ClearRolePermissionAuthzInvalidationTest
{
  private Runnable originalCallback;

  private TaskScheduler taskScheduler;

  private ReadableContextAuthzCache readableContextAuthzCache;

  private ClearRolePermissionCache clearRolePermissionCache;

  @BeforeEach
  public void setUp() {
    originalCallback = RolePermissionDAO.getClearRolePermissionCacheForAllOtherNodes();
    taskScheduler = mock(TaskScheduler.class);
    readableContextAuthzCache = mock(ReadableContextAuthzCache.class);
    clearRolePermissionCache = new ClearRolePermissionCache(taskScheduler, readableContextAuthzCache);
  }

  @AfterEach
  public void tearDown() {
    RolePermissionDAO.setClearRolePermissionCacheForAllOtherNodes(originalCallback);
  }

  @Test
  public void localRolePermissionMutationBumpsEpochAndSchedulesRemoteInvalidation() {
    clearRolePermissionCache.register();

    RolePermissionDAO.getClearRolePermissionCacheForAllOtherNodes().run();

    verify(readableContextAuthzCache).bumpEpoch();
    verify(taskScheduler).scheduleOneTimeTaskForAllOtherNodes(
        eq(clearRolePermissionCache),
        eq(Map.of(ClearRolePermissionCache.TENANT_SLUG_PARAMETER,
            com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT.tenantSlug)));
  }

  @Test
  public void remoteRolePermissionInvalidationBumpsLocalEpoch() {
    clearRolePermissionCache.clearRolePermissionCache();

    verify(readableContextAuthzCache).bumpEpoch();
  }
}
