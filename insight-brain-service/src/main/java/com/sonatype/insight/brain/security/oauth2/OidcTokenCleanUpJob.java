/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.OidcTokenDAO;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.tenancy.MtiqBatchJob;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Job, cleans up the oidc_token table used for OIDC authentication. It prevents that table to keep growing with
 * orphan oidc tokens, that are never used.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class OidcTokenCleanUpJob
    implements InsightJob, MtiqBatchJob
{
  private static final Logger log = LoggerFactory.getLogger(OidcTokenCleanUpJob.class);

  static final long JOB_FREQUENCY_IN_HOURS = 24L;

  static final String JOB_NAME = "OidcTokenCleanUpJob";

  private final TaskScheduler taskScheduler;

  private final OidcTokenDAO oidcTokenDAO;

  @Inject
  public OidcTokenCleanUpJob(TaskScheduler taskScheduler, OidcTokenDAO oidcTokenDAO) {
    this.taskScheduler = taskScheduler;
    this.oidcTokenDAO = oidcTokenDAO;
  }

  @Override
  public void register() {
    taskScheduler.schedulePeriodicTask(this, Duration.ofHours(JOB_FREQUENCY_IN_HOURS));
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.debug("Cleaning up OIDC tokens");
    oidcTokenDAO.cleanUpOidcTokens();
  }
}
