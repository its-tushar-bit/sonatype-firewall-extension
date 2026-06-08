/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideVulnerabilitiesResourceTest
    extends AbstractResourceTest
{
  private static final String VULNERABILITIES_PATH = "api/v2/guide/vulnerabilities/search";

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
}
