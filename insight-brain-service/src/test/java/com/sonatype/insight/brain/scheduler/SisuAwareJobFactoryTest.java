/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SisuAwareJobFactoryTest
    extends AbstractComponentTest
{
  @Inject
  private SisuAwareJobFactory sisuAwareJobFactory;

  @Test
  public void testNewJob_JobClassExists() throws Exception {
    TriggerFiredBundle triggerFiredBundleMock = mock(TriggerFiredBundle.class);
    JobDetail jobDetailMock = mock(JobDetail.class);
    doReturn(TestJob.class).when(jobDetailMock).getJobClass();
    when(triggerFiredBundleMock.getJobDetail()).thenReturn(jobDetailMock);

    Job job = sisuAwareJobFactory.newJob(triggerFiredBundleMock, null);

    assertThat(job).isNotNull();
  }

  @Test
  public void testNewJob_JobClassDoesNotExist() {
    TriggerFiredBundle triggerFiredBundleMock = mock(TriggerFiredBundle.class);
    JobDetail jobDetailMock = mock(JobDetail.class);
    doReturn(Object.class).when(jobDetailMock).getJobClass();
    when(triggerFiredBundleMock.getJobDetail()).thenReturn(jobDetailMock);

    assertThatExceptionOfType(SchedulerException.class)
        .isThrownBy(() -> sisuAwareJobFactory.newJob(triggerFiredBundleMock, null))
        .withMessage("Missing job component for type: java.lang.Object");
  }
}
