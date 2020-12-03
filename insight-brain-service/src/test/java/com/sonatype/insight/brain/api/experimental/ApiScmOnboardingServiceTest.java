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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.experimental.dto.SCMRepositories;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.sonatype.plexus.components.cipher.PlexusCipher;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
  private PlexusCipher plexusCipher;

  private static final String ENC = "CMMDwoV";

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
    String repo1Url = "https://github.com/depshield-ci/ci-project-1.git";
    String repo2Url = "https://github.com/depshield-ci/ci-project-16.git";
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
    assertThat(page0).contains("https://admin:admin123@github.com/depshield-ci/create-react-app.git");
    assertThat(page0).contains("https://admin@github.com/sonatype-nexus-community/nexus-repository-p2.git");

    // then the repository listing will strip out this embedded information to ensure it doesn't leak
    SCMRepositories repositories = apiScmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    Optional<SCMRepository> createReactApp = repositories.availableRepositories.stream()
        .filter(repository -> repository.getProject().equals("create-react-app")).findFirst();
    assertThat(createReactApp.get().getHttpCloneUrl()).isEqualTo("https://github.com/depshield-ci/create-react-app");
    Optional<SCMRepository> nxrmP2 = repositories.availableRepositories.stream()
        .filter(repository -> repository.getProject().equals("nexus-repository-p2")).findFirst();
    assertThat(nxrmP2.get().getHttpCloneUrl())
        .isEqualTo("https://github.com/sonatype-nexus-community/nexus-repository-p2");
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
    // given a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
        new SCMRepository(SourceControlProvider.GITHUB, "http://github.com/org/repo1", false, "org", "repo1", ""),
        new SCMRepository(SourceControlProvider.GITHUB, "http://github.com/org/repo2", false, "org", "repo2", ""),
        new SCMRepository(SourceControlProvider.GITHUB, "http://github.com/org/repo3", false, "org", "repo3", "")
    };

    // then the repos can be imported
    List<SCMRepository> imported =
        apiScmOnboardingService.importRepositories(org.getId(), Arrays.asList(reposToImport));
    assertThat(imported).containsExactlyInAnyOrder(reposToImport);

    // and they exist in the DB
    List<Application> allApps = sourceControlDAO.getAll().stream()
        .filter(sc -> sc.getOwnerId() != ROOT_ORGANIZATION_ID)
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("org__repo1", "org__repo2", "org__repo3");
    assertThat(allApps.stream().map(Application::getName))
        .containsExactlyInAnyOrder("Org - Repo1", "Org - Repo2", "Org - Repo3");
  }

  @Test
  public void testImportRepos_existingApp() throws Exception {
    // given an existing application which will match a repo which we'll import
    tempEntity.newApplication("org__repo1", org.getId());

    // and a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
        new SCMRepository(SourceControlProvider.GITHUB, "http://github.com/org/repo1", false, "org", "repo1", "")
    };

    // then the repos can be imported
    List<SCMRepository> imported =
        apiScmOnboardingService.importRepositories(org.getId(), Arrays.asList(reposToImport));
    assertThat(imported).containsExactlyInAnyOrder(reposToImport);

    // and the only apps that are present are the ones for our selected repos
    List<Application> allApps = applicationDAO.getAll();
    assertThat(allApps.stream().map(Application::getOrganizationId).distinct().collect(Collectors.toList()))
        .containsExactlyInAnyOrder(org.getId());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("org__repo1", app.getPublicId());

    // and the source control entries was created
    List<Application> allSourceControlApps = sourceControlDAO.getAll().stream()
        .filter(sc -> sc.getOwnerId() != ROOT_ORGANIZATION_ID)
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allSourceControlApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("org__repo1");
  }

  @Test
  public void testImportRepos_existingSourceControl() {
    // given an existing application with a Source Control entry that matches one we'll import
    Application targetApp = tempEntity.newApplication("org__repo1", org.getId());
    tempEntity.newSourceControl(targetApp.getId(), "http://github.com/org/repo1", new Date());

    // and a list of repos to import
    SCMRepository[] reposToImport = new SCMRepository[]{
        new SCMRepository(SourceControlProvider.GITHUB, "http://github.com/org/repo1", false, "org", "repo1", "")
    };

    // then the repos can be imported
    List<SCMRepository> imported =
        apiScmOnboardingService.importRepositories(org.getId(), Arrays.asList(reposToImport));
    assertThat(imported).containsExactlyInAnyOrder(reposToImport);

    // and the only apps that are present are the ones for our selected repos
    List<Application> allApps = applicationDAO.getAll();
    assertThat(allApps.stream().map(Application::getOrganizationId).distinct().collect(Collectors.toList()))
        .containsExactlyInAnyOrder(org.getId());
    assertThat(allApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("org__repo1", app.getPublicId());

    // and all of the source control entries were created
    List<Application> allSourceControlApps = sourceControlDAO.getAll().stream()
        .filter(sc -> sc.getOwnerId() != ROOT_ORGANIZATION_ID)
        .map(sc -> applicationDAO.getById(sc.getOwnerId()))
        .collect(Collectors.toList());
    assertThat(allSourceControlApps.stream().map(Application::getPublicId))
        .containsExactlyInAnyOrder("org__repo1");
  }
}
