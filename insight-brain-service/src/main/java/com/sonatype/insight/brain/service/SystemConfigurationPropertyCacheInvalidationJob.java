/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.tenancy.TenantManaged;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz job that invalidates the {@link SystemConfigurationPropertyDAO} cache on the current node.
 * Scheduled on other cluster nodes when a configuration property or feature toggle changes,
 * ensuring cross-node cache consistency.
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class SystemConfigurationPropertyCacheInvalidationJob
    implements InsightJob, TenantManaged
{
  private static final Logger log = LoggerFactory.getLogger(SystemConfigurationPropertyCacheInvalidationJob.class);

  static final String TASK_NAME = "SystemConfigurationPropertyCacheInvalidation";

  private static final String ERROR_MSG = "Error invalidating system configuration property cache";

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public SystemConfigurationPropertyCacheInvalidationJob(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    execute(systemConfigurationPropertyDAO::invalidateCache, log, ERROR_MSG);
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
