/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scheduler;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.quartz.JobPersistenceException;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.utils.ConnectionProvider;
import org.springframework.context.annotation.Primary;

@Named
@Singleton
@Primary
public class MultiTenantQuartzJobStoreTX
    extends QuartzJobStoreTX
{
  private final TenantUtil tenantUtil;

  @Inject
  public MultiTenantQuartzJobStoreTX(
      ProductLicense productLicense,
      InsightConfig insightConfig,
      OperationalDataStore operationalDataStore,
      TenantUtil tenantUtil) throws InvalidConfigurationException
  {
    super(productLicense, insightConfig, operationalDataStore);
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
}
