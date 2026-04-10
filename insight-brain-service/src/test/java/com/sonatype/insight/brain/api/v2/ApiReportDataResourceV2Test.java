/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDiffDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationForDiffDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.lang.time.DateUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import static com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2.SCAN_PATH;
import static com.sonatype.insight.brain.api.v2.service.ApiReportViolationsDiffService.CANT_CALCULATE_DIFF_MESSAGE;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.PolicyViolation.POLICY_VIOLATION;

public class ApiReportDataResourceV2Test
    extends AbstractResourceTest
{
  private static final String FROM_COMMIT_HASH = "abcdef1234abcdef1234abcdef1234abcdef1234";

  private static final String TO_COMMIT_HASH = "1234567890123456789012345678901234567890";

  private static final String FROM_SCAN_ID = "fromScanId";

  private static final String TO_SCAN_ID = "toScanId";

  private static final String ORG_NAME = "TEST ORG";

  private static final String ORG_ID = "TEST_ORG_ID";

  private static final String APP_NAME = "TEST APP";

  private static final String APP_INTERNAL_ID = "TEST_APP_INTERNAL_ID";

  private static final String APP_PUBLIC_ID = "TEST_APP_PUBLIC_ID";

  private Application app;

  private final Date date = new Date();

  private InsightWork insightWork;

  private String fromEvalId;

  private String toEvalId;

  @Before
  public void setupApplication() {
    tempEntity.newOrganizationWithSpecificId(ORG_ID, ORG_NAME);
    app = tempEntity.newApplicationWithSpecificId(APP_INTERNAL_ID, APP_NAME, APP_PUBLIC_ID, ORG_ID);
    insightWork = new InsightWork(getCLMServer().getConfiguration());
  }

  @Test
  public void testGetData_Redirect() throws Exception {
    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .parameter("testAppPublicId", "testScanId")
        .get();

    assertResponseStatus(307, response);
    assertThat(response.getHeader("Location"))
        .isEqualTo(getRestBaseUrl() + "api/v2/applications/testAppPublicId/reports/testScanId/raw");
  }

  @Test
  public void testGetDataUrl() {
    assertThat(ApiReportDataResourceV2.getDataUrl("testAppPublicId", "testScanId"))
        .isEqualTo("api/v2/applications/testAppPublicId/reports/testScanId/raw");
  }

  @Test
  public void testGetRawData() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report");

    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.RAW_DATA_PATH)
        .parameter(appPublicId, scanId)
        .get();

    assertResponseStatus(200, response);
    ApiReportRawDataDTOV2 dto = response.getBody(ApiReportRawDataDTOV2.class);
    assertThat(dto.components).hasSize(2);
  }

  @Test
  public void testGetPolicyViolations() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report");

    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.POLICY_DATA_PATH)
        .parameter(appPublicId, scanId)
        .get();

    assertResponseStatus(200, response);
    ApiReportPolicyDataDTOV2 dto = response.getBody(ApiReportPolicyDataDTOV2.class);
    assertThat(dto.components).hasSize(2);
    assertThat(dto.counts.get("totalComponentCount")).isEqualTo(2);
    // counts should not have null props
    assertThat(dto.counts).doesNotContainKey("grandfatheredPolicyViolationCount");
    assertThat(dto.counts).doesNotContainKey("legacyViolationCount");
  }

  @Test
  public void testGetPolicyViolations_noCounts() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report-no-counts");

    HttpResponse response = restRequest().path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.POLICY_DATA_PATH)
        .parameter(appPublicId, scanId)
        .get();

    assertResponseStatus(200, response);
    ApiReportPolicyDataDTOV2 dto = response.getBody(ApiReportPolicyDataDTOV2.class);
    assertThat(dto.components).hasSize(2);
    // should not have counts prop if there are no counts
    assertThat(response.getBodyText()).doesNotContain("counts");
  }

  @Test
  public void testGetPolicyViolations_includeViolationTimes() throws Exception {
    String scanId = "scanId";
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    // Use an existing report and insert a corresponding policy violation in the db but update its times
    createReportFile(application.getId(), scanId, "/" + getClass().getSimpleName() + "/report");
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ReleaseStageType.ID, scanId);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setLegacyViolationTime(DateUtils.addDays(policyEvaluation.getTime(), 1));
    policyViolation.setWaiveTime(DateUtils.addDays(policyEvaluation.getTime(), 2));
    policyViolation.setFixTime(DateUtils.addDays(policyEvaluation.getTime(), 3));
    PolicyViolationDAO policyViolationDAO = getCLMServer().getInstance(PolicyViolationDAO.class);
    policyViolationDAO.update(policyViolation);
    try (com.sonatype.insight.dataaccess.TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(POLICY_VIOLATION)
          .set(POLICY_VIOLATION.POLICY_VIOLATION_ID, "1a2b754bd39345c0a2a3af85f04d68da")
          .where(POLICY_VIOLATION.POLICY_VIOLATION_ID.eq(policyViolation.getId()))
          .execute();
      tx.commit();
    }

    // If we don't include db data the times should not be included
    HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, SCAN_PATH, ApiReportDataResourceV2.POLICY_DATA_PATH)
        .parameter(application.getPublicId(), scanId)
        .get();

    assertResponseStatus(200, response);
    ApiReportPolicyDataDTOV2 dto = response.getBody(ApiReportPolicyDataDTOV2.class);
    assertThat(dto.components).hasSize(2);
    assertThat(dto.components.get(0).violations).hasSize(2);

    assertThat(dto.components.get(0).violations.get(0).openTime).isNull();
    assertThat(dto.components.get(0).violations.get(0).waiveTime).isNull();
    assertThat(dto.components.get(0).violations.get(0).fixTime).isNull();
    assertThat(dto.components.get(0).violations.get(0).legacyViolationTime).isNull();

    assertThat(dto.components.get(0).violations.get(1).openTime).isNull();
    assertThat(dto.components.get(0).violations.get(1).waiveTime).isNull();
    assertThat(dto.components.get(0).violations.get(1).fixTime).isNull();
    assertThat(dto.components.get(0).violations.get(1).legacyViolationTime).isNull();

    assertThat(dto.components.get(1).violations).isEmpty();

    // If we do include db data the times should be included
    response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, SCAN_PATH, ApiReportDataResourceV2.POLICY_DATA_PATH)
        .parameter(application.getPublicId(), scanId)
        .query("includeViolationTimes", true)
        .get();
    assertResponseStatus(200, response);
    dto = response.getBody(ApiReportPolicyDataDTOV2.class);
    assertThat(dto.components.get(0).violations).hasSize(2);

    assertThat(dto.components.get(0).violations.get(0).openTime).isNotNull().isEqualTo(policyViolation.getOpenTime());
    assertThat(dto.components.get(0).violations.get(0).waiveTime).isNotNull().isEqualTo(policyViolation.getWaiveTime());
    assertThat(dto.components.get(0).violations.get(0).fixTime).isNotNull().isEqualTo(policyViolation.getFixTime());
    assertThat(dto.components.get(0).violations.get(0).legacyViolationTime).isNotNull()
        .isEqualTo(
            policyViolation.getLegacyViolationTime());

    // We only inserted a record for the first policy violation
    assertThat(dto.components.get(0).violations.get(1).openTime).isNull();
    assertThat(dto.components.get(0).violations.get(1).waiveTime).isNull();
    assertThat(dto.components.get(0).violations.get(1).fixTime).isNull();
    assertThat(dto.components.get(0).violations.get(1).legacyViolationTime).isNull();

    assertThat(dto.components.get(1).violations).isEmpty();
  }

  @Test
  public void testGetPolicyViolationDiff() throws Exception {
    setupValidReportsAndEvaluations();

    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();

    assertResponseStatus(200, response);

    assertValidDiffResults(response);
  }

  @Test
  public void testGetPolicyViolationDiff_AbbreviatedCommits() throws Exception {
    setupValidReportsAndEvaluations();

    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH.substring(0, 7),
            TO_COMMIT_HASH.substring(0, 7)))
        .get();

    assertResponseStatus(200, response);

    assertValidDiffResults(response);
  }

  @Test
  public void testGetPolicyViolationDiff_NoFromCommitSpecified() throws Exception {
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("toCommit=%s", TO_COMMIT_HASH))
        .get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The commit identifier or policy evaluation id for the `from` evaluation needs to be specified");
  }

  @Test
  public void testGetPolicyViolationDiff_NoToCommitSpecified() throws Exception {
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s", FROM_COMMIT_HASH))
        .get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The commit identifier or policy evaluation id for the `to` evaluation needs to be specified");
  }

  @Test
  public void testGetPolicyViolationDiff_SameCommitSpecified() throws Exception {
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, FROM_COMMIT_HASH))
        .get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The specified commits or evaluation ids cannot be identical.");
  }

  @Test
  public void testGetPolicyViolationDiff_InvalidFromCommitSpecified() throws Exception {
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", "aaa", TO_COMMIT_HASH))
        .get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The commit identifier `aaa` supplied is not a valid commit hash");
  }

  @Test
  public void testGetPolicyViolationDiff_InvalidToCommitSpecified() throws Exception {
    final Date date = new Date();
    tempEntity
        .newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            date, FROM_COMMIT_HASH);
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, "aaa"))
        .get();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The commit identifier `aaa` supplied is not a valid commit hash");
  }

  @Test
  public void testGetPolicyViolationDiff_FromCommitNotFound() throws Exception {
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo(CANT_CALCULATE_DIFF_MESSAGE);
  }

  @Test
  public void testGetPolicyViolationDiff_ToCommitNotFound() throws Exception {
    final Date date = new Date();
    tempEntity
        .newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            date, FROM_COMMIT_HASH);
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo(CANT_CALCULATE_DIFF_MESSAGE);
  }

  @Test
  public void testGetPolicyViolationDiff_AppNotFound() throws Exception {
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter("INVALID APP")
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo("Could not find an application with public ID INVALID APP.");
  }

  @Test
  public void testGetPolicyViolationDiff_FromCommitNoReport() throws Exception {
    // setup evaluations
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID, false, false, false,
        date, FROM_COMMIT_HASH);
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID, false, false, false,
        date, TO_COMMIT_HASH);

    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo(CANT_CALCULATE_DIFF_MESSAGE);
  }

  @Test
  public void testGetPolicyViolationDiff_FromCommitMissingAlerts() throws Exception {
    // setup reports
    ReportTestUtils.createReportFile(app.getId(), FROM_SCAN_ID,
        zipReportDir("/" + getClass().getSimpleName() + "/report-missing-policyalerts", tempDir), insightWork);
    ReportTestUtils
        .createReportFile(app.getId(), TO_SCAN_ID,
            zipReportDir("/" + getClass().getSimpleName() + "/to-report", tempDir),
            insightWork);

    // setup evaluations
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID, false, false, false,
        date, FROM_COMMIT_HASH);
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID, false, false, false,
        date, TO_COMMIT_HASH);

    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo(CANT_CALCULATE_DIFF_MESSAGE);
  }

  private void createReport(String appPublicId, String scanId, String resource) throws IOException {
    Application app = tempEntity.newApplicationWithParent(appPublicId);
    createReportFile(app.getId(), scanId, "/" + getClass().getSimpleName() + "/" + resource);
    tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, scanId);
  }

  private void setupValidReportsAndEvaluations() throws IOException, URISyntaxException {
    // setup reports
    ReportTestUtils.createReportFile(app.getId(), FROM_SCAN_ID,
        zipReportDir("/" + getClass().getSimpleName() + "/from-report", tempDir), insightWork);
    ReportTestUtils
        .createReportFile(app.getId(), TO_SCAN_ID,
            zipReportDir("/" + getClass().getSimpleName() + "/to-report", tempDir),
            insightWork);

    // setup evaluations
    fromEvalId = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, FROM_SCAN_ID, false, false, false,
        date, FROM_COMMIT_HASH).getId();
    toEvalId = tempEntity.newPolicyEvaluation(app.getId(), ReleaseStageType.ID, TO_SCAN_ID, false, false, false,
        date, TO_COMMIT_HASH).getId();
  }

  @Test
  public void testGetPolicyViolationDiff_FromEvaluations() throws Exception {
    // setup
    setupValidReportsAndEvaluations();

    // when fetching diff with evaluation ids
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromPolicyEvaluationId=%s&toPolicyEvaluationId=%s", fromEvalId, toEvalId))
        .get();

    // then assert success response status and verify correct results
    assertResponseStatus(200, response);
    assertValidDiffResults(response);
  }

  @Test
  public void testGetPolicyViolationDiff_NoFromEvaluationSpecified() throws Exception {
    // when fetching diff without from eval specified
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("toPolicyEvaluationId=%s", toEvalId))
        .get();

    // then verify bad request response
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The commit identifier or policy evaluation id for the `from` evaluation needs to be specified");
  }

  @Test
  public void testGetPolicyViolationDiff_NoToEvaluationSpecified() throws Exception {
    // when fetching diff without to eval specified
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromPolicyEvaluationId=%s", fromEvalId))
        .get();

    // then verify bad request response
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("The commit identifier or policy evaluation id for the `to` evaluation needs to be specified");
  }

  @Test
  public void testGetPolicyViolationDiff_SameEvaluationSpecified() throws Exception {
    // when fetching diff with same from and to evals
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromPolicyEvaluationId=%s&toPolicyEvaluationId=%s", fromEvalId, fromEvalId))
        .get();

    // then verify bad request response
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("The specified commits or evaluation ids cannot be identical.");
  }

  @Test
  public void testGetPolicyViolationDiff_FromEvaluationNotFound() throws Exception {
    // when fetching diff with non existing from eval id
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromPolicyEvaluationId=%s&toPolicyEvaluationId=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();

    // then verify not found response
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo(CANT_CALCULATE_DIFF_MESSAGE);
  }

  @Test
  public void testGetPolicyViolationDiff_ToEvaluationNotFound() throws Exception {
    // setup
    final Date date = new Date();
    tempEntity
        .newPolicyEvaluation(app.getId(), Stage.ID_RELEASE, "scan1", false, false, false,
            date, FROM_COMMIT_HASH);

    // when fetching diff with non existing to eval id
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromPolicyEvaluationId=%s&toPolicyEvaluationId=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();

    // then verify not found response
    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .isEqualTo(CANT_CALCULATE_DIFF_MESSAGE);
  }

  @Test
  public void testGetPolicyViolationDiff_FromEvaluationAndCommitSpecified() throws Exception {
    // when fetching diff with from commit and eval id specified
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String
            .format("fromPolicyEvaluationId=%s&toPolicyEvaluationId=%s&fromCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH,
                FROM_COMMIT_HASH))
        .get();

    // then verify bad request response
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot specify both commit identifier and evaluation id for `from` evaluation.");
  }

  @Test
  public void testGetPolicyViolationDiff_ToEvaluationAndCommitSpecified() throws Exception {
    // when fetching diff with to commit and eval id specified
    final HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String
            .format("fromPolicyEvaluationId=%s&toPolicyEvaluationId=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH,
                TO_COMMIT_HASH))
        .get();

    // then verify bad request response
    assertResponseStatus(400, response);
    assertThat(response.getBodyText())
        .isEqualTo("Cannot specify both commit identifier and evaluation id for `to` evaluation.");
  }

  @Test
  public void testGetPolicyViolationDiff_includeViolationTimes() throws Exception {
    setupValidReportsAndEvaluations();
    // Use an existing report and insert a corresponding policy violation in the db but update its times
    Policy policy = tempEntity.newPolicy(app);
    PolicyEvaluation policyEvaluation = getCLMServer().getInstance(PolicyEvaluationDAO.class).getById(fromEvalId);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setLegacyViolationTime(DateUtils.addDays(policyEvaluation.getTime(), 1));
    policyViolation.setWaiveTime(DateUtils.addDays(policyEvaluation.getTime(), 2));
    policyViolation.setFixTime(DateUtils.addDays(policyEvaluation.getTime(), 3));
    PolicyViolationDAO policyViolationDAO = getCLMServer().getInstance(PolicyViolationDAO.class);
    policyViolationDAO.update(policyViolation);
    try (com.sonatype.insight.dataaccess.TransactionContext tx = policyViolationDAO.createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(POLICY_VIOLATION)
          .set(POLICY_VIOLATION.POLICY_VIOLATION_ID, "appeared_1")
          .where(POLICY_VIOLATION.POLICY_VIOLATION_ID.eq(policyViolation.getId()))
          .execute();
      tx.commit();
    }

    // If we don't include db data the times should not be included
    HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .get();

    assertResponseStatus(200, response);
    ApiPolicyViolationDiffDTO dto = response.getBody(ApiPolicyViolationDiffDTO.class);
    ApiPolicyViolationForDiffDTO appeared1 = dto.addedViolations.stream()
        .filter(v -> v.policyViolationId.equals("appeared_1"))
        .findFirst()
        .orElse(null);
    assertThat(appeared1).isNotNull();
    assertThat(appeared1.openTime).isNull();
    assertThat(appeared1.waiveTime).isNull();
    assertThat(appeared1.fixTime).isNull();
    assertThat(appeared1.legacyViolationTime).isNull();

    // If we do include db data the times should be included
    response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2, ApiReportDataResourceV2.VIOLATION_DIFF_PATH)
        .parameter(app.getPublicId())
        .query(String.format("fromCommit=%s&toCommit=%s", FROM_COMMIT_HASH, TO_COMMIT_HASH))
        .query("includeViolationTimes", true)
        .get();

    assertResponseStatus(200, response);
    dto = response.getBody(ApiPolicyViolationDiffDTO.class);
    appeared1 = dto.addedViolations.stream()
        .filter(v -> v.policyViolationId.equals("appeared_1"))
        .findFirst()
        .orElse(null);
    assertThat(appeared1).isNotNull();
    assertThat(appeared1.openTime).isNotNull().isEqualTo(policyViolation.getOpenTime());
    assertThat(appeared1.waiveTime).isNotNull().isEqualTo(policyViolation.getWaiveTime());
    assertThat(appeared1.fixTime).isNotNull().isEqualTo(policyViolation.getFixTime());
    assertThat(appeared1.legacyViolationTime).isNotNull().isEqualTo(policyViolation.getLegacyViolationTime());
  }

  @Test
  public void testGetDependencyTree_noDependencyTree() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.DEPENDENCY_TREE_PATH)
        .parameter(appPublicId, scanId)
        .get();

    assertResponseStatus(200, response);
    ApiDependencyTreeResponseDTO dto = response.getBody(ApiDependencyTreeResponseDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.getDependencyTree()).isNotNull();
    assertThat(dto.getDependencyTree().getChildren()).isNull();
    assertThat(dto.getDependencyTree().getComponentIdentifier()).isNull();
  }

  @Test
  public void testGetDependencyTree() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report-dependencyTree");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.DEPENDENCY_TREE_PATH)
        .parameter(appPublicId, scanId)
        .get();

    assertResponseStatus(200, response);
    ApiDependencyTreeResponseDTO dto = response.getBody(ApiDependencyTreeResponseDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.getDependencyTree()).isNotNull();
    assertThat(dto.getDependencyTree().getComponentIdentifier()).isNull();

    List<ApiDependencyTreeNodeDTO> children = dto.getDependencyTree().getChildren();
    assertThat(children).isNotEmpty();
    assertThat(children.size()).isEqualTo(1);
    ApiDependencyTreeNodeDTO node = children.get(0);
    assertThat(node.getPackageUrl()).isNotNull();

  }

  @Test
  public void testGetDependencyTree_innerSource() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report-innersource");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.DEPENDENCY_TREE_PATH)
        .parameter(appPublicId, scanId)
        .get();

    assertResponseStatus(200, response);
    ApiDependencyTreeResponseDTO dto = response.getBody(ApiDependencyTreeResponseDTO.class);
    assertThat(dto).isNotNull();
    assertThat(dto.getDependencyTree()).isNotNull();
    assertThat(dto.getDependencyTree().getComponentIdentifier()).isNull();

    List<ApiDependencyTreeNodeDTO> children = dto.getDependencyTree().getChildren();
    assertThat(children).isNotEmpty();
    assertThat(children.size()).isEqualTo(2);

    children.forEach(node -> {
      assertThat(node.getPackageUrl()).isNotNull();
      assertThat(node.getComponentIdentifier()).isNotNull();
    });

  }

  @Test
  public void testGetDependencyTree_notFound() throws Exception {
    final String appPublicId = "ApiReportDataResourceV2Test_AppId";
    final String scanId = "ScanId";
    createReport(appPublicId, scanId, "report");

    HttpResponse response = restRequest()
        .path(PublicApiPaths.REPORT_DATA_RESOURCE_PATH_V2)
        .path(SCAN_PATH)
        .path(ApiReportDataResourceV2.DEPENDENCY_TREE_PATH)
        .parameter(appPublicId, "FalseScanId")
        .get();

    assertResponseStatus(404, response);
  }

  private void assertValidDiffResults(
      final HttpResponse response) throws URISyntaxException, JSONException, IOException
  {
    final String result = response.getBodyText();

    final String expectedResult = new String(Files.readAllBytes(Paths
        .get(ApiReportDataResourceV2Test.class
            .getResource("/" + getClass().getSimpleName() + "/diff-result/diffResult.json")
            .toURI())),
        StandardCharsets.UTF_8);
    JSONAssert.assertEquals(expectedResult, result, new CustomComparator(JSONCompareMode.NON_EXTENSIBLE,
        new Customization("diffTime", (o1, o2) -> true),
        new Customization("*Commit.scanTime", (o1, o2) -> true)));
  }
}
