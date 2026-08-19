/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.config;

import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;

public class MultiTenantS3DataStoreConfig
    extends S3DataStoreConfig
{
  @Override
  public String getObjectKeyPrefix() {
    return super.getObjectKeyPrefix() + TenantThreadLocal.getTenant().tenantSlug + "/";
  }
}
