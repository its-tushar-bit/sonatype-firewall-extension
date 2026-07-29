/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MultiTenantInsightWorkTest
    extends AbstractMultiTenantTest
{
  static final String WORK_ROOT =
      "." + File.separator + "sonatype-work" + File.separator + "clm-server";

  static final String CLUSTER_ROOT =
      "." + File.separator + "sonatype-work" + File.separator + "clm-cluster";

  static final String RELATIVE_CLONE_DIRECTORY = "source-control";

  InsightWork underTest;

  @Before
  public void setup() {
    underTest = new InsightWork(newMultiTenantInsightConfig());
  }

  @Test
  public void testGetIerDashboardIconsDirectory() {
    testAsNewTenant(t -> assertThat(underTest.getIerDashboardIconsDirectory().getPath()).isEqualTo(
        WORK_ROOT + File.separator + "global" + File.separator + "cache" + File.separator +
            "enterpriseReportingDashboardIcons"));
  }

  @Test
  public void testGetResolvedCloneDirectory_isTenantIsolatedUnderSonatypeWorkWhenDisabled() {
    InsightWork insightWork = newInsightWork(RELATIVE_CLONE_DIRECTORY, false);

    testAsNewTenant(t -> assertThat(insightWork.getResolvedCloneDirectory().getPath()).isEqualTo(
        WORK_ROOT + File.separator + t.tenantSlug + File.separator + RELATIVE_CLONE_DIRECTORY));
  }

  @Test
  public void testGetResolvedCloneDirectory_isTenantIsolatedUnderClusterDirectoryWhenEnabled() {
    InsightWork insightWork = newInsightWork(RELATIVE_CLONE_DIRECTORY, true);

    testAsNewTenant(t -> assertThat(insightWork.getResolvedCloneDirectory().getPath()).isEqualTo(
        CLUSTER_ROOT + File.separator + t.tenantSlug + File.separator + RELATIVE_CLONE_DIRECTORY));
  }

  @Test
  public void testGetResolvedCloneDirectory_absolutePathIsUsedVerbatimWhenEnabled() {
    String absoluteCloneDirectory = new File("target", "absolute-clone-directory").getAbsolutePath();
    InsightWork insightWork = newInsightWork(absoluteCloneDirectory, true);

    testAsNewTenant(
        t -> assertThat(insightWork.getResolvedCloneDirectory()).isEqualTo(new File(absoluteCloneDirectory)));
  }

  @Test
  public void testGetResolvedCloneDirectory_absolutePathIsUsedVerbatimWhenDisabled() {
    String absoluteCloneDirectory = new File("target", "absolute-clone-directory").getAbsolutePath();
    InsightWork insightWork = newInsightWork(absoluteCloneDirectory, false);

    testAsNewTenant(
        t -> assertThat(insightWork.getResolvedCloneDirectory()).isEqualTo(new File(absoluteCloneDirectory)));
  }

  private MultiTenantInsightConfig newMultiTenantInsightConfig() {
    MultiTenantInsightConfig multiTenantInsightConfig = new MultiTenantInsightConfig();
    multiTenantInsightConfig.setSonatypeWork(WORK_ROOT);
    multiTenantInsightConfig.setClusterDirectory(CLUSTER_ROOT);
    return multiTenantInsightConfig;
  }

  private InsightWork newInsightWork(final String cloneDirectory, final boolean cloneDirectoryOnClusterStorage) {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setCloneDirectory(cloneDirectory);

    Configuration mockConfiguration = mock(Configuration.class);
    when(mockConfiguration.getSourceControlConfigurationOrDefault()).thenReturn(sourceControlConfiguration);
    when(mockConfiguration.isSourceControlCloneDirectoryOnClusterStorage())
        .thenReturn(cloneDirectoryOnClusterStorage);

    return new InsightWork(newMultiTenantInsightConfig(), mockConfiguration);
  }
}
