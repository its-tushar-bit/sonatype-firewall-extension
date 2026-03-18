/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDiffDTO;
import com.sonatype.insight.brain.dataaccess.DataAccessException;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApiReportViolationsDiffServiceTest
    extends AbstractComponentTest
{
  private static final String FROM_COMMIT_HASH = "abcdef1234abcdef1234abcdef1234abcdef1234";

  private static final String TO_COMMIT_HASH = "1234567890123456789012345678901234567890";

  private static final String FROM_SCAN_ID = "fromScanId";

  private static final String TO_SCAN_ID = "toScanId";

  private Application app;

  private final Date date = new Date();

  private String fromEvalId;

  private String toEvalId;

  @Inject
  private ApiReportViolationsDiffService apiReportViolationsDiffService;

  @Inject
  private InsightWork insightWork;

  @Before
  public void before() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testGetPolicyViolationDiff() throws URISyntaxException, IOException {
    setupValidReportsAndEvaluations();

    final ApiPolicyViolationDiffDTO apiPolicyViolationDiffDTO =
        apiReportViolationsDiffService
            .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false);

    assertValidDiffResults(apiPolicyViolationDiffDTO);
  }

  @Test
  public void testGetPolicyViolationDiff_FromEvaluationIds() throws URISyntaxException, IOException {
    // setup
    setupValidReportsAndEvaluations();

    // when calculating diff
    final ApiPolicyViolationDiffDTO apiPolicyViolationDiffDTO =
        apiReportViolationsDiffService
            .getPolicyViolationDiff(app.getPublicId(), null, null, fromEvalId, toEvalId, false);

    // then assert correct diff results
    assertValidDiffResults(apiPolicyViolationDiffDTO);
  }

  @Test
  public void testGetPolicyViolationDiff_AbbreviatedCommits() throws IOException, URISyntaxException {
    setupValidReportsAndEvaluations();

    final ApiPolicyViolationDiffDTO apiPolicyViolationDiffDTO =
        apiReportViolationsDiffService
            .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH.substring(0, 7), TO_COMMIT_HASH
                .substring(0, 7), null, null, false);

    assertValidDiffResults(apiPolicyViolationDiffDTO);
  }

  @Test
  public void testGetPolicyViolationDiff_NoFromCommitSpecified() {
    assertThatThrownBy(() -> apiReportViolationsDiffService.getPolicyViolationDiff(null, null, TO_COMMIT_HASH, null,
        null, false)).isInstanceOf(BadRequestException.class)
            .hasMessage(
                "The commit identifier or policy evaluation id for the `from` evaluation needs to be specified");
  }

  @Test
  public void testGetPolicyViolationDiff_NoFromEvaluationSpecified() {
    assertThatThrownBy(() -> apiReportViolationsDiffService.getPolicyViolationDiff(null, null, null, null,
        TO_COMMIT_HASH, false)).isInstanceOf(BadRequestException.class)
            .hasMessage(
                "The commit identifier or policy evaluation id for the `from` evaluation needs to be specified");
  }

  @Test
  public void testGetPolicyViolationDiff_NoToCommitSpecified() {
    assertThatThrownBy(() -> apiReportViolationsDiffService.getPolicyViolationDiff(null, FROM_COMMIT_HASH, null, null,
        null, false)).isInstanceOf(BadRequestException.class)
            .hasMessage("The commit identifier or policy evaluation id for the `to` evaluation needs to be specified");
  }

  @Test
  public void testGetPolicyViolationDiff_NoToEvaluationSpecified() {
    assertThatThrownBy(() -> apiReportViolationsDiffService.getPolicyViolationDiff(null, null, null, FROM_COMMIT_HASH,
        null, false)).isInstanceOf(BadRequestException.class)
            .hasMessage("The commit identifier or policy evaluation id for the `to` evaluation needs to be specified");
  }

  @Test
  public void testGetPolicyViolationDiff_SameCommitSpecified() {
    assertThatThrownBy(
        () -> apiReportViolationsDiffService.getPolicyViolationDiff(null, FROM_COMMIT_HASH, FROM_COMMIT_HASH, null,
            null, false)).isInstanceOf(BadRequestException.class)
                .hasMessage("The specified commits or evaluation ids cannot be identical.");
  }

  @Test
  public void testGetPolicyViolationDiff_SameEvaluationSpecified() {
    assertThatThrownBy(() -> apiReportViolationsDiffService.getPolicyViolationDiff(null, null, null, FROM_COMMIT_HASH,
        FROM_COMMIT_HASH, false)).isInstanceOf(BadRequestException.class)
            .hasMessage("The specified commits or evaluation ids cannot be identical.");
  }

  @Test
  public void testGetPolicyViolationDiff_InvalidFromCommitSpecified() {
    assertThatThrownBy(
        () -> apiReportViolationsDiffService.getPolicyViolationDiff(app.getPublicId(), "aaa", FROM_COMMIT_HASH, null,
            null, false)).isInstanceOf(BadRequestException.class)
                .hasMessage("The commit identifier `aaa` supplied is not a valid commit hash");
  }

  @Test
  public void testGetPolicyViolationDiff_InvalidToCommitSpecified() {
    tempEntity
        .newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            date, FROM_COMMIT_HASH);
    assertThatThrownBy(
        () -> apiReportViolationsDiffService.getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, "aaa", null,
            null, false)).isInstanceOf(BadRequestException.class)
                .hasMessage("The commit identifier `aaa` supplied is not a valid commit hash");
  }

  @Test
  public void testGetPolicyViolationDiff_FromCommitNotFound() {
    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("The policy violation diff could not be determined for the given request.");
  }

  @Test
  public void testGetPolicyViolationDiff_FromEvaluationNotFound() {
    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("The policy violation diff could not be determined for the given request.");
  }

  @Test
  public void testGetPolicyViolationDiff_ToCommitNotFound() {
    toEvalId = tempEntity
        .newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            date, FROM_COMMIT_HASH)
        .getId();
    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), null, null, "aaa", toEvalId, false))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("The policy violation diff could not be determined for the given request.");
  }

  @Test
  public void testGetPolicyViolationDiff_ToEvaluationNotFound() {
    fromEvalId = tempEntity
        .newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            date, FROM_COMMIT_HASH)
        .getId();
    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), null, null, fromEvalId, "aaa", false))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("The policy violation diff could not be determined for the given request.");
  }

  @Test
  public void testGetPolicyViolationDiff_NoAppIdSpecified() {
    assertThatThrownBy(
        () -> apiReportViolationsDiffService.getPolicyViolationDiff(null, FROM_COMMIT_HASH, TO_COMMIT_HASH, null,
            null, false)).isInstanceOf(DataAccessException.class)
                .hasMessage("The application public ID cannot be null or empty.");
  }

  @Test
  public void testGetPolicyViolationDiff_AppNotFound() {
    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff("INVALID_APP", FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false)).isInstanceOf(
            NotFoundException.class)
            .hasMessage("Could not find an application with public ID INVALID_APP.");
  }

  @Test
  public void testGetPolicyViolationDiff_FromCommitNoReport() {
    // setup evaluations
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID, false, false, false,
        date, FROM_COMMIT_HASH);
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID, false, false, false,
        date, TO_COMMIT_HASH);

    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false)).isInstanceOf(
            NotFoundException.class)
            .hasMessage("The policy violation diff could not be determined for the given request.");
  }

  @Test
  public void testGetPolicyViolationDiff_FromCommitMissingAlerts() throws URISyntaxException, IOException {
    // setup reports
    createReportFile(app.getId(), FROM_SCAN_ID,
        zipReportDir("/PolicyEvaluationDiffServiceTest/report-missing-policyalerts", tempDir), insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/to-report", tempDir),
        insightWork);

    // setup evaluations
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID, false, false, false,
        date, FROM_COMMIT_HASH);
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID, false, false, false,
        date, TO_COMMIT_HASH);

    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, null, false)).isInstanceOf(
            NotFoundException.class)
            .hasMessage("The policy violation diff could not be determined for the given request.");
  }

  @Test
  public void testGetPolicyViolationDiff_FromEvaluationAndCommitSpecified() {
    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, "aaa", null, false))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Cannot specify both commit identifier and evaluation id for `from` evaluation.");
  }

  @Test
  public void testGetPolicyViolationDiff_ToEvaluationAndCommitSpecified() {
    assertThatThrownBy(() -> apiReportViolationsDiffService
        .getPolicyViolationDiff(app.getPublicId(), FROM_COMMIT_HASH, TO_COMMIT_HASH, null, "aaa", false)).isInstanceOf(
            BadRequestException.class)
            .hasMessage("Cannot specify both commit identifier and evaluation id for `to` evaluation.");
  }

  private void assertValidDiffResults(final ApiPolicyViolationDiffDTO apiPolicyViolationDiffDTO) {
    assertThat(apiPolicyViolationDiffDTO.fromCommit).isNotNull();
    assertThat(apiPolicyViolationDiffDTO.toCommit).isNotNull();
    assertThat(apiPolicyViolationDiffDTO.application).isNotNull();
    assertThat(apiPolicyViolationDiffDTO.addedViolations).isNotNull();
    assertThat(apiPolicyViolationDiffDTO.addedViolations).hasSize(4);
    assertThat(apiPolicyViolationDiffDTO.addedViolations).extracting(dto -> dto.policyViolationId)
        .containsExactlyInAnyOrder("appeared_1", "appeared_2", "appeared_3", "appeared_4");
    assertThat(apiPolicyViolationDiffDTO.sameViolations).isNotNull();
    assertThat(apiPolicyViolationDiffDTO.sameViolations).hasSize(3);
    assertThat(apiPolicyViolationDiffDTO.sameViolations).extracting(dto -> dto.policyViolationId)
        .containsExactlyInAnyOrder("same_1", "same_2", "same_3");
    assertThat(apiPolicyViolationDiffDTO.removedViolations).isNotNull();
    assertThat(apiPolicyViolationDiffDTO.removedViolations).hasSize(4);
    assertThat(apiPolicyViolationDiffDTO.removedViolations).extracting(dto -> dto.policyViolationId)
        .containsExactlyInAnyOrder("cleared_1", "cleared_2", "cleared_3", "cleared_4");
    assertThat(apiPolicyViolationDiffDTO.fromCommit.commitHash).isEqualTo(FROM_COMMIT_HASH);
    assertThat(apiPolicyViolationDiffDTO.fromCommit.scanId).isEqualTo(FROM_SCAN_ID);
    assertThat(apiPolicyViolationDiffDTO.fromCommit.scanTime).isEqualTo(date);
    assertThat(apiPolicyViolationDiffDTO.toCommit.commitHash).isEqualTo(TO_COMMIT_HASH);
    assertThat(apiPolicyViolationDiffDTO.toCommit.scanId).isEqualTo(TO_SCAN_ID);
    assertThat(apiPolicyViolationDiffDTO.toCommit.scanTime).isEqualTo(date);
    assertThat(apiPolicyViolationDiffDTO.application.id).isEqualTo(app.getId());
    assertThat(apiPolicyViolationDiffDTO.application.contactUserName).isEqualTo(app.getContactInternalName());
    assertThat(apiPolicyViolationDiffDTO.application.publicId).isEqualTo(app.getPublicId());
    assertThat(apiPolicyViolationDiffDTO.application.name).isEqualTo(app.getName());
    assertThat(apiPolicyViolationDiffDTO.application.organizationId).isEqualTo(app.getOrganizationId());
  }

  private void setupValidReportsAndEvaluations() throws IOException, URISyntaxException {
    // setup reports
    createReportFile(app.getId(), FROM_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/from-report", tempDir),
        insightWork);
    createReportFile(app.getId(), TO_SCAN_ID, zipReportDir("/PolicyEvaluationDiffServiceTest/to-report", tempDir),
        insightWork);

    // setup evaluations
    fromEvalId = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID, false, false, false,
        date, FROM_COMMIT_HASH).getId();
    toEvalId = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID, false, false, false,
        date, TO_COMMIT_HASH).getId();
  }
}
