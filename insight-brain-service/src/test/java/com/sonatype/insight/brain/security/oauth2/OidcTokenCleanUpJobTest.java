/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.OidcTokenDAO;
import com.sonatype.insight.brain.model.security.OidcToken;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class OidcTokenCleanUpJobTest
    extends AbstractComponentTest
{
  @Inject
  private OidcTokenCleanUpJob oidcTokenCleanUpJob;

  @Inject
  private OidcTokenDAO oidcTokenDAO;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(OidcTokenCleanUpJob.class).build()
        .isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testRegister() {
    oidcTokenCleanUpJob.register();

    verify(taskSchedulerMock).schedulePeriodicTask(oidcTokenCleanUpJob,
        Duration.ofHours(OidcTokenCleanUpJob.JOB_FREQUENCY_IN_HOURS));
  }

  @Test
  public void testGetJobName() {
    String jobName = oidcTokenCleanUpJob.getJobName();

    assertThat(jobName).isEqualTo(OidcTokenCleanUpJob.JOB_NAME);
  }

  @Test
  public void testExecute() throws JobExecutionException {
    // Insert Tokens
    OidcToken oidcToken1 = new OidcToken("id-token-1");
    OidcToken oidcToken2 = new OidcToken("id-token-2", Date.from(Instant.now().minus(Duration.ofMinutes(10))));
    oidcTokenDAO.insert(oidcToken1);
    oidcTokenDAO.insert(oidcToken2);

    // Run the cleanup
    oidcTokenCleanUpJob.execute(null);

    // Check result
    assertThat(oidcTokenDAO.getById(oidcToken1.getId())).isNotNull();
    assertThat(oidcTokenDAO.getById(oidcToken2.getId())).isNull();
  }
}
