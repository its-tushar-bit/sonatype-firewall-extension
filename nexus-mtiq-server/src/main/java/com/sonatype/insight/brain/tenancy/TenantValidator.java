/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class TenantValidator
{
  private final OperationalDataStore operationalDataStore;

  @Inject
  public TenantValidator(OperationalDataStore operationalDataStore) {
    this.operationalDataStore = operationalDataStore;
  }

  public boolean validateTenantExists(String tenant) {
    if (StringUtils.isBlank(tenant)) {
      throw new IllegalArgumentException("Invalid tenant parameter");
    }

    return validateTenantExists(new Tenant(tenant));
  }

  public boolean validateTenantExists(Tenant tenant) {
    if (tenant == null) {
      throw new IllegalArgumentException("Invalid tenant parameter");
    }

    return DatabaseUtil.databaseSchemaExists(operationalDataStore.getDataSourceWithoutInit(), tenant.databaseSchema);
  }
}
