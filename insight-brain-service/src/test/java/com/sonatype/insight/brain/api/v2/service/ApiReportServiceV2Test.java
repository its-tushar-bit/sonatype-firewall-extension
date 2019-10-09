/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.telemetry.ReportsTelemetry;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.verify;

public class ApiReportServiceV2Test
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiReportServiceV2 apiReportServiceV2;

  @Mock
  private ReportsTelemetry reportsTelemetryMock;

  private Application appOne;

  private Application appThree;

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    binder.bind(ReportsTelemetry.class).toInstance(reportsTelemetryMock);
  }

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

    assertThat(reports).hasSize(3);

    assertContainsReport(appOne, StageTypes.BUILD, "one-build", reports);
    assertContainsReport(appOne, StageTypes.RELEASE, "one-release", reports);
    assertContainsReport(appThree, StageTypes.OPERATE, "three", reports);
  }

  @Test
  public void testAll_SendTelemetry() {
    apiReportServiceV2.getAll();
    verify(reportsTelemetryMock).sendAllApplicationsTelemetry();
  }

  @Test
  public void testSpecific() {
    List<ApiApplicationReportDTOV2> reports = apiReportServiceV2.getByApplicationId(appOne.getId());

    assertThat(reports).hasSize(2);

    assertContainsReport(appOne, StageTypes.BUILD, "one-build", reports);
    assertContainsReport(appOne, StageTypes.RELEASE, "one-release", reports);
  }

  @Test
  public void testSpecific_SendTelemetry() {
    apiReportServiceV2.getByApplicationId(appOne.getId());
    verify(reportsTelemetryMock).sendSingleApplicationTelemetry();
  }

  private void assertContainsReport(
      Application app,
      StageType expectedStage,
      String expectedScanId,
      List<ApiApplicationReportDTOV2> actual)
  {
    String expectedStageId = expectedStage.getId();
    for (ApiApplicationReportDTOV2 report : actual) {
      if (app.getId().equals(report.applicationId) && expectedStageId.equals(report.stage)) {
        assertThat(report.reportPdfUrl)
            .isEqualTo(UserInterfaceLinksResource.getPdfUrl(app.getPublicId(), expectedScanId));
        assertThat(report.reportHtmlUrl)
            .isEqualTo(UserInterfaceLinksResource.getReportUrl(app.getPublicId(), expectedScanId));
        assertThat(report.embeddableReportHtmlUrl)
            .isEqualTo(UserInterfaceLinksResource.getEmbeddableReportUrl(app.getPublicId(), expectedScanId));
        assertThat(report.reportDataUrl)
            .isEqualTo(ApiReportDataResourceV2.getDataUrl(app.getPublicId(), expectedScanId));
        return;
      }
    }
    fail("Did not find appId:" + app.getPublicId() + " stage:" + expectedStageId + " scanId:" + expectedScanId);
  }
}
