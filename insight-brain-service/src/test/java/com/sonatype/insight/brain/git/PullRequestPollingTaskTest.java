/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.tenancy.MultiTenantTestSupport;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestPollingTaskTest
    extends MultiTenantTestSupport
{
  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private PullRequestPollingService pullRequestPollingService;

  @Mock
  private IqForScmLicenseChecker licenseChecker;

  private PullRequestPollingTask underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    underTest = new PullRequestPollingTask(taskSchedulerMock, pullRequestPollingService, licenseChecker, 2, 1);
  }

  @Test
  public void testPullRequestPollingTask_register_deregister() {
    testAsNewTenant(tenant -> {
      when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);

      underTest.register();

      verify(taskSchedulerMock).schedulePeriodicTask(eq(underTest), eq(Duration.ofSeconds(1)), any());

      underTest.deregister();

      verify(taskSchedulerMock).unscheduleTask(underTest);
    });
  }

  @Test
  public void testPullRequestPollingTask_productLicenseChanged_shouldUnscheduleIfUnlicensed() {
    testAsGlobalTenant(global -> {
      underTest.productLicenseChanged();

      verify(taskSchedulerMock).unscheduleTask(underTest);
    });
  }

  @Test
  public void testPullRequestPollingTask_productLicenseChanged_shouldScheduleForSingleTenant() {
    testAsSingleTenant(single -> {
      when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);

      underTest.productLicenseChanged();

      verify(taskSchedulerMock).schedulePeriodicTask(eq(underTest), eq(Duration.ofSeconds(1)), any());
    });
  }

  @Test
  public void testPullRequestPollingTask_productLicenseChanged_shouldScheduleForGlobalTenant() {
    testAsGlobalTenant(global -> {
      when(licenseChecker.isPullRequestCommentingSupported()).thenReturn(true);

      underTest.productLicenseChanged();

      verify(taskSchedulerMock).schedulePeriodicTask(eq(underTest), eq(Duration.ofSeconds(1)), any());
    });
  }

  @Test
  public void testPullRequestPollingTask_productLicenseChanged_shouldNotScheduleForTenant() {
    // should not run as tenant
    testAsNewTenant(tenant -> {
      underTest.productLicenseChanged();

      verifyNoInteractions(taskSchedulerMock);
    });
  }

  @Test
  public void testPullRequestPollingTask_dropWizardTaskExecuted_shouldTriggerNow() {
    testAsGlobalTenant(global -> {
      final StringWriter writer = new StringWriter();
      final PrintWriter pw = new PrintWriter(writer);
      underTest.execute(null, pw);

      verify(taskSchedulerMock).triggerTaskNow(underTest, null);
      assertThat(writer.toString()).isEqualTo("Triggered polling for all PRs");
    });
  }
}
