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
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code /api/v2/cycloneDx/...} — {@code ApiCycloneDxResourceV2}.
 * The resource is GET-only: it exports (never imports) CycloneDX SBOMs. SBOM ingest is
 * covered by {@link SbomApiRegressionTest} against {@code POST /api/v2/sbom/import}.
 *
 * <p>
 * The 200-body shape of a valid export requires a real scan-report file on disk, which the
 * DB-only fixtures used here do not produce. The tests below pin the contract at the
 * boundaries where DB-only seeds are sufficient: unknown-app / unknown-scan return 404,
 * invalid CycloneDX versions return 4xx on both {@code byStage} and {@code byReport}
 * path variants, and the auth contract holds on every path variant. Positive-path
 * acceptance of valid versions (1.1–1.6) is not tested here because it requires a real
 * scan-report file; those are covered in the SBOM manager integration suite.
 *
 * <p>
 * No {@code @Before setFeatures(LicensedFeature.SBOM_REPORTS)} is needed here:
 * {@code TestProductLicenseManager.verifyLicenseAndFeature()} is a no-op, so all
 * {@code @ProductLicenseEnforcementPoint} checks pass by default. This differs from
 * {@link SbomApiRegressionTest} which calls {@code setFeatures(SBOM_MANAGER)} for its
 * SBOM-stage side effects, not for license gating.
 */
@Category(ApiRegressionTest.class)
public class CycloneDxExportApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String CDX_BASE = PublicApiPaths.CYCLONE_DX_RESOURCE_PATH;

  private static String byStage(final String applicationId, final String stageId) {
    return CDX_BASE + "/" + applicationId + "/stages/" + stageId;
  }

  private static String byStageWithVersion(final String cdxVersion, final String applicationId, final String stageId) {
    return CDX_BASE + "/" + cdxVersion + "/" + applicationId + "/stages/" + stageId;
  }

  private static String byReport(final String applicationId, final String reportId) {
    return CDX_BASE + "/" + applicationId + "/reports/" + reportId;
  }

  private static String byReportWithVersion(
      final String cdxVersion,
      final String applicationId,
      final String reportId)
  {
    return CDX_BASE + "/" + cdxVersion + "/" + applicationId + "/reports/" + reportId;
  }

  @Test
  public void testGetLatestByStage_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(byStage(uniqueId("nonexistent-app"), BuildStageType.ID));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetLatestByStageWithVersion_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(byStageWithVersion("1.5", uniqueId("nonexistent-app"), BuildStageType.ID));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetByReportId_unknownReport_returns404() throws Exception {
    HttpResponse response = apiGet(byReport(uniqueId("nonexistent-app"), uniqueId("nonexistent-report")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetByReportIdWithVersion_unknownReport_returns404() throws Exception {
    HttpResponse response = apiGet(byReportWithVersion("1.4", uniqueId("nonexistent-app"),
        uniqueId("nonexistent-report")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  /**
   * Regression guard: the versioned path variant restricts {@code cdxVersion} to
   * {@code 1.1|1.2|1.3|1.4|1.5|1.6}. An unsupported version must not silently succeed with
   * empty output — it must return a 4xx status so caller-supplied bad versions are surfaced.
   * Body content is not asserted here — JAX-RS regex validation may return an empty error
   * page depending on the container, so we pin the status range only.
   */
  @Test
  public void testGetLatestByStageWithVersion_invalidVersion_returns4xx() throws Exception {
    HttpResponse response = apiGet(byStageWithVersion("9.9", uniqueId("any-app"), BuildStageType.ID));
    assertThat(response.getStatusCode())
        .as("body='%s'", response.getBodyText())
        .isBetween(400, 499);
  }

  @Test
  public void testGetByReportIdWithVersion_invalidVersion_returns4xx() throws Exception {
    HttpResponse response = apiGet(byReportWithVersion("9.9", uniqueId("any-app"), uniqueId("any-report")));
    assertThat(response.getStatusCode())
        .as("body='%s'", response.getBodyText())
        .isBetween(400, 499);
  }

  /**
   * Per-JAX-RS-method auth coverage. {@link ApiCycloneDxResourceV2} exposes four distinct
   * {@code @GET} methods (byStage, byStageWithVersion, byReport, byReportWithVersion), each
   * with its own Shiro annotation — a future per-method {@code @PermitAll} on any one of
   * them would silently bypass auth without a per-method test catching it.
   */
  @Test
  public void testGetLatestByStage_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(byStage(uniqueId("any-app"), BuildStageType.ID));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetLatestByStageWithVersion_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(byStageWithVersion("1.5", uniqueId("any-app"), BuildStageType.ID));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetByReportId_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(byReport(uniqueId("any-app"), uniqueId("any-report")));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetByReportIdWithVersion_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(byReportWithVersion("1.4", uniqueId("any-app"), uniqueId("any-report")));
    assertResponseStatus(401, response);
  }
}
