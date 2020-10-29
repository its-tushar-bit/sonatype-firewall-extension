/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
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
  @Rule
  public WireMockRule gitService = new WireMockRule(wireMockConfig().dynamicPort());

  @Inject
  private ApiScmOnboardingService apiScmOnboardingService;

  private Application app;

  private Organization org;

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  @Inject
  private PlexusCipher plexusCipher;

  private static final String ENC = "CMMDwoV";

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

    // and given root org is configured for github
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt("TOKEN", ENC), SourceControlProvider.GITHUB);
    // TODO INT-3695 adds default host support, until then prime the pump
    tempEntity.newSourceControl(app.getId(), gitService.baseUrl() + "/org/repo.git", null);

    // then loading repositories returns the expected results
    List<SCMRepository> repositories = apiScmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.size()).isEqualTo(13);
  }
  
  @Test
  public void testFilteringAlreadyConfiguredRepositories() throws Exception {
    // configure urls to point to our mock git server, as these are used to guess at a base api url
    String repo1Url = "https://github.com/depshield-ci/ci-project-1.git";
    String repo2Url = "https://github.com/depshield-ci/ci-project-16.git";
    String repo1ReplacementUrl = gitService.baseUrl() + "/org/repo1.git";
    String repo2ReplacementUrl = gitService.baseUrl() + "/org/repo2.git";
    
    mockRepoForPage(gitService, 0, 
        getResourceAsString("/ApiScmOnboardingServiceTest/allRepos0.json")
        .replaceFirst(repo1Url, repo1ReplacementUrl)
        .replaceFirst(repo2Url, repo2ReplacementUrl)
    );
    mockRepoForPage(gitService, 1, getResourceAsString("/ApiScmOnboardingServiceTest/emptyResponse.json"));

    // given root org is configured for github
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, plexusCipher.encrypt("TOKEN", ENC), SourceControlProvider.GITHUB);

    // given some of the repositories are already configured for SCM
    tempEntity.newSourceControl(app.getId(), repo1ReplacementUrl, new Date());
    Application tmpapp2 = tempEntity.newApplication("tmpapp2", org.getId());
    tempEntity.newSourceControl(tmpapp2.getId(), repo2ReplacementUrl, new Date());
    // duplicate repo url entries as they can be configured as such to scan different modules(a la insight-brain)
    Application tmpapp3 = tempEntity.newApplication("tmpapp3", org.getId());
    tempEntity.newSourceControl(tmpapp3.getId(), repo2ReplacementUrl, new Date());
    
    // then loading repositories returns the trimmed results
    List<SCMRepository> repositories = apiScmOnboardingService.loadRepositories(org.getId(), gitService.baseUrl());
    assertThat(repositories.size()).isEqualTo(11);
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
    // given a root org SCM entry for github
    tempEntity.newSourceControl(org.getParentOrganizationId(), null, null, SourceControlProvider.GITHUB);

    // test a variety of different hosts
    testDefaultHostUrl_repoUrl("http://example.com:8899/owner/app", "http://example.com:8899");
    testDefaultHostUrl_repoUrl("https://example.com:8443/owner/app", "https://example.com:8443");
    testDefaultHostUrl_repoUrl("http://example.com/owner/app", "http://example.com");
    testDefaultHostUrl_repoUrl("http://example.com:80/owner/app", "http://example.com:80");
    testDefaultHostUrl_repoUrl("https://example.com/owner/app", "https://example.com");
    testDefaultHostUrl_repoUrl("https://example.com:443/owner/app", "https://example.com:443");
  }

  private void testDefaultHostUrl_repoUrl(String repoUrl, String expectedDefaultHosturl) {
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
    String defaultHostUrl = apiScmOnboardingService.getDefaultHostUrl("github", organization.getId());

    // then it should be custom, not the github default
    assertThat(defaultHostUrl).isEqualTo(expectedDefaultHosturl);
  }

  @Test
  public void testDefaultHostUrl_otherOrgsWithScm() {
    // given a root org SCM entry for github
    tempEntity.newSourceControl(org.getParentOrganizationId(), null, null, SourceControlProvider.GITHUB);
    // and an app with a custom repo URL
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
}
