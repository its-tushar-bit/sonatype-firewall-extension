/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import java.util.List;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiReportDataResourceV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code /api/v2/applications/{applicationPublicId}/reports/...}
 * ({@link ApiReportDataResourceV2}).
 *
 * <p>
 * Positive-path body-shape tests are intentionally omitted for the {@code /raw},
 * {@code /policy}, and {@code /dependencyTree} sub-endpoints — those read a scan report
 * bundle from disk, and reproducing that fixture in the regression suite would duplicate
 * (with a large maintenance surface) the coverage already provided by
 * {@code ApiReportDataResourceV2Test}. The {@code /metadata} endpoint, by contrast, is
 * DB-only and IS positive-tested here for body shape. The {@code /policyViolations/diff}
 * endpoint's {@code validateInputs()} guards fire before file access, so its 400 contracts
 * are covered here too.
 */
public class ReportDataApiRegressionTest
    extends AbstractIqApiTest
{
  private static String reportPath(final String applicationPublicId, final String scanId, final String suffix) {
    return PublicApiPaths.APP_RESOURCE_PATH + "/" + applicationPublicId + "/reports/" + scanId
        + (suffix == null ? "" : "/" + suffix);
  }

  private static String diffPath(final String applicationPublicId) {
    return PublicApiPaths.APP_RESOURCE_PATH + "/" + applicationPublicId + "/reports/"
        + ApiReportDataResourceV2.VIOLATION_DIFF_PATH;
  }

  /**
   * Every report-data sub-endpoint ({@code /raw}, {@code /policy}, {@code /dependencyTree},
   * {@code /metadata}) must return 404 when the scan ID does not exist for a real application.
   * Seeding a real application here (instead of using a nonexistent {@code applicationPublicId})
   * is deliberate: it bypasses the shared {@code applicationDAO.getByPublicIdNotNull} lookup
   * and actually exercises each endpoint's own scan-lookup path
   * ({@code reportDataStore.getLifecycleReport(...)}), which is the real regression surface.
   */
  @Test
  public void testAllReportDataEndpoints_unknownScan_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-reportdata-unknown-scan"),
        Organization.ROOT_ORGANIZATION_ID);
    String unknownScan = uniqueId("nonexistent-scan");

    for (String suffix : List.of(
        ApiReportDataResourceV2.RAW_DATA_PATH,
        ApiReportDataResourceV2.POLICY_DATA_PATH,
        ApiReportDataResourceV2.DEPENDENCY_TREE_PATH,
        ApiReportDataResourceV2.METADATA_PATH))
    {
      HttpResponse response = apiGet(reportPath(app.getPublicId(), unknownScan, suffix));
      assertThat(response.getStatusCode())
          .as("report-data suffix '%s' body='%s'", suffix, response.getBodyText())
          .isEqualTo(404);
    }
  }

  /**
   * Regression guard for the legacy pre-v63 endpoint: {@code GET /reports/{scanId}} (no
   * suffix) must return 307 redirecting to {@code /reports/{scanId}/raw}. Downstream clients
   * pinned to the legacy URL still rely on this redirect. {@link AbstractIqApiTest}'s
   * {@code apiRequest()} helper defaults to no redirect-following (the {@code redirects} flag
   * is initialized to {@code false} in {@code AbstractHttpRequest}), so the raw 307 status is
   * observed here.
   */
  @Test
  public void testGetData_legacyEndpoint_returnsRedirect() throws Exception {
    HttpResponse response = apiGet(reportPath(uniqueId("any-app"), uniqueId("any-scan"), null));
    assertResponseStatus(307, response);
  }

  /**
   * Pagination validation: {@code pageSize} above the {@code @Max(500)} cap must be
   * rejected by JAX-RS bean validation with 400. Prevents accidental OOM-inducing queries
   * from clients requesting arbitrarily large pages. This is the resource-safety guard that
   * matters here; the analogous {@code @Min(1)} check on {@code page} is not covered because
   * a below-minimum page value produces only benign pagination noise (offset math resolves
   * to page 1), not a real regression scenario worth a per-request test.
   */
  @Test
  public void testGetPolicyViolations_pageSizeTooLarge_returns400() throws Exception {
    HttpResponse response = apiGet(
        reportPath(uniqueId("any-app"), uniqueId("any-scan"), ApiReportDataResourceV2.POLICY_DATA_PATH),
        "pageSize", "1000");
    assertResponseStatus(400, response);
  }

  /**
   * The {@code /metadata} endpoint is DB-only (no report file needed) — it echoes the
   * seeded {@code PolicyEvaluation} into {@code ApiReportMetadataResponseDto}. Regression
   * guard for the {@code data} + {@code source} DTO shape that CI plugins depend on.
   * {@code source} is asserted only as an object (not its contents) because it is populated
   * only when SCM metadata is present; asserting its keys would couple this test to
   * unrelated SCM-source enum ordering.
   */
  @Test
  public void testGetMetadata_seededEvaluation_returns200_withExpectedShape() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-reportdata-metadata"),
        Organization.ROOT_ORGANIZATION_ID);
    String scanId = uniqueId("scan");
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);

    HttpResponse response = apiGet(reportPath(app.getPublicId(), scanId, ApiReportDataResourceV2.METADATA_PATH));

    assertResponseStatus(200, response);
    String body = response.getBodyText();
    assertThatJson(body).node("data.scanId").isEqualTo(scanId);
    assertThatJson(body).node("data.applicationPublicId").isEqualTo(app.getPublicId());
    assertThatJson(body).node("data.stage").isEqualTo(BuildStageType.ID);
    assertThatJson(body).node("data.scanDate").isPresent();
    assertThatJson(body).node("data.ownerType").isEqualTo("application");
    assertThatJson(body).node("data.hrcId").isAbsent();
    assertThatJson(body).node("source").isObject();
  }

  /**
   * Regression guard for {@code validateInputs()} rule 1 in {@code ApiReportViolationsDiffService}:
   * callers must provide either a commit hash or an evaluation id for the {@code from} side.
   * Neither present → 400.
   */
  @Test
  public void testGetPolicyViolationDiff_missingBothIdentifiers_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-reportdata-diff-missing"),
        Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiGet(diffPath(app.getPublicId()));

    assertResponseStatus(400, response);
    // Pin the specific validateInputs() branch.
    assertThat(response.getBodyText()).containsIgnoringCase("needs to be specified");
  }

  /**
   * Regression guard: SCM plugins that pass the same commit hash for both sides (a silent
   * misconfiguration observed in the wild) must be rejected up-front with 400.
   */
  @Test
  public void testGetPolicyViolationDiff_identicalCommits_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-reportdata-diff-same"),
        Organization.ROOT_ORGANIZATION_ID);
    String sameHash = "abcdef1234abcdef1234abcdef1234abcdef1234";

    HttpResponse response = apiRequest()
        .path(diffPath(app.getPublicId()))
        .query("fromCommit", sameHash)
        .query("toCommit", sameHash)
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("cannot be identical");
  }

  /**
   * Regression guard: mixing {@code fromCommit} + {@code fromPolicyEvaluationId} in the
   * same request is contractually ambiguous and must be rejected with 400.
   */
  @Test
  public void testGetPolicyViolationDiff_bothCommitAndEvalId_returns400() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("api-reportdata-diff-both"),
        Organization.ROOT_ORGANIZATION_ID);

    HttpResponse response = apiRequest()
        .path(diffPath(app.getPublicId()))
        .query("fromCommit", "abcdef1234abcdef1234abcdef1234abcdef1234")
        .query("fromPolicyEvaluationId", "some-eval-id")
        .query("toCommit", "1234567890123456789012345678901234567890")
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("Cannot specify both");
  }

  @Test
  public void testGetReportData_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(
        reportPath(uniqueId("any-app"), uniqueId("any-scan"), ApiReportDataResourceV2.RAW_DATA_PATH));
    assertResponseStatus(401, response);
  }
}
