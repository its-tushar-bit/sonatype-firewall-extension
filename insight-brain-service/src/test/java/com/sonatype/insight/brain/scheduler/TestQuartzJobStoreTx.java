/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;

import org.quartz.JobPersistenceException;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;

@Named
@Singleton
public class TestQuartzJobStoreTx
    extends QuartzJobStoreTX
{
  private volatile boolean standby = false;

  private volatile boolean checkingIn = false;

  private volatile boolean recoveringMisfires = false;

  @Inject
  public TestQuartzJobStoreTx(
      ProductLicense productLicense,
      InsightConfig insightConfig,
      OperationalDataStore operationalDataStore)
      throws InvalidConfigurationException
  {
    super(productLicense, insightConfig, operationalDataStore);
  }

  @Override
  protected boolean doCheckin() throws JobPersistenceException {
    if (standby) {
      return false;
    }
    try {
      checkingIn = true;
      return super.doCheckin();
    }
    finally {
      checkingIn = false;
    }
  }

  @Override
  protected RecoverMisfiredJobsResult doRecoverMisfires() throws JobPersistenceException {
    if (standby) {
      return RecoverMisfiredJobsResult.NO_OP;
    }
    try {
      recoveringMisfires = true;
      return super.doRecoverMisfires();
    }
    finally {
      recoveringMisfires = false;
    }
  }

  @Override
  public void schedulerResumed() {
    standby = false;
    super.schedulerResumed();
  }

  @Override
  public void schedulerPaused() {
    super.schedulerPaused();
    standby = true;
    waitForCheckinOrRecoverMisfires();
  }

  private void waitForCheckinOrRecoverMisfires() {
    long start = System.currentTimeMillis();
    while ((checkingIn || recoveringMisfires) && (System.currentTimeMillis() - start) < 10000) {
      Thread.yield();
    }
  }
}
