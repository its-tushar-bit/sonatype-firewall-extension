/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Date;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.ci.config.ApiReportMetadataResponseDto;
import com.sonatype.clm.dto.model.ci.config.MetadataSource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiReportMetadataServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiReportMetadataServiceV2 metadataService;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  private Application app;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    app = tempEntity.newApplicationWithParent();
  }

  /**
   * Create a PolicyEvaluation with metadata fields set, then insert it.
   * PolicyEvaluationDAO.update() is not supported, so fields must be set before insert.
   */
  private PolicyEvaluation createEvaluationWithMetadata(
      String applicationId,
      String scanId,
      Date time,
      String commitHash,
      String branchName,
      String scmRepositoryUrl,
      MetadataSource commitHashSource,
      MetadataSource branchNameSource,
      MetadataSource scmRepositoryUrlSource)
  {
    PolicyEvaluation evaluation = new PolicyEvaluation(
        applicationId, BuildStageType.ID, scanId, "system", ScanTriggerType.CLI);
    evaluation.setTime(time);
    evaluation.setCommitHash(commitHash);
    evaluation.setBranchName(branchName);
    evaluation.setScmRepositoryUrl(scmRepositoryUrl);
    evaluation.setCommitHashSource(commitHashSource);
    evaluation.setBranchNameSource(branchNameSource);
    evaluation.setScmRepositoryUrlSource(scmRepositoryUrlSource);
    policyEvaluationDAO.insert(evaluation);
    return evaluation;
  }

  @Test
  public void getMetadata_ReturnsAllFields_WhenEvaluationExists() {
    // Given: evaluation with all metadata fields
    String scanId = "test-scan-id";
    Date scanDate = new Date();
    String commitHash = "abcdef1234567890";
    String branchName = "feature/test-branch";
    String scmRepositoryUrl = "https://github.com/org/repo.git";

    createEvaluationWithMetadata(
        app.getId(), scanId, scanDate, commitHash, branchName, scmRepositoryUrl,
        MetadataSource.GIT_AUTO_DETECTED, MetadataSource.ENVIRONMENT_VARIABLE, MetadataSource.ENVIRONMENT_VARIABLE);

    // When: get metadata
    ApiReportMetadataResponseDto response = metadataService.getMetadata(app, scanId);

    // Then: all fields populated
    assertThat(response).isNotNull();
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().getScanId()).isEqualTo(scanId);
    assertThat(response.getData().getApplicationId()).isEqualTo(app.getId());
    assertThat(response.getData().getApplicationPublicId()).isEqualTo(app.getPublicId());
    assertThat(response.getData().getStage()).isEqualTo(BuildStageType.ID);
    assertThat(response.getData().getScanDate()).isEqualTo(scanDate);
    assertThat(response.getData().getCommitHash()).isEqualTo(commitHash);
    assertThat(response.getData().getBranchName()).isEqualTo(branchName);
    assertThat(response.getData().getScmRepositoryUrl()).isEqualTo(scmRepositoryUrl);

    // And: source provenance populated
    assertThat(response.getSource()).isNotNull();
    assertThat(response.getSource().get("commitHash")).isEqualTo("GIT_AUTO_DETECTED");
    assertThat(response.getSource().get("branchName")).isEqualTo("ENVIRONMENT_VARIABLE");
    assertThat(response.getSource().get("scmRepositoryUrl")).isEqualTo("ENVIRONMENT_VARIABLE");
  }

  @Test
  public void getMetadata_ReturnsNullFields_WhenMetadataNotSet() {
    // Given: evaluation with minimal metadata (upgrade scenario)
    String scanId = "minimal-scan-id";

    createEvaluationWithMetadata(
        app.getId(), scanId, new Date(), null, null, null,
        null, null, null);

    // When: get metadata
    ApiReportMetadataResponseDto response = metadataService.getMetadata(app, scanId);

    // Then: null fields handled gracefully
    assertThat(response).isNotNull();
    assertThat(response.getData().getCommitHash()).isNull();
    assertThat(response.getData().getBranchName()).isNull();
    assertThat(response.getData().getScmRepositoryUrl()).isNull();
    assertThat(response.getSource()).isEmpty();
  }

  @Test
  public void getMetadata_MasksCredentials_WhenUrlContainsAuth() {
    // Given: evaluation with URL containing credentials
    String scanId = "cred-scan-id";
    String urlWithCreds = "https://user:secret-token@github.com/org/repo.git";
    String expectedMaskedUrl = "https://****:****@github.com/org/repo.git";

    createEvaluationWithMetadata(
        app.getId(), scanId, new Date(), null, null, urlWithCreds,
        null, null, null);

    // When: get metadata
    ApiReportMetadataResponseDto response = metadataService.getMetadata(app, scanId);

    // Then: credentials masked
    assertThat(response.getData().getScmRepositoryUrl()).isEqualTo(expectedMaskedUrl);
  }

  @Test
  public void getMetadata_MasksCredentials_WhenUrlContainsToken() {
    // Given: evaluation with git URL containing token
    String scanId = "token-scan-id";
    String urlWithToken = "https://x-access-token:ghp_12345abcdef@github.com/org/repo.git";
    String expectedMaskedUrl = "https://****:****@github.com/org/repo.git";

    createEvaluationWithMetadata(
        app.getId(), scanId, new Date(), null, null, urlWithToken,
        null, null, null);

    // When: get metadata
    ApiReportMetadataResponseDto response = metadataService.getMetadata(app, scanId);

    // Then: token masked
    assertThat(response.getData().getScmRepositoryUrl()).isEqualTo(expectedMaskedUrl);
  }

  @Test
  public void getMetadata_ThrowsNotFoundException_WhenScanNotFound() {
    // Given: non-existent scan ID
    String nonExistentScanId = "non-existent-scan";

    // When/Then: throws NotFoundException with descriptive message
    assertThatThrownBy(() -> metadataService.getMetadata(app, nonExistentScanId))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Policy evaluation not found for scan: " + nonExistentScanId);
  }

  @Test
  public void getMetadata_ThrowsNotFoundException_WhenApplicationNotFound() {
    // Given: non-existent application
    String nonExistentAppId = "non-existent-app";
    String scanId = "some-scan";

    // When/Then: throws NotFoundException (application lookup fails first)
    assertThatThrownBy(() -> metadataService.getMetadata(applicationDAO.getByPublicIdNotNull(nonExistentAppId), scanId))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void getMetadata_IncludesSourceMap_WhenPartialSourcesSet() {
    // Given: evaluation with only some source fields set
    String scanId = "partial-source-scan";

    createEvaluationWithMetadata(
        app.getId(), scanId, new Date(), "abc123", null, null,
        MetadataSource.ENVIRONMENT_VARIABLE, null, null);

    // When: get metadata
    ApiReportMetadataResponseDto response = metadataService.getMetadata(app, scanId);

    // Then: only non-null sources in map
    assertThat(response.getSource()).containsKey("commitHash");
    assertThat(response.getSource().get("commitHash")).isEqualTo("ENVIRONMENT_VARIABLE");
    assertThat(response.getSource()).doesNotContainKey("branchName");
    assertThat(response.getSource()).doesNotContainKey("scmRepositoryUrl");
  }

  @Test
  public void getMetadata_ReturnsCorrectApplicationPublicId() {
    // Given: specific application public ID
    String specificPublicId = "my-specific-app-id";
    Application specificApp = tempEntity.newApplicationWithParent(specificPublicId);
    String scanId = "test-scan";

    createEvaluationWithMetadata(
        specificApp.getId(), scanId, new Date(), null, null, null,
        null, null, null);

    // When: get metadata
    ApiReportMetadataResponseDto response = metadataService.getMetadata(specificApp, scanId);

    // Then: correct public ID returned
    assertThat(response.getData().getApplicationPublicId()).isEqualTo(specificPublicId);
  }

  @Test
  public void getMetadata_Hrc_ReturnsFieldsWithHrcIdAsBothInternalAndPublicId() {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    String scanId = "hrc-scan-id";
    Date scanDate = new Date();

    createEvaluationWithMetadata(hrc.getId(), scanId, scanDate,
        "hrc-commit", "hrc-branch", null, null, null, null);

    ApiReportMetadataResponseDto response = metadataService.getMetadata(hrc, scanId);

    assertThat(response).isNotNull();
    assertThat(response.getData().getScanId()).isEqualTo(scanId);
    assertThat(response.getData().getApplicationId()).isEqualTo(hrc.getId());
    assertThat(response.getData().getApplicationPublicId()).isEqualTo(hrc.getId());
    assertThat(response.getData().getStage()).isEqualTo(BuildStageType.ID);
    assertThat(response.getData().getScanDate()).isEqualTo(scanDate);
    assertThat(response.getData().getCommitHash()).isEqualTo("hrc-commit");
    assertThat(response.getData().getBranchName()).isEqualTo("hrc-branch");
  }

  @Test
  public void getMetadata_Hrc_ThrowsNotFoundException_WhenScanNotFound() {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);
    String scanId = "no-such-hrc-scan";

    assertThatThrownBy(() -> metadataService.getMetadata(hrc, scanId))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Policy evaluation not found for scan: " + scanId);
  }
}
