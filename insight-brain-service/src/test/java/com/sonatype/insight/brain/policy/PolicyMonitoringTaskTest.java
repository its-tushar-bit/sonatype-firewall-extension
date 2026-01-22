/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.policy.evaluator.PolicyMonitor;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PolicyMonitoringTaskTest
    extends AbstractComponentTest
{
  @Inject
  private PolicyMonitoringTask policyMonitoringTask;

  @Mock
  private PolicyMonitor policyMonitorMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(PolicyMonitor.class).toInstance(policyMonitorMock);
    super.configure(binder);
  }

  @Test
  public void testExecute_QuartzJob() {
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(policyMonitorMock).run();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      policyMonitoringTask.execute(mock(JobExecutionContext.class));
    }

    verify(policyMonitorMock).run();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(PolicyMonitoringTask.class).build().isConcurrentExectionDisallowed()).isTrue();
  }
}
