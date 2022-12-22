/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.TenantReference;

@Named
@Singleton
public class MultiTenantTelemetryId
    extends TelemetryId
{
  private final TenantReference<String> tenantTelemetryId = new TenantReference<>();

  @Inject
  public MultiTenantTelemetryId(InsightConfig insightConfig) {
    super(insightConfig);
  }

  @Override
  public String getId() {
    return tenantTelemetryId.computeIfAbsent(t -> generateId());
  }
}
