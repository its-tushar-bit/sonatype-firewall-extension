/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiEvaluationResourceV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPromoteScanRequestDTOV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code /api/v2/evaluation/applications/...}
 * ({@link ApiEvaluationResourceV2}). Covers ad-hoc component evaluation (POST + poll),
 * validation error contract, promote-scan, and the auth contract on all mutating verbs.
 *
 * <p>
 * <b>Intentionally excluded:</b> {@code POST {applicationId}/sourceControlEvaluation}
 * — gated behind {@code LicensedFeature.SOURCE_CONTROL} and requires SCM configuration
 * plumbing that is out of scope for the CI-plugin write-path tranche. Auth coverage is
 * deferred to the source-control regression class (P5 §6 in the coverage plan).
 */
@Category(ApiRegressionTest.class)
public class EvaluationApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String EVALUATION_BASE = PublicApiPaths.APPLICATION_EVALUATION_PATH_V2;

  private static String applicationEvalPath(final String applicationId) {
    return EVALUATION_BASE + "/" + applicationId;
  }

  private static String applicationEvalResultPath(final String applicationId, final String resultId) {
    return EVALUATION_BASE + "/" + applicationId + "/results/" + resultId;
  }

  private static String promoteScanPath(final String applicationId) {
    return EVALUATION_BASE + "/" + applicationId + "/promoteScan";
  }

  private static String evaluationStatusPath(final String applicationId, final String statusId) {
    return EVALUATION_BASE + "/" + applicationId + "/status/" + statusId;
  }

  private static ApiComponentEvaluationRequestDTOV2 requestWithPackageUrl(final String packageUrl) {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = packageUrl;
    request.components.add(component);
    return request;
  }

  @Test
  public void testEvaluateComponents_validPackageUrl_returnsTicket() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-eval"), Organization.ROOT_ORGANIZATION_ID);
    ApiComponentEvaluationRequestDTOV2 request = requestWithPackageUrl("pkg:maven/com.api.eval/artifact@1.0?type=jar");

    HttpResponse response = apiPostJson(applicationEvalPath(app.getId()), request);
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText()).node("resultId").isString().isNotEmpty();
    assertThatJson(response.getBodyText()).node("applicationId").isEqualTo(app.getId());
  }

  /**
   * Regression guard: an invalid package URL surfaces as 400 with a specific message.
   * Silent 200 or NPE-based 500 would let bad SBOM ingest payloads propagate downstream.
   * The message assertion is intentional — downstream CI plugins parse this string.
   */
  @Test
  public void testEvaluateComponents_invalidPackageUrl_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-eval-badpurl"), Organization.ROOT_ORGANIZATION_ID);
    ApiComponentEvaluationRequestDTOV2 request = requestWithPackageUrl("pkg/invalid_package_url");

    HttpResponse response = apiPostJson(applicationEvalPath(app.getId()), request);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("invalid package url");
  }

  /**
   * Regression guard: missing extension coordinate for a Maven component returns 400 with the
   * fixed "The following coordinates are missing" message. Downstream CI plugins parse this
   * message to surface actionable errors to developers.
   */
  @Test
  public void testEvaluateComponents_missingCoordinates_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-eval-missing"), Organization.ROOT_ORGANIZATION_ID);
    // raw JSON: the DTO can't express a coordinate-less componentIdentifier, which is
    // exactly the 400 branch under test (missing extension coordinates for Maven format)
    String jsonRequest =
        "{\"components\":[{\"hash\":\"h1\",\"componentIdentifier\":{\"format\":\"maven\"},\"proprietary\":false}]}";

    HttpResponse response = apiRequest().path(applicationEvalPath(app.getId())).body(jsonRequest).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("at least one coordinate");
  }

  @Test
  public void testGetEvaluationResult_unknownResultId_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-eval-poll"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = apiGet(applicationEvalResultPath(app.getId(), uniqueId("nonexistent-result")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("not found");
  }

  /**
   * PromoteScan with an unknown scanId returns 400 (bad request), not 500. Guards against
   * NPE on missing scan reference in the promotion flow. The message assertion is
   * intentional — CI plugins surface this text to developers.
   */
  @Test
  public void testPromoteScan_unknownScanId_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-eval-promote"), Organization.ROOT_ORGANIZATION_ID);
    ApiPromoteScanRequestDTOV2 request =
        ApiPromoteScanRequestDTOV2.fromScan(uniqueId("nonexistent-scan"), BuildStageType.ID);

    HttpResponse response = apiPostJson(promoteScanPath(app.getId()), request);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetApplicationEvaluationStatus_unknownStatusId_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-eval-status"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = apiGet(evaluationStatusPath(app.getId(), uniqueId("nonexistent-status")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("not found");
  }

  /**
   * Per-JAX-RS-method auth coverage. {@link ApiEvaluationResourceV2} exposes distinct
   * mutating (POST evaluate, POST promoteScan) and read (GET result, GET status) methods,
   * each with its own Shiro annotation — a future per-method {@code @PermitAll} on any one
   * of them would silently bypass auth without a per-method test catching it.
   */
  @Test
  public void testEvaluateComponents_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(
        applicationEvalPath(uniqueId("any-app")),
        requestWithPackageUrl("pkg:maven/com.api/any@1.0?type=jar"));
    assertResponseStatus(401, response);
  }

  @Test
  public void testPromoteScan_unauthenticated_returns401() throws Exception {
    ApiPromoteScanRequestDTOV2 request = ApiPromoteScanRequestDTOV2.fromScan(uniqueId("any-scan"), BuildStageType.ID);
    HttpResponse response = anonApiPostJson(promoteScanPath(uniqueId("any-app")), request);
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetEvaluationResult_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(applicationEvalResultPath(uniqueId("any-app"), uniqueId("any-result")));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetApplicationEvaluationStatus_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(evaluationStatusPath(uniqueId("any-app"), uniqueId("any-status")));
    assertResponseStatus(401, response);
  }
}
