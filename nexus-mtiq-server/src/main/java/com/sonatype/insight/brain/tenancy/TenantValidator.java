/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

@Named
@Singleton
public class TenantValidator
{
  private final OperationalDataStore operationalDataStore;

  @Inject
  public TenantValidator(OperationalDataStore operationalDataStore) {
    this.operationalDataStore = operationalDataStore;
  }

  public boolean validateTenantExists(Tenant tenant) {
    if (tenant == null) {
      throw new IllegalArgumentException("Invalid tenant parameter");
    }

    return DatabaseUtil.schemaExists(operationalDataStore.getDataSourceWithoutInit(), tenant.databaseSchema);
  }
}
