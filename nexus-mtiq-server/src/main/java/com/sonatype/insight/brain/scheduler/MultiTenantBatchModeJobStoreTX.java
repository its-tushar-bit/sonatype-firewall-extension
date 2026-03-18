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
import com.sonatype.insight.brain.tenancy.TenantUtil;

import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;

@Named
@Singleton
public class MultiTenantBatchModeJobStoreTX
    extends MultiTenantQuartzJobStoreTX
{
  @Inject
  public MultiTenantBatchModeJobStoreTX(
      ProductLicense productLicense,
      InsightConfig insightConfig,
      OperationalDataStore operationalDataStore,
      TenantUtil tenantUtil) throws InvalidConfigurationException
  {
    super(productLicense, insightConfig, operationalDataStore, tenantUtil);
  }
}
