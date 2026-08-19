/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

@ComponentH2Test
public class ClearRolePermissionCacheTest
    extends AbstractComponentH2Test
{
  @Inject
  private RoleDAO roleDAO;

  @Inject
  private ClearRolePermissionCache clearRolePermissionCache;

  @Inject
  private ReadableContextAuthzCache readableContextAuthzCache;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Test
  public void testScheduleClearRolePermissionCacheForAllOtherNodes() {
    clearRolePermissionCache.scheduleClearRolePermissionCacheForAllOtherNodes();

    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(
        clearRolePermissionCache,
        Map.of(ClearRolePermissionCache.TENANT_SLUG_PARAMETER,
            com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT.tenantSlug));
  }

  @Test
  public void testStart() {
    Runnable original = RolePermissionDAO.getClearRolePermissionCacheForAllOtherNodes();
    try {
      RolePermissionDAO.setClearRolePermissionCacheForAllOtherNodes(null);

      clearRolePermissionCache.register();

      // Insert should trigger the job
      long epochBeforeInsert = readableContextAuthzCache.currentEpoch();
      Role role = tempEntity.newRole(true, Permission.READ);
      verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(
          clearRolePermissionCache,
          Map.of(ClearRolePermissionCache.TENANT_SLUG_PARAMETER,
              com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT.tenantSlug));
      assertThat(readableContextAuthzCache.currentEpoch()).isGreaterThan(epochBeforeInsert);

      Mockito.reset(mockTaskScheduler);

      // Delete should trigger the job
      long epochBeforeDelete = readableContextAuthzCache.currentEpoch();
      roleDAO.delete(role);
      verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(
          clearRolePermissionCache,
          Map.of(ClearRolePermissionCache.TENANT_SLUG_PARAMETER,
              com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT.tenantSlug));
      assertThat(readableContextAuthzCache.currentEpoch()).isGreaterThan(epochBeforeDelete);
    }
    finally {
      RolePermissionDAO.setClearRolePermissionCacheForAllOtherNodes(original);
    }
  }

  @Test
  public void testClearRolePermissionCache() {
    long epochBeforeClear = readableContextAuthzCache.currentEpoch();
    clearRolePermissionCache.clearRolePermissionCache();
    assertThat(readableContextAuthzCache.currentEpoch()).isGreaterThan(epochBeforeClear);

    try (MockedStatic<RolePermissionDAO> rolePermissionDAO = Mockito.mockStatic(RolePermissionDAO.class,
        CALLS_REAL_METHODS))
    {
      clearRolePermissionCache.clearRolePermissionCache();

      rolePermissionDAO.verify(RolePermissionDAO::clearRolePermissionCache);
    }
  }

  @Test
  public void testExecuteWithTenantSlugClearsCache() {
    ClearRolePermissionCache spyClearRolePermissionCache = spy(clearRolePermissionCache);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spyClearRolePermissionCache).clearRolePermissionCache();

    JobExecutionContext context = mock(JobExecutionContext.class);
    org.quartz.JobDataMap dataMap = new org.quartz.JobDataMap();
    dataMap.put(ClearRolePermissionCache.TENANT_SLUG_PARAMETER,
        com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT.tenantSlug);
    when(context.getMergedJobDataMap()).thenReturn(dataMap);

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spyClearRolePermissionCache.execute(context);
    }

    verify(spyClearRolePermissionCache).clearRolePermissionCache();
  }

  @Test
  public void testExecuteWithoutTenantSlugSkipsClear() {
    ClearRolePermissionCache spyClearRolePermissionCache = spy(clearRolePermissionCache);

    spyClearRolePermissionCache.execute(mock(JobExecutionContext.class));

    verify(spyClearRolePermissionCache, Mockito.never()).clearRolePermissionCache();
  }
}
