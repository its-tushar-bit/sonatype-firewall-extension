/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideSecurityEventSearchResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Guide Security Events API.
 * <p>
 * Tests the full HTTP stack including:
 * - Authentication (401 for unauthenticated requests)
 * - License enforcement (403 for unlicensed features)
 * - HDS integration (200 for successful queries)
 * - Error handling (502 for HDS failures)
 * <p>
 * Unit tests for business logic are in
 * {@link com.sonatype.insight.brain.guide.api.resource.GuideSecurityEventsResourceTest}.
 */
public class GuideSecurityEventsResourceIT
    extends AbstractResourceTest
{
  private static final String SEARCH_PATH = "api/v2/guide/security-events/search";

  private static final String DETAIL_PATH_PREFIX = "api/v2/guide/security-events/";

  /**
   * Guide features (GUIDE, GUIDE_MCP, GUIDE_SEARCH) are HDS-controlled.
   * The default integration-test HDS mock response doesn't include them, so we must
   * explicitly enable the required features before each test.
   * <p>
   * Note: setFeatures replaces the entire feature set — tests that require different
   * features must call setFeatures/setMissingFeatures explicitly in the test body.
   */
  @Before
  public void enableGuideFeatures() throws Exception {
    setFeatures(LicensedFeature.GUIDE, LicensedFeature.GUIDE_MCP, LicensedFeature.GUIDE_SEARCH);
  }

  // ===================================================================================
  // Authentication Tests
  // ===================================================================================

  @Test
  public void unauthenticatedRequest_returns401() throws Exception {
    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  // ===================================================================================
  // License Enforcement Tests
  // ===================================================================================

  @Test
  public void missingGuideSearchFeature_returns403() throws Exception {
    // The Guide API admits on GUIDE_SEARCH or AI_DEVELOPER
    setMissingFeatures(LicensedFeature.GUIDE_SEARCH, LicensedFeature.AI_DEVELOPER);

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).contains("not available with the current license");
  }

  // ===================================================================================
  // GET /api/v2/guide/security-events/search - List Tests
  // ===================================================================================

  @Test
  public void searchSecurityEvents_noParams_returns200WithEmptyResults() throws Exception {
    GuideSecurityEventSearchResponse hdsResponse = new GuideSecurityEventSearchResponse(
        List.of(), 0L, 0, 25, Map.of());
    hdsRespondWith(hdsResponse).atUri("/rest/search/security-events");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"hits\":[]");
    assertThat(response.getBodyText()).contains("\"total\":0");
  }

  @Test
  public void searchSecurityEvents_withQuery_returns200() throws Exception {
    // Raw JSON keyed to the security-events-v1 wire shape, exercising thin-DTO deserialization.
    String hdsResponse = """
        {
          "hits": [
            {
              "eventId": "SEC-2026-001",
              "title": "Log4Shell RCE",
              "overview": "Critical remote code execution in Apache Log4j",
              "eventSeverityCategory": "critical",
              "eventThreatType": "VULNERABILITY",
              "isKnownExploited": true,
              "publishedDate": "2021-12-10T00:00:00Z",
              "lastUpdatedDate": "2021-12-14T00:00:00Z"
            }
          ],
          "total": 1,
          "offset": 0,
          "limit": 25,
          "aggregations": null
        }
        """;
    hdsRespondWith(hdsResponse).atUri("/rest/search/security-events");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("SEC-2026-001");
    assertThat(response.getBodyText()).contains("Log4Shell RCE");
    assertThat(response.getBodyText()).contains("VULNERABILITY");
  }

  @Test
  public void searchSecurityEvents_withFilters_returns200() throws Exception {
    GuideSecurityEventSearchResponse hdsResponse = new GuideSecurityEventSearchResponse(
        List.of(), 0L, 0, 25, Map.of("severities", Map.of("critical", 100L)));
    hdsRespondWith(hdsResponse).atUri("/rest/search/security-events");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .query("query", "vulnerability")
        .query("severities", "critical")
        .query("threatTypes", "VULNERABILITY")
        .query("knownExploited", "true")
        .query("affectedEcosystems", "maven")
        .query("offset", "0")
        .query("limit", "25")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void searchSecurityEvents_withPagination_returns200() throws Exception {
    GuideSecurityEventSearchResponse hdsResponse = new GuideSecurityEventSearchResponse(
        List.of(), 0L, 10, 25, null);
    hdsRespondWith(hdsResponse).atUri("/rest/search/security-events");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .query("offset", "10")
        .query("limit", "25")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"offset\":10");
    assertThat(response.getBodyText()).contains("\"limit\":25");
  }

  @Test
  public void searchSecurityEvents_hdsFailure_returns502() throws Exception {
    hdsRespondWith("").atUri("/rest/search/security-events").andStatus(500);

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "test")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(502);
    assertThat(response.getBodyText()).contains("Bad Gateway");
  }

  // ===================================================================================
  // GET /api/v2/guide/security-events/{id} - Detail Tests
  // ===================================================================================

  @Test
  public void getSecurityEventById_validId_returns200() throws Exception {
    String hdsResponse = """
        {
          "eventId": "SEC-2026-001",
          "title": "Log4Shell RCE",
          "overview": "Critical remote code execution in Apache Log4j",
          "eventSeverityCategory": "critical",
          "eventThreatType": "VULNERABILITY",
          "isKnownExploited": true,
          "publishedDate": "2021-12-10T00:00:00Z",
          "lastUpdatedDate": "2021-12-14T00:00:00Z",
          "detail": "Apache Log4j2 JNDI features do not protect against attacker controlled LDAP.",
          "guidance": "Upgrade to Log4j 2.17.0 or later.",
          "sonatypeBlogUrl": "https://www.sonatype.com/blog/log4shell",
          "advisoryReferenceIds": ["CVE-2021-44228"],
          "cwes": ["CWE-502"],
          "malwareThreatTypes": [],
          "malwareAttackVectors": [],
          "affectedEcosystems": ["maven"],
          "affectedComponentVersionsCount": 150
        }
        """;
    hdsRespondWith(hdsResponse).atUri("/rest/search/security-events/SEC-2026-001");

    HttpResponse response = restRequest()
        .path(DETAIL_PATH_PREFIX + "SEC-2026-001")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("SEC-2026-001");
    assertThat(response.getBodyText()).contains("Log4Shell RCE");
    assertThat(response.getBodyText()).contains("CVE-2021-44228");
  }

  @Test
  public void getSecurityEventById_notFound_returns404() throws Exception {
    hdsRespondWith("").atUri("/rest/search/security-events/SEC-NONEXISTENT").andStatus(404);

    HttpResponse response = restRequest()
        .path(DETAIL_PATH_PREFIX + "SEC-NONEXISTENT")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void getSecurityEventById_hdsFailure_returns502() throws Exception {
    hdsRespondWith("").atUri("/rest/search/security-events/SEC-2026-001").andStatus(500);

    HttpResponse response = restRequest()
        .path(DETAIL_PATH_PREFIX + "SEC-2026-001")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(502);
    assertThat(response.getBodyText()).contains("Bad Gateway");
  }

  @Test
  public void getSecurityEventById_blankId_returns400() throws Exception {
    // A whitespace path segment reaches getSecurityEventById with a blank id, which
    // requireNonBlankId rejects with a 400 before any HDS call.
    HttpResponse response = restRequest()
        .path(DETAIL_PATH_PREFIX + "%20")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(400);
    assertThat(response.getBodyText()).contains("is required");
  }
}
