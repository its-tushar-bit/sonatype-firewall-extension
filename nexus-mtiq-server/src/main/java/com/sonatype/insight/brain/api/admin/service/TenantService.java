/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;

@Named
public class TenantService
{
  private final OperationalDataStore operationalDataStore;

  @Inject
  public TenantService(final OperationalDataStore operationalDataStore) {
    this.operationalDataStore = operationalDataStore;
  }

  public List<String> getAllTenantsNames() {
    return DatabaseUtil
        .getTenantSchemas(operationalDataStore.getDataSource())
        .stream()
        .map(TenantUtil::getTenantNameFromSchema)
        .collect(Collectors.toList());
  }

  public String getTenantSlug() {
    return TenantThreadLocal.getTenant().tenantSlug;
  }
}
