/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.quartz.JobPersistenceException;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.utils.ConnectionProvider;

@Named
@Singleton
public class MultiTenantQuartzJobStoreTX
    extends QuartzJobStoreTX
{
  private final TenantUtil tenantUtil;

  @Inject
  public MultiTenantQuartzJobStoreTX(
      ProductLicense productLicense,
      InsightConfig insightConfig,
      OperationalDataStore operationalDataStore,
      TenantUtil tenantUtil,
      ClusterLockManager clusterLockManager)
      throws InvalidConfigurationException
  {
    super(productLicense, insightConfig, operationalDataStore, clusterLockManager);
    this.tenantUtil = tenantUtil;
  }

  @Override
  protected ConnectionProvider buildQuartzConnectionProvider() {
    return new MultiTenantQuartzConnectionProvider(operationalDataStore);
  }

  @Override
  protected boolean doCheckin() throws JobPersistenceException {
    tenantUtil.setGlobalTenant();

    return doSuperCheckIn();
  }

  // This is a separate method so that it can be overridden during testing.
  protected boolean doSuperCheckIn() throws JobPersistenceException {
    return super.doCheckin();
  }

  @Override
  protected boolean shouldExitDueToSchemaMigration() {
    // For MTIQ our deployment explicitly requires that a new deployment pod/container coming online runs migrations so
    // existing pods/containers should NEVER exit when a migration happens
    return false;
  }
}
