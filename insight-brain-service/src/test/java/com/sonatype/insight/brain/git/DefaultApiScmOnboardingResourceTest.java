/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.EXPERIMENTAL_ONBOARDING_RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultApiScmOnboardingResourceTest
    extends AbstractScmOnboardingResourceTest
{
  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(EXPERIMENTAL_ONBOARDING_RESOURCE_PATH);
  }

  @Test
  public void testImportRepositories_DistributeInToChildOrgs() throws Exception {
    mockRepoForPage(gitService, 1, getResourceAsString("/ScmOnboardingServiceTest/allRepos0.json"));
    mockRepoForPage(gitService, 2, getResourceAsString("/ScmOnboardingServiceTest/emptyResponse.json"));

    PasswordHandler pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity.newSourceControl(org.getId(), null, encryptedPwd, SourceControlProvider.GITHUB);

    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = gitService.baseUrl();
    importRequest.desiredSubOrganizationCount = 3;

    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(DefaultApiScmOnboardingResource.IMPORT_REPO_PATH).build(org.getId()).toString())
        .body(importRequest)
        .post();

    assertResponseStatus(200, response);

    List<Organization> childOrgs = organizationDAO.getByParentOrganizationId(org.getId());
    tempEntity.register(childOrgs.toArray(new Organization[0]));
    assertThat(childOrgs).hasSize(3);

    ImportResults importResults = response.getBody(ImportResults.class);
    List<SCMRepository> importedRepoList = importResults.getImportedRepositories();
    assertThat(importedRepoList).hasSize(13);

    List<Integer> importedAppCountsPerOrg =
        childOrgs.stream().map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(5, 4, 4);
  }

  @Test
  public void testImportRepositories_InToParentOrgWithLimit() throws Exception {
    mockRepoForPage(gitService, 1, getResourceAsString("/ScmOnboardingServiceTest/allRepos0.json"));
    mockRepoForPage(gitService, 2, getResourceAsString("/ScmOnboardingServiceTest/emptyResponse.json"));

    PasswordHandler pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity.newSourceControl(org.getId(), null, encryptedPwd, SourceControlProvider.GITHUB);

    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = gitService.baseUrl();
    importRequest.importLimit = 5;

    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(DefaultApiScmOnboardingResource.IMPORT_REPO_PATH).build(org.getId()).toString())
        .body(importRequest)
        .post();

    assertResponseStatus(200, response);
    ImportResults importResults = response.getBody(ImportResults.class);
    List<SCMRepository> importedRepoList = importResults.getImportedRepositories();
    assertThat(importedRepoList).hasSize(5);

    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(5);
  }

  @Test
  public void testImportRepositories_Error() throws Exception {
    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = gitService.baseUrl();

    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(DefaultApiScmOnboardingResource.IMPORT_REPO_PATH)
            .build("orgThatDoesNotExist").toString())
        .body(importRequest)
        .post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains("Cannot find organization with ID orgThatDoesNotExist");
  }
}
