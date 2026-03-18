/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.git.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent.ImportStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.model.SCMRepository;
import org.sonatype.plexus.components.cipher.PlexusCipher;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.git.ScmOnboardingService.setImportEventStatusUpdateThreshold;
import static com.sonatype.insight.brain.git.ScmOnboardingService.setScmParallelImportMaxRepositoriesPerBatch;
import static com.sonatype.insight.brain.git.ScmOnboardingService.setScmParallelImportThreshold;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Category(SlowTest.class)
public class ScmOnboardingServiceParallelTest
    extends AbstractComponentTest
{
  private static final String PAGE_1 = "allRepos0.json";

  public static final String PAGE_2 = "emptyResponse.json";

  public static final String BITBUCKET_DEFAULT_BRANCH_RESPONSE = "bitbucketDefaultBranchResponse.json";

  private static final String MOCK_USER_JSON = "{\"username\":\"foo\"}";

  public static final String MAIN_BRANCH = "main";

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private ScmOnboardingService scmOnboardingService;

  @Rule
  public LogOutput logOutput = new LogOutput(ScmOnboardingService.class);

  private Organization org;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private PlexusCipher plexusCipher;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  private static final String ENC = "CMMDwoV";

  @Mock
  private TelemetrySender telemetrySenderMock;

  private SourceControlOrganizationImportEventDAO sourceControlOrganizationImportEventDAO;

  @Override
  public void configure(final Binder binder) {
    sourceControlOrganizationImportEventDAO = spy(daoFactory.createSourceControlOrganizationImportEventDAO());
    binder.bind(SourceControlEventPublisher.class).toInstance(mockSourceControlEventPublisher);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(SourceControlOrganizationImportEventDAO.class).toInstance(sourceControlOrganizationImportEventDAO);
    super.configure(binder);
  }

  @Before
  public void setup() throws Exception {
    org = tempEntity.newOrganization();
    mockGetRequest(gitService, "/api/v3/user", MOCK_USER_JSON, HttpStatus.SC_OK);
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));

    SourceControl rootOrgSourceControl = tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt("TOKEN", ENC), SourceControlProvider.GITHUB);
    rootOrgSourceControl.setSourceControlEvaluationsEnabled(true);
    sourceControlDAO.update(rootOrgSourceControl);
    setScmParallelImportThreshold(1);
  }

  private String getResourceAsString(String filename) throws IOException {
    String resourceAsString = IOUtils.toString(
        getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/" + filename),
        StandardCharsets.UTF_8);
    resourceAsString = resourceAsString.replaceAll("https://localhost", gitService.baseUrl());
    resourceAsString = resourceAsString.replaceAll("https://admin@localhost", gitService.baseUrl());
    resourceAsString = resourceAsString.replaceAll("https://admin:admin123@localhost", gitService.baseUrl());
    return resourceAsString;
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
  public void testImportRepositories_Bitbucket_repositoriesWithInvalidDefaultBranch() throws Exception {
    // given git repositories details
    String repo1GetDefaultBranchURL = "/rest/api/1.0/projects/org/repos/repo1/branches/default";
    String repo2GetDefaultBranchURL = "/rest/api/1.0/projects/org/repos/repo2/branches/default";
    String bitBucketResponse = getResourceAsString(BITBUCKET_DEFAULT_BRANCH_RESPONSE);
    mockGetRequest(gitService, repo1GetDefaultBranchURL, bitBucketResponse, HttpStatus.SC_OK);
    mockGetRequest(gitService, repo2GetDefaultBranchURL, "", HttpStatus.SC_NO_CONTENT);
    mockGetRequest(gitService, "/rest/user", MOCK_USER_JSON, HttpStatus.SC_OK);
    gitService.stubFor(get(urlPathMatching("/rest/repos/scm/org"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));

    // given a list of repos to import
    String repo1URL = String.format("%s/scm/org/repo1", gitService.baseUrl());
    String repo2URL = String.format("%s/scm/org/repo2", gitService.baseUrl());
    SCMRepository[] reposToImport = new SCMRepository[]{
      // existing repository on BB with unknown default branch
      // should get default branch from SCM
      new SCMRepository(SourceControlProvider.BITBUCKET, repo1URL, null,
          false, "org", "repo1", "", GeneralSCMApiClient.UNKNOWN_DEFAULT_BRANCH),
      // empty repository on BB with unknown default branch
      // should get null default branch
      new SCMRepository(SourceControlProvider.BITBUCKET, repo2URL, null,
          false, "org", "repo2", "", GeneralSCMApiClient.UNKNOWN_DEFAULT_BRANCH)
    };
    int totalRepoCount = 50;
    int prevImportedCount = 10;

    // when the repos are imported
    ImportResults response = scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount));

    // then all the repos are imported
    List<SCMRepository> imported = response.getImportedRepositories();
    assertThat(imported).hasSize(2);
    assertThat(response.getFailedRepositories()).isEmpty();

    // and the proper default branch is set for each repository
    assertThat(imported.get(0).getDefaultBranch()).isEqualTo(MAIN_BRANCH);
    assertThat(imported.get(1).getDefaultBranch()).isNull();

    // and Git client was used to get the default branch name
    WireMock.verify(1, getRequestedFor(urlPathEqualTo(repo1GetDefaultBranchURL)));
    WireMock.verify(1, getRequestedFor(urlPathEqualTo(repo2GetDefaultBranchURL)));

    // and they exist in the DB
    List<Application> allApps = sourceControlDAO.getAll()
        .stream()
        .filter(sc -> !sc.getOwnerId().equals(ROOT_ORGANIZATION_ID))
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org", "repo2__org");
    assertThat(allApps.stream().map(Application::getName))
        .containsExactlyInAnyOrder("Repo1 - Org", "Repo2 - Org");

    // and the default branches are stored on DB
    assertThat(sourceControlDAO.getAll()
        .stream()
        .filter(sc -> sc.getOwnerId() != ROOT_ORGANIZATION_ID)
        .map(SourceControl::getBaseBranch))
            .containsExactly(MAIN_BRANCH, null);
  }

  private void mockGetRequest(WireMockRule gitService, String urlPath, String json, int status) {
    gitService.stubFor(get(urlPathEqualTo(urlPath))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(json)
            .withStatus(status)));
  }

  @Test
  public void testImportScmOrganization_DistributeIntoChildOrgs() throws Exception {
    org = tempEntity.newOrganization();
    List<Organization> childOrgsBeforeImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsBeforeImport).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));
    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 3);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(13);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).hasSize(3);

    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(5, 4, 4);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(13);

    // verify source control evaluations triggered for all imported apps
    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(4)).update(any(SourceControlOrganizationImportEvent.class));

    // check the telemetry was sent properly
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(29)).send(telemetryDataArgumentCaptor.capture());
    final List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
    assertThat(telemetryDataList).hasSize(29);
    List<TelemetryData> telemetryData = telemetryDataList.stream()
        .filter(td -> td.getPurpose().equals(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING))
        .collect(Collectors.toList());
    assertThat(telemetryData).hasSize(3);

    int prevImportedCount = 0;
    // By passing the total repo count equal to batch count we are asserting the entire batch was successful
    telemetryData.forEach(data -> assertBatchedImportTelemetries(data,
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        prevImportedCount));
  }

  @Test
  public void testImportScmOrganization_ImportTwiceUnderSameParentOrg() throws Exception {
    org = tempEntity.newOrganization();
    List<Organization> childOrgsBeforeImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsBeforeImport).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), 7, 3);
    SourceControlOrganizationImportEvent secondImportEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 3);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    scmOnboardingService.doScmOrganizationImport(secondImportEvent);

    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    SourceControlOrganizationImportEvent secondUpdatedEvent =
        sourceControlOrganizationImportEventDAO.getById(secondImportEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(7);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    assertThat(secondUpdatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(secondUpdatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(secondUpdatedEvent.getImportSuccessCount()).isEqualTo(6);
    assertThat(secondUpdatedEvent.getImportFailureCount()).isZero();
    assertThat(secondUpdatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).hasSize(6);

    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(3, 2, 2, 2, 2, 2);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(13);

    // verify source control evaluations triggered for all imported apps
    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(8)).update(any(SourceControlOrganizationImportEvent.class));

    // check the telemetry was sent properly
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(32)).send(telemetryDataArgumentCaptor.capture());
    final List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
    assertThat(telemetryDataList).hasSize(32);
    List<TelemetryData> telemetryData = telemetryDataList.stream()
        .filter(td -> td.getPurpose().equals(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING))
        .collect(Collectors.toList());
    assertThat(telemetryData).hasSize(6);

    int prevImportedCount = 0;
    // By passing the total repo count equal to batch count we are asserting the entire batch was successful
    telemetryData.forEach(data -> assertBatchedImportTelemetries(data,
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        prevImportedCount));
  }

  @Test
  public void testImportScmOrganization_DistributeIntoChildOrgsWithLimit() throws Exception {
    org = tempEntity.newOrganization();
    List<Organization> childOrgsBeforeImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsBeforeImport).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), 10, 3);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(10);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());

    assertThat(childOrgsAfterImport).hasSize(3);
    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(4, 3, 3);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(10);

    verifySourceControlEvaluationEventsCreated(10);
    verify(sourceControlOrganizationImportEventDAO, times(4)).update(any(SourceControlOrganizationImportEvent.class));

    // check the telemetry was sent properly
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(23)).send(telemetryDataArgumentCaptor.capture());
    final List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
    assertThat(telemetryDataList).hasSize(23);
    List<TelemetryData> telemetryData = telemetryDataList.stream()
        .filter(td -> td.getPurpose().equals(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING))
        .collect(Collectors.toList());
    assertThat(telemetryData).hasSize(3);

    int prevImportedCount = 0;
    // By passing the total repo count equal to batch count we are asserting the entire batch was successful
    telemetryData.forEach(data -> assertBatchedImportTelemetries(data,
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        prevImportedCount));
  }

  @Test
  public void testImportScmOrganization_IntoParentOrgOnly() throws Exception {
    org = tempEntity.newOrganization();
    assertThat(organizationDAO.getByParentOrganizationId(org.getId())).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 0);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(13);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(13);

    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(2)).update(any(SourceControlOrganizationImportEvent.class));

    assertScmImportTelemetries(13, 13);
  }

  private void assertBatchedImportTelemetries(
      TelemetryData telemetryData,
      int batchCount,
      int totalRepoCount,
      int prevImportedCount)
  {
    int batchPercent = (int) Math.round(batchCount * 100.0 / totalRepoCount);
    int totalPercent = (int) Math.round((batchCount + prevImportedCount) * 100.0 / totalRepoCount);
    Map<String, Object> attributes = telemetryData.getAttributes();
    assertThat(attributes).contains(entry("onboarding_batch_count", batchCount));
    assertThat(attributes).contains(entry("onboarding_batch_percent", batchPercent));
    assertThat(attributes).contains(entry("onboarding_total_percent", totalPercent));
  }

  @Test
  public void testImportScmOrganization_IntoToParentOrgOnlyWithLimit() throws Exception {
    org = tempEntity.newOrganization();
    assertThat(organizationDAO.getByParentOrganizationId(org.getId())).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), 5, 0);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(5);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(5);

    verifySourceControlEvaluationEventsCreated(5);
    verify(sourceControlOrganizationImportEventDAO, times(2)).update(any(SourceControlOrganizationImportEvent.class));

    assertScmImportTelemetries(5, 5, 5);
  }

  @Test
  public void testImportScmOrganization_DistributeIntoChildOrgs_BatchingLimitMet() throws Exception {
    org = tempEntity.newOrganization();
    setScmParallelImportMaxRepositoriesPerBatch(2);
    assertThat(organizationDAO.getByParentOrganizationId(org.getId())).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 3);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(13);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).hasSize(3);
    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(5, 4, 4);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(13);

    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(7)).update(any(SourceControlOrganizationImportEvent.class));

    // check the telemetry was sent properly
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(32)).send(telemetryDataArgumentCaptor.capture());
    final List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
    assertThat(telemetryDataList).hasSize(32);
    List<TelemetryData> telemetryData = telemetryDataList.stream()
        .filter(td -> td.getPurpose().equals(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING))
        .collect(Collectors.toList());
    assertThat(telemetryData).hasSize(6);
    int prevImportedCount = 0;
    // By passing the total repo count equal to batch count we are asserting the entire batch was successful
    telemetryData.forEach(data -> assertBatchedImportTelemetries(data,
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        prevImportedCount));
    setScmParallelImportMaxRepositoriesPerBatch(25);
  }

  @Test
  public void testImportScmOrganization_IntoToParentOrgOnly_BatchingLimitMet() throws Exception {
    org = tempEntity.newOrganization();
    setScmParallelImportMaxRepositoriesPerBatch(2);
    assertThat(organizationDAO.getByParentOrganizationId(org.getId())).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 0);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(13);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(13);

    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(7)).update(any(SourceControlOrganizationImportEvent.class));
    // check the telemetry was sent properly
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(32)).send(telemetryDataArgumentCaptor.capture());
    final List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
    assertThat(telemetryDataList).hasSize(32);
    List<TelemetryData> telemetryData = telemetryDataList.stream()
        .filter(td -> td.getPurpose().equals(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING))
        .collect(Collectors.toList());
    assertThat(telemetryData).hasSize(6);
    int prevImportedCount = 0;
    // By passing the total repo count equal to batch count we are asserting the entire batch was successful
    telemetryData.forEach(data -> assertBatchedImportTelemetries(data,
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        prevImportedCount));
    setScmParallelImportMaxRepositoriesPerBatch(25);
  }

  @Test
  public void testImportScmOrganization_WithLimitGreaterThanAvailableRepos() throws Exception {
    org = tempEntity.newOrganization();
    assertThat(organizationDAO.getByParentOrganizationId(org.getId())).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), 50, 0);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(13);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(2)).update(any(SourceControlOrganizationImportEvent.class));

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(13);
    verifySourceControlEvaluationEventsCreated(13);
    assertScmImportTelemetries(13, 13);
  }

  @Test
  public void testImportScmOrganization_DesiredNoOfReposGreaterThanAvailableRepos() throws Exception {
    org = tempEntity.newOrganization();
    assertThat(organizationDAO.getByParentOrganizationId(org.getId())).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = gitService.baseUrl();
    importRequest.desiredSubOrganizationCount = 50; // should only import the available 13 repos

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 50);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(13);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(14)).update(any(SourceControlOrganizationImportEvent.class));

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());

    assertThat(childOrgsAfterImport).hasSize(13);
    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(13);
  }

  @Test
  public void testImportScmOrganization_DistributeIntoChildOrgs_UpdateThresholdMet() throws Exception {
    org = tempEntity.newOrganization();
    setImportEventStatusUpdateThreshold(2);
    List<Organization> childOrgsBeforeImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsBeforeImport).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 3);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(13);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).hasSize(3);

    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(5, 4, 4);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(13);

    // verify source control evaluations triggered for all imported apps
    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(8)).update(any(SourceControlOrganizationImportEvent.class));

    // check the telemetry was sent properly
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(29)).send(telemetryDataArgumentCaptor.capture());
    final List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
    assertThat(telemetryDataList).hasSize(29);
    List<TelemetryData> telemetryData = telemetryDataList.stream()
        .filter(td -> td.getPurpose().equals(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING))
        .collect(Collectors.toList());
    assertThat(telemetryData).hasSize(3);
    int prevImportedCount = 0;
    // By passing the total repo count equal to batch count we are asserting the entire batch was successful
    telemetryData.forEach(data -> assertBatchedImportTelemetries(data,
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        (Integer) data.getAttributes().get("onboarding_batch_count"),
        prevImportedCount));

    setImportEventStatusUpdateThreshold(20);
  }

  @Test
  public void testImportScmOrganization_IntoParentOrgOnly_UpdateThresholdMet() throws Exception {
    org = tempEntity.newOrganization();
    setImportEventStatusUpdateThreshold(2);
    assertThat(organizationDAO.getByParentOrganizationId(org.getId())).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 0);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(13);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    assertThat(updatedEvent.getImportErrors()).isNull();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(13);

    verifySourceControlEvaluationEventsCreated(13);
    verify(sourceControlOrganizationImportEventDAO, times(8)).update(any(SourceControlOrganizationImportEvent.class));

    assertScmImportTelemetries(13, 13);

    setImportEventStatusUpdateThreshold(20);
  }

  private void assertScmImportTelemetries(int batchCount, int updatedApps) {
    // calculation mentioned here for clarification only
    int totalRepoCount = 13;
    int prevImportedCount = 0;
    int batchPercent = (int) Math.round(batchCount * 100.0 / totalRepoCount);
    int totalPercent = (int) ((prevImportedCount + batchCount) * 100.0 / totalRepoCount);
    int updatedCount = batchCount + updatedApps;
    assertTelemetry(batchPercent, batchCount, totalPercent, updatedCount);
  }

  private void assertScmImportTelemetries(int batchCount, int repoCount, int updatedApps) {
    // calculation mentioned here for clarification only
    int prevImportedCount = 0;
    int batchPercent = (int) Math.round(batchCount * 100.0 / repoCount);
    int totalPercent = (int) ((prevImportedCount + batchCount) * 100.0 / repoCount);
    int updatedCount = batchCount + updatedApps;
    assertTelemetry(batchPercent, batchCount, totalPercent, updatedCount);
  }

  private void verifySourceControlEvaluationEventsCreated(int count) {
    if (count > 0) {
      ArgumentCaptor<SourceControlEvent> eventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
      verify(mockSourceControlEventPublisher, times(count)).publishEvent(eventCaptor.capture());
      assertThat(eventCaptor.getAllValues()).hasSize(count);
      eventCaptor.getAllValues()
          .forEach(
              event -> {
                assertThat(event.getEventType()).isEqualTo(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);
                assertThat(event.getScanTriggerType())
                    .isEqualTo(ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);
              });
    }
    else {
      verify(mockSourceControlEventPublisher, never()).publishEvent(any());
    }
  }

  private void assertTelemetry(final int batchPercent, final int batchCount, final int totalPercent, int updateCount) {
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(1 + updateCount)).send(telemetryDataArgumentCaptor.capture());
    final List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
    TelemetryData telemetryData = telemetryDataList.stream()
        .filter(td -> td.getPurpose().equals(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING))
        .findFirst()
        .get();
    final Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("onboarding_batch_percent", batchPercent);
    expectedAttributes.put("onboarding_batch_count", batchCount);
    expectedAttributes.put("onboarding_total_percent", totalPercent);
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING);
    assertThat(telemetryData.getTimestamp())
        .isBetween(System.currentTimeMillis() - 10_000, System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
    reset(telemetrySenderMock);
  }
}
