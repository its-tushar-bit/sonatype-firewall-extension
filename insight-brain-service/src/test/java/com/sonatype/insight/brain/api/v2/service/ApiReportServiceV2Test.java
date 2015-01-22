/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationReportDTOV2;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ApiReportServiceV2Test
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiReportServiceV2 apiReportServiceV2;

  private Application appOne;

  private Application appThree;

  @Before
  public void setup() {
    appOne = tempEntity.newApplicationWithParent("one");
    tempEntity.newPolicyEvaluation(appOne.getId(), StageTypes.BUILD.getId(), "one-old",
        new Date(System.currentTimeMillis() - 1000));
    tempEntity.newPolicyEvaluation(appOne.getId(), StageTypes.BUILD.getId(), "one-build");
    tempEntity.newPolicyEvaluation(appOne.getId(), StageTypes.RELEASE.getId(), "one-release");
    grantReadPermission(appOne.getId());

    Application app = tempEntity.newApplicationWithParent("two");
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.STAGE_RELEASE.getId(), "two");

    appThree = tempEntity.newApplicationWithParent("three");
    tempEntity.newPolicyEvaluation(appThree.getId(), StageTypes.OPERATE.getId(), "three");
    grantReadPermission(appThree.getId());
  }

  @Test
  public void testAll() {
    List<ApiApplicationReportDTOV2> reports = apiReportServiceV2.getAll();

    assertEquals(3, reports.size());

    assertContainsReport(appOne, StageTypes.BUILD, "one-build", reports);
    assertContainsReport(appOne, StageTypes.RELEASE, "one-release", reports);
    assertContainsReport(appThree, StageTypes.OPERATE, "three", reports);
  }

  @Test
  public void testSpecific() {
    List<ApiApplicationReportDTOV2> reports = apiReportServiceV2.getByApplicationId(appOne.getId());

    assertEquals(2, reports.size());

    assertContainsReport(appOne, StageTypes.BUILD, "one-build", reports);
    assertContainsReport(appOne, StageTypes.RELEASE, "one-release", reports);
  }

  private void assertContainsReport(Application app, StageType expectedStage, String expectedScanId,
      List<ApiApplicationReportDTOV2> actual)
  {
    String expectedStageId = expectedStage.getId();
    for (ApiApplicationReportDTOV2 report : actual) {
      if (app.getId().equals(report.applicationId) && expectedStageId.equals(report.stage)) {
        assertEquals(UserInterfaceLinksResource.getPdfUrl(app.getPublicId(), expectedScanId), report.reportPdfUrl);
        assertEquals(UserInterfaceLinksResource.getReportUrl(app.getPublicId(), expectedScanId), report.reportHtmlUrl);
        assertEquals(ApiReportDataResourceV2.getDataUrl(app.getPublicId(), expectedScanId), report.reportDataUrl);
        return;
      }
    }
    fail("Did not find appId:" + app.getPublicId() + " stage:" + expectedStageId + " scanId:" + expectedScanId);
  }
}
