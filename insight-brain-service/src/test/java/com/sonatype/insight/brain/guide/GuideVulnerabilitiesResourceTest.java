/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideVulnerabilitiesResourceTest
    extends AbstractResourceTest
{
  private static final String VULNERABILITIES_PATH = "api/v2/guide/vulnerabilities/search";

  /**
   * Guide features (GUIDE, GUIDE_MCP, GUIDE_SEARCH) are HDS-controlled (see
   * CLMLicenseManager.populateLicenseCache hdsControlledFeatures). The default integration-test HDS
   * mock response in productLicenseDetails.json doesn't include them, so the Guide-feature-gated
   * resources under test would 403 on every happy-path test without an explicit setFeatures here.
   * <p>
   * Note: setFeatures replaces the entire feature set — tests in this class that require non-Guide
   * licensed features must call setFeatures/setMissingFeature explicitly in the test body.
   * <p>
   * Auth filtering runs before license-feature filtering in the Dropwizard filter chain, so
   * unauthenticatedRequest_returns401 is unaffected by this narrowed feature set.
   */
  @Before
  public void enableGuideFeatures() throws Exception {
    setFeatures(LicensedFeature.GUIDE, LicensedFeature.GUIDE_MCP, LicensedFeature.GUIDE_SEARCH);
  }

  @Test
  public void unauthenticatedRequest_returns401() throws Exception {
    HttpResponse response = restRequest()
        .path(VULNERABILITIES_PATH)
        .query("query", "log4j")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void missingGuideSearchFeature_returns403() throws Exception {
    setMissingFeature(LicensedFeature.GUIDE_SEARCH);

    HttpResponse response = restRequest()
        .path(VULNERABILITIES_PATH)
        .query("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).contains("not available with the current license");
  }

  @Test
  public void validSearch_returnsResults() throws Exception {
    GuideVulnerabilityDocument document = new GuideVulnerabilityDocument(
        "CVE-2021-44228",
        List.of("GHSA-jfh8-c2jp-5v3q"),
        "Apache Log4j2 JNDI features do not protect against attacker controlled LDAP and other JNDI related endpoints",
        10.0,
        10.0,
        List.of("CWE-502"),
        List.of("CWE-502"),
        List.of("maven"),
        false,
        true,
        0.975,
        "NVD",
        null,
        "VULNERABILITY");
    GuideVulnerabilitySearchResponse hdsResponse = new GuideVulnerabilitySearchResponse(
        List.of(document), 1, 0, 20, Map.of());
    hdsRespondWith(hdsResponse).atUri("/rest/search/vulnerabilities");

    HttpResponse response = restRequest()
        .path(VULNERABILITIES_PATH)
        .query("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("CVE-2021-44228");
    assertThat(response.getBodyText()).contains("Apache Log4j2");
  }

  @Test
  public void emptySearch_returnsEmptyHits() throws Exception {
    GuideVulnerabilitySearchResponse hdsResponse = new GuideVulnerabilitySearchResponse(
        List.of(), 0, 0, 20, Map.of());
    hdsRespondWith(hdsResponse).atUri("/rest/search/vulnerabilities");

    HttpResponse response = restRequest()
        .path(VULNERABILITIES_PATH)
        .query("query", "nonexistent-vuln-xyz")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"total\":0");
    assertThat(response.getBodyText()).contains("\"hits\":[]");
  }

  @Test
  public void hdsFailure_returns502() throws Exception {
    hdsRespondWith("").atUri("/rest/search/vulnerabilities").andStatus(500);

    HttpResponse response = restRequest()
        .path(VULNERABILITIES_PATH)
        .query("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(502);
    assertThat(response.getBodyText()).contains("Bad Gateway");
  }

  // GUIDE-2821: only /{id}/components (the policy-enriched affected-components endpoint) carries
  // the @Max(25) cap. /search above returns vulnerabilities, not components, and is not capped.

  @Test
  public void getVulnerabilityAffectedComponents_limitOver25_returns400() throws Exception {
    HttpResponse response = restRequest()
        .path("api/v2/guide/vulnerabilities/CVE-2021-44228/components")
        .query("limit", "26")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("limit must not exceed 25");
  }

  // --- GUIDE-3045: ownerId/stage query params -------------------------------------------------------

  @Test
  public void getVulnerabilityAffectedComponents_bothOmitted_matchesDefaultBehavior() throws Exception {
    GuideAffectedComponentVersionSearchResponse hdsResponse =
        new GuideAffectedComponentVersionSearchResponse(List.of(), 0, 0, 20, null);
    hdsRespondWith(hdsResponse).atUri("/rest/search/vulnerabilities/CVE-2021-44228/components");

    HttpResponse response = restRequest()
        .path("api/v2/guide/vulnerabilities/CVE-2021-44228/components")
        .get();

    // Byte-identical to the GUIDE-2745 default now that the resource also accepts ownerId/stage.
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"hits\":[]");
    assertThat(response.getBodyText()).contains("\"total\":0");
  }

  @Test
  public void getVulnerabilityAffectedComponents_invalidStage_returns400() throws Exception {
    // Stage is validated up front (GuidePolicyService.requireValidStage) before the upstream HDS
    // fetch, so no HDS mock is needed here.
    HttpResponse response = restRequest()
        .path("api/v2/guide/vulnerabilities/CVE-2021-44228/components")
        .query("stage", "not-a-stage")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("not-a-stage");
  }

  @Test
  public void getVulnerabilityAffectedComponents_unknownOwnerId_returns200WithoutPolicyCompliance() throws Exception {
    GuideAffectedComponentVersionSearchResponse hdsResponse =
        new GuideAffectedComponentVersionSearchResponse(List.of(), 0, 0, 20, null);
    hdsRespondWith(hdsResponse).atUri("/rest/search/vulnerabilities/CVE-2021-44228/components");

    HttpResponse response = restRequest()
        .path("api/v2/guide/vulnerabilities/CVE-2021-44228/components")
        .query("ownerId", "does-not-exist")
        .get();

    // Per GUIDE-3045 AC #3: unknown owner mirrors MCP's silent soft-fail (200, no enrichment),
    // not HTTP 400 — see GuidePolicyService.resolveScope javadoc.
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).doesNotContain("policyCompliance");
  }
}
