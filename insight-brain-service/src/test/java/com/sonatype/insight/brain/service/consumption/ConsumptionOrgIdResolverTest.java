/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenantAndInvalidate;
import static org.assertj.core.api.Assertions.assertThat;

public class ConsumptionOrgIdResolverTest
{
  @Test
  public void resolveForRequest_globalTenant_returnsNull() {
    testAsTenant(Tenant.GLOBAL_TENANT, t -> assertThat(ConsumptionOrgIdResolver.resolveForRequest()).isNull());
  }

  @Test
  public void resolveForRequest_singleTenant_returnsRootOrg() {
    testAsTenant(Tenant.SINGLE_TENANT, t -> assertThat(ConsumptionOrgIdResolver.resolveForRequest())
        .isEqualTo(Organization.ROOT_ORGANIZATION_ID));
  }

  @Test
  public void resolveForRequest_realTenant_returnsTenantSlug() {
    testAsTenantAndInvalidate("acme-corp",
        t -> assertThat(ConsumptionOrgIdResolver.resolveForRequest()).isEqualTo("acme-corp"));
  }

  @Test
  public void resolveForBackgroundJob_globalTenant_returnsNull() {
    testAsTenant(Tenant.GLOBAL_TENANT, t -> assertThat(ConsumptionOrgIdResolver.resolveForBackgroundJob()).isNull());
  }

  @Test
  public void resolveForBackgroundJob_singleTenant_returnsRootOrg() {
    testAsTenant(Tenant.SINGLE_TENANT, t -> assertThat(ConsumptionOrgIdResolver.resolveForBackgroundJob())
        .isEqualTo(Organization.ROOT_ORGANIZATION_ID));
  }

  @Test
  public void resolveForBackgroundJob_realTenant_returnsTenantSlug() {
    testAsTenantAndInvalidate("acme-corp-bg",
        t -> assertThat(ConsumptionOrgIdResolver.resolveForBackgroundJob()).isEqualTo("acme-corp-bg"));
  }
}
