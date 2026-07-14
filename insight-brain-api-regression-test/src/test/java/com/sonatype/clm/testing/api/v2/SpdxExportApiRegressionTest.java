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
 * API regression suite for {@code /api/v2/spdx/...} — {@code ApiSpdxResource}. Like the
 * CycloneDX resource, this is GET-only: it exports (never imports) SPDX SBOMs. SBOM ingest is
 * covered by {@link SbomApiRegressionTest} against {@code POST /api/v2/sbom/import}.
 *
 * <p>
 * Negative-path tests only: unknown-app / unknown-scan return 404 and auth holds.
 * Positive-path acceptance of valid SPDX versions (2.2, 2.3, 3.0), body-shape assertions, and
 * CLM-38381 SPDX license-expression handling live in the SBOM manager integration suite where
 * a real scan report is produced.
 *
 * <p>
 * <b>Intentionally excluded:</b> negative-path coverage for unsupported
 * {@code spdxVersion} query values. {@code ApiSpdxService.validateSpdxVersion} lets the
 * external {@code UnsupportedSbomException} from
 * {@code com.sonatype.insight.scan.file.ThirdPartyUtils} propagate — JAX-RS renders it as
 * HTTP 500, not the expected 4xx. The sibling {@code ApiThirdPartyScanService} catches the
 * same exception and maps it to {@code NotAcceptableException} (406); this asymmetry is a
 * product defect that must be fixed on a separate ticket (map to {@code BadRequestException}
 * / {@code NotAcceptableException} in {@code ApiSpdxService}) before this regression can be
 * asserted. Tracking that fix outside this test-authoring PR keeps the change scoped.
 */
@Category(ApiRegressionTest.class)
public class SpdxExportApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String SPDX_BASE = PublicApiPaths.SPDX_RESOURCE_PATH;

  private static String byStage(final String applicationId, final String stageId) {
    return SPDX_BASE + "/" + applicationId + "/stages/" + stageId;
  }

  private static String byReport(final String applicationId, final String scanId) {
    return SPDX_BASE + "/" + applicationId + "/reports/" + scanId;
  }

  @Test
  public void testGetLatestForStage_unknownApp_returns404() throws Exception {
    HttpResponse response = apiGet(byStage(uniqueId("nonexistent-app"), BuildStageType.ID));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetByScanId_unknownScan_returns404() throws Exception {
    HttpResponse response = apiGet(byReport(uniqueId("nonexistent-app"), uniqueId("nonexistent-scan")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  @Test
  public void testGetLatestForStage_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(byStage(uniqueId("any-app"), BuildStageType.ID));
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetByScanId_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(byReport(uniqueId("any-app"), uniqueId("any-scan")));
    assertResponseStatus(401, response);
  }
}
