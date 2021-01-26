/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.dto.ImportRepositoriesRequest;
import com.sonatype.insight.brain.api.experimental.dto.SCMRepositories;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticSourceControlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.sonatype.plexus.components.cipher.PlexusCipher;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ApiScmOnboardingServiceTest
    extends AbstractComponentTest
{
  private static final String PAGE_0 = "/ApiScmOnboardingServiceTest/allRepos0.json";

  public static final String PAGE_1 = "/ApiScmOnboardingServiceTest/emptyResponse.json";

  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private ApiScmOnboardingService apiScmOnboardingService;

  private Application app;

  private Organization org;

  @Inject
  private SourceControlDAO sourceControlDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private AutomaticSourceControlConfigurationDAO automaticSourceControlConfigurationDAO;

  @Inject
  private PlexusCipher plexusCipher;

  private static final String ENC = "CMMDwoV";

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(final Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Before
  public void setup() throws Exception {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication("tmpapp", org.getId());
    gitService.stubFor(get(urlPathEqualTo("/api/v3/user"))
        .willReturn(aResponse()
        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .withBody("{\"username\":\"foo\"}")));
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt("TOKEN", ENC), SourceControlProvider.GITHUB);
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
    mockRepoForPage(gitService, 0, getResourceAsString(PAGE_0));
    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));

    // then loading repositories returns the expected results
    SCMRepositories repositories = apiScmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.availableRepositories.size()).isEqualTo(13);
  }

  @Test
  public void testLoadRepositories_hierarchyNoToken() throws Exception {
    // given root org with no token
    sourceControlDAO.delete(sourceControlDAO.getByOwnerId(ROOT_ORGANIZATION_ID));
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);

    // and an org with no token either
    tempEntity
        .newSourceControl(org.getId(), null, null, null);

    // then loading repositories fails
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
      apiScmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    }).withMessageContaining("'token' must not be null");
  }

  @Test
  public void testLoadRepositories() throws Exception {
    mockRepoForPage(gitService, 0, getResourceAsString(PAGE_0));
    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));

    // then loading repositories returns the expected results
    SCMRepositories repositories = apiScmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.availableRepositories.size()).isEqualTo(13);
    assertThat(repositories.totalRepositories).isEqualTo(13);
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

    mockRepoForPage(gitService, 0, 
        getResourceAsString(PAGE_0)
        .replaceFirst(repo1Url, repo1ReplacementUrl)
        .replaceFirst(repo2Url, repo2ReplacementUrl)
    );
    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));

    // given some of the repositories are already configured for SCM
    tempEntity.newSourceControl(app.getId(), gitService.baseUrl() + repo1, new Date());
    Application tmpapp2 = tempEntity.newApplication("tmpapp2", org.getId());
    tempEntity.newSourceControl(tmpapp2.getId(), gitService.baseUrl() + repo2, new Date());
    // duplicate repo url entries as they can be configured as such to scan different modules(a la insight-brain)
    Application tmpapp3 = tempEntity.newApplication("tmpapp3", org.getId());
    tempEntity.newSourceControl(tmpapp3.getId(), gitService.baseUrl() + repo2, new Date());
    
    // then loading repositories returns the trimmed results
    SCMRepositories repositories = apiScmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.availableRepositories.size()).isEqualTo(11);
    assertThat(repositories.totalRepositories).isEqualTo(13);
  }

  @Test
  public void testLoadRepositories_sanitizeCloneUrls() throws Exception {
    String page0 = getResourceAsString(PAGE_0);
    mockRepoForPage(gitService, 0, page0);
    mockRepoForPage(gitService, 1, getResourceAsString(PAGE_1));

    // given the raw data contains urls with embedded information
    assertThat(page0).contains("https://admin:admin123@localhost/depshield-ci/create-react-app.git");
    assertThat(page0).contains("https://admin@localhost/sonatype-nexus-community/nexus-repository-p2.git");

    // then the repository listing will strip out this embedded information to ensure it doesn't leak
    SCMRepositories repositories = apiScmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    Optional<SCMRepository> createReactApp = repositories.availableRepositories.stream()
        .filter(repository -> repository.getProject().equals("create-react-app")).findFirst();
    assertThat(createReactApp.get().getHttpCloneUrl()).isEqualTo("https://localhost/depshield-ci/create-react-app");
    Optional<SCMRepository> nxrmP2 = repositories.availableRepositories.stream()
        .filter(repository -> repository.getProject().equals("nexus-repository-p2")).findFirst();
    assertThat(nxrmP2.get().getHttpCloneUrl())
        .isEqualTo("https://localhost/sonatype-nexus-community/nexus-repository-p2");
  }

  @Test
  public void testLoadRepositories_invalidOrgId() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      apiScmOnboardingService.loadRepositories("organizationThatDoesntExist", gitService.baseUrl());
    }).withMessageContaining("Cannot find organization with ID organizationThatDoesntExist.");
  }

  private String getResourceAsString(String filename) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(this.getClass().getResourceAsStream(filename), writer, StandardCharsets.UTF_8);
    return writer.toString();
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
  public void testDefaultHostUrl_noProvider() {
    testNoProvider(null);
    testNoProvider("");
    testNoProvider(" ");
  }

  private void testNoProvider(String provider) {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      apiScmOnboardingService.getDefaultHostUrl(provider, "org-id-not-checked");
    }).withMessageContaining("Provider has not been specified");
  }

  @Test
  public void testDefaultHostUrl_invalidProvider() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      apiScmOnboardingService.getDefaultHostUrl("invalid", "org-id-not-checked");
    }).withMessageContaining("Invalid provider: invalid");
  }

  @Test
  public void testDefaultHostUrl_noOrgId() {
    testDefaultByProvider("github", "https://github.com/");
    testDefaultByProvider("gitlab", "https://gitlab.com/");
    testDefaultByProvider("bitbucket", "https://bitbucket.org/");
  }

  private void testDefaultByProvider(String provider, String expectedUrl) {
    assertThat(apiScmOnboardingService.getDefaultHostUrl(provider, null)).isEqualTo(expectedUrl);
  }

  @Test
  public void testDefaultHostUrl_orgWithScm() {
    // test a variety of different hosts
    testDefaultHostUrl_repoUrlGH("http://example.com:8899/owner/app", "http://example.com:8899");
    testDefaultHostUrl_repoUrlGH("https://example.com:8443/owner/app", "https://example.com:8443");
    testDefaultHostUrl_repoUrlGH("http://example.com/owner/app", "http://example.com");
    testDefaultHostUrl_repoUrlGH("http://example.com:80/owner/app", "http://example.com:80");
    testDefaultHostUrl_repoUrlGH("https://example.com/owner/app", "https://example.com");
    testDefaultHostUrl_repoUrlGH("https://example.com:443/owner/app", "https://example.com:443");
  }

  @Test
  public void testDefaultHostUrl_bitbucketOrgWithScm() {
    // test a variety of different hosts
    testDefaultHostUrl_repoUrlBB("https://localhost:7990/biz/scm/scm/org/project", "https://localhost:7990/biz/scm");
    testDefaultHostUrl_repoUrlBB("https://localhost:7990/scm/biz/scm/org/project", "https://localhost:7990/scm/biz");
    testDefaultHostUrl_repoUrlBB("https://example.com:5000/bitbucket/scm/org/proj",
        "https://example.com:5000/bitbucket");
    testDefaultHostUrl_repoUrlBB("https://example.com/scm/org/proj", "https://example.com");
    testDefaultHostUrl_repoUrlBB("https://bitbucket.org/org/proj", "https://bitbucket.org");
    testDefaultHostUrl_repoUrlBB("https://example.com:443/scm/owner/app", "https://example.com:443");
  }

  private void testDefaultHostUrl_repoUrlGH(String repoUrl, String expectedDefaultHostUrl) {
    testDefaultHostUrl_repoUrl(repoUrl, expectedDefaultHostUrl, SourceControlProvider.GITHUB.name());
  }

  private void testDefaultHostUrl_repoUrlBB(String repoUrl, String expectedDefaultHostUrl) {
    testDefaultHostUrl_repoUrl(repoUrl, expectedDefaultHostUrl, SourceControlProvider.BITBUCKET.name());
  }

  private void testDefaultHostUrl_repoUrl(String repoUrl, String expectedDefaultHosturl, String provider) {
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
    String defaultHostUrl = apiScmOnboardingService.getDefaultHostUrl(provider, organization.getId());

    // then it should be custom, not the github default
    assertThat(defaultHostUrl).isEqualTo(expectedDefaultHosturl);
  }

  @Test
  public void testDefaultHostUrl_otherOrgsWithScm() {
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
    String defaultHostUrl = apiScmOnboardingService.getDefaultHostUrl("github", newApp.getOrganizationId());

    // then it should be the URL defined in the existing org, using the host with the largest count
    assertThat(defaultHostUrl).isEqualTo("http://prefix.example.com");
  }

  @Test
  public void testDefaultHostUrl_orgWithNoScm() {
    // when we get the host URL for an org with no SCM defined
    String defaultHostUrl = apiScmOnboardingService.getDefaultHostUrl("github", org.getId());

    // then it should be the default
    assertThat(defaultHostUrl).isEqualTo("https://github.com/");
  }

  @Test
  public void testImportRepos_allNew() throws Exception {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // given a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
        new SCMRepository(SourceControlProvider.GITHUB, "http://localhost/org/repo1", false, "org", "repo1", ""),
        new SCMRepository(SourceControlProvider.GITHUB, "http://localhost/org/repo2", false, "org", "repo2", ""),
        new SCMRepository(SourceControlProvider.GITHUB, "http://localhost/org/repo3", false, "org", "repo3", ""),
        // use org & app names with IQ app name restrictions
        new SCMRepository(SourceControlProvider.GITHUB, "http://localhost/org/repo4", false,
            "--bad-__-org", "--bad_name_99--", ""),
    };
    int totalRepoCount = 50;
    int prevImportedCount = 10;

    // then the repos can be imported
    List<SCMRepository> imported =
        apiScmOnboardingService
            .importRepositories(org.getId(),
                new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount))
            .getImportedRepositories();
    assertThat(imported).containsExactlyInAnyOrder(reposToImport);

    // and they exist in the DB
    List<Application> allApps = sourceControlDAO.getAll().stream()
        .filter(sc -> sc.getOwnerId() != ROOT_ORGANIZATION_ID)
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org", "repo2__org", "repo3__org", "--bad_name_99--__--bad-__-org");
    assertThat(allApps.stream().map(Application::getName))
        .containsExactlyInAnyOrder("Repo1 - Org", "Repo2 - Org", "Repo3 - Org", "Bad_name_99 - Bad __ Org");

    // and that all of the clone URLs were added
    assertThat(sourceControlDAO.getAll().stream()
        .filter(sc -> sc.getOwnerId() != ROOT_ORGANIZATION_ID)
        .map(SourceControl::getRepositoryUrl)).containsExactly("http://localhost/org/repo1",
        "http://localhost/org/repo2", "http://localhost/org/repo3", "http://localhost/org/repo4");

    // and the telemetry was sent properly
    int batchPercent = 8;
    int batchCount = reposToImport.length;
    int totalPercent = (int)((prevImportedCount + batchCount) * 100.0 / totalRepoCount);
    assertTelemetry(batchPercent, batchCount, totalPercent, batchCount);
  }

  @Test
  public void testImportRepos_existingApp() throws Exception {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // given an existing application which will match a repo which we'll import
    tempEntity.newApplication("repo1__org", org.getId());

    // and a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
        new SCMRepository(SourceControlProvider.GITHUB, "http://localhost/org/repo1", false, "org", "repo1", "")
    };
    int totalRepoCount = 50;
    int prevImportedCount = 8;

    // then the repos can be imported
    List<SCMRepository> imported =
        apiScmOnboardingService
            .importRepositories(org.getId(),
                new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount))
            .getImportedRepositories();
    assertThat(imported).containsExactlyInAnyOrder(reposToImport);

    // and the only apps that are present are the ones for our selected repos
    List<Application> allApps = applicationDAO.getAll();
    assertThat(allApps.stream().map(Application::getOrganizationId).distinct().collect(Collectors.toList()))
        .containsExactlyInAnyOrder(org.getId());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org", app.getPublicId());

    // and the source control entries was created
    List<Application> allSourceControlApps = sourceControlDAO.getAll().stream()
        .filter(sc -> sc.getOwnerId() != ROOT_ORGANIZATION_ID)
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allSourceControlApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org");

    // and the telemetry was sent properly indicating no items were imported
    int batchPercent = reposToImport.length * 2;
    int batchCount = reposToImport.length;
    int totalPercent = (int)((prevImportedCount + batchCount) * 100.0 / totalRepoCount);
    assertTelemetry(batchPercent, batchCount, totalPercent, reposToImport.length);
  }

  @Test
  public void testImportRepos_existingSourceControl() {
    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // given an existing application with a Source Control entry that matches one we'll import
    Application targetApp = tempEntity.newApplication("repo1__org", org.getId());
    tempEntity.newSourceControl(targetApp.getId(), "http://localhost/org/repo1", new Date());

    // and a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
        new SCMRepository(SourceControlProvider.GITHUB, "http://localhost/org/repo1", false, "org", "repo1", "")
    };
    int totalRepoCount = 50;
    int prevImportedCount = 8;

    // then the repos can be imported
    List<SCMRepository> imported =
        apiScmOnboardingService
            .importRepositories(org.getId(),
                new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount))
            .getImportedRepositories();
    assertThat(imported).containsExactlyInAnyOrder(reposToImport);

    // and the only apps that are present are the ones for our selected repos
    List<Application> allApps = applicationDAO.getAll();
    assertThat(allApps.stream().map(Application::getOrganizationId).distinct().collect(Collectors.toList()))
        .containsExactlyInAnyOrder(org.getId());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org", app.getPublicId());

    // and all of the source control entries were created
    List<Application> allSourceControlApps = sourceControlDAO.getAll().stream()
        .filter(sc -> sc.getOwnerId() != ROOT_ORGANIZATION_ID)
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allSourceControlApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("repo1__org");

    // and the telemetry was sent properly indicating no items were imported
    int batchPercent = reposToImport.length * 2;
    int batchCount = reposToImport.length;
    // note that the totalPercent goes up (because we return the repo that we attempted to import but
    // which was not changed so that the UI can remove it) but updatedApps is 0 (because no actual
    // DB changes were made)
    int totalPercent = (int)((prevImportedCount + batchCount) * 100.0 / totalRepoCount);
    int updatedApps = 0;
    assertTelemetry(batchPercent, batchCount, totalPercent, updatedApps);
  }

  @Test(expected = BadRequestException.class)
  public void testImportRepos_nullScmRepos() {
    // when import with null scm repos, it throws an exception
    apiScmOnboardingService.importRepositories(org.getId(), new ImportRepositoriesRequest());
  }

  @Test
  public void testImportRepos_invalidBatchParams_zeroTotalRepoCount() {
    // given invalid totalRepoCount
    int totalRepoCount = 0;

    // given arbitrary prevImportedCount
    int prevImportedCount = 5;

    // given SCM imports are enabled
    automaticSourceControlConfigurationDAO.setSourceControlConfigurationEnabled(true);

    // given a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
        new SCMRepository(SourceControlProvider.GITHUB, "http://localhost/org/repo1", false, "org", "repo1", "")
        };

    // and we call import
    apiScmOnboardingService
        .importRepositories(org.getId(),
            new ImportRepositoriesRequest(Arrays.asList(reposToImport), totalRepoCount, prevImportedCount))
        .getImportedRepositories();

    // and the telemetry was sent properly
    int batchCount = reposToImport.length;

    final ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock, times(2)).send(telemetryDataArgumentCaptor.capture());
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
  public void testValidation() {
    // expect null response when no error is found
    assertThat(apiScmOnboardingService.validateScmHostUrl("github", "http://example.com/").isValid).isTrue();

    // expect provider to be case insensitive
    assertThat(apiScmOnboardingService.validateScmHostUrl("GiThUb", "http://example.com/").isValid).isTrue();

    // expect server side parsing error messages
    assertThat(apiScmOnboardingService.validateScmHostUrl("github", "http://example.com/ ").errorMessages)
        .isEqualTo(singletonList("Unable to parse repository URL: java.net.URISyntaxException: Illegal character in " +
            "path at index 19: http://example.com/ "));

    // expect provider to be case insensitive
    assertThat(apiScmOnboardingService.validateScmHostUrl("invalid", "http://example.com/").errorMessages)
        .isEqualTo(singletonList("Invalid SCM provider."));
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
