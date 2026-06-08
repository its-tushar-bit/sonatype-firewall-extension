/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideGlobalSearchResourceTest
    extends AbstractResourceTest
{
  private static final String SEARCH_PATH = "api/v2/guide/global/search";

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
    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(
        List.of(), 0, 0, 20, null);
    hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = restRequest()
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
  public void withQuery_returnsComponentHits() throws Exception {
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
    hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = restRequest()
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
  public void withQuery_returnsVulnerabilityHits() throws Exception {
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
    hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "CVE-2021-44228")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"CVE-2021-44228\"");
    assertThat(response.getBodyText()).contains("\"Remote code execution in Log4j2\"");
    assertThat(response.getBodyText()).contains("\"publishedAt\":\"2021-12-10T00:00:00Z\"");
  }

  @Test
  public void withQuery_returnsMixedHits() throws Exception {
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
    hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "log4j")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyText()).contains("\"log4j-core\"");
    assertThat(response.getBodyText()).contains("\"CVE-2021-44228\"");
    assertThat(response.getBodyText()).contains("\"total\":2");
  }

  @Test
  public void withFilters_returns200() throws Exception {
    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(
        List.of(), 0, 0, 20, Map.of("formats", Map.of("maven", 100L)));
    hdsRespondWith(hdsResponse).atUri("/rest/search/global");

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "spring",
            "formats", "maven",
            "latestStable", "true",
            "publishedWindow", "30d",
            "offset", "0",
            "limit", "20")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(200);
  }

  @Test
  public void withPagination_returns200() throws Exception {
    GuideGlobalSearchResponse hdsResponse = new GuideGlobalSearchResponse(
        List.of(), 0, 10, 50, null);
    hdsRespondWith(hdsResponse).atUri("/rest/search/global");

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
    hdsRespondWith("").atUri("/rest/search/global").andStatus(500);

    HttpResponse response = restRequest()
        .path(SEARCH_PATH)
        .parameter("query", "test")
        .get();

    assertThat(response.getStatusCode()).isEqualTo(502);
    assertThat(response.getBodyText()).contains("Bad Gateway");
  }
}
