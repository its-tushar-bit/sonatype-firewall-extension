/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingService.BuiltJob;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;
import com.sonatype.insight.test.LogOutput;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Multi-tenant test for {@link QuartzJobSchedulingService}. See {@link QuartzJobSchedulingServiceTest} for the primary
 * test for {@link QuartzJobSchedulingService}. This is just testing the most complex case as we only need to verify
 * tenancy is working.
 */
public class MultiTenantQuartzJobSchedulingServiceTest
    extends AbstractMultiTenantTest
{
  private static final int TENANT_COUNT = 3;

  private static final String TEST_JOB_NAME = "testJob";

  private static final String TEST_TRIGGER_NAME = "testTrigger";

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public LogOutput logOutput = new LogOutput(QuartzJobSchedulingService.class);

  @Rule
  public QuartzJobSchedulingServiceRule quartzJobSchedulingServiceRule = new QuartzJobSchedulingServiceRule();

  @Mock
  private Scheduler mockQuartzScheduler;

  @Mock
  private JobLogger mockJobLogger;

  @Captor
  ArgumentCaptor<Map<JobDetail, Set<? extends Trigger>>> mapCaptor;

  private QuartzJobSchedulingService underTest;

  @Before
  public void setup() {
    underTest = new QuartzJobSchedulingService();
  }

  @Test
  public void testScheduleTask_MultiTenancy() throws Exception {
    List<Tenant> tenants = new ArrayList<>();

    for (int i = 0; i < TENANT_COUNT; i++) {
      // Create a tenant for each iteration
      testAsNewTenant(tenant -> {
        tenants.add(tenant);

        // Given
        JobDetail jobDetail = createJobDetail(TEST_JOB_NAME, tenant.tenantSlug);
        Trigger[] triggers = new Trigger[]{createTrigger(TEST_TRIGGER_NAME, tenant.tenantSlug)};

        // When
        underTest.scheduleTask(mockQuartzScheduler, jobDetail.getKey(),
            () -> new BuiltJob(jobDetail, Set.of(triggers), mockJobLogger));
        quartzJobSchedulingServiceRule.waitForRealSchedulingToComplete(underTest);

        // Sleep for a short tick to ensure the timers execute in the correct order so we can verify that order
        Thread.sleep(10);
      });
    }

    // Then
    verifyJobs(
        List.of(
            Pair.of(createJobDetail(TEST_JOB_NAME, tenants.get(0).tenantSlug),
                Set.of(createTrigger(TEST_TRIGGER_NAME, tenants.get(0).tenantSlug))),
            Pair.of(createJobDetail(TEST_JOB_NAME, tenants.get(1).tenantSlug),
                Set.of(createTrigger(TEST_TRIGGER_NAME, tenants.get(1).tenantSlug))),
            Pair.of(createJobDetail(TEST_JOB_NAME, tenants.get(2).tenantSlug),
                Set.of(createTrigger(TEST_TRIGGER_NAME, tenants.get(2).tenantSlug)))));

    // Verify log output
    assertThat(logOutput).atDebugLevel()
        .contains("Adding job " + tenants.get(0).tenantSlug + "." + TEST_JOB_NAME)
        .contains("Adding job " + tenants.get(1).tenantSlug + "." + TEST_JOB_NAME)
        .contains("Adding job " + tenants.get(2).tenantSlug + "." + TEST_JOB_NAME)
        .contains("Scheduling 1 jobs on scheduler");
  }

  @SuppressWarnings("unchecked")
  private void verifyJobs(
      List<Pair<JobDetail, Set<Trigger>>> jobsWithTriggers) throws SchedulerException
  {
    // verify quartz called for the test tenants
    verify(mockQuartzScheduler, times(TENANT_COUNT)).scheduleJobs(mapCaptor.capture(), eq(true));

    // Capture all values for all invocations of `scheduleJobs` (list size should be equal to the number of tenants)
    List<Map<JobDetail, Set<? extends Trigger>>> capturedArgs = mapCaptor.getAllValues();
    assertThat(capturedArgs).hasSameSizeAs(jobsWithTriggers);

    // iterate over the captured arguments and verify each job and its triggers match that of the tenant
    for (Pair<JobDetail, Set<Trigger>> jobsWithTrigger : jobsWithTriggers) {
      JobDetail jobDetail = jobsWithTrigger.getLeft();
      Set<Trigger> triggers = jobsWithTrigger.getRight();
      // pop the first captured argument, which corresponds to the tenant order
      Map<JobDetail, Set<? extends Trigger>> tenantCapturedArgs = capturedArgs.remove(0);

      assertThat(tenantCapturedArgs).containsKey(jobDetail);

      Set<? extends Trigger> argumentTriggers = tenantCapturedArgs.get(jobDetail);
      assertThat((Set<Trigger>) argumentTriggers).containsExactlyInAnyOrderElementsOf(triggers);
    }
  }

  private JobDetail createJobDetail(String name, String group) {
    JobDetailImpl jobDetail = new JobDetailImpl();
    jobDetail.setName(name);
    jobDetail.setGroup(group);
    return jobDetail;
  }

  private Trigger createTrigger(String name, String group) {
    SimpleTriggerImpl trigger = new SimpleTriggerImpl();
    trigger.setName(name);
    trigger.setGroup(group);
    return trigger;
  }
}
