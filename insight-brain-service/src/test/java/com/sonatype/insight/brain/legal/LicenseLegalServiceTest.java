/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LicenseLegalServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LicenseLegalService licenseLegalService;

  @Inject
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  private InsightWork insightWork;

  @Test
  public void testGetLatestRawReportForApplication() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Application otherApp = tempEntity.newApplicationWithParent();
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, tempEntity.uuid(), new Date(1));
    tempEntity.newPolicyEvaluation(app.getId(), StageReleaseStageType.ID, tempEntity.uuid(), new Date(2));
    PolicyEvaluation policyEvaluation3 =
        tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, tempEntity.uuid(), new Date(3));
    mockReport(policyEvaluation3);
    tempEntity.newPolicyEvaluation(otherApp.getId(), ReleaseStageType.ID, tempEntity.uuid(), new Date(4));

    Optional<ApiReportRawDataDTOV2> latestRawReportForApplication =
        licenseLegalService.getLatestRawReportForApplication(app.getPublicId());

    assertThat(latestRawReportForApplication).isPresent().get().usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app.getPublicId(), policyEvaluation3.getScanId()));
  }

  @Test
  public void testGetLatestRawReportForApplication_NoApplication() {
    assertThat(licenseLegalService.getLatestRawReportForApplication("doesNotExist")).isEmpty();
  }

  @Test
  public void testGetLatestRawReportForApplication_NoEvaluations() {
    Application app = tempEntity.newApplicationWithParent();

    assertThat(licenseLegalService.getLatestRawReportForApplication(app.getPublicId())).isEmpty();
  }

  @Test
  public void testGetApplications() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplication(app1.getOrganizationId());
    Application app3 = tempEntity.newApplicationWithParent();

    assertThat(licenseLegalService.getApplications()).extracting(Application::getId)
        .containsExactlyInAnyOrder(app1.getId(), app2.getId(), app3.getId());
  }

  @Test
  public void testGetApplications_NoApplications() {
    assertThat(licenseLegalService.getApplications()).isEmpty();
  }

  @Test
  public void testGetReportsForOrg() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplication(app1.getOrganizationId());
    tempEntity.newApplication(app1.getOrganizationId());
    Application otherApp = tempEntity.newApplicationWithParent();

    PolicyEvaluation policyEvaluation1 =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, tempEntity.uuid());
    PolicyEvaluation policyEvaluation2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, tempEntity.uuid());
    PolicyEvaluation otherPolicyEvaluation =
        tempEntity.newPolicyEvaluation(otherApp.getId(), BuildStageType.ID, tempEntity.uuid());

    mockReport(policyEvaluation1);
    mockReport(policyEvaluation2);
    mockReport(otherPolicyEvaluation);

    Set<ApplicationReportRawDataDTO> reportsForOrg = licenseLegalService.getReportsForOrg(app1.getOrganizationId());

    assertThat(reportsForOrg).extracting(dto -> dto.applicationPublicId)
        .containsExactlyInAnyOrder(app1.getPublicId(), app2.getPublicId());
    ApplicationReportRawDataDTO app1Result = reportsForOrg.stream()
        .filter(dto -> dto.applicationPublicId.equals(app1.getPublicId())).findFirst().orElse(null);
    assertThat(app1Result).isNotNull().extracting(dto -> dto.apiReportRawDataDTOV2).usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app1.getPublicId(), policyEvaluation1.getScanId()));
    ApplicationReportRawDataDTO app2Result = reportsForOrg.stream()
        .filter(dto -> dto.applicationPublicId.equals(app2.getPublicId())).findFirst().orElse(null);
    assertThat(app2Result).isNotNull().extracting(dto -> dto.apiReportRawDataDTOV2).usingRecursiveComparison()
        .isEqualTo(apiReportDataServiceV2.getDataNoAuth(app2.getPublicId(), policyEvaluation2.getScanId()));
  }

  @Test
  public void testGetReportsForOrg_NoApplications() {
    Organization org = tempEntity.newOrganization();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> licenseLegalService.getReportsForOrg(org.getId()))
        .withMessage("Cannot find applications for organization with id " + org.getId() + ".");
  }

  private void mockReport(PolicyEvaluation evaluation) {
    try {
      Path reportDir = insightWork.getReportDir(evaluation.getApplicationId(), evaluation.getScanId()).toPath();
      Files.createDirectories(reportDir);
      Files.write(reportDir.resolve("report.zip"), Collections.singletonList("report.zip"));
      File reportFile = reportDir.resolve("report.zip").toFile();
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(reportFile))) {
        zos.putNextEntry(new ZipEntry("index.html"));
      }
      String[] filenames = {
          Report.BOM_JSON_FILENAME, Report.SECURITY_JSON_FILENAME, Report.LICENSES_JSON_FILENAME,
          Report.DATA_JSON_FILENAME, Report.DEPENDENCIES_JSON_FILENAME
      };
      for (String filename : filenames) {
        File file = Report.getCacheFile(reportFile, filename);
        FileUtils.copyURLToFile(getClass().getResource("/LicenseLegalServiceTest/report/" + filename), file);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
