/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;

public class MultiTenantThirdPartyScansDataStore
    extends AbstractMultiTenantDataStore
    implements ThirdPartyScansDataStore
{
  public MultiTenantThirdPartyScansDataStore(
      final MultiTenantDataSourceFactory dataSourceFactory,
      final DatabaseMigrator databaseMigrator)
  {
    super(dataSourceFactory, databaseMigrator);
    // Populate the legacy class
    ThirdPartyScansProvider.setInstance(this);
  }

  @Override
  protected String getFactoryName() {
    return "InsightBrainThirdPartyScans";
  }
}
