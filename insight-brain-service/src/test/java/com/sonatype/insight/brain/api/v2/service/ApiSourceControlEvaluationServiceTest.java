/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationStatus;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationStatusDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSourceControlEvaluationRequestDTO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ApiSourceControlEvaluationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SourceControlEventDAO sourceControlEventDAO;

  @Inject
  private ApiSourceControlEvaluationService apiSourceControlEvaluationService;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testEvaluateSourceControl_NoScanTarget() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(app.getId(), "http://example.com/my/repo.git", null,
        new String(passwordHandler.encryptPassword("TOKEN".toCharArray())), null, null, true, null, null);

    String stageId = Stage.ID_DEVELOP;
    String branchName = "a-branch";
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(stageId, branchName);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 = apiSourceControlEvaluationService
        .evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, null /* userAgent */);
    assertThat(apiApplicationEvaluationStatusDTOV2.statusUrl)
        .startsWith("api/v2/evaluation/applications/" + app.getId() + "/status/");

    List<SourceControlEvent> sourceControlEvents = sourceControlEventDAO.getAll();
    assertThat(sourceControlEvents).hasSize(1);
    SourceControlEvent sourceControlEvent = sourceControlEvents.get(0);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(app.getId());
    assertThat(sourceControlEvent.getStageTypeId()).isEqualTo(stageId);
    assertThat(sourceControlEvent.getBranchName()).isEqualTo(branchName);
    assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);
    assertThat(sourceControlEvent.getScanTriggerType())
        .isEqualTo(ScanTriggerType.SOURCE_CONTROL_API);
  }

  @Test
  public void testEvaluateSourceControl_WithScanTarget() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(app.getId(), "http://example.com/my/repo.git", null,
        new String(passwordHandler.encryptPassword("TOKEN".toCharArray())), null, null, true, null, null);

    String stageId = Stage.ID_DEVELOP;
    String branchName = "a-branch";
    List<String> scanTargets = Collections.singletonList("testScanTarget");
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(stageId, branchName, scanTargets);
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 = apiSourceControlEvaluationService
        .evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, null /* userAgent */);
    assertThat(apiApplicationEvaluationStatusDTOV2.statusUrl)
        .startsWith("api/v2/evaluation/applications/" + app.getId() + "/status/");

    List<SourceControlEvent> sourceControlEvents = sourceControlEventDAO.getAll();
    assertThat(sourceControlEvents).hasSize(1);
    SourceControlEvent sourceControlEvent = sourceControlEvents.get(0);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(app.getId());
    assertThat(sourceControlEvent.getStageTypeId()).isEqualTo(stageId);
    assertThat(sourceControlEvent.getBranchName()).isEqualTo(branchName);
    assertThat(sourceControlEvent.getScanTargets()).isEqualTo(scanTargets);
    assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);
    assertThat(sourceControlEvent.getScanTriggerType()).isEqualTo(ScanTriggerType.SOURCE_CONTROL_API);
  }

  @Test(expected = InvalidLicenseException.class)
  public void testEvaluateSourceControl_Unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION, LicensedFeature.NOTIFICATIONS);

    apiSourceControlEvaluationService.evaluateSourceControl("appId",
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch"), null /* userAgent */);
  }

  @Test
  public void testEvaluateSourceControl_InvalidStage() {
    Application app = tempEntity.newApplicationWithParent();

    String stageId = "InvalidStageId";
    String branchName = "a-branch";
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(stageId, branchName);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSourceControlEvaluationService
        .evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, null /* userAgent */))
        .withMessage("Stage " + stageId + " is invalid.");
  }

  @Test
  public void testEvaluateSourceControl_InvalidStageCompliance() {
    Application app = tempEntity.newApplicationWithParent();

    String stageId = Stage.ID_COMPLIANCE;
    String branchName = "a-branch";
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(stageId, branchName);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSourceControlEvaluationService
        .evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, null /* userAgent */))
        .withMessage("Stage " + stageId + " is invalid.");
  }

  @Test
  public void testEvaluateSourceControl_ScanTargetContainsPathTraversal() {
    Application app = tempEntity.newApplicationWithParent();
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch");

    apiSourceControlEvaluationRequestDTO.scanTargets = Collections.singletonList("a/../b");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiSourceControlEvaluationService.evaluateSourceControl(app.getId(),
            apiSourceControlEvaluationRequestDTO, null /* userAgent */))
        .withMessage("Scan targets cannot contain ../ or ..\\");

    apiSourceControlEvaluationRequestDTO.scanTargets = Collections.singletonList("a/..\\b");
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiSourceControlEvaluationService.evaluateSourceControl(app.getId(),
            apiSourceControlEvaluationRequestDTO, null /* userAgent */))
        .withMessage("Scan targets cannot contain ../ or ..\\");
  }

  @Test
  public void testEvaluateSourceControl_NullRequestDTO() {
    Application app = tempEntity.newApplicationWithParent();
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO = null;

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSourceControlEvaluationService
        .evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, null /* userAgent */))
        .withMessage("Missing parameters.");
  }

  @Test
  public void testEvaluateSourceControl_NoGitRepoInfo() {
    Application app = tempEntity.newApplicationWithParent();
    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> apiSourceControlEvaluationService
        .evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, "useragent"))
        .withMessage("No SCM configuration defined for application ID " + app.getId());
  }

  @Test
  public void testGetApplicationEvaluationStatus() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(app.getId(), "http://example.com/my/repo.git", null,
        new String(passwordHandler.encryptPassword("TOKEN".toCharArray())), null, null, true, null, null);

    ApiSourceControlEvaluationRequestDTO apiSourceControlEvaluationRequestDTO =
        new ApiSourceControlEvaluationRequestDTO(Stage.ID_DEVELOP, "a-branch");
    ApiApplicationEvaluationStatusDTOV2 apiApplicationEvaluationStatusDTOV2 = apiSourceControlEvaluationService
        .evaluateSourceControl(app.getId(), apiSourceControlEvaluationRequestDTO, null /* userAgent */);
    String statusId = getStatusId(apiApplicationEvaluationStatusDTOV2.statusUrl);

    ApiApplicationEvaluationResultDTOV2 apiApplicationEvaluationResultDTOV2 =
        apiSourceControlEvaluationService.getApplicationEvaluationStatus(app.getId(), statusId);

    assertThat(apiApplicationEvaluationResultDTOV2).isNotNull();
    assertThat(apiApplicationEvaluationResultDTOV2.status).isEqualTo(PolicyEvaluationStatus.PENDING.name());
    assertThat(apiApplicationEvaluationResultDTOV2.reason).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportHtmlUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.embeddableReportHtmlUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportPdfUrl).isNull();
    assertThat(apiApplicationEvaluationResultDTOV2.reportDataUrl).isNull();
  }

  private String getStatusId(String statusUrl) {
    return statusUrl.substring(statusUrl.lastIndexOf("/") + 1);
  }
}
