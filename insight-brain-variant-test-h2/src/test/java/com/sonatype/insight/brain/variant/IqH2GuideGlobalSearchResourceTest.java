/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 port of {@code GuideGlobalSearchResourceTest}.
 */
@IqH2Test
class IqH2GuideGlobalSearchResourceTest
{
  private static final String SEARCH_PATH = "api/v2/guide/global/search";

  private IqTestContext ctx;

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
  @BeforeEach
  void enableGuideFeatures() throws Exception {
    ctx.setFeatures(LicensedFeature.GUIDE, LicensedFeature.GUIDE_MCP, LicensedFeature.GUIDE_SEARCH);
  }

  @Test
  void unauthenticatedRequest_returns401() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  void missingGuideSearchFeature_returns403() throws Exception {
    // The Guide API admits on GUIDE_SEARCH or AI_DEVELOPER, so both must be absent to get a 403.
    ctx.setMissingFeatures(LicensedFeature.GUIDE_SEARCH, LicensedFeature.AI_DEVELOPER);

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).contains("not available with the current license");
  }

  @Test
  void aiDeveloperLicense_returns200() throws Exception {
    // A self-hosted AI Developer license (AI_DEVELOPER feature + AiDeveloper product, single-tenant)
    // unlocks the Guide API without GUIDE_SEARCH, clearing BOTH SearchLicenseFilter and the resource's
    // @ProductLicenseEnforcementPoint (which now accepts AI_DEVELOPER via anyOf).
    ctx.setLicenseProducts(ProductLicenseDetails.PRODUCT_AI_DEVELOPER);
    ctx.setFeatures(LicensedFeature.AI_DEVELOPER);

    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(List.of(), 0, 0, 20, null);
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"hits\":[]");
  }

  @Test
  void noParams_returns200WithEmptyResults() throws Exception {
    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(
        List.of(), 0, 0, 20, null);
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"hits\":[]");
    assertThat(response.getBodyText()).contains("\"total\":0");
  }

  // Raw JSON strings used in the next three tests so they bypass the hds-mock-server's ObjectMapper,
  // which does not register JavaTimeModule and therefore cannot serialize the Instant publishedDate /
  // publishedAt fields on the typed DTOs.

  @Test
  void withQuery_returnsComponentHits() throws Exception {
    String hdsResponse = """
        {
          "hits": [
            {
              "format": "maven",
              "originId": "org.apache.logging.log4j:log4j-core:2.17.1",
              "namespace": "org.apache.logging.log4j",
              "name": "log4j-core",
              "version": "2.17.1",
              "registryLink": "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.17.1/",
              "licenses": [
                {"licenseName": "Apache-2.0", "licenseThreatGroup": "no-copyleft", "licenseThreatLevel": 1}
              ],
              "categories": ["library"],
              "latestStable": true,
              "versionScore": 90,
              "maxCvss": 0.0,
              "publishedDate": "2021-12-17T00:00:00Z",
              "isMalware": false,
              "dts": {
                "overall": 85,
                "age": 70,
                "license": 100,
                "popularity": 80,
                "releaseStability": 60,
                "security": 90
              }
            }
          ],
          "total": 1,
          "offset": 0,
          "limit": 20,
          "aggregations": null
        }
        """;
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"log4j-core\"");
    assertThat(response.getBodyText()).contains("\"maven\"");
    assertThat(response.getBodyText()).contains("\"publishedDate\":\"2021-12-17T00:00:00Z\"");
    assertThat(response.getBodyText()).contains("\"dts\":{");
    assertThat(response.getBodyText()).contains("\"overall\":85");
  }

  @Test
  void withQuery_returnsVulnerabilityHits() throws Exception {
    String hdsResponse = """
        {
          "hits": [
            {
              "vulnId": "CVE-2021-44228",
              "aliases": ["GHSA-jfh8-c2jp-5v3q"],
              "summary": "Remote code execution in Log4j2",
              "cvssSeverity": 10.0,
              "sonatypeCvssSeverity": 10.0,
              "cwes": ["CWE-502"],
              "sonatypeCwes": ["CWE-502"],
              "affectedEcosystems": ["maven"],
              "isMalware": false,
              "kev": true,
              "epss": 0.976,
              "source": "NVD",
              "publishedAt": "2021-12-10T00:00:00Z",
              "researchType": "sonatype"
            }
          ],
          "total": 1,
          "offset": 0,
          "limit": 20,
          "aggregations": null
        }
        """;
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "CVE-2021-44228")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"CVE-2021-44228\"");
    assertThat(response.getBodyText()).contains("\"Remote code execution in Log4j2\"");
    assertThat(response.getBodyText()).contains("\"publishedAt\":\"2021-12-10T00:00:00Z\"");
  }

  @Test
  void withQuery_returnsMixedHits() throws Exception {
    String hdsResponse = """
        {
          "hits": [
            {
              "format": "maven",
              "originId": "org.apache.logging.log4j:log4j-core:2.17.1",
              "namespace": "org.apache.logging.log4j",
              "name": "log4j-core",
              "version": "2.17.1",
              "licenses": [],
              "categories": ["library"],
              "latestStable": true,
              "versionScore": 90,
              "maxCvss": 0.0,
              "publishedDate": "2021-12-17T00:00:00Z",
              "isMalware": false
            },
            {
              "vulnId": "CVE-2021-44228",
              "aliases": [],
              "summary": "Remote code execution in Log4j2",
              "cvssSeverity": 10.0,
              "sonatypeCvssSeverity": 10.0,
              "cwes": ["CWE-502"],
              "sonatypeCwes": ["CWE-502"],
              "affectedEcosystems": ["maven"],
              "isMalware": false,
              "kev": true,
              "epss": 0.976,
              "source": "NVD",
              "publishedAt": "2021-12-10T00:00:00Z",
              "researchType": "sonatype"
            }
          ],
          "total": 2,
          "offset": 0,
          "limit": 20,
          "aggregations": null
        }
        """;
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"log4j-core\"");
    assertThat(response.getBodyText()).contains("\"CVE-2021-44228\"");
    assertThat(response.getBodyText()).contains("\"total\":2");
  }

  @Test
  void withFilters_returns200() throws Exception {
    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(
        List.of(), 0, 0, 20, Map.of("formats", Map.of("maven", 100L)));
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .query("query", "spring")
        .query("formats", "maven")
        .query("latestStable", "true")
        .query("publishedWindow", "30d")
        .query("offset", "0")
        .query("limit", "20")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  void withPagination_returns200() throws Exception {
    // limit kept at the GUIDE-2821 cap (25) — values above are rejected by the cap check.
    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(
        List.of(), 0, 10, 25, null);
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .query("offset", "10")
        .query("limit", "25")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"offset\":10");
    assertThat(response.getBodyText()).contains("\"limit\":25");
  }

  @Test
  void globalSearch_limitOver25_returns400() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .query("query", "log4j")
        .query("limit", "26")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("limit must not exceed 25");
  }

  @Test
  void hdsFailure_returns502() throws Exception {
    ctx.hdsRespondWith("").atUri("/rest/search/global").andStatus(500);

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "test")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(502);
    assertThat(response.getBodyText()).contains("Bad Gateway");
  }

  // --- GUIDE-3045: ownerId/stage query params -------------------------------------------------------

  @Test
  void bothOmitted_matchesDefaultBehavior() throws Exception {
    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(List.of(), 0, 0, 20, null);
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    // Identical assertions to noParams_returns200WithEmptyResults — locks in AC #1 (byte-identical
    // to the GUIDE-2745 default) now that the resource also accepts ownerId/stage.
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"hits\":[]");
    assertThat(response.getBodyText()).contains("\"total\":0");
  }

  @Test
  void invalidStage_returns400() throws Exception {
    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .query("query", "log4j")
        .query("stage", "not-a-stage")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("not-a-stage");
  }

  @Test
  void unknownOwnerId_returns200WithoutPolicyCompliance() throws Exception {
    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(List.of(), 0, 0, 20, null);
    ctx.hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = ctx.restRequest()
        .path(SEARCH_PATH)
        .query("query", "log4j")
        .query("ownerId", "does-not-exist")
        .get();

    // Per GUIDE-3045 AC #3: unknown owner mirrors MCP's silent soft-fail (200, no enrichment),
    // not HTTP 400 — see GuidePolicyService.resolveScope javadoc.
    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).doesNotContain("policyCompliance");
  }
}
