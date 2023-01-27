/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shiro;

import java.time.Duration;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.MultiTenantTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.assertTenantSet;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class QuartzShiroSessionValidationSchedulerTest
    extends MultiTenantTest
{
  @Mock
  TaskScheduler taskScheduler;

  @Mock
  DefaultWebSessionManager sessionManager;

  QuartzShiroSessionValidationScheduler underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    this.underTest = new QuartzShiroSessionValidationScheduler(taskScheduler, sessionManager);
  }

  @Test
  public void testSetSessionValidationScheduler_onCreation() {
    verify(sessionManager).setSessionValidationScheduler(underTest);
  }

  @Test
  public void testIsEnabled_isPerTenant() {
    Tenant tenant1 = new Tenant("tenant1");
    Tenant tenant2 = new Tenant("tenant2");

    testAs(tenant1, t -> assertThat(underTest.isEnabled()).isFalse());

    testAs(tenant2, t -> {
      assertThat(underTest.isEnabled()).isFalse();

      underTest.enableSessionValidation();

      assertThat(underTest.isEnabled()).isTrue();
    });

    testAs(tenant1, t -> assertThat(underTest.isEnabled()).isFalse());
  }

  @Test
  public void testScheduleTask_perTenant() {
    Tenant tenant1 = new Tenant("tenant1");

    testAs(tenant1, t -> {
      doAnswer(i -> {
        assertTenantSet(tenant1);
        return null;
      }).when(taskScheduler).schedulePeriodicTask(any(), any(), any());

      underTest.enableSessionValidation();

      verify(taskScheduler)
          .schedulePeriodicTask(eq(QuartzShiroSessionValidationScheduler.class), anyString(), any(Duration.class));
    });
  }

  @Test
  public void testValidateSessions_onExecute() {
    underTest.execute(null);

    verify(sessionManager).validateSessions();
  }
}
