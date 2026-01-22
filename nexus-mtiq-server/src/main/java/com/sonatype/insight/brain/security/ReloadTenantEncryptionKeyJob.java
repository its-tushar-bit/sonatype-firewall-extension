/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.InsightJob;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ReloadTenantEncryptionKeyJob
    implements InsightJob
{
  private static final Logger log = LoggerFactory.getLogger(ReloadTenantEncryptionKeyJob.class);

  private static final String TASK_NAME = "ReloadTenantEncryptionKey";

  private final EncryptionKeyStore encryptionKeyStore;

  @Inject
  ReloadTenantEncryptionKeyJob(final EncryptionKeyStore encryptionKeyStore) {
    this.encryptionKeyStore = encryptionKeyStore;
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    // Set the tenant
    log.info("Reloading tenant encryption key");
    encryptionKeyStore.initializeKey();
  }

  @Override
  public String getJobName() {
    return TASK_NAME;
  }
}
