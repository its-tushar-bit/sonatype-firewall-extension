/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.experimental.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.api.experimental.dto.SCMRepositories;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.SCMRepository;

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
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.IMPORT_REPO_PATH;
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.LOAD_REPO_PATH;
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.RESOURCE_PATH;
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.VALIDATE_SCM_HOST_URL;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiScmOnboardingResourceTest
    extends AbstractResourceTest
{
  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  private Organization org;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"username\":\"foo\"}")));
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RESOURCE_PATH);
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

    // when repositories are loaded
    HttpResponse response = restRequest().path(LOAD_REPO_PATH)
        .query("orgId", org.getId())
        .query("defaultHostUrl", gitService.baseUrl())
        .get();

    // then the response is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    SCMRepositories responseList = response.getBody(SCMRepositories.class);
    assertThat(responseList.availableRepositories).hasSize(13);
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
    HttpResponse response = restRequest().path(DEFAULT_HOST_URL)
        .query("provider", "github")
        .query("orgId", "no-org-here")
        .get();

    // then the response is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    Map<String, String> responseList = response.getBody(Map.class);
    assertThat(responseList).hasSize(1);
    assertThat(responseList.get("defaultHostUrl")).isEqualTo("https://github.com/");
  }

  @Test
  public void testImportRepositories() throws Exception {
    // given we are configured to use Github
    PasswordHandler pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, encryptedPwd, SourceControlProvider.GITHUB);

    // when we make a call to import a repository
    String repoUrl = "https://localhost:5333/org/repo.git";
    List<SCMRepository> toAdd = Arrays.asList(new SCMRepository(SourceControlProvider.GITHUB,
        repoUrl, true, "org", "repo", null));
    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(IMPORT_REPO_PATH).build(org.getId()).toString())
        .body(new ImportRepositoriesRequest(toAdd, 5, 2))
        .post();

    // then the response is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    ImportResults importResults = response.getBody(ImportResults.class);
    List<SCMRepository> importedRepoList = importResults.getImportedRepositories();
    assertThat(importedRepoList).hasSize(1);
    SCMRepository importedRepo = importedRepoList.get(0);
    assertThat(importedRepo.getHttpCloneUrl()).isEqualTo(repoUrl);
    assertThat(importResults.getFailedImportCount()).isEqualTo(0);
  }

  @Test
  public void testImportRepositories_missingOrg() throws Exception {
    // when we make a call to import a repository that doesn't exist
    String repoUrl = "https://localhost:5333/org/repo.git";
    List<SCMRepository> toAdd = Arrays.asList(new SCMRepository(SourceControlProvider.GITHUB,
        repoUrl, true, "org", "repo", null));
    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(IMPORT_REPO_PATH).build("missing-org-id").toString())
        .body(new ImportRepositoriesRequest(toAdd, 5, 2))
        .post();

    // then the response is NOT_FOUND
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void testCheckScmUrl_valid() throws Exception {
    // when validating the SCM URL
    HttpResponse response = restRequest().path(VALIDATE_SCM_HOST_URL)
        .parameter("github")
        .query("scmHostUrl", "https://github.com")
        .get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

    // and value is present
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = response.getBody(Map.class);
    assertThat(responseMap).containsEntry("isValid", true)
        .containsEntry("errorMessages", emptyList());
  }

  @Test
  public void testCheckScmUrl_invalidUrl() throws Exception {
    // when validating the SCM URL
    HttpResponse response = restRequest().path(VALIDATE_SCM_HOST_URL)
        .parameter("github")
        .query("scmHostUrl", "I n v a l i d")
        .get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

    // and value is present
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = response.getBody(Map.class);
    assertThat(responseMap).containsEntry("isValid", false)
        .containsEntry("errorMessages", singletonList("Unable to parse repository URL: " +
            "java.net.URISyntaxException: Illegal character in path at index 1: I n v a l i d"));
  }

  @Test
  public void testCheckScmUrl_invalidProvider() throws Exception {
    // when validating the SCM URL
    HttpResponse response = restRequest().path(VALIDATE_SCM_HOST_URL)
        .parameter("invalid")
        .query("scmHostUrl", "http://example.com/")
        .get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);

    // and value is present
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = response.getBody(Map.class);
    assertThat(responseMap).containsEntry("isValid", false)
        .containsEntry("errorMessages", singletonList("Invalid SCM provider."));
  }
}
