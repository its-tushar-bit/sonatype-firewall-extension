/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SystemConfigurationPropertyCacheInvalidationJobTest
{
  private SystemConfigurationPropertyDAO mockDAO;

  private SystemConfigurationPropertyCacheInvalidationJob job;

  @BeforeEach
  public void setUp() {
    mockDAO = mock(SystemConfigurationPropertyDAO.class);
    job = new SystemConfigurationPropertyCacheInvalidationJob(mockDAO);
  }

  @Test
  public void testExecute_invalidatesCache() throws Exception {
    JobExecutionContext context = mock(JobExecutionContext.class);

    job.execute(context);

    verify(mockDAO).invalidateCache();
  }

  @Test
  public void testImplementsCorrectInterfaces() {
    assertThat(job).isInstanceOf(InsightJob.class);
    assertThat(job).isInstanceOf(TenantManaged.class);
  }

  @Test
  public void testGetJobName() {
    assertThat(job.getJobName()).isEqualTo("SystemConfigurationPropertyCacheInvalidation");
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(
        JobBuilder.newJob(SystemConfigurationPropertyCacheInvalidationJob.class)
            .build()
            .isConcurrentExectionDisallowed()).isTrue();
  }
}
