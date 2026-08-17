/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiSbomResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code /api/v2/sbom/...} ({@link ApiSbomResource}).
 *
 * <p>
 * Regression guards:
 * <ul>
 * <li>CLM-37982: DELETE SBOM API broke in v195. The DELETE path is exercised against a real
 * application with a known-nonexistent version and must return 404 ({@code "Cannot find
 * version"}), not silently succeed with 204.
 * <li>CLM-37904: Delete SBOM binary only if file exists — verified at the 404 layer here;
 * end-to-end file-system semantics live in SBOM manager Playwright suite.
 * <li>Every JAX-RS method on {@link ApiSbomResource} — including the vulnerability sub-resource
 * paths ({@code getVulnerabilityDetails}, {@code saveVulnerabilityAnalysis},
 * {@code deleteVulnerabilityAnalysis}) and {@code POST import} — must return 401 for
 * anonymous callers, not silently allow anonymous access.
 * </ul>
 *
 * <p>
 * Positive-path SBOM import is not covered here — {@code POST /api/v2/sbom/import} is
 * multipart/form-data and requires a real CycloneDX/SPDX fixture file. The full ingest
 * pipeline and request-validation contract (e.g. missing {@code applicationId} → 400) are
 * exercised in the SBOM Manager integration suite.
 *
 * <p>
 * <b>Intentionally excluded:</b> positive-path {@code GET /api/v2/sbom/applications/{id}}
 * (metadata summary). The underlying aggregation query
 * ({@code ThirdPartySbomMetadataDAO.getSbomApplicationVulnerabilities}) uses
 * {@code round(numeric, scale)} over a {@code nullif(cast(...))} division that jOOQ renders
 * in a form H2 does not accept, so the endpoint 500s under this module's in-process H2
 * server. Existing DAO coverage
 * ({@link com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO})
 * is already Postgres-only ({@code @PostgresTest}); positive-path acceptance for this
 * endpoint lives in the SBOM Manager integration suite. The auth contract is still
 * guarded here via {@code testGetSbomMetadata_unauthenticated_returns401}.
 *
 * <p>
 * Negative-path tests seed a real application via {@code tempEntity} and pass an unknown
 * version/request ID so the resource method body executes (not just the
 * {@code @AuthzContext(APPLICATION_ID)} interceptor). A single dedicated unknown-app test
 * ({@code testGetSbomVersion_unknownApp_returns404}) pins the auth-interceptor 404 contract.
 */
public class SbomApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String SBOM_BASE = PublicApiPaths.SBOM_RESOURCE_PATH;

  /**
   * SBOM Manager endpoints are gated behind {@link LicensedFeature#SBOM_MANAGER} — without it,
   * every endpoint returns {@code 402 Payment Required} regardless of app/version state. That
   * license enforcement itself is exercised separately in {@link ApiSbomResource} authz tests;
   * this regression suite targets the resource contract once the feature is enabled.
   */
  @BeforeEach
  public void enableSbomManagerFeature() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  private static String appPath(final String applicationId) {
    return SBOM_BASE + "/applications/" + applicationId;
  }

  private static String versionPath(final String applicationId, final String version) {
    return SBOM_BASE + "/applications/" + applicationId + "/versions/" + version;
  }

  private static String versionsPath(final String applicationId) {
    return SBOM_BASE + "/applications/" + applicationId + "/versions";
  }

  private static String importStatusPath(final String applicationId, final String importRequestId) {
    return SBOM_BASE + "/applications/" + applicationId + "/status/" + importRequestId;
  }

  private static String componentsPath(final String applicationId, final String version) {
    return versionPath(applicationId, version) + "/components";
  }

  private static String exportOptionsPath(final String applicationId, final String version) {
    return versionPath(applicationId, version) + "/export-options";
  }

  private static String vulnerabilityPath(
      final String applicationId,
      final String version,
      final String refId)
  {
    return versionPath(applicationId, version) + "/vulnerability/" + refId;
  }

  private static String vulnerabilityAnalysisPath(
      final String applicationId,
      final String version,
      final String refId)
  {
    return vulnerabilityPath(applicationId, version, refId) + "/analysis";
  }

  private static String importPath() {
    return SBOM_BASE + "/import";
  }

  @Test
  public void testGetSbomVersion_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(versionPath(uniqueId("nonexistent-app"), "1.0"));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  /**
   * Regression guard for CLM-37982: DELETE on an unknown SBOM version must return 404, not
   * 204. Seeds a real application so the {@code @AuthzContext(APPLICATION_ID)} interceptor
   * passes and the version-lookup path ({@code getThirdPartySbomMetadataNotNull}) executes.
   */
  @Test
  public void testDeleteSbomVersion_unknownVersion_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("sbom-del"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = apiDelete(versionPath(app.getId(), uniqueId("no-version")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("cannot find version");
  }

  @Test
  public void testGetSbomComponents_unknownVersion_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("sbom-comp"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = apiGet(componentsPath(app.getId(), uniqueId("no-version")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("cannot find version");
  }

  @Test
  public void testGetExportOptions_unknownVersion_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("sbom-exp"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = apiGet(exportOptionsPath(app.getId(), uniqueId("no-version")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("cannot find version");
  }

  @Test
  public void testGetImportStatus_unknownRequest_returns404() throws Exception {
    Application app = tempEntity.newApplication(uniqueId("sbom-status"), Organization.ROOT_ORGANIZATION_ID);
    HttpResponse response = apiGet(importStatusPath(app.getId(), uniqueId("nonexistent-request")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("was not found");
  }

  /**
   * Per-JAX-RS-method auth coverage. {@link ApiSbomResource} exposes many distinct read /
   * mutating methods on the {@code /applications/} sub-tree, each with its own Shiro
   * annotation. A future per-method {@code @PermitAll} on any one of them would silently
   * bypass auth without a per-method test catching it.
   */
  @Test
  public void testGetSbomVersion_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(versionPath(uniqueId("any-app"), "1.0"));
    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteSbomVersion_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(versionPath(uniqueId("any-app"), "1.0"));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetSbomMetadata_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(appPath(uniqueId("any-app")));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetActiveSbomVersions_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(versionsPath(uniqueId("any-app")));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetSbomComponents_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(componentsPath(uniqueId("any-app"), "1.0"));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetExportOptions_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(exportOptionsPath(uniqueId("any-app"), "1.0"));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetImportStatus_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(importStatusPath(uniqueId("any-app"), uniqueId("any-request")));
    assertResponseStatus(401, response);
  }

  /**
   * The endpoint is {@code @Consumes(MULTIPART_FORM_DATA)}, but Shiro's auth interceptor
   * runs before JAX-RS content-type negotiation — the 401 fires before a 415 would.
   * A JSON body is used here because it's simpler than constructing a multipart request
   * for a test that will never reach the resource method body.
   */
  @Test
  public void testImportSbom_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiRequest()
            .path(importPath())
            .body("{}", MediaType.APPLICATION_JSON)
            .post();
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetVulnerabilityDetails_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiGet(vulnerabilityPath(uniqueId("any-app"), "1.0", uniqueId("any-ref")));
    assertResponseStatus(401, response);
  }

  @Test
  public void testSaveVulnerabilityAnalysis_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiPutJson(
            vulnerabilityAnalysisPath(uniqueId("any-app"), "1.0", uniqueId("any-ref")),
            "{}");
    assertResponseStatus(401, response);
  }

  @Test
  public void testDeleteVulnerabilityAnalysis_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiDelete(vulnerabilityAnalysisPath(uniqueId("any-app"), "1.0", uniqueId("any-ref")));
    assertResponseStatus(401, response);
  }
}
