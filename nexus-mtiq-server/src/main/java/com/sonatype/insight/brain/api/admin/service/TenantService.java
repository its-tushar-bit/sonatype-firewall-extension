/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.brain.tenancy.TenantUtil;

@Named
public class TenantService
{
  private final TenantUtil tenantUtil;

  private final OperationalDataStore operationalDataStore;

  @Inject
  public TenantService(final TenantUtil tenantUtil, final OperationalDataStore operationalDataStore) {
    this.tenantUtil = tenantUtil;
    this.operationalDataStore = operationalDataStore;
  }

  public List<String> getAllTenantsNames() {
    List<String> schemas = DatabaseUtil.getSchemasList(operationalDataStore.getDataSource());

    return schemas.stream()
        .filter(schema -> schema.startsWith("t_"))
        .map(t -> tenantUtil.getTenantNameFromSchema(t))
        .collect(Collectors.toList());
  }

  public String getTenantSlug() {
    return TenantThreadLocal.getTenant().tenantSlug;
  }
}
