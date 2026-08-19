/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantInsightConfigTest
    extends AbstractMultiTenantTest
{
  static final String WORK_ROOT =
      "." + File.separator + "sonatype-work" + File.separator + "clm-server";

  static final String CLUSTER_ROOT =
      "." + File.separator + "sonatype-work" + File.separator + "clm-cluster";

  MultiTenantInsightConfig underTest;

  @BeforeEach
  public void setup() {
    underTest = new MultiTenantInsightConfig();
    underTest.setSonatypeWork(WORK_ROOT);
    underTest.setClusterDirectory(CLUSTER_ROOT);
  }

  @Test
  public void testGetSonatypeWork() {
    assertThat(underTest.getSonatypeWork().getPath()).isEqualTo(WORK_ROOT + File.separator + "global");

    testAsNewTenant(
        t -> assertThat(underTest.getSonatypeWork().getPath()).isEqualTo(WORK_ROOT + File.separator + t.tenantSlug));
  }

  @Test
  public void testGetClusterDirectory() {
    assertThat(underTest.getClusterDirectory().getPath()).isEqualTo(CLUSTER_ROOT + File.separator + "global");

    testAsNewTenant(
        t -> assertThat(underTest.getClusterDirectory().getPath()).isEqualTo(
            CLUSTER_ROOT + File.separator + t.tenantSlug));
  }

  @Test
  public void testGetApplicationConnectorPorts() {
    assertThat(underTest.getApplicationConnectorPorts()).isNotNull();
  }
}
