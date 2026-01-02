/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.git.dto.ImportFailures;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationStatus;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationTicket;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent.ImportStatus;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.EXPERIMENTAL_ONBOARDING_RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class ApiScmOnboardingResourceTest
    extends AbstractScmOnboardingResourceTest
{
  private static final Pattern STATUS_URL_PATTERN = Pattern.compile(
      "api/experimental/onboarding/importRepositories/[a-f0-9]*/event/(?<eventId>[a-f0-9]*)");

  private OrganizationDAO organizationDAO;

  private ApplicationDAO applicationDAO;

  private SourceControlOrganizationImportEventDAO scmImportEventDAO;

  @Before
  public void setUp() {
    organizationDAO = lookup(OrganizationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);
    scmImportEventDAO = lookup(SourceControlOrganizationImportEventDAO.class);
  }

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
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_PATH).build(org.getId()).toString())
        .body(importRequest)
        .post();

    assertResponseStatus(202, response);
    ImportScmOrganizationTicket importTicket = response.getBody(ImportScmOrganizationTicket.class);
    assertThat(importTicket.statusUrl).matches(STATUS_URL_PATTERN);
    String eventId = extractEventId(importTicket);

    await().atMost(10, TimeUnit.SECONDS).until(() ->
        ImportStatus.COMPLETE.equals(scmImportEventDAO.getById(eventId).getImportStatus()));

    List<Organization> childOrgs = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgs).hasSize(3);
    SourceControlOrganizationImportEvent importEvent = scmImportEventDAO.getById(eventId);
    assertThat(importEvent.getLastUpdatedTime()).isAfter(importEvent.getStartTime());

    ImportFailures importResults = JsonUtils.parse(importEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importResults.failures).isEmpty();

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
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_PATH).build(org.getId()).toString())
        .body(importRequest)
        .post();

    assertResponseStatus(202, response);
    ImportScmOrganizationTicket importTicket = response.getBody(ImportScmOrganizationTicket.class);
    assertThat(importTicket.statusUrl).matches(STATUS_URL_PATTERN);
    String eventId = extractEventId(importTicket);

    await().atMost(10, TimeUnit.SECONDS).until(() ->
        ImportStatus.COMPLETE.equals(scmImportEventDAO.getById(eventId).getImportStatus()));
    SourceControlOrganizationImportEvent importEvent = scmImportEventDAO.getById(eventId);
    assertThat(importEvent.getLastUpdatedTime()).isAfter(importEvent.getStartTime());

    ImportFailures importResults = JsonUtils.parse(importEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importResults.failures).isEmpty();

    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(5);
  }

  @Test
  public void testImportRepositories_Error() throws Exception {
    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = gitService.baseUrl();

    HttpResponse response = restRequest()
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_PATH)
            .build("orgThatDoesNotExist").toString())
        .body(importRequest)
        .post();

    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).contains("Organization with ID orgThatDoesNotExist does not exist.");
  }

  @Test
  public void testGetImportRepositoriesStatus_Completed() throws Exception {
    mockRepoForPage(gitService, 1, getResourceAsString("/ScmOnboardingServiceTest/allRepos0.json"));
    mockRepoForPage(gitService, 2, getResourceAsString("/ScmOnboardingServiceTest/emptyResponse.json"));

    PasswordHandler pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    String encryptedPwd = new String(pwHandler.encryptPassword("TOKEN".toCharArray()));
    tempEntity.newSourceControl(org.getId(), null, encryptedPwd, SourceControlProvider.GITHUB);

    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = gitService.baseUrl();
    importRequest.importLimit = 5;

    HttpResponse submitResponse = restRequest()
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_PATH).build(org.getId()).toString())
        .body(importRequest)
        .post();

    assertResponseStatus(202, submitResponse);
    ImportScmOrganizationTicket importTicket = submitResponse.getBody(ImportScmOrganizationTicket.class);
    assertThat(importTicket.statusUrl).matches(STATUS_URL_PATTERN);
    String eventId = extractEventId(importTicket);

    await().atMost(10, TimeUnit.SECONDS).until(() ->
        ImportStatus.COMPLETE.equals(scmImportEventDAO.getById(eventId).getImportStatus()));

    HttpResponse statusResponse = restRequest()
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_STATUS_PATH)
            .build(org.getId(), eventId).toString())
        .get();

    assertResponseStatus(200, statusResponse);

    ImportScmOrganizationStatus importStatus = statusResponse.getBody(ImportScmOrganizationStatus.class);
    assertThat(importStatus.status).isEqualTo(ImportStatus.COMPLETE.toString());
    assertThat(importStatus.importSuccessCount).isEqualTo(5);
    assertThat(importStatus.importFailureCount).isZero();
    assertThat(importStatus.request.scmHostUrl).isEqualTo(gitService.baseUrl());
    assertThat(importStatus.request.importLimit).isEqualTo(5);
    assertThat(importStatus.lastUpdatedTimeAsDate()).isNotNull().isAfterOrEqualTo(importStatus.startTimeAsDate());
    assertThat(importStatus.request.desiredSubOrganizationCount).isZero();

    ImportFailures importResults = JsonUtils.parse(importStatus.errors.getBytes(), ImportFailures.class);
    assertThat(importResults.failures).isEmpty();
  }

  @Test
  public void testGetImportRepositoriesStatus_InProgress() throws Exception {
    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), "scm-url", 5, 0);

    HttpResponse statusResponse = restRequest()
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_STATUS_PATH)
            .build(org.getId(), importEvent.getId()).toString())
        .get();

    assertResponseStatus(200, statusResponse);
    ImportScmOrganizationStatus importStatus = statusResponse.getBody(ImportScmOrganizationStatus.class);
    assertThat(importStatus.status).isEqualTo(ImportStatus.IN_PROGRESS.toString());
    assertThat(importStatus.importSuccessCount).isZero();
    assertThat(importStatus.importFailureCount).isZero();
    assertThat(importStatus.request.scmHostUrl).isEqualTo("scm-url");
    assertThat(importStatus.request.importLimit).isEqualTo(5);
    assertThat(importStatus.lastUpdatedTimeAsDate()).isNotNull().isAfterOrEqualTo(importStatus.startTimeAsDate());
    assertThat(importStatus.request.desiredSubOrganizationCount).isZero();
    assertThat(importStatus.errors).isNull();
  }

  @Test
  public void testGetImportRepositoriesStatus_NotFound() throws Exception {
    HttpResponse statusResponse = restRequest()
        .path(UriBuilder.fromPath(ApiScmOnboardingResource.IMPORT_REPO_STATUS_PATH)
            .build(org.getId(), "nonExistentId").toString())
        .get();

    assertResponseStatus(404, statusResponse);
  }

  private static String extractEventId(final ImportScmOrganizationTicket importTicket) {
    Matcher matcher = STATUS_URL_PATTERN.matcher(importTicket.statusUrl);
    matcher.find();
    return matcher.group("eventId");
  }
}
