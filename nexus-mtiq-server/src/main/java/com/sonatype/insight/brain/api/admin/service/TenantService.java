/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin.service;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.tenancy.TenantUtil;

@Named
public class TenantService
{
  private final TenantUtil tenantUtil;

  @Inject
  public TenantService(TenantUtil tenantUtil) {
    this.tenantUtil = tenantUtil;
  }

  public List<String> getAllTenantsNames() {
    return tenantUtil.getAllTenantsNames();
  }
}
