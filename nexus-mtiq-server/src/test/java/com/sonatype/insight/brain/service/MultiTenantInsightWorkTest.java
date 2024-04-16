/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantInsightWorkTest
    extends AbstractMultiTenantTest
{
  static final String WORK_ROOT =
      "." + File.separator + "sonatype-work" + File.separator + "clm-server";

  static final String CLUSTER_ROOT =
      "." + File.separator + "sonatype-work" + File.separator + "clm-cluster";

  InsightWork underTest;

  @Before
  public void setup() {
    MultiTenantInsightConfig multiTenantInsightConfig = new MultiTenantInsightConfig();
    multiTenantInsightConfig.setSonatypeWork(WORK_ROOT);
    multiTenantInsightConfig.setClusterDirectory(CLUSTER_ROOT);
    underTest = new InsightWork(multiTenantInsightConfig);
  }

  @Test
  public void testGetIerDashboardIconsDirectory() {
    testAsNewTenant(t -> assertThat(underTest.getIerDashboardIconsDirectory().getPath()).isEqualTo(
        WORK_ROOT + File.separator + "global" + File.separator + "cache" + File.separator +
            "enterpriseReportingDashboardIcons"));
  }
}
