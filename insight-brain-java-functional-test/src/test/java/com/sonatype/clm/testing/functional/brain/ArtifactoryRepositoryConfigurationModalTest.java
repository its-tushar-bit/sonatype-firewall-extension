/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Collections;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ArtifactoryRepositoryBaseConfigurationsPage;
import com.sonatype.clm.testing.functional.pages.ArtifactoryRepositoryBaseConfigurationsPage.ArtifactoryConnectionRow;
import com.sonatype.clm.testing.functional.pages.ArtifactoryRepositoryConfigurationModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.artifactory.ArtifactoryClient;
import com.sonatype.insight.brain.artifactory.client.ArtifactoryQueryLanguageUtils;
import com.sonatype.insight.brain.artifactory.client.ChecksumType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.artifactory.ArtifactoryConnectionDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.PasswordHandler;

import com.codeborne.selenide.Condition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.artifactory.ArtifactoryClient.ARTIFACTORY_ID_HEADER_NAME;
import static com.sonatype.insight.brain.artifactory.ArtifactoryClient.CHECKSUM_SEARCH_PATH;
import static com.sonatype.insight.brain.artifactory.ArtifactoryClient.TEST_SHA256;
import static com.sonatype.insight.brain.artifactory.ArtifactoryMockServerRule.ARTIFACTORY_ID_HEADER_MOCK_VALUE;
import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactoryRepositoryConfigurationModalTest
    extends AbstractFunctionalTest
{
  @Rule
  public WireMockRule artifactoryMockSever = new WireMockRule(wireMockConfig().dynamicPort());

  private PasswordHandler passwordHandler;

  private Organization org;

  private ArtifactoryConnectionDAO artifactoryConnectionDAO;

  private OrganizationDAO organizationDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    artifactoryConnectionDAO = lookup(ArtifactoryConnectionDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);

    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    passwordHandler = testCLMServer.getCLMServer().getInstance(PasswordHandler.class);

    org = tempEntity.newOrganization();
    org.setArtifactoryConnectionEnabled(true);
    org.setAllowArtifactoryConnectionOverride(true);
    organizationDAO.update(org);
  }

  @After
  public void after() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(false);
  }

  private ArtifactoryRepositoryBaseConfigurationsPage visitPage() {
    refreshOrOpen(ArtifactoryRepositoryBaseConfigurationsPage.url(org.getType().toString(), org.getId()));
    ArtifactoryRepositoryBaseConfigurationsPage page = new ArtifactoryRepositoryBaseConfigurationsPage();
    page.shouldBe(visible);
    return page;
  }

  @Test
  public void testInitialState_Add() {
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();
    page.add().click();
    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();
    modal.baseUrl().input().shouldBe(empty);
    modal.allowAnonymousAccess().shouldBe(selected);
    modal.test().shouldHave(Condition.cssClass("disabled"));
    modal.authentication().shouldNotBe(visible);
  }

  @Test
  public void testInitialState_Edit_WithAnonymous() {
    ArtifactoryConnection artifactoryConnection =
        tempEntity.newArtifactoryConnection(org.getId(), artifactoryMockSever.baseUrl(), null, null);

    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    ArtifactoryConnectionRow artifactoryConnectionRow = page.row(artifactoryConnection.getId()).shouldBe(visible);

    artifactoryConnectionRow.edit().click();
    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();
    modal.baseUrl().input().shouldHave(value(artifactoryConnection.getBaseUrl()));
    modal.allowAnonymousAccess().shouldBe(selected);
    modal.test().shouldNotHave(Condition.cssClass("disabled"));
    modal.authentication().shouldNotBe(visible);
  }

  @Test
  public void testInitialState_Edit_WithCredentials() {
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        org.getId(),
        artifactoryMockSever.baseUrl(),
        "username", passwordHandler.encryptPassword("password".toCharArray()));

    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    ArtifactoryConnectionRow artifactoryConnectionRow = page.row(artifactoryConnection.getId());

    artifactoryConnectionRow.edit().click();

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    modal.baseUrl().input().shouldHave(value(artifactoryConnection.getBaseUrl()));
    modal.enterUsernameAndPassword().shouldBe(selected);
    modal.test().shouldNotHave(Condition.cssClass("disabled"));
    modal.authentication().shouldBe(visible);
    modal.username().input().shouldHave(value(artifactoryConnection.getUsername()));
    modal.password().input().shouldNotBe(empty);
    modal.password().input().shouldNotBe(value("password"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSave_Add_WithAnonymous() {
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(artifactoryMockSever.baseUrl());

    modal.save().shouldBe(Condition.enabled).click();

    visitPage();

    assertOwnerArtifactoryConnection(artifactoryMockSever.baseUrl(), null, null);
  }

  @Test
  public void testSave_Add_WithCredentials() {
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(artifactoryMockSever.baseUrl());
    modal.enterUsernameAndPassword().click();
    modal.username().input().setValue("username");
    modal.password().input().setValue("password");
    modal.save().shouldBe(Condition.enabled).click();

    visitPage();

    assertOwnerArtifactoryConnection(artifactoryMockSever.baseUrl(), "username", "password");
  }

  private void assertOwnerArtifactoryConnection(
      String expectedBaseUrl,
      String expectedUsername,
      String expectedPassword)
  {
    ArtifactoryConnection artifactoryConnection = artifactoryConnectionDAO.getByOwnerId(org.getId());
    assertThat(artifactoryConnection).isNotNull();
    assertArtifactoryConnection(artifactoryConnection, expectedBaseUrl, expectedUsername, expectedPassword);
  }

  private void assertArtifactoryConnection(
      ArtifactoryConnection artifactoryConnection,
      String expectedBaseUrl,
      String expectedUsername,
      String expectedPassword)
  {
    assertThat(artifactoryConnection).isNotNull();
    assertThat(artifactoryConnection.getId()).isNotBlank();
    assertThat(artifactoryConnection.getBaseUrl()).isEqualTo(expectedBaseUrl);
    assertThat(artifactoryConnection.getUsername()).isEqualTo(expectedUsername);
    assertThat(artifactoryConnection.getPassword() == null
        ? null
        : String.valueOf(
            passwordHandler.decryptPassword(artifactoryConnection.getPassword()))).isEqualTo(expectedPassword);
  }

  @Test
  public void testSave_Failure() {
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(artifactoryMockSever.baseUrl());

    tempEntity.newArtifactoryConnection(
        org.getId(),
        artifactoryMockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    modal.save().shouldBe(Condition.enabled).click();

    modal.getElement()
        .find(".nx-alert--error")
        .shouldBe(visible)
        .shouldHave(Condition.text("An error occurred saving data."));
  }

  @Test
  public void testTest_Add_Success() {
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(artifactoryMockSever.baseUrl());

    artifactoryMockSever.stubFor(get(urlPathMatching(CHECKSUM_SEARCH_PATH))
        .withQueryParam("sha256", equalTo(TEST_SHA256))
        .willReturn(aResponse()
            .withHeader(ARTIFACTORY_ID_HEADER_NAME, ARTIFACTORY_ID_HEADER_MOCK_VALUE)
            .withStatus(200)));

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--success")
        .shouldBe(visible)
        .shouldHave(Condition.text("Repository configuration test successful."));
  }

  @Test
  public void testTest_Add_Failure() {
    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(artifactoryMockSever.baseUrl());

    artifactoryMockSever.stubFor(get(urlPathMatching(CHECKSUM_SEARCH_PATH)).willReturn(
        aResponse().withHeader(ARTIFACTORY_ID_HEADER_NAME, ARTIFACTORY_ID_HEADER_MOCK_VALUE)
            .withStatus(404)));

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--error")
        .shouldBe(visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));
  }

  @Test
  public void testTest_Edit_Unchanged_Success() {
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        org.getId(),
        artifactoryMockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    ArtifactoryConnectionRow artifactoryConnectionRow = page.row(artifactoryConnection.getId());

    artifactoryConnectionRow.edit().click();

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    artifactoryMockSever.stubFor(post(urlPathMatching(ArtifactoryClient.AQL_SEARCH_PATH))
        .withBasicAuth(artifactoryConnection.getUsername(),
            String.valueOf(passwordHandler.decryptPassword(artifactoryConnection.getPassword())))
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(ArtifactoryClient.TEST_SHA256),
                    Collections.emptySet())))
        .willReturn(
            aResponse().withHeader(ARTIFACTORY_ID_HEADER_NAME, ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withStatus(200)));

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--success")
        .shouldBe(visible)
        .shouldHave(Condition.text("Repository configuration test successful."));
  }

  @Test
  public void testTest_Edit_Unchanged_Failure() {
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        org.getId(),
        artifactoryMockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    ArtifactoryConnectionRow artifactoryConnectionRow = page.row(artifactoryConnection.getId());

    artifactoryConnectionRow.edit().click();

    artifactoryMockSever.stubFor(post(urlPathMatching(ArtifactoryClient.AQL_SEARCH_PATH))
        .withBasicAuth(artifactoryConnection.getUsername(),
            String.valueOf(passwordHandler.decryptPassword(artifactoryConnection.getPassword())))
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(ArtifactoryClient.TEST_SHA256),
                    Collections.emptySet())))
        .willReturn(
            aResponse().withHeader(ARTIFACTORY_ID_HEADER_NAME, ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withStatus(404)));

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--error")
        .shouldBe(visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));
  }

  @Test
  public void testTest_Edit_Changed_Success() {
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        org.getId(),
        artifactoryMockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    ArtifactoryConnectionRow artifactoryConnectionRow = page.row(artifactoryConnection.getId());

    artifactoryConnectionRow.edit().click();

    artifactoryMockSever.stubFor(post(urlPathMatching(ArtifactoryClient.AQL_SEARCH_PATH))
        .withBasicAuth(artifactoryConnection.getUsername(),
            String.valueOf(passwordHandler.decryptPassword(artifactoryConnection.getPassword())))
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(ArtifactoryClient.TEST_SHA256),
                    Collections.emptySet())))
        .willReturn(
            aResponse().withHeader(ARTIFACTORY_ID_HEADER_NAME, ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withStatus(200)));

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--success")
        .shouldBe(visible)
        .shouldHave(Condition.text("Repository configuration test successful."));
  }

  @Test
  public void testTest_Edit_Changed_Failure() {
    ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(
        org.getId(),
        artifactoryMockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    ArtifactoryRepositoryBaseConfigurationsPage page = visitPage();

    ArtifactoryConnectionRow artifactoryConnectionRow = page.row(artifactoryConnection.getId());

    artifactoryConnectionRow.edit().click();

    artifactoryMockSever.stubFor(post(urlPathMatching(ArtifactoryClient.AQL_SEARCH_PATH))
        .withBasicAuth(artifactoryConnection.getUsername(),
            String.valueOf(passwordHandler.decryptPassword(artifactoryConnection.getPassword())))
        .withRequestBody(
            equalTo(
                ArtifactoryQueryLanguageUtils.createChecksumSearch(
                    ChecksumType.SHA256,
                    Collections.singleton(ArtifactoryClient.TEST_SHA256),
                    Collections.emptySet())))
        .willReturn(
            aResponse().withHeader(ARTIFACTORY_ID_HEADER_NAME, ARTIFACTORY_ID_HEADER_MOCK_VALUE)
                .withStatus(404)));

    ArtifactoryRepositoryConfigurationModal modal = new ArtifactoryRepositoryConfigurationModal();

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--error")
        .shouldBe(visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));
  }
}
