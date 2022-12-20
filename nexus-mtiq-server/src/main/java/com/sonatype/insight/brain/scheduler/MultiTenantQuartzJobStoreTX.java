/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.TenantManager;

import org.quartz.JobPersistenceException;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.utils.ConnectionProvider;

@Named
@Singleton
public class MultiTenantQuartzJobStoreTX
    extends QuartzJobStoreTX
{
  @Inject
  public MultiTenantQuartzJobStoreTX(
      ProductLicense productLicense,
      InsightConfig insightConfig,
      OperationalDataStore operationalDataStore)
      throws InvalidConfigurationException
  {
    super(productLicense, insightConfig, operationalDataStore);
  }

  @Override
  protected ConnectionProvider buildQuartzConnectionProvider() {
    return new MultiTenantQuartzConnectionProvider();
  }

  @Override
  protected boolean doCheckin() throws JobPersistenceException {
    TenantManager.initGlobalTenant();

    return doSuperCheckIn();
  }

  // This is a separate method so that it can be overriden during testing.
  protected boolean doSuperCheckIn() throws JobPersistenceException {
    return super.doCheckin();
  }
}
