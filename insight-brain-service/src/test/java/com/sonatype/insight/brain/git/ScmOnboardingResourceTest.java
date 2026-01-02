/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.List;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.git.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.SCMRepositories;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static com.sonatype.insight.brain.git.ScmOnboardingResource.DEFAULT_HOST_URL;
import static com.sonatype.insight.brain.git.ScmOnboardingResource.IMPORT_REPO_PATH;
import static com.sonatype.insight.brain.git.ScmOnboardingResource.LOAD_REPO_PATH;
import static com.sonatype.insight.brain.git.ScmOnboardingResource.RESOURCE_PATH;
import static com.sonatype.insight.brain.git.ScmOnboardingResource.VALIDATE_SCM_HOST_URL;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class ScmOnboardingResourceTest
    extends AbstractScmOnboardingResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RESOURCE_PATH);
  }

  @Test
  public void testLoadRepositories() throws Exception {
    mockRepoForPage(gitService, 1, getResourceAsString("/ScmOnboardingServiceTest/allRepos0.json"));
    mockRepoForPage(gitService, 2, getResourceAsString("/ScmOnboardingServiceTest/emptyResponse.json"));

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
    assertResponseStatus(200, response);
    SCMRepositories responseList = response.getBody(SCMRepositories.class);
    assertThat(responseList.availableRepositories).hasSize(13);
  }

  @Test
  public void testGetDefaultHostUrl() throws Exception {
    // when
    HttpResponse response = restRequest().path(DEFAULT_HOST_URL)
        .query("provider", "github")
        .query("orgId", org.getId())
        .get();

    // then the response is OK
    assertResponseStatus(200, response);
    Map<String, String> responseList = response.getBody(Map.class);
    assertThat(responseList).hasSize(1);
    assertThat(responseList.get("defaultHostUrl")).isEqualTo("");
  }

  @Test
  public void testImportRepositories() throws Exception {
    // given we are configured to use Github
    PasswordHandler pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, encryptedPwd, SourceControlProvider.GITHUB);

    // when we make a call to import a repository
    String repoUrl = String.format("%s/org/repo.git", gitService.baseUrl());
    List<SCMRepository> toAdd = singletonList(new SCMRepository(SourceControlProvider.GITHUB,
        repoUrl, null, true, "org", "repo", null));
    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(IMPORT_REPO_PATH).build(org.getId()).toString())
        .body(new ImportRepositoriesRequest(toAdd, 5, 2))
        .post();

    // then the response is OK
    assertResponseStatus(200, response);
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
    List<SCMRepository> toAdd = singletonList(new SCMRepository(SourceControlProvider.GITHUB,
        repoUrl, null, true, "org", "repo", null));
    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(IMPORT_REPO_PATH).build("missing-org-id").toString())
        .body(new ImportRepositoriesRequest(toAdd, 5, 2))
        .post();

    // then the response is NOT_FOUND
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
  }

  @Test
  public void testValidateScmHostUrl_valid() throws Exception {
    // when validating the SCM URL
    HttpResponse response = restRequest().path(VALIDATE_SCM_HOST_URL)
        .parameter("github")
        .query("scmHostUrl", "https://github.com")
        .get();

    // then result is OK
    assertResponseStatus(200, response);

    // and value is present
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = response.getBody(Map.class);
    assertThat(responseMap).containsEntry("isValid", true)
        .containsEntry("errorMessages", emptyList());
  }

  @Test
  public void testValidateScmHostUrl_invalidUrl() throws Exception {
    // when validating the SCM URL
    HttpResponse response = restRequest().path(VALIDATE_SCM_HOST_URL)
        .parameter("github")
        .query("scmHostUrl", "I n v a l i d")
        .get();

    // then result is OK
    assertResponseStatus(200, response);

    // and value is present
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = response.getBody(Map.class);
    assertThat(responseMap).containsEntry("isValid", false)
        .containsEntry("errorMessages", singletonList("Unable to parse repository URL: " +
            "java.net.URISyntaxException: Illegal character in path at index 1: I n v a l i d"));
  }

  @Test
  public void testValidateScmHostUrl_invalidProvider() throws Exception {
    // when validating the SCM URL
    HttpResponse response = restRequest().path(VALIDATE_SCM_HOST_URL)
        .parameter("invalid")
        .query("scmHostUrl", "http://example.com/")
        .get();

    // then result is OK
    assertResponseStatus(200, response);

    // and value is present
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = response.getBody(Map.class);
    assertThat(responseMap).containsEntry("isValid", false)
        .containsEntry("errorMessages", singletonList("Invalid SCM provider."));
  }
}
