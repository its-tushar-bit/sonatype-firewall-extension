/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shiro;

import java.time.Duration;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class QuartzShiroSessionValidationSchedulerTest
    extends AbstractMultiTenantTest
{
  @Mock
  TaskScheduler taskScheduler;

  @Mock
  DefaultWebSessionManager sessionManager;

  QuartzShiroSessionValidationScheduler underTest;

  @BeforeEach
  public void setup() {
    this.underTest = new QuartzShiroSessionValidationScheduler(taskScheduler, sessionManager);
  }

  @Test
  public void testSetSessionValidationScheduler_onCreation() {
    verify(sessionManager).setSessionValidationScheduler(underTest);
  }

  @Test
  public void testIsEnabled_isPerTenant() {

    Tenant tenant1 = testAsNewTenant(t1 -> assertThat(underTest.isEnabled()).isFalse());

    testAsNewTenant(t2 -> {
      assertThat(underTest.isEnabled()).isFalse();

      underTest.enableSessionValidation();

      assertThat(underTest.isEnabled()).isTrue();
    });

    testAsTenant(tenant1, t1 -> assertThat(underTest.isEnabled()).isFalse());
  }

  @Test
  public void testScheduleTask_perTenant() {
    testAsNewTenant(t1 -> {
      doAnswer(i -> {
        assertTenantSet(t1);
        return null;
      }).when(taskScheduler).schedulePeriodicTask(any(), any());

      underTest.enableSessionValidation();

      verify(taskScheduler)
          .schedulePeriodicTask(eq(underTest), any(Duration.class));
    });
  }

  @Test
  public void testValidateSessions_onExecute() {
    underTest.execute(null);

    verify(sessionManager).validateSessions();
  }

  @Test
  public void testDeleteJob_onDeregister() {
    underTest.deregister();

    verify(taskScheduler, never()).unscheduleTask(underTest);
  }
}
