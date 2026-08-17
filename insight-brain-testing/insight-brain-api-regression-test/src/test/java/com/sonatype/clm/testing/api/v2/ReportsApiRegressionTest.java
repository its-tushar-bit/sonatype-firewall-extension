/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiReportResourceV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code /api/v2/reports/applications} ({@link ApiReportResourceV2}).
 * Covers three endpoints: single-app latest, server-wide all, and per-app history.
 *
 * <p>
 * These endpoints back {@code Applications &gt; Reports} in the IQ UI and every CI plugin's
 * post-scan "view report" link — silent regressions here surface as broken user-facing links.
 *
 * <p>
 * Note on the {@code /history} endpoint: each evaluation surfaces in the {@code reports}
 * array only if the corresponding report file exists in the report data store (see
 * {@code ApiReportServiceV2#addPolicyEvaluationResult}). This regression suite seeds
 * PolicyEvaluation rows but not report files, so positive-path assertions on {@code /history}
 * check only response shape (200 + expected JSON structure). End-to-end history behavior
 * with real report content lives in the reports Playwright suite.
 */
public class ReportsApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String REPORTS_BASE =
      PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiReportResourceV2.PATH;

  /**
   * The single-app "latest report per stage" endpoint uses {@code getLastByApplicationIds},
   * which does not need a report file on disk — so we can assert real content here.
   */
  @Test
  public void testGetReportsByApplicationId_returnsSeededEvaluation() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Reports By App"));
    Application app = tempEntity.newApplication(uniqueId("api-reports-by-app"), org.getId());
    String scanId = uniqueId("api-reports-scan");
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    HttpResponse response = apiGet(REPORTS_BASE + "/" + app.getId());
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText())
        .isArray()
        .isNotEmpty();
    assertThatJson(response.getBodyText())
        .inPath("$[*].stage")
        .isArray()
        .contains(BuildStageType.ID);
  }

  /**
   * Regression guard: unknown application returns 404, not a silent 200 with an empty array.
   * Silent 200 would let CI plugins think a report exists.
   */
  @Test
  public void testGetReportsByApplicationId_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(REPORTS_BASE + "/" + uniqueId("nonexistent-app"));
    assertResponseStatus(404, response);
    // Pin the 404 reason so an unrelated 404 (e.g. path routing change) does not silently
    // pass this test.
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetAllReports_returnsSeededEvaluation() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-reports-all"), Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-reports-all-scan"));

    HttpResponse response = apiGet(REPORTS_BASE);
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText())
        .isArray()
        .isNotEmpty();
    assertThatJson(response.getBodyText())
        .inPath("$[*].applicationId")
        .isArray()
        .contains(app.getId());
  }

  /**
   * Shape assertion for {@code /history}: valid app returns 200 with {@code applicationId} and
   * a {@code reports} array (may be empty absent report files — see class Javadoc). Positive-
   * path content coverage lives in the Playwright reports suite; the regression guard here is
   * three-fold: (1) valid app is not 404'd, (2) DTO fields stay named {@code applicationId} /
   * {@code reports} (the historical shape that all downstream tooling parses), (3) the
   * {@code stage} and {@code limit} query params, when valid, still route to the same 200
   * shape (complements the {@code invalidStage_returns400} / {@code zeroLimit_returns400}
   * tests below — together they prove the {@code @QueryParam} bindings and their validators
   * are wired).
   */
  @Test
  public void testGetReportHistory_validApp_returnsShape() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-reports-history"), Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("api-reports-history-scan"));
    String historyPath = REPORTS_BASE + "/" + app.getId() + "/history";

    assertReportHistoryShape(apiGet(historyPath), app);
    assertReportHistoryShape(apiGet(historyPath, "stage", BuildStageType.ID), app);
    assertReportHistoryShape(apiGet(historyPath, "limit", "5"), app);
  }

  /**
   * Regression guard: {@code /history} on an unknown app returns 404, not 200 with an empty
   * shell. CI plugins reading history use the 404 as the "app not found" signal.
   */
  @Test
  public void testGetReportHistory_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(REPORTS_BASE + "/" + uniqueId("nonexistent-app") + "/history");
    assertResponseStatus(404, response);
    // Fragment is stable — comes from AbstractSqlDAO#getByIdNotNull's NotFoundException
    // ("<Entity> with ID <id> does not exist.").
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  /**
   * Regression guard: an unknown stage id returns 400 with a specific validation message
   * (from {@code ApiReportServiceV2#getReportHistoryForApplication}). Silent 200 would let
   * clients think the invalid stage matched something.
   */
  @Test
  public void testGetReportHistory_invalidStage_returns400() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("api-reports-badstage"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response =
        apiGet(REPORTS_BASE + "/" + app.getId() + "/history", "stage", "not-a-real-stage");
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Invalid stage");
  }

  /**
   * Regression guard: {@code limit=0} returns 400 with the "Limit must be positive integer"
   * message. Off-by-one regressions here have historically slipped past unit tests.
   */
  @Test
  public void testGetReportHistory_zeroLimit_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-reports-badlimit"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = apiGet(REPORTS_BASE + "/" + app.getId() + "/history", "limit", "0");
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("Limit must be positive integer");
  }

  /**
   * Per-JAX-RS-method auth coverage. {@link ApiReportResourceV2} exposes three distinct
   * {@code @GET} methods ({@code getAll}, {@code getByApplicationId},
   * {@code getReportHistoryForApplication}), each with its own Shiro annotation — a future
   * per-method {@code @PermitAll} on any one of them would silently bypass auth without a
   * per-endpoint test catching it. Same rationale as the per-verb auth tests on
   * {@code ApiLegalAttributionReportTemplateResourceV2} and the JSON/CSV auth split in
   * {@code MetricsReportApiRegressionTest}.
   */
  @Test
  public void testGetReports_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(REPORTS_BASE);
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetReportsByApplicationId_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(REPORTS_BASE + "/" + uniqueId("any-app"));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetReportHistory_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(REPORTS_BASE + "/" + uniqueId("any-app") + "/history");
    assertResponseStatus(401, response);
  }

  private static void assertReportHistoryShape(final HttpResponse response, final Application app) {
    // Include the status assertion here — the /history refactor extracted this helper from
    // three call sites that each previously called assertResponseStatus(200, response). A
    // 4xx / 5xx regression that returned a body containing the expected keys would slip
    // through a body-only assertion.
    assertResponseStatus(200, response);
    String body = response.getBodyText();
    assertThatJson(body).node("applicationId").isEqualTo(app.getId());
    assertThatJson(body).node("reports").isArray();
  }
}
