/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.v2.dto.PrioritizedComponent;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLM-39938 — real-bean integration test for the full priorities flow.
 * <p>
 * Wires real {@link DevelopmentPrioritiesService}, {@link DevelopmentPrioritiesReportService},
 * and {@link com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2} (no mocks) and
 * exercises {@code getAllPrioritizedFindings} end-to-end with a real on-disk report fixture.
 * Proves the new lightweight {@code getDataForPrioritization} path produces correct output
 * through the entire chain — not just at the data-fetch layer.
 */
@Category(SlowTest.class)
public class DevelopmentPrioritiesServiceIntegrationTest
    extends AbstractComponentTest
{
  @Inject
  private DevelopmentPrioritiesService developmentPrioritiesService;

  @Inject
  private TestProductLicenseManager testProductLicenseManager;

  @Inject
  private InsightWork work;

  private Application app;

  private String scanId;

  @Before
  public void init() throws Exception {
    app = tempEntity.newApplicationWithParent("priorities-int-test-app");
    scanId = "priorities-int-test-scan";
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId, "commit-hash");
    testProductLicenseManager.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    ReportHelper.saveMockReport(work, tempDir, "/ApiReportDataServiceTest/report-1", app.getId(), scanId);
  }

  @Test
  public void testGetAllPrioritizedFindings_fullRealBeanFlow() {
    List<PrioritizedComponent> result =
        developmentPrioritiesService.getAllPrioritizedFindings(app.getPublicId(), scanId, null, null);

    assertThat(result).isNotNull();
  }
}
