/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.UUID;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.tenancy.MultiTenantTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenant;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class MultiTenantTelemetryIdTest
    extends MultiTenantTest
{
  @Mock
  InsightConfig config;

  Tenant tenant1 = new Tenant("tenant1");

  Tenant tenant2 = new Tenant("tenant2");

  MultiTenantTelemetryId underTest;

  @Before
  @Override
  public void setup() {
    super.setup();

    this.underTest = new TestMultiTenantTelemetryId(config);
  }

  @Test
  public void getIdShouldStoreValuePerTenant() {
    setTenant(tenant1);

    String tenant1Id = underTest.getId();
    assertThat(tenant1Id).isNotNull();

    // Set the value for a new tenant
    setTenant(tenant2);
    String tenant2Id = underTest.getId();
    assertThat(tenant2Id).isNotNull();

    assertThat(tenant1Id).isNotEqualTo(tenant2Id);
  }

  private static class TestMultiTenantTelemetryId
      extends MultiTenantTelemetryId
  {
    public TestMultiTenantTelemetryId(InsightConfig insightConfig) {
      super(insightConfig);
    }

    @Override
    protected String generateId() {
      return UUID.randomUUID().toString();
    }
  }
}
