/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.config;

public class MultiTenantStorageConfig
    extends StorageConfig
{
  private MultiTenantS3DataStoreConfig s3Config;

  @Override
  public MultiTenantS3DataStoreConfig getS3Config() {
    return s3Config;
  }

  public void setS3Config(final MultiTenantS3DataStoreConfig s3Config) {
    this.s3Config = s3Config;
  }
}
