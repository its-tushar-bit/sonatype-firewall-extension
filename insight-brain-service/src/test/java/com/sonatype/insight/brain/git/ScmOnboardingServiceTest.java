/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlOrganizationImportEventDAO;
import com.sonatype.insight.brain.git.dto.ImportFailure;
import com.sonatype.insight.brain.git.dto.ImportFailures;
import com.sonatype.insight.brain.git.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.git.dto.ImportResults;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationRequest;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationStatus;
import com.sonatype.insight.brain.git.dto.ImportScmOrganizationTicket;
import com.sonatype.insight.brain.git.dto.SCMRepositories;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl.AuthenticationType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlOrganizationImportEvent.ImportStatus;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.GeneralSCMApiClient;
import com.sonatype.nexus.scm.api.GitApiClient;
import com.sonatype.nexus.scm.api.base.MissingSourceControlConfigException;
import com.sonatype.nexus.scm.api.model.SCMRepository;
import org.sonatype.plexus.components.cipher.PlexusCipher;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.google.inject.Binder;
import org.apache.commons.io.FileUtils;
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
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum.SCM_USERNAME;
import static com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum.IQ_USERNAME;
import static com.sonatype.insight.brain.git.ScmOnboardingService.MAX_PUBLICID_RENAME_ATTEMPTS;
import static com.sonatype.insight.brain.git.ScmOnboardingService.setScmParallelImportThreshold;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.utils.ScmUserMappingsHelper.getMappingForScmUserJsonStorage;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ScmOnboardingServiceTest
    extends AbstractComponentTest
{
  private static final String PAGE_1 = "allRepos0.json";

  public static final String PAGE_2 = "emptyResponse.json";

  public static final String LIST_WITH_EMPTY_REPOS = "listWithEmptyRepos.json";

  public static final String BITBUCKET_DEFAULT_BRANCH_RESPONSE = "bitbucketDefaultBranchResponse.json";

  private static final String MOCK_USER_JSON = "{\"username\":\"foo\"}";

  public static final String MAIN_BRANCH = "main";

  private static final Pattern STATUS_URL_PATTERN = Pattern.compile(
      "api/experimental/onboarding/importRepositories/[a-f0-9]*/event/(?<eventId>[a-f0-9]*)");

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private ScmOnboardingService scmOnboardingService;

  @Rule
  public LogOutput logOutput = new LogOutput(ScmOnboardingService.class);

  private Application app;

  private Organization org;

  private SourceControl rootOrgSourceControl;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private SourceControlOrganizationImportEventDAO sourceControlOrganizationImportEventDAO;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private ScmApplicationNameConverter scmApplicationNameConverter;

  private static final String ENC = "CMMDwoV";

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Override
  public void configure(final Binder binder) {
    binder.bind(SourceControlEventPublisher.class).toInstance(mockSourceControlEventPublisher);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    super.configure(binder);
  }

  @Before
  public void setup() throws Exception {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication("tmpapp", org.getId());
    mockGetRequest(gitService, "/api/v3/user", MOCK_USER_JSON, HttpStatus.SC_OK);

    // returns contributors for automatic role assignment
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/.*/.*"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getResourceContents("/ScmOnboardingServiceTest/contributorsResponse.json"))));

    rootOrgSourceControl = tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt("TOKEN", ENC), SourceControlProvider.GITHUB);
    rootOrgSourceControl.setSourceControlEvaluationsEnabled(true);
    sourceControlDAO.update(rootOrgSourceControl);
    setScmParallelImportThreshold(100);
  }

  @Test
  public void testScmOnboardingService_AddsToShutdownHandler() {
    verify(mockShutdownHandler).add(scmOnboardingService.getExecutor());
  }

  @Test
  public void testLoadRepositories_hierarchy() throws Exception {
    // given root org with no token
    sourceControlDAO.delete(sourceControlDAO.getByOwnerId(ROOT_ORGANIZATION_ID));
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);

    // and an org with a token
    tempEntity
        .newSourceControl(org.getId(), null, plexusCipher.encrypt("TOKEN", ENC), null);

    // and a git service
    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    // then loading repositories returns the expected results
    SCMRepositories repositories = scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.availableRepositories.size()).isEqualTo(13);

    // and: no source control evaluation events
    verifyNoSourceControlEvaluationEventsCreated();
  }

  @Test
  public void testLoadRepositories_hierarchyNoToken() {
    // given root org with no token
    sourceControlDAO.delete(sourceControlDAO.getByOwnerId(ROOT_ORGANIZATION_ID));
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);

    // and an org with no token either
    tempEntity
        .newSourceControl(org.getId(), null, null, null);

    // then loading repositories fails
    assertThatExceptionOfType(MissingSourceControlConfigException.class)
        .isThrownBy(() -> scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl()))
        .withMessageContaining("'token' must not be null");

    // and: no source control evaluation events
    verifyNoSourceControlEvaluationEventsCreated();
  }

  @Test
  public void testLoadRepositories() throws Exception {
    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    // then loading repositories returns the expected results
    SCMRepositories repositories = scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.availableRepositories.size()).isEqualTo(13);
    assertThat(repositories.totalRepositories).isEqualTo(13);

    // and: no source control evaluation events
    verifyNoSourceControlEvaluationEventsCreated();
  }

  @Test
  public void testLoadRepositories_trimExistingConfiguredRepositories() throws Exception {
    // configure urls to point to our mock git server, as these are used to guess at a base api url
    String repo1Url = "https://localhost/depshield-ci/ci-project-1.git";
    String repo2Url = "https://localhost/depshield-ci/ci-project-16.git";
    String repo1 = "/org/repo1.git";
    String repo2 = "/org/repo2.git";
    String repo1ReplacementUrl = gitService.baseUrl().replace("localhost", "admin:admin123@localhost")
        + repo1;
    String repo2ReplacementUrl = gitService.baseUrl().replace("localhost", "admin@localhost") + repo2;

    String resourceAsString = IOUtils.toString(
        getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/" + PAGE_1),
        StandardCharsets.UTF_8);
    mockRepoForPage(gitService, 1, resourceAsString
        .replaceFirst(repo1Url, repo1ReplacementUrl)
        .replaceFirst(repo2Url, repo2ReplacementUrl));
    resourceAsString = IOUtils.toString(
        getClass().getResourceAsStream("/" + getClass().getSimpleName() + "/" + PAGE_2),
        StandardCharsets.UTF_8);
    mockRepoForPage(gitService, 2, resourceAsString);

    // given some of the repositories are already configured for SCM
    tempEntity.newSourceControl(app.getId(), gitService.baseUrl() + repo1, new Date());
    Application tmpapp2 = tempEntity.newApplication("tmpapp2", org.getId());
    tempEntity.newSourceControl(tmpapp2.getId(), gitService.baseUrl() + repo2, new Date());
    // duplicate repo url entries as they can be configured as such to scan different modules(a la insight-brain)
    Application tmpapp3 = tempEntity.newApplication("tmpapp3", org.getId());
    tempEntity.newSourceControl(tmpapp3.getId(), gitService.baseUrl() + repo2, new Date());

    // then loading repositories returns the trimmed results
    SCMRepositories repositories = scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.availableRepositories.size()).isEqualTo(11);
    assertThat(repositories.totalRepositories).isEqualTo(13);

    // and: no source control evaluation events
    verifyNoSourceControlEvaluationEventsCreated();
  }

  @Test
  public void testLoadRepositories_trimExistingConfiguredRepositories_MixedCase() throws Exception {
    // configure urls to point to our mock git server, as these are used to guess at a base api url
    String repoUrl = "https://localhost/org/MixedCase.git";
    String repo = "/org/MixedCase.git";
    String repoReplacementUrl = gitService.baseUrl().replace("localhost", "admin:admin123@localhost") + repo;

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1).replaceFirst(repoUrl, repoReplacementUrl));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    // given the repo with mixed case url is already configured for SCM
    tempEntity.newSourceControl(app.getId(), gitService.baseUrl() + repo, new Date());

    // then loading repositories returns the trimmed results (i.e. not including the already configured one)
    SCMRepositories repositories = scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.totalRepositories).isEqualTo(13);
    assertThat(repositories.availableRepositories.size()).isEqualTo(12);
    assertThat(repositories.availableRepositories.stream()
        .map(scmRepo -> scmRepo.getHttpCloneUrl().toLowerCase(Locale.ENGLISH))
        .anyMatch(url -> url.contains("mixedcase")))
            .isFalse();

    // and: no source control evaluation events
    verifyNoSourceControlEvaluationEventsCreated();
  }

  @Test
  public void testLoadRepositories_trimExistingConfiguredRepositories_caseInsensitive() throws Exception {
    // given a mock SCM server is configured
    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    // then loading repositories returns the trimmed results (i.e. not including the already configured one)
    SCMRepositories repositories = scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.totalRepositories).isEqualTo(13);
    assertThat(repositories.availableRepositories.size()).isEqualTo(13);
    assertThat(repositories.availableRepositories.stream() //
        .map(SCMRepository::getHttpCloneUrl) //
        .anyMatch(url -> url.contains("MixedCase"))) //
            .isTrue();

    // when the repo is added with lower-case
    String repoUrl = String.format("%s/org/mixedcase.git", gitService.baseUrl());
    tempEntity.newSourceControl(app.getId(), repoUrl, new Date());

    // then loading repositories returns the trimmed results without the already-added one, even though the case
    // doesn't match
    repositories = scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.totalRepositories).isEqualTo(13);
    // avail repos is one fewer to account for the existing repo getting filtered out
    assertThat(repositories.availableRepositories.size()).isEqualTo(12);
    assertThat(repositories.availableRepositories.stream() //
        .map(SCMRepository::getHttpCloneUrl) //
        .anyMatch(url -> url.contains("MixedCase"))) //
            .isFalse();

    // and: no source control evaluation events
    verifyNoSourceControlEvaluationEventsCreated();
  }

  @Test
  public void testLoadRepositories_sanitizeCloneUrls() throws Exception {
    String page1 = getResourceAsString(PAGE_1);
    mockRepoForPage(gitService, 1, page1);
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    // then the repository listing will strip out this embedded information to ensure it doesn't leak
    SCMRepositories repositories = scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    Optional<SCMRepository> createReactApp = repositories.availableRepositories.stream()
        .filter(repository -> repository.getProject().equals("create-react-app"))
        .findFirst();
    assertThat(createReactApp.get().getHttpCloneUrl())
        .isEqualTo(String.format("%s/depshield-ci/create-react-app", gitService.baseUrl()));
    Optional<SCMRepository> nxrmP2 = repositories.availableRepositories.stream()
        .filter(repository -> repository.getProject().equals("nexus-repository-p2"))
        .findFirst();
    assertThat(nxrmP2.get().getHttpCloneUrl())
        .isEqualTo(String.format("%s/sonatype-nexus-community/nexus-repository-p2", gitService.baseUrl()));

    // and: no source control evaluation events
    verifyNoSourceControlEvaluationEventsCreated();
  }

  @Test
  public void testLoadRepositories_setDefaultBranchWithStandardValue_forEmptyStringAndNull() throws Exception {
    // given we receive a list of repos with some having empty default branch
    String emptyRepos = getResourceAsString(LIST_WITH_EMPTY_REPOS);
    mockRepoForPage(gitService, 1, emptyRepos);
    mockRepoForPage(gitService, 2, "[]");

    // when we get the list of repositories
    SCMRepositories repositories = scmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());

    // then for repos with default branch with value null or empty string the new value should be
    // GitApiClient.DEFAULT_BRANCH_NOT_DEFINED
    assertThat(repositories.availableRepositories.stream().map(SCMRepository::getDefaultBranch))
        .containsExactly(GitApiClient.DEFAULT_BRANCH_NOT_DEFINED,
            GitApiClient.DEFAULT_BRANCH_NOT_DEFINED,
            MAIN_BRANCH);
  }

  @Test
  public void testLoadRepositories_invalidOrgId() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> scmOnboardingService.loadRepositories("organizationThatDoesntExist", gitService.baseUrl()))
        .withMessageContaining("Organization with ID organizationThatDoesntExist does not exist.");

    // and: no source control evaluation events
    verifyNoSourceControlEvaluationEventsCreated();
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
  public void testGetDefaultHostUrl_noProvider() {
    testGetDefaultHostUrl_noProvider(null);
    testGetDefaultHostUrl_noProvider("");
    testGetDefaultHostUrl_noProvider(" ");
  }

  private void testGetDefaultHostUrl_noProvider(String provider) {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> scmOnboardingService.getDefaultHostUrl(provider, "org-id-not-checked"))
        .withMessageContaining("Provider has not been specified");
  }

  @Test
  public void testGetDefaultHostUrl_invalidProvider() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> scmOnboardingService.getDefaultHostUrl("invalid", "org-id-not-checked"))
        .withMessageContaining("Invalid provider: invalid");
  }

  @Test
  public void testGetDefaultHostUrl_noOrgId() {
    testGetDefaultHostUrlByProvider("github", "");
    testGetDefaultHostUrlByProvider("gitlab", "");
    testGetDefaultHostUrlByProvider("bitbucket", "");
  }

  private void testGetDefaultHostUrlByProvider(String provider, String expectedUrl) {
    assertThat(scmOnboardingService.getDefaultHostUrl(provider, org.getId())).isEqualTo(expectedUrl);
  }

  @Test
  public void testGetDefaultHostUrl_orgWithScm() {
    // test a variety of different hosts
    testGetDefaultHostUrl_repoUrlGH("http://example.com:8899/owner/app", "http://example.com:8899");
    testGetDefaultHostUrl_repoUrlGH("https://example.com:8443/owner/app", "https://example.com:8443");
    testGetDefaultHostUrl_repoUrlGH("http://example.com/owner/app", "http://example.com");
    testGetDefaultHostUrl_repoUrlGH("http://example.com:80/owner/app", "http://example.com:80");
    testGetDefaultHostUrl_repoUrlGH("https://example.com/owner/app", "https://example.com");
    testGetDefaultHostUrl_repoUrlGH("https://example.com:443/owner/app", "https://example.com:443");
  }

  @Test
  public void testGetDefaultHostUrl_bitbucketOrgWithScm() {
    // test a variety of different hosts
    testGetDefaultHostUrl_repoUrlBB("https://localhost:7990/biz/scm/scm/org/project", "https://localhost:7990/biz/scm");
    testGetDefaultHostUrl_repoUrlBB("https://localhost:7990/scm/biz/scm/org/project", "https://localhost:7990/scm/biz");
    testGetDefaultHostUrl_repoUrlBB("https://example.com:5000/bitbucket/scm/org/proj",
        "https://example.com:5000/bitbucket");
    testGetDefaultHostUrl_repoUrlBB("https://example.com/scm/org/proj", "https://example.com");
    testGetDefaultHostUrl_repoUrlBB("https://bitbucket.org/org/proj", "https://bitbucket.org");
    testGetDefaultHostUrl_repoUrlBB("https://example.com:443/scm/owner/app", "https://example.com:443");
  }

  private void testGetDefaultHostUrl_repoUrlGH(String repoUrl, String expectedDefaultHostUrl) {
    testGetDefaultHostUrl_repoUrl(repoUrl, expectedDefaultHostUrl, SourceControlProvider.GITHUB.name());
  }

  private void testGetDefaultHostUrl_repoUrlBB(String repoUrl, String expectedDefaultHostUrl) {
    testGetDefaultHostUrl_repoUrl(repoUrl, expectedDefaultHostUrl, SourceControlProvider.BITBUCKET.name());
  }

  private void testGetDefaultHostUrl_repoUrl(String repoUrl, String expectedDefaultHosturl, String provider) {
    // given an org
    Organization organization = tempEntity.newOrganization();

    // and an application in that org
    Application application = tempEntity.newApplication(organization.getId());

    // and a source control entry for that app
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(application.getId())
        .setRepositoryUrl(repoUrl)
        .build();
    sourceControlDAO.insert(sourceControl);

    // when we get the host URL
    String defaultHostUrl = scmOnboardingService.getDefaultHostUrl(provider, organization.getId());

    // then it should be custom, not the github default
    assertThat(defaultHostUrl).isEqualTo(expectedDefaultHosturl);
  }

  @Test
  public void testGetDefaultHostUrl_otherOrgsWithScm() {
    // given an app with a custom repo URL
    SourceControl scApp1a = new SourceControl.Builder()
        .setOwnerId(app.getId())
        .setRepositoryUrl("http://example.com/owner/app")
        .build();
    sourceControlDAO.insert(scApp1a);

    // and two apps with a different custom repo URL
    Application app1b = tempEntity.newApplication(org.getId());
    SourceControl scApp1b = new SourceControl.Builder()
        .setOwnerId(app1b.getId())
        .setRepositoryUrl("http://prefix.example.com/owner/app2")
        .build();
    sourceControlDAO.insert(scApp1b);

    Application app1c = tempEntity.newApplication(org.getId());
    SourceControl scApp1c = new SourceControl.Builder()
        .setOwnerId(app1c.getId())
        .setRepositoryUrl("http://prefix.example.com/owner/app")
        .build();
    sourceControlDAO.insert(scApp1c);

    // and an app in a new org that does NOT have a repo URL defined
    Application newApp = tempEntity.newApplicationWithParent();

    // when we get the host URL for an org without SCMs defined
    String defaultHostUrl = scmOnboardingService.getDefaultHostUrl("github", newApp.getOrganizationId());

    // then it should be the URL defined in the existing org, using the host with the largest count
    assertThat(defaultHostUrl).isEqualTo("http://prefix.example.com");
  }

  @Test
  public void testGetDefaultHostUrl_otherOrgsWithCustomTokens() {
    // given an org with a custom token
    Organization orgCustom = tempEntity.newOrganization("custom");
    sourceControlDAO.insert(new SourceControl.Builder()
        .setOwnerId(orgCustom.getId())
        .setToken("token")
        .build());

    // given an app with a non-github repo URL
    Application appCustom = tempEntity.newApplication(orgCustom.getId());
    sourceControlDAO.insert(new SourceControl.Builder()
        .setOwnerId(appCustom.getId())
        .setRepositoryUrl("http://example.com/owner/app")
        .build());

    // when we get the host URL for an org without SCMs defined
    String defaultHostUrl = scmOnboardingService.getDefaultHostUrl("github", org.getId());

    // then it should be just the default and skip the app
    assertThat(defaultHostUrl).isEqualTo("");
  }

  @Test
  public void testGetDefaultHostUrl_orgWithNoScm() {
    // when we get the host URL for an org with no SCM defined
    String defaultHostUrl = scmOnboardingService.getDefaultHostUrl("github", org.getId());

    // then it should be the default
    assertThat(defaultHostUrl).isEqualTo("");
  }

  @Test
  public void testImportRepositories_allNew() {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    String repo1URL = String.format("%s/org/repo1", gitService.baseUrl());
    String repo2URL = String.format("%s/org/repo2", gitService.baseUrl());
    String repo3URL = String.format("%s/org/repo3", gitService.baseUrl());
    String repo4URL = String.format("%s/org/repo4", gitService.baseUrl());
    // given a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
      new SCMRepository(SourceControlProvider.GITHUB, repo1URL,
          "git@localhost:org/repo1.git", false, "org", "repo1", ""),
      new SCMRepository(SourceControlProvider.GITHUB, repo2URL,
          "git@localhost:org/repo2.git", false, "org", "repo2", ""),
      new SCMRepository(SourceControlProvider.GITHUB, repo3URL,
          "git@localhost:org/repo3.git", false, "org", "repo3", ""),
      // use org & app names with IQ app name restrictions
      new SCMRepository(SourceControlProvider.GITHUB, repo4URL,
          "git@localhost:org/repo4.git", false, "--bad-__-org", "--bad_name_99--", ""),
    };
    int totalRepoCount = 50;
    int prevImportedCount = 10;

    givenScmMappingEnabledAndSomeIQUsersToMapTo();

    // when the repos are imported
    ImportResults response = scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount));

    // then the imported repo is returned
    List<SCMRepository> imported = response.getImportedRepositories();
    assertThat(imported.size()).isEqualTo(4);
    for (int i = 0; i < imported.size(); i++) {
      assertThat(imported.get(i).getNamespace()).isEqualTo(reposToImport[i].getNamespace());
      assertThat(imported.get(i).getProject()).isEqualTo(reposToImport[i].getProject());
      assertThat(imported.get(i).getHttpCloneUrl()).isEqualTo(reposToImport[i].getHttpCloneUrl());
      assertThat(imported.get(i).getSshCloneUrl()).isEqualTo(reposToImport[i].getSshCloneUrl());
      assertThat(imported.get(i).getSourceControlProvider()).isEqualTo(reposToImport[i].getSourceControlProvider());
      assertThat(imported.get(i).getDescription()).isEqualTo(reposToImport[i].getDescription());
    }
    assertThat(response.getFailedRepositories()).isEmpty();

    // and they exist in the DB
    List<Application> allApps = sourceControlDAO.getAll()
        .stream()
        .filter(sc -> !sc.getOwnerId().equals(ROOT_ORGANIZATION_ID))
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org", "repo2__org", "repo3__org", "--bad_name_99--__--bad-__-org");
    assertThat(allApps.stream().map(Application::getName))
        .containsExactlyInAnyOrder("Repo1 - Org", "Repo2 - Org", "Repo3 - Org", "Bad Name 99 - Bad Org");

    // and that all the clone URLs were added
    assertThat(sourceControlDAO.getAll()
        .stream()
        .filter(sc -> !ROOT_ORGANIZATION_ID.equals(sc.getOwnerId()))
        .map(SourceControl::getRepositoryUrl)).containsExactly(repo1URL, repo2URL, repo3URL, repo4URL);

    // and that all the clone URLs were added
    assertThat(sourceControlDAO.getAll()
        .stream()
        .filter(sc -> !ROOT_ORGANIZATION_ID.equals(sc.getOwnerId()))
        .map(SourceControl::getRepositorySshUrl)).containsExactly("git@localhost:org/repo1.git",
            "git@localhost:org/repo2.git", "git@localhost:org/repo3.git", "git@localhost:org/repo4.git");

    // and: source control evaluation request events were created
    verifySourceControlEvaluationEventsCreated(imported.size());

    // and the telemetry was sent properly
    int batchPercent = 8;
    int batchCount = reposToImport.length;
    int totalPercent = (int) ((prevImportedCount + batchCount) * 100.0 / totalRepoCount);
    assertTelemetry(batchPercent, batchCount, totalPercent, batchCount, batchCount);

    final var mappedMemberships = tempEntity.getMembershipMappings(RoleDAO.DEVELOPER);

    // should have created 2 membership mappings per each of the 4 apps created
    assertThat(mappedMemberships).isNotNull();
    assertThat(mappedMemberships.size()).isEqualTo(8);

    final var groupedByApps = mappedMemberships.stream()
        .collect(Collectors.groupingBy(MembershipMapping::getContextId));
    assertThat(groupedByApps.size()).isEqualTo(4);
    for (var membership : groupedByApps.values()) {
      assertThat(membership.size()).isEqualTo(2);

      assertThat(membership.get(0).getMemberName()).isEqualTo("some-other-user");
      assertThat(membership.get(0).getRoleId()).isEqualTo(DEVELOPER_ROLE_ID);

      assertThat(membership.get(1).getMemberName()).isEqualTo("some-user-1");
      assertThat(membership.get(1).getRoleId()).isEqualTo(DEVELOPER_ROLE_ID);
    }
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
    assertThat(imported.size()).isEqualTo(2);
    assertThat(response.getFailedRepositories()).isEmpty();

    // and the proper default branch is set for each repository
    assertThat(imported.get(0).getDefaultBranch()).isEqualTo(MAIN_BRANCH);
    assertThat(imported.get(1).getDefaultBranch()).isEqualTo(null);

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
        .filter(sc -> !ROOT_ORGANIZATION_ID.equals(sc.getOwnerId()))
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
  public void testImportRepositories_existingApp() {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // given an existing application which will match a repo which we'll import
    final var existingApp = tempEntity.newApplication("repo1__org", org.getId());

    String repo1URL = String.format("%s/org/repo1", gitService.baseUrl());
    // and a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
      new SCMRepository(SourceControlProvider.GITHUB, repo1URL, null, false, "org", "repo1",
          "a description")
    };
    int totalRepoCount = 50;
    int prevImportedCount = 8;

    givenScmMappingEnabledAndSomeIQUsersToMapTo();

    // when the repos are imported
    ImportResults response = scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount));

    // then the imported repo is returned
    List<SCMRepository> imported = response.getImportedRepositories();
    assertThat(imported.size()).isEqualTo(1);
    assertThat(imported.get(0).getNamespace()).isEqualTo("org");
    assertThat(imported.get(0).getProject()).isEqualTo("repo1");
    assertThat(imported.get(0).getHttpCloneUrl()).isEqualTo(repo1URL);
    assertThat(imported.get(0).getSourceControlProvider()).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(imported.get(0).getDescription()).isEqualTo("a description");
    assertThat(response.getFailedRepositories()).isEmpty();

    // and the new app was imported, with a suffix at the end
    List<Application> allApps = applicationDAO.getAll();
    assertThat(allApps.stream().map(Application::getOrganizationId).distinct().collect(Collectors.toList()))
        .containsExactlyInAnyOrder(org.getId());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org", "repo1__org_2", app.getPublicId());

    // and the source control entries was created on the new application
    List<Application> allSourceControlApps = sourceControlDAO.getAll()
        .stream()
        .filter(sc -> !sc.getOwnerId().equals(ROOT_ORGANIZATION_ID))
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allSourceControlApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org_2");

    // and: a source control evaluation event was created
    verifySourceControlEvaluationEventsCreated(1);

    // and the telemetry was sent properly indicating no items were imported
    int batchPercent = reposToImport.length * 2;
    int batchCount = reposToImport.length;
    int totalPercent = (int) ((prevImportedCount + batchCount) * 100.0 / totalRepoCount);
    assertTelemetry(batchPercent, batchCount, totalPercent, reposToImport.length, reposToImport.length);

    // should create 2 memberships for the single created app
    final var mappedMemberships = tempEntity.getMembershipMappings(RoleDAO.DEVELOPER);
    assertThat(mappedMemberships).isNotNull();
    assertThat(mappedMemberships.size()).isEqualTo(2);

    assertThat(mappedMemberships.get(0).getContextId()).isNotEqualTo(existingApp.getId());
    assertThat(mappedMemberships.get(0).getRoleId()).isEqualTo(DEVELOPER_ROLE_ID);
    assertThat(mappedMemberships.get(0).getMemberName()).isEqualTo("some-other-user");

    assertThat(mappedMemberships.get(1).getContextId()).isNotEqualTo(existingApp.getId());
    assertThat(mappedMemberships.get(1).getRoleId()).isEqualTo(DEVELOPER_ROLE_ID);
    assertThat(mappedMemberships.get(1).getMemberName()).isEqualTo("some-user-1");
  }

  @Test
  public void testImportRepositories_unlicensed() {
    // given SCM imports are enabled, but IQ for SCM is not supported by license
    testProductLicense.setMissingFeatures(LicensedFeature.AUTOMATION, LicensedFeature.NOTIFICATIONS);
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // given an existing application which will match a repo which we'll import
    tempEntity.newApplication("repo1__org", org.getId());

    // and a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
      new SCMRepository(SourceControlProvider.GITHUB, "http://localhost/org/repo1", false, "org", "repo1",
          "a description")
    };

    // when the repos are imported
    scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(Arrays.asList(reposToImport), 50, 8));

    // then no source control evaluation event was created
    verifyNoSourceControlEvaluationEventsCreated();
  }

  @Test
  public void testImportRepositories_existingSourceControl() {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // given an existing application with a Source Control entry that matches one we'll import
    final String repoUrl = String.format("%s/org/repo1", gitService.baseUrl());
    Application targetApp = tempEntity.newApplication("repo1__org", org.getId());
    tempEntity.newSourceControl(targetApp.getId(), repoUrl, new Date());

    // and a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
      new SCMRepository(SourceControlProvider.GITHUB, repoUrl, null, false, "org", "repo1",
          "foo")
    };
    int totalRepoCount = 50;
    int prevImportedCount = 8;

    givenScmMappingEnabledAndSomeIQUsersToMapTo();

    // when the repos are imported
    ImportResults response = scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount));

    // then the imported repo is returned
    List<SCMRepository> imported = response.getImportedRepositories();
    assertThat(imported.size()).isEqualTo(1);
    assertThat(imported.get(0).getNamespace()).isEqualTo("org");
    assertThat(imported.get(0).getProject()).isEqualTo("repo1");
    assertThat(imported.get(0).getHttpCloneUrl()).isEqualTo(repoUrl);
    assertThat(imported.get(0).getSourceControlProvider()).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(imported.get(0).getDescription()).isEqualTo("foo");
    assertThat(response.getFailedRepositories()).isEmpty();

    // and the only apps that are present are the ones for our selected repos
    List<Application> allApps = applicationDAO.getAll();
    assertThat(allApps.stream().map(Application::getOrganizationId).distinct().collect(Collectors.toList()))
        .containsExactlyInAnyOrder(org.getId());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org", app.getPublicId());

    // and all the source control entries were created
    List<Application> allSourceControlApps = sourceControlDAO.getAll()
        .stream()
        .filter(sc -> !ROOT_ORGANIZATION_ID.equals(sc.getOwnerId()))
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allSourceControlApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org");

    // and: a source control evaluation event was created for each imported repository
    verifySourceControlEvaluationEventsCreated(imported.size());

    // and the telemetry was sent properly indicating no items were imported
    int batchPercent = reposToImport.length * 2;
    int batchCount = reposToImport.length;
    // note that the totalPercent goes up (because we return the repo that we attempted to import but
    // which was not changed so that the UI can remove it) but updatedApps is 0 (because no actual
    // DB changes were made)
    int totalPercent = (int) ((prevImportedCount + batchCount) * 100.0 / totalRepoCount);
    int updatedApps = 0;
    int roleAssignmentCount = 1;
    assertTelemetry(batchPercent, batchCount, totalPercent, updatedApps, roleAssignmentCount);

    // should still create memberships even though the app was existing
    final var mappedMemberships = tempEntity.getMembershipMappings(RoleDAO.DEVELOPER);
    assertThat(mappedMemberships).isNotNull();
    assertThat(mappedMemberships.size()).isEqualTo(2);

    assertThat(mappedMemberships.get(0).getContextId()).isEqualTo(targetApp.getId());
    assertThat(mappedMemberships.get(0).getRoleId()).isEqualTo(DEVELOPER_ROLE_ID);
    assertThat(mappedMemberships.get(0).getMemberName()).isEqualTo("some-other-user");

    assertThat(mappedMemberships.get(1).getContextId()).isEqualTo(targetApp.getId());
    assertThat(mappedMemberships.get(1).getRoleId()).isEqualTo(DEVELOPER_ROLE_ID);
    assertThat(mappedMemberships.get(1).getMemberName()).isEqualTo("some-user-1");
  }

  @Test(expected = BadRequestException.class)
  public void testImportRepositories_nullScmRepos() {
    // when import with null scm repos, it throws an exception
    scmOnboardingService.importRepositories(org.getId(), new ImportRepositoriesRequest());
  }

  @Test
  public void testImportRepositories_invalidBatchParams_zeroTotalRepoCount() {
    // given invalid totalRepoCount
    int totalRepoCount = 0;

    // given arbitrary prevImportedCount
    int prevImportedCount = 5;

    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    String repo1URL = String.format("%s/org/repo1", gitService.baseUrl());
    // given a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
      new SCMRepository(SourceControlProvider.GITHUB, repo1URL, null, false, "org", "repo1", "")
    };

    // and we call import
    scmOnboardingService
        .importRepositories(org.getId(),
            new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount))
        .getImportedRepositories();

    // and the telemetry was sent properly
    int batchCount = reposToImport.length;

    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(3)).send(telemetryDataArgumentCaptor.capture());
    final List<TelemetryData> telemetryDataList = telemetryDataArgumentCaptor.getAllValues();
    TelemetryData telemetryData = telemetryDataList.stream()
        .filter(td -> td.getPurpose().equals(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING))
        .findFirst()
        .get();

    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.SOURCE_CONTROL_ONBOARDING);
    assertThat(telemetryData.getTimestamp())
        .isBetween(System.currentTimeMillis() - 10_000, System.currentTimeMillis());

    // no onboarding_batch_percent or onboarding_total_percent
    final Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("onboarding_batch_count", batchCount);
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);

    reset(telemetrySenderMock);
  }

  @Test
  public void testImportRepositories_invalidCharacters() {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // when we make a call to import a repository
    String repoUrl = String.format("%s/org/repo1", gitService.baseUrl());
    List<SCMRepository> toAdd = singletonList(new SCMRepository(SourceControlProvider.GITHUB,
        repoUrl, null, true, "??invalidorg??", "!!invalidproject!!", null));
    ImportResults importResults = scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(toAdd, 5, 2));

    // then the response is OK
    List<SCMRepository> importedRepoList = importResults.getImportedRepositories();
    assertThat(importedRepoList).hasSize(1);
    SCMRepository importedRepo = importedRepoList.get(0);
    assertThat(importedRepo.getProject()).isEqualTo("!!invalidproject!!");
    assertThat(importedRepo.getNamespace()).isEqualTo("??invalidorg??");
  }

  @Test
  public void testImportRepositories_conflictingProjectName() {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // and we have an existing repository
    String repoUrl1 = String.format("%s/org/repo1.git", gitService.baseUrl());
    List<SCMRepository> toAdd = singletonList(new SCMRepository(SourceControlProvider.GITHUB,
        repoUrl1, null, true, "org", "project", null));
    scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(toAdd, 5, 2));

    // when we import another repository where the name only differs in invalid characters that have been stripped out
    String repoUrl2 = String.format("%s/org/repo2.git", gitService.baseUrl());
    List<SCMRepository> toAddConflicting = singletonList(new SCMRepository(SourceControlProvider.GITHUB,
        repoUrl2, null, true, "org", "project!!", null));
    ImportResults importResults = scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(toAddConflicting, 5, 2));

    // then the response is OK
    List<SCMRepository> importedRepoList = importResults.getImportedRepositories();
    assertThat(importedRepoList).hasSize(1);

    // and the projectname has been extended with a postfix
    SCMRepository importedRepo = importedRepoList.get(0);
    assertThat(importedRepo.getProject()).isEqualTo("project!!");
    assertThat(importedRepo.getNamespace()).isEqualTo("org");
  }

  @Test
  public void testImportRepositories_multipleConflictingProjectName() {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    String repoUrl = String.format("%s/org/repo", gitService.baseUrl());
    // when we import another repository where the name only differs in invalid characters that have been stripped out
    // with more conflicts than we are willing to fix by renaming
    List<SCMRepository> toAdd = IntStream.range(0, MAX_PUBLICID_RENAME_ATTEMPTS + 2)
        .mapToObj(i -> new SCMRepository(SourceControlProvider.GITHUB, repoUrl + i + ".git",
            null, true, "org", "project!!", null, null))
        .collect(Collectors.toList());
    ImportResults importResults = scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(toAdd, 5, 2));

    // then the response is OK
    assertThat(importResults.getImportedRepositories()).hasSize(6);
    assertThat(importResults.getFailedRepositories()).hasSize(1);
    assertThat(importResults.getFailedImportCount()).isEqualTo(1);

    // and a failure is created
    ImportFailure failedRepo = importResults.getFailedRepositories().get(0);
    assertThat(failedRepo.getRepository().getProject()).isEqualTo("project!!");
    assertThat(failedRepo.getRepository().getNamespace()).isEqualTo("org");
    assertThat(failedRepo.getErrorMessage()).isEqualTo("Could not find unique name for publicId: [project__org]");
  }

  @Test
  public void testValidateScmHostUrl() {
    // expect null response when no error is found
    assertThat(scmOnboardingService.validateScmHostUrl("github", "http://example.com/").isValid).isTrue();

    // expect provider to be case insensitive
    assertThat(scmOnboardingService.validateScmHostUrl("GiThUb", "http://example.com/").isValid).isTrue();

    // expect server side parsing error messages
    assertThat(scmOnboardingService.validateScmHostUrl("github", "http://example.com/ ").errorMessages)
        .isEqualTo(singletonList("Unable to parse repository URL: java.net.URISyntaxException: Illegal character in " +
            "path at index 19: http://example.com/ "));

    // expect provider to be case insensitive
    assertThat(scmOnboardingService.validateScmHostUrl("invalid", "http://example.com/").errorMessages)
        .isEqualTo(singletonList("Invalid SCM provider."));
  }

  @Test
  public void testImportRepositories_disabledInternalSourceControlPolicyEvaluations_null() {
    testImportRepositories_disabledInternalSourceControlPolicyEvaluations(null);
  }

  @Test
  public void testImportRepositories_disabledInternalSourceControlPolicyEvaluations_false() {
    testImportRepositories_disabledInternalSourceControlPolicyEvaluations(false);
  }

  @Test
  public void testImportScmOrganization_NullRequest() throws Exception {
    org = tempEntity.newOrganization();
    testImportScmOrganization_InvalidParameters(org.getId(), null, "No host URL defined");
  }

  @Test
  public void testImportScmOrganization_InvalidHostUrl() throws Exception {
    org = tempEntity.newOrganization();
    testImportScmOrganization_InvalidParameters(org.getId(), new ImportScmOrganizationRequest(), "No host URL defined");
  }

  @Test
  public void testImportScmOrganization_InvalidImportLimit() {
    org = tempEntity.newOrganization();
    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = "http://example.com/owner/app";
    importRequest.importLimit = 0;
    testImportScmOrganization_InvalidParameters(org.getId(), importRequest, "repository import limit must not be 0");
  }

  @Test
  public void testImportScmOrganization_InvalidDefaultOrgCount() {
    org = tempEntity.newOrganization();
    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = "http://example.com/owner/app";
    importRequest.desiredSubOrganizationCount = -1;
    testImportScmOrganization_InvalidParameters(org.getId(), importRequest,
        "desiredSubOrganizationCount must be a positive integer");
  }

  @Test
  public void testImportScmOrganization_NoReposToImport_FiresImportEventAsynchronously() throws Exception {
    org = tempEntity.newOrganization();
    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_2));

    ImportScmOrganizationRequest importRequest = new ImportScmOrganizationRequest();
    importRequest.scmHostUrl = gitService.baseUrl();

    ImportScmOrganizationTicket ticket = scmOnboardingService.importScmOrganization(org.getId(), importRequest);
    assertThat(ticket.statusUrl).matches(STATUS_URL_PATTERN);
    String eventId = extractEventId(ticket);

    await().atMost(10, TimeUnit.SECONDS)
        .until(
            () -> ImportStatus.COMPLETE
                .equals(sourceControlOrganizationImportEventDAO.getById(eventId).getImportStatus()));

    SourceControlOrganizationImportEvent updatedEvent = sourceControlOrganizationImportEventDAO.getById(eventId);
    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isZero();
    assertThat(updatedEvent.getImportFailureCount()).isZero();

    ImportFailures importFailures = JsonUtils.parse(updatedEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures.failures).isEmpty();
    verifyNoSourceControlEvaluationEventsCreated();
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

    ImportFailures importFailures = JsonUtils.parse(updatedEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures.failures).isEmpty();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).hasSize(3);
    assertThat(childOrgsAfterImport).map(Nameable::getName).allSatisfy(childOrgName -> {
      String[] nameParts = childOrgName.split("-");
      assertThat(nameParts[0]).isEqualTo(org.getName());
      assertThat(nameParts[1]).hasSize(8);
    });

    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(5, 4, 4);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(13);

    // verify source control evaluations triggered for all imported apps
    verifySourceControlEvaluationEventsCreated(13);

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
    assertBatchedImportTelemetries(telemetryData.get(0), 5, 13, prevImportedCount);
    prevImportedCount += 5;
    assertBatchedImportTelemetries(telemetryData.get(1), 4, 13, prevImportedCount);
    prevImportedCount += 4;
    assertBatchedImportTelemetries(telemetryData.get(2), 4, 13, prevImportedCount);
  }

  @Test
  public void testImportScmOrganization_ImportTwiceToTheSameParentOrg() throws Exception {
    org = tempEntity.newOrganization();
    List<Organization> childOrgsBeforeImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsBeforeImport).isEmpty();

    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));
    mockRepoForPage(gitService, 2, getResourceAsString(PAGE_2));

    SourceControlOrganizationImportEvent importEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), 7, 3);
    SourceControlOrganizationImportEvent secondEvent =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), gitService.baseUrl(), -1, 3);

    scmOnboardingService.doScmOrganizationImport(importEvent);
    scmOnboardingService.doScmOrganizationImport(secondEvent);

    SourceControlOrganizationImportEvent updatedEvent =
        sourceControlOrganizationImportEventDAO.getById(importEvent.getId());
    SourceControlOrganizationImportEvent updatedSecondEvent =
        sourceControlOrganizationImportEventDAO.getById(secondEvent.getId());

    assertThat(updatedEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedEvent.getImportSuccessCount()).isEqualTo(7);
    assertThat(updatedEvent.getImportFailureCount()).isZero();
    ImportFailures importFailures = JsonUtils.parse(updatedEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures.failures).isEmpty();

    assertThat(updatedSecondEvent.getImportStatus()).isEqualTo(ImportStatus.COMPLETE);
    assertThat(updatedSecondEvent.getLastUpdatedTime()).isAfter(updatedEvent.getStartTime());
    assertThat(updatedSecondEvent.getImportSuccessCount()).isEqualTo(6);
    assertThat(updatedSecondEvent.getImportFailureCount()).isZero();
    ImportFailures importFailures2 =
        JsonUtils.parse(updatedSecondEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures2.failures).isEmpty();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).hasSize(6);

    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(3, 2, 2, 2, 2, 2);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(13);
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

    ImportFailures importFailures = JsonUtils.parse(updatedEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures.failures).isEmpty();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());

    assertThat(childOrgsAfterImport).hasSize(3);
    List<Integer> importedAppCountsPerOrg =
        childOrgsAfterImport.stream()
            .map(childOrg -> applicationDAO.getByOrganizationId(childOrg.getId()).size())
            .collect(Collectors.toList());
    assertThat(importedAppCountsPerOrg).containsExactlyInAnyOrder(4, 3, 3);
    assertThat(importedAppCountsPerOrg.stream().mapToInt(i -> i).sum()).isEqualTo(10);
    verifySourceControlEvaluationEventsCreated(10);

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
    assertBatchedImportTelemetries(telemetryData.get(0), 4, 10, prevImportedCount);
    prevImportedCount += 4;
    assertBatchedImportTelemetries(telemetryData.get(1), 3, 10, prevImportedCount);
    prevImportedCount += 3;
    assertBatchedImportTelemetries(telemetryData.get(2), 3, 10, prevImportedCount);
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

    ImportFailures importFailures = JsonUtils.parse(updatedEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures.failures).isEmpty();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(13);
    verifySourceControlEvaluationEventsCreated(13);

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

    ImportFailures importFailures = JsonUtils.parse(updatedEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures.failures).isEmpty();

    List<Organization> childOrgsAfterImport = organizationDAO.getByParentOrganizationId(org.getId());
    assertThat(childOrgsAfterImport).isEmpty();
    assertThat(applicationDAO.getByOrganizationId(org.getId())).hasSize(5);
    verifySourceControlEvaluationEventsCreated(5);

    assertScmImportTelemetries(5, 5);
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

    ImportFailures importFailures = JsonUtils.parse(updatedEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures.failures).isEmpty();

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

    ImportFailures importFailures = JsonUtils.parse(updatedEvent.getImportErrors().getBytes(), ImportFailures.class);
    assertThat(importFailures.failures).isEmpty();

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
  public void testGetImportScmOrganizationStatus_Complete() {
    org = tempEntity.newOrganization();
    SourceControlOrganizationImportEvent event =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), "scmUrl", -1, 0);
    String importErrors = JsonUtils.writeUnformatted(
        new ImportFailures(Arrays.asList(new ImportFailure(newSCMRepository("url", "p", "o"), "error1"))));
    event.setImportErrors(importErrors);
    event.setImportSuccessCount(50);
    event.setImportFailureCount(10);
    event.setLastUpdatedTime(new Date());
    event.setImportStatus(ImportStatus.COMPLETE);
    sourceControlOrganizationImportEventDAO.update(event);

    ImportScmOrganizationStatus importStatus =
        scmOnboardingService.getImportScmOrganizationStatus(org.getId(), event.getId());
    assertThat(importStatus).isNotNull();
    assertThat(importStatus.request.scmHostUrl).isEqualTo("scmUrl");
    assertThat(importStatus.request.importLimit).isEqualTo(-1);
    assertThat(importStatus.request.desiredSubOrganizationCount).isZero();
    assertThat(importStatus.status).isEqualTo(ImportStatus.COMPLETE.toString());
    assertThat(importStatus.importSuccessCount).isEqualTo(50);
    assertThat(importStatus.importFailureCount).isEqualTo(10);
    assertThat(importStatus.startTime).isNotNull();
    assertThat(importStatus.lastUpdatedTime).isNotNull();
    assertThat(importStatus.errors).isEqualTo(importErrors);
  }

  @Test
  public void testGetImportScmOrganizationStatus_InProgress() {
    org = tempEntity.newOrganization();
    SourceControlOrganizationImportEvent event =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), "scmUrl", -1, 0);

    ImportScmOrganizationStatus importStatus =
        scmOnboardingService.getImportScmOrganizationStatus(org.getId(), event.getId());
    assertThat(importStatus).isNotNull();
    assertThat(importStatus.request.scmHostUrl).isEqualTo("scmUrl");
    assertThat(importStatus.request.importLimit).isEqualTo(-1);
    assertThat(importStatus.request.desiredSubOrganizationCount).isZero();
    assertThat(importStatus.status).isEqualTo(ImportStatus.IN_PROGRESS.toString());
    assertThat(importStatus.importSuccessCount).isZero();
    assertThat(importStatus.importFailureCount).isZero();
    assertThat(importStatus.startTime).isNotNull();
    assertThat(importStatus.lastUpdatedTime).isNotNull();
    assertThat(importStatus.errors).isNull();
  }

  @Test
  public void testGetImportScmOrganizationStatus_Error() {
    org = tempEntity.newOrganization();
    SourceControlOrganizationImportEvent event =
        tempEntity.newSourceControlOrganizationImportEvent(org.getId(), "scmUrl", -1, 0);
    event.setImportErrors("runtime exception");
    event.setImportStatus(ImportStatus.ERROR);
    sourceControlOrganizationImportEventDAO.update(event);

    ImportScmOrganizationStatus importStatus =
        scmOnboardingService.getImportScmOrganizationStatus(org.getId(), event.getId());

    assertThat(importStatus).isNotNull();
    assertThat(importStatus.request.scmHostUrl).isEqualTo("scmUrl");
    assertThat(importStatus.request.importLimit).isEqualTo(-1);
    assertThat(importStatus.request.desiredSubOrganizationCount).isZero();
    assertThat(importStatus.status).isEqualTo(ImportStatus.ERROR.toString());
    assertThat(importStatus.errors).isEqualTo("runtime exception");
  }

  @Test(expected = NotFoundException.class)
  public void testGetImportScmOrganizationStatus_NotFound() {
    SourceControlOrganizationImportEvent event = tempEntity.newSourceControlOrganizationImportEvent();
    scmOnboardingService.getImportScmOrganizationStatus("orgId", event.getId());
  }

  @Test
  public void testCreateNewApplication_RetryOnDuplicateName() {
    Application existingApp = tempEntity.newApplication("Pdf Extract - Iq Scm", "pdfextract__IQ-SCM", org.getId());
    SCMRepository scmRepository = newSCMRepository("http://example.com", "pdf-extract", "IQ-SCM");
    String publicId = scmApplicationNameConverter.buildPublicId(scmRepository);
    String name = scmApplicationNameConverter.buildName(scmRepository);
    Application app = scmOnboardingService.createNewApplication(org.getId(), publicId, name);

    assertThat(logOutput).atDebugLevel()
        .contains(
            String.format("Resulted app name %s conflicts with an existing app. Randomizing name and retrying",
                existingApp.getName()));
    assertThat(app.getName().length()).isGreaterThan(existingApp.getName().length());
    assertThat(app.getName()).startsWith(existingApp.getName());
  }

  private SCMRepository newSCMRepository(String scmUrl, String project, String namespace) {
    SCMRepository scmRepository = new SCMRepository();
    scmRepository.setHttpCloneUrl(scmUrl);
    scmRepository.setSshCloneUrl(scmUrl);
    scmRepository.setPrivate(false);
    scmRepository.setDefaultBranch("main");
    scmRepository.setNamespace(namespace);
    scmRepository.setProject(project);
    return scmRepository;
  }

  private void assertScmImportTelemetries(int batchCount, int updatedApps) {
    // calculation mentioned here for clarification only
    int totalRepoCount = 13;
    int prevImportedCount = 0;
    int batchPercent = (int) Math.round(batchCount * 100.0 / totalRepoCount);
    int totalPercent = (int) ((prevImportedCount + batchCount) * 100.0 / totalRepoCount);
    assertTelemetry(batchPercent, batchCount, totalPercent, updatedApps);
  }

  private void testImportScmOrganization_InvalidParameters(
      final String org,
      final ImportScmOrganizationRequest importRequest,
      final String errorMessage)
  {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> scmOnboardingService.importScmOrganization(org, importRequest))
        .withMessageContaining(errorMessage);

    verifyNoSourceControlEvaluationEventsCreated();
  }

  private void testImportRepositories_disabledInternalSourceControlPolicyEvaluations(
      Boolean internalSourceControlPolicyEvaluationsEnabled)
  {
    // given SCM imports are enabled and internal SCM policy evaluations are disabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);
    rootOrgSourceControl.setSourceControlEvaluationsEnabled(internalSourceControlPolicyEvaluationsEnabled);
    sourceControlDAO.update(rootOrgSourceControl);

    String repoUrl = String.format("%s/org/repo", gitService.baseUrl());
    // given a repo to import
    SCMRepository scmRepository =
        new SCMRepository(SourceControlProvider.GITHUB, repoUrl, null, true, "org", "repo", "");
    int totalRepoCount = 10;
    int prevImportedCount = 1;

    // when the repo is imported
    ImportResults response = scmOnboardingService.importRepositories(org.getId(),
        new ImportRepositoriesRequest(Collections.singletonList(scmRepository), totalRepoCount, prevImportedCount));

    // then the repo is imported
    List<SCMRepository> importedSCMRepositories = response.getImportedRepositories();
    assertThat(importedSCMRepositories).hasSize(1);
    SCMRepository importedSCMRepository = importedSCMRepositories.get(0);
    assertThat(importedSCMRepository.getNamespace()).isEqualTo(scmRepository.getNamespace());
    assertThat(importedSCMRepository.getProject()).isEqualTo(scmRepository.getProject());
    assertThat(importedSCMRepository.getHttpCloneUrl()).isEqualTo(scmRepository.getHttpCloneUrl());
    assertThat(importedSCMRepository.getSourceControlProvider()).isEqualTo(scmRepository.getSourceControlProvider());
    assertThat(importedSCMRepository.getDescription()).isEqualTo(scmRepository.getDescription());

    // and the repo exists in the DB
    List<Application> allApps = sourceControlDAO.getAll()
        .stream() //
        .filter(sc -> !sc.getOwnerId().equals(ROOT_ORGANIZATION_ID)) //
        .map(sc -> applicationDAO.getById(sc.getOwnerId())) //
        .collect(Collectors.toList());
    assertThat(allApps).hasSize(1);
    assertThat(allApps.get(0).getPublicId()).isEqualTo("repo__org");
    assertThat(allApps.get(0).getName()).isEqualTo("Repo - Org");

    // and that all of the clone URLs were added
    assertThat(sourceControlDAO.getAll()
        .stream() //
        .filter(sc -> !ROOT_ORGANIZATION_ID.equals(sc.getOwnerId())) //
        .map(SourceControl::getRepositoryUrl)).containsExactly(repoUrl);

    // and source control evaluation request events were not created
    verifyNoSourceControlEvaluationEventsCreated();

    // and the telemetry was sent properly
    int batchPercent = 10;
    int batchCount = 1;
    int totalPercent = (prevImportedCount + batchCount) * 100 / totalRepoCount;
    assertTelemetry(batchPercent, batchCount, totalPercent, batchCount);
  }

  private void verifyNoSourceControlEvaluationEventsCreated() {
    verifySourceControlEvaluationEventsCreated(0);
  }

  private void verifySourceControlEvaluationEventsCreated(int count) {
    if (count > 0) {
      ArgumentCaptor<SourceControlEvent> eventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
      verify(mockSourceControlEventPublisher, times(count)).publishEvent(eventCaptor.capture());
      assertThat(eventCaptor.getAllValues().size()).isEqualTo(count);
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

  private static String extractEventId(final ImportScmOrganizationTicket importTicket) {
    Matcher matcher = STATUS_URL_PATTERN.matcher(importTicket.statusUrl);
    matcher.find();
    return matcher.group("eventId");
  }

  private void assertTelemetry(final int batchPercent, final int batchCount, final int totalPercent, int updateCount) {
    assertTelemetry(batchPercent, batchCount, totalPercent, updateCount, 0);
  }

  private void assertTelemetry(
      final int batchPercent,
      final int batchCount,
      final int totalPercent,
      int updateCount,
      int roleAssignmentCount)
  {
    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(1 + (updateCount * 2) + roleAssignmentCount)).send(
        telemetryDataArgumentCaptor.capture());
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

  private String getResourceContents(final String path) throws IOException {
    final var resource = getClass().getResource(path);
    return FileUtils.readFileToString(new File(resource.getFile()), StandardCharsets.UTF_8);
  }

  private void givenScmMappingEnabledAndSomeIQUsersToMapTo() {
    // given mapping enabled
    tempEntity.createScmUserMappings(
        DEVELOPER_ROLE_ID,
        org.getId(),
        Lists.newArrayList(getMappingForScmUserJsonStorage(SCM_USERNAME.name(), IQ_USERNAME.name())));

    // given some existing users in IQ that will match the scm contributors
    tempEntity.newUser("some-user-1");
    tempEntity.newUser("some-other-user");
  }

  @Test
  public void testImportRepositories_GitHubAppAuth_Success() throws Exception {
    // Given: Organization with GitHub App authentication configured
    Organization testOrg = tempEntity.newOrganization("github-app-org");

    // Create GitHub App using tempEntity helper
    tempEntity.newGitHubApp(testOrg.getId());

    // Configure source control with GitHub App authentication using tempEntity
    SourceControl orgSourceControl = new SourceControl();
    orgSourceControl.setId(tempEntity.uuid());
    orgSourceControl.setOwnerId(testOrg.getId());
    orgSourceControl.setProvider(SourceControlProvider.GITHUB);
    orgSourceControl.setAuthenticationType(AuthenticationType.GITHUB_APP);
    orgSourceControl.setToken(null); // No PAT token for GitHub App
    tempEntity.newSourceControl(orgSourceControl);

    // Enable SCM imports
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // Mock GitHub API responses for repository listing and contributors
    String repo1URL = String.format("%s/github-org/repo1", gitService.baseUrl());
    String repo2URL = String.format("%s/github-org/repo2", gitService.baseUrl());

    gitService.stubFor(get(urlPathEqualTo("/api/v3/installation/repositories"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{\"total_count\": 2, \"repositories\": []}")));

    // Mock repository info endpoint for checking if repos are private
    gitService.stubFor(get(urlPathEqualTo("/api/v3/repos/github-org/repo1"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));

    gitService.stubFor(get(urlPathEqualTo("/api/v3/repos/github-org/repo2"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody("{ \"private\": false }")));

    // Mock contributors endpoint for automatic role assignment
    gitService.stubFor(get(urlPathMatching("/api/v3/repos/github-org/repo[12]"))
        .willReturn(aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .withBody(getResourceContents("/ScmOnboardingServiceTest/contributorsResponse.json"))));

    // Given: List of repositories to import
    SCMRepository[] reposToImport = new SCMRepository[]{
      new SCMRepository(
          SourceControlProvider.GITHUB,
          repo1URL,
          "git@github.com:github-org/repo1.git",
          false,
          "github-org",
          "repo1",
          "Test repository 1"),
      new SCMRepository(
          SourceControlProvider.GITHUB,
          repo2URL,
          "git@github.com:github-org/repo2.git",
          false,
          "github-org",
          "repo2",
          "Test repository 2")
    };

    int totalRepoCount = 50;
    int prevImportedCount = 10;

    givenScmMappingEnabledAndSomeIQUsersToMapTo();

    // When: Repositories are imported using GitHub App authentication
    ImportResults response = scmOnboardingService.importRepositories(
        testOrg.getId(),
        new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount));

    // Then: All repositories should be successfully imported
    List<SCMRepository> imported = response.getImportedRepositories();
    assertThat(imported).hasSize(2);
    assertThat(response.getFailedRepositories()).isEmpty();

    // And: Imported repositories have correct details
    assertThat(imported.get(0).getNamespace()).isEqualTo("github-org");
    assertThat(imported.get(0).getProject()).isEqualTo("repo1");
    assertThat(imported.get(0).getHttpCloneUrl()).isEqualTo(repo1URL);
    assertThat(imported.get(0).getSshCloneUrl()).isEqualTo("git@github.com:github-org/repo1.git");
    assertThat(imported.get(0).getSourceControlProvider()).isEqualTo(SourceControlProvider.GITHUB);
    assertThat(imported.get(0).getDescription()).isEqualTo("Test repository 1");

    assertThat(imported.get(1).getNamespace()).isEqualTo("github-org");
    assertThat(imported.get(1).getProject()).isEqualTo("repo2");
    assertThat(imported.get(1).getHttpCloneUrl()).isEqualTo(repo2URL);
    assertThat(imported.get(1).getDescription()).isEqualTo("Test repository 2");

    // And: Applications are created in the database
    List<Application> apps = applicationDAO.getByOrganizationId(testOrg.getId());
    assertThat(apps).hasSize(2);

    // Verify applications are created with correct display names (title case with dash separator)
    List<String> appNames = apps.stream()
        .map(Application::getName)
        .sorted()
        .collect(Collectors.toList());
    assertThat(appNames).containsExactly("Repo1 - Github Org", "Repo2 - Github Org");

    // Verify application public IDs (lowercase with double underscore)
    List<String> appPublicIds = apps.stream()
        .map(Application::getPublicId)
        .sorted()
        .collect(Collectors.toList());
    assertThat(appPublicIds).containsExactly("repo1__github-org", "repo2__github-org");

    // And: Verify repository URLs are correctly stored
    List<SourceControl> appSourceControls = sourceControlDAO.getAll()
        .stream()
        .filter(sc -> !sc.getOwnerId().equals(ROOT_ORGANIZATION_ID) && !sc.getOwnerId().equals(testOrg.getId()))
        .collect(Collectors.toList());
    assertThat(appSourceControls).hasSize(2);

    // Verify source control details for first repository
    SourceControl repo1SourceControl = appSourceControls.stream()
        .filter(sc -> sc.getRepositoryUrl().equals(repo1URL))
        .findFirst()
        .orElseThrow();
    assertThat(repo1SourceControl.getRepositoryUrl()).isEqualTo(repo1URL);
    assertThat(repo1SourceControl.getRepositorySshUrl()).isEqualTo("git@github.com:github-org/repo1.git");

    // Verify source control details for second repository
    SourceControl repo2SourceControl = appSourceControls.stream()
        .filter(sc -> sc.getRepositoryUrl().equals(repo2URL))
        .findFirst()
        .orElseThrow();
    assertThat(repo2SourceControl.getRepositoryUrl()).isEqualTo(repo2URL);
    assertThat(repo2SourceControl.getRepositorySshUrl()).isEqualTo("git@github.com:github-org/repo2.git");

    // And: Verify the organization has GitHub App authentication configured
    SourceControl verifiedOrgSourceControl = sourceControlDAO.getByOwnerId(testOrg.getId());
    assertThat(verifiedOrgSourceControl).isNotNull();
    assertThat(verifiedOrgSourceControl.getAuthenticationType()).isEqualTo(AuthenticationType.GITHUB_APP);
    assertThat(verifiedOrgSourceControl.getToken()).isNull(); // No PAT token for GitHub App auth

    // And: Telemetry was sent (2 repos x 2 telemetry each + 1 import telemetry = 5 total)
    // - 2 owner maintenance telemetry events (one per application created)
    // - 2 source control telemetry events (one per repository imported)
    // - 1 import telemetry event
    verify(telemetrySenderMock, times(5)).send(any(TelemetryData.class));
  }
}
