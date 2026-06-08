/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide;

import java.util.List;
import java.util.Map;

import com.sonatype.guide.api.request.LatestVersionRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideComponentsResourceTest
    extends AbstractResourceTest
{
  private static final String SEARCH_PATH = "api/v2/guide/components/search";

  @Test
  public void unauthenticatedRequest_returns401() throws Exception {
    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .anon()
        .get();

    assertThat(response.getStatusCode()).isEqualTo(401);
  }

  @Test
  public void missingGuideSearchFeature_returns403() throws Exception {
    setMissingFeature(LicensedFeature.GUIDE_SEARCH);

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(403);
    assertThat(response.getBodyText()).contains("not available with the current license");
  }

  @Test
  public void noParams_returns200WithEmptyResults() throws Exception {
    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"hits\":[]");
    assertThat(response.getBodyText()).contains("\"total\":0");
  }

  @Test
  public void withQuery_returns200() throws Exception {
    // Raw JSON string used here so the test bypasses the hds-mock-server's ObjectMapper, which does not
    // register JavaTimeModule and therefore cannot serialize the Instant publishedDate field.
    String hdsResponse = """
        {
          "hits": [
            {
              "format": "maven",
              "originId": "org.apache.logging.log4j:log4j-core:2.14.1",
              "namespace": "org.apache.logging.log4j",
              "name": "log4j-core",
              "version": "2.14.1",
              "registryLink": "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.14.1/",
              "licenses": [
                {"licenseName": "Apache-2.0", "licenseThreatGroup": "no-copyleft", "licenseThreatLevel": 1}
              ],
              "categories": ["library"],
              "latestStable": false,
              "versionScore": 85,
              "maxCvss": 10.0,
              "publishedDate": "2021-12-09T00:00:00Z",
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
    hdsRespondWith(hdsResponse).atUri("/rest/search/components");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"log4j-core\"");
    assertThat(response.getBodyText()).contains("\"Apache-2.0\"");
    assertThat(response.getBodyText()).contains("\"publishedDate\":\"2021-12-09T00:00:00Z\"");
    assertThat(response.getBodyText()).contains("\"dts\":{");
    assertThat(response.getBodyText()).contains("\"overall\":85");
  }

  @Test
  public void withFilters_returns200() throws Exception {
    GuideComponentSearchResponse hdsResponse = new GuideComponentSearchResponse(
        List.of(), 0, 0, 20, Map.of("formats", Map.of("maven", 100L)));
    hdsRespondWith(hdsResponse).atUri("/rest/search/components");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "test",
            "formats", "maven",
            "categories", "library",
            "severities", "critical",
            "minCvss", "7.0",
            "maxCvss", "10.0",
            "minEpss", "0.5",
            "maxEpss", "1.0",
            "licenseFamilies", "apache",
            "licenses", "Apache-2.0",
            "minVersionScore", "50",
            "maxVersionScore", "100",
            "latestStable", "true",
            "publishedWindow", "30d",
            "hasMalware", "false",
            "offset", "0",
            "limit", "20")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void withPagination_returns200() throws Exception {
    GuideComponentSearchResponse hdsResponse = new GuideComponentSearchResponse(
        List.of(), 0, 10, 50, null);
    hdsRespondWith(hdsResponse).atUri("/rest/search/components");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("offset", "10", "limit", "50")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"offset\":10");
    assertThat(response.getBodyText()).contains("\"limit\":50");
  }

  @Test
  public void hdsFailure_returns502() throws Exception {
    hdsRespondWith("").atUri("/rest/search/components").andStatus(500);

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "test")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(502);
    assertThat(response.getBodyText()).contains("Bad Gateway");
  }

  // The existing /search tests above only exercise one of the seven endpoints exposed by
  // GuideComponentsResource. The remaining five (/detail, /versions, /vulnerabilities,
  // /dependencies, /latest-version) have unit-level coverage in the resource-package
  // GuideComponentsResourceTest and SearchApiClientImplTest, but the full Dropwizard stack
  // path (auth → SearchLicenseFilter → license enforcement → PURL validation → HDS
  // deserialization → JSON serialization) is only exercised below. The JSON payloads use
  // raw strings rather than typed DTOs because the hds-mock-server's ObjectMapper does not
  // register JavaTimeModule, so it cannot round-trip the {@code Instant publishedDate} field
  // on {@code GuideComponentDocument} / {@code GuideComponentDetailDocument}. The /vulnerabilities
  // case below uses a typed DTO because {@code GuideVulnerabilityDocument} has no Instant
  // fields and serializes cleanly through the mock.

  private static final String LOG4J_CORE_PURL = "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1";

  @Test
  public void getComponentDetail_byPurl_returns200() throws Exception {
    String hdsResponse = """
        {
          "format": "maven",
          "originId": "org.apache.logging.log4j:log4j-core:2.14.1",
          "namespace": "org.apache.logging.log4j",
          "name": "log4j-core",
          "version": "2.14.1",
          "registryLink": "https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-core/2.14.1/",
          "licenses": [
            {"licenseName": "Apache-2.0", "licenseThreatGroup": "no-copyleft", "licenseThreatLevel": 1}
          ],
          "categories": ["library"],
          "latestStable": false,
          "versionScore": 85,
          "maxCvss": 10.0,
          "publishedDate": "2021-03-09T00:00:00Z",
          "isMalware": false,
          "dts": {"overall": 85, "age": 70, "license": 100, "popularity": 80, "releaseStability": 60, "security": 90}
        }
        """;
    hdsRespondWith(hdsResponse).atUri("/rest/search/components/detail");

    HttpResponse response = restRequest()
        .path("api/v2/guide/components/detail")
        .query("purl", LOG4J_CORE_PURL)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"log4j-core\"");
    assertThat(response.getBodyText()).contains("\"publishedDate\":\"2021-03-09T00:00:00Z\"");
    assertThat(response.getBodyText()).contains("\"dts\":{");
    assertThat(response.getBodyText()).contains("\"overall\":85");
  }

  @Test
  public void getComponentVersions_byPurl_returns200() throws Exception {
    String hdsResponse = """
        {
          "hits": [
            {
              "format": "maven",
              "namespace": "org.apache.logging.log4j",
              "name": "log4j-core",
              "version": "2.21.1",
              "publishedDate": "2023-10-14T00:00:00Z",
              "versionScore": 95,
              "isMalware": false
            }
          ],
          "total": 1,
          "offset": 0,
          "limit": 20,
          "aggregations": null
        }
        """;
    hdsRespondWith(hdsResponse).atUri("/rest/search/components/versions");

    HttpResponse response = restRequest()
        .path("api/v2/guide/components/versions")
        .query("purl", LOG4J_CORE_PURL)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"version\":\"2.21.1\"");
    assertThat(response.getBodyText()).contains("\"total\":1");
  }

  @Test
  public void getComponentVulnerabilities_byPurl_returns200() throws Exception {
    GuideVulnerabilityDocument vuln = new GuideVulnerabilityDocument(
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
        List.of(vuln), 1, 0, 20, Map.of());
    hdsRespondWith(hdsResponse).atUri("/rest/search/components/vulnerabilities");

    HttpResponse response = restRequest()
        .path("api/v2/guide/components/vulnerabilities")
        .query("purl", LOG4J_CORE_PURL)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("CVE-2021-44228");
    assertThat(response.getBodyText()).contains("Apache Log4j2");
  }

  @Test
  public void getComponentDependencies_byPurl_returns200() throws Exception {
    String hdsResponse = """
        {
          "hits": [
            {
              "format": "maven",
              "originId": "org.apache.logging.log4j:log4j-api:2.14.1",
              "namespace": "org.apache.logging.log4j",
              "name": "log4j-api",
              "version": "2.14.1",
              "publishedDate": "2021-03-09T00:00:00Z",
              "isMalware": false
            }
          ],
          "total": 1,
          "offset": 0,
          "limit": 20,
          "aggregations": null
        }
        """;
    hdsRespondWith(hdsResponse).atUri("/rest/search/components/dependencies");

    HttpResponse response = restRequest()
        .path("api/v2/guide/components/dependencies")
        .query("purl", LOG4J_CORE_PURL)
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"name\":\"log4j-api\"");
    assertThat(response.getBodyText()).contains("\"total\":1");
  }

  @Test
  public void getLatestVersion_byPurl_returns200() throws Exception {
    String hdsResponse = """
        {
          "format": "maven",
          "originId": "org.apache.logging.log4j:log4j-core:2.21.1",
          "namespace": "org.apache.logging.log4j",
          "name": "log4j-core",
          "version": "2.21.1",
          "publishedDate": "2023-10-14T00:00:00Z",
          "versionScore": 95,
          "isMalware": false
        }
        """;
    hdsRespondWith(hdsResponse).atUri("/rest/search/components/latest-version");

    HttpResponse response = restRequest()
        .path("api/v2/guide/components/latest-version")
        .body(new LatestVersionRequest(LOG4J_CORE_PURL))
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"version\":\"2.21.1\"");
    assertThat(response.getBodyText()).contains("\"publishedDate\":\"2023-10-14T00:00:00Z\"");
  }
}
