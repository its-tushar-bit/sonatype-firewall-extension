/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.RolePermissionDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ClearRolePermissionCacheTest
    extends AbstractComponentTest
{
  @Inject
  private RoleDAO roleDAO;

  @Inject
  private ClearRolePermissionCache clearRolePermissionCache;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
  }

  @Test
  public void testScheduleClearRolePermissionCacheForAllOtherNodes() {
    clearRolePermissionCache.scheduleClearRolePermissionCacheForAllOtherNodes();

    verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(clearRolePermissionCache);
  }

  @Test
  public void testStart() {
    Runnable original = RolePermissionDAO.getClearRolePermissionCacheForAllOtherNodes();
    try {
      RolePermissionDAO.setClearRolePermissionCacheForAllOtherNodes(null);

      clearRolePermissionCache.register();

      // Insert should trigger the job
      Role role = tempEntity.newRole(true, Permission.READ);
      verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(clearRolePermissionCache);

      Mockito.reset(mockTaskScheduler);

      // Delete should trigger the job
      roleDAO.delete(role);
      verify(mockTaskScheduler).scheduleOneTimeTaskForAllOtherNodes(clearRolePermissionCache);
    }
    finally {
      RolePermissionDAO.setClearRolePermissionCacheForAllOtherNodes(original);
    }
  }

  @Test
  public void testClearRolePermissionCache() {
    clearRolePermissionCache.clearRolePermissionCache();

    try (MockedStatic<RolePermissionDAO> rolePermissionDAO = Mockito.mockStatic(RolePermissionDAO.class,
        CALLS_REAL_METHODS))
    {
      clearRolePermissionCache.clearRolePermissionCache();

      rolePermissionDAO.verify(RolePermissionDAO::clearRolePermissionCache);
    }
  }

  @Test
  public void testExecute() {
    ClearRolePermissionCache spyClearRolePermissionCache = spy(clearRolePermissionCache);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(spyClearRolePermissionCache).clearRolePermissionCache();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      spyClearRolePermissionCache.execute(mock(JobExecutionContext.class));
    }

    verify(spyClearRolePermissionCache).clearRolePermissionCache();
  }
}
