/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.tenancy.MultiTenantTest;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.setTenant;
import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantInsightConfigTest
    extends MultiTenantTest
{
  static final String WORK_ROOT = "./sonatype-work/clm-server";

  static final String CLUSTER_ROOT = "./sonatype-work/clm-cluster";

  static final String TENANT_NAME = "new-tenant";

  MultiTenantInsightConfig underTest;

  @Before
  @Override
  public void setup() {
    super.setup();
    underTest = new MultiTenantInsightConfig();
    underTest.setSonatypeWork(WORK_ROOT);
    underTest.setClusterDirectory(CLUSTER_ROOT);
  }

  @Test
  public void testGetSonatypeWork() {
    assertThat(underTest.getSonatypeWork().getPath()).isEqualTo(WORK_ROOT + "/global");

    setTenant(new Tenant(TENANT_NAME));

    assertThat(underTest.getSonatypeWork().getPath()).isEqualTo(WORK_ROOT + "/new-tenant");
  }

  @Test
  public void testGetClusterDirectory() {
    assertThat(underTest.getClusterDirectory().getPath()).isEqualTo(CLUSTER_ROOT + "/global");

    setTenant(new Tenant(TENANT_NAME));

    assertThat(underTest.getClusterDirectory().getPath()).isEqualTo(CLUSTER_ROOT + "/new-tenant");
  }
}
