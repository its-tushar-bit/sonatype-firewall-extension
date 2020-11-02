/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.experimental.dto.SCMRepositories;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.DEFAULT_HOST_URL;
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.LOAD_REPO_PATH;
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.RESOURCE_PATH;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiScmOnboardingResourceTest
    extends AbstractResourceTest
{
  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  private Organization org;

  private Application app;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication("tmpapp", org.getId());
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));
  }

  @Test
  public void testLoadRepositories() throws Exception {
    mockRepoForPage(gitService, 0, getResourceAsString("/ApiScmOnboardingServiceTest/allRepos0.json"));
    mockRepoForPage(gitService, 1, getResourceAsString("/ApiScmOnboardingServiceTest/emptyResponse.json"));

    // given root org is configured for github
    PasswordHandler pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, encryptedPwd, SourceControlProvider.GITHUB);
    // TODO INT-3695 adds default host support, until then prime the pump
    tempEntity.newSourceControl(app.getId(), gitService.baseUrl() + "/org/repo.git", null);

    // when repositories are loaded
    HttpResponse response = restRequest().path(RESOURCE_PATH + "/" + LOAD_REPO_PATH)
        .query("orgId", org.getId())
        .get();

    // then the response is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    SCMRepositories responseList = response.getBody(SCMRepositories.class);
    assertThat(responseList.availableRepositories.size()).isEqualTo(13);
  }

  private String getResourceAsString(String filename) throws IOException {
    return IOUtil.toString(this.getClass().getResourceAsStream(filename));
  }

  private void mockRepoForPage(WireMockRule gitService, int page, String json) {
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user/repos"))
        .withQueryParam("per_page", equalTo("100"))
        .withQueryParam("page", equalTo(Integer.toString(page)))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(json)));
  }

  @Test
  public void testDefaultHostUrl() throws Exception {
    // when
    HttpResponse response = restRequest().path(RESOURCE_PATH + "/" + DEFAULT_HOST_URL)
        .query("provider", "github")
        .query("orgId", "no-org-here")
        .get();

    // then the response is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    Map<String, String> responseList = response.getBody(Map.class);
    assertThat(responseList.size()).isEqualTo(1);
    assertThat(responseList.get("defaultHostUrl")).isEqualTo("https://github.com/");
  }
}
