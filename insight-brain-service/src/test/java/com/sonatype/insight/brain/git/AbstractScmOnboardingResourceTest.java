/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpHeaders;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Rule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class AbstractScmOnboardingResourceTest
    extends AbstractResourceTest
{
  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  protected Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));
  }

  protected String getResourceAsString(String filename) throws IOException {
    return IOUtil.toString(this.getClass().getResourceAsStream(filename));
  }

  protected void mockRepoForPage(WireMockRule gitService, int page, String json) {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .withQueryParam("per_page", equalTo("100"))
        .withQueryParam("page", equalTo(Integer.toString(page)))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(json)));
  }
}
