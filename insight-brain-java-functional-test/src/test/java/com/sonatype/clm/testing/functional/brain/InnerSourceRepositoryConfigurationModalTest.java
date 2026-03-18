/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.InnerSourceRepositoryBaseConfigurationsPage;
import com.sonatype.clm.testing.functional.pages.InnerSourceRepositoryBaseConfigurationsPage.RepositoryConnectionRow;
import com.sonatype.clm.testing.functional.pages.InnerSourceRepositoryConfigurationModal;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.repository.client.NexusRepository3Client;
import com.sonatype.insight.brain.security.PasswordHandler;

import com.codeborne.selenide.Condition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.visible;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.repository.client.NexusRepository3Client.NXRM_STATUS_RESOURCE;
import static org.assertj.core.api.Assertions.assertThat;

public class InnerSourceRepositoryConfigurationModalTest
    extends AbstractFunctionalTest
{
  @Rule
  public WireMockRule nxrm3MockSever = new WireMockRule(wireMockConfig().dynamicPort());

  public static final String NXRM_VERSION_HEADER_MOCK_VALUE = "Nexus/3.37.3-02 (PRO)";

  private RepositoryConnectionDAO repositoryConnectionDAO;

  private PasswordHandler passwordHandler;

  private Organization org;

  private OrganizationDAO organizationDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    repositoryConnectionDAO = lookup(RepositoryConnectionDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    passwordHandler = lookup(PasswordHandler.class);

    org = tempEntity.newOrganization();
    org.setRepositoryConnectionEnabled(true);
    org.setAllowRepositoryConnectionOverride(true);
    organizationDAO.update(org);
  }

  private InnerSourceRepositoryBaseConfigurationsPage visitPage() {
    refreshOrOpen(InnerSourceRepositoryBaseConfigurationsPage.url(org.getType().toString(), org.getId()));
    InnerSourceRepositoryBaseConfigurationsPage page = new InnerSourceRepositoryBaseConfigurationsPage();
    page.shouldBe(visible);
    return page;
  }

  @Test
  public void testInitialState_Add() {
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();
    page.add().click();
    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();
    modal.format().getElement().getSelectedOption().shouldHave(Condition.value("generic"));
    modal.baseUrl().input().shouldBe(empty);
    modal.allowAnonymousAccess().shouldBe(selected);
    modal.test().shouldHave(cssClass("disabled"));
    modal.save().shouldBe(visible);
    modal.authentication().shouldNotBe(visible);
  }

  @Test
  public void testInitialState_Edit_WithAnonymous() {
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(org.getId(), nxrm3MockSever.baseUrl(), null, null);

    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    RepositoryConnectionRow repositoryConnectionRow = page.row(repositoryConnection.getId()).shouldBe(visible);

    repositoryConnectionRow.edit().click();
    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();
    modal.format().getElement().getSelectedOption().shouldHave(Condition.value("generic"));
    modal.baseUrl().input().shouldHave(Condition.value(repositoryConnection.getBaseUrl()));
    modal.allowAnonymousAccess().shouldBe(selected);
    modal.test().shouldNotHave(cssClass("disabled"));
    modal.save().shouldBe(visible);
    modal.authentication().shouldNotBe(visible);
  }

  @Test
  public void testInitialState_Edit_WithCredentials() {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(
        org.getId(),
        nxrm3MockSever.baseUrl(),
        "username", passwordHandler.encryptPassword("password".toCharArray()));

    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    RepositoryConnectionRow repositoryConnectionRow = page.row(repositoryConnection.getId());

    repositoryConnectionRow.edit().click();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.format().getElement().getSelectedOption().shouldHave(Condition.value("generic"));
    modal.baseUrl().input().shouldHave(Condition.value(repositoryConnection.getBaseUrl()));
    modal.enterUsernameAndPassword().shouldBe(selected);
    modal.test().shouldNotHave(cssClass("disabled"));
    modal.save().shouldBe(visible);
    modal.authentication().shouldBe(visible);
    modal.username().input().shouldHave(Condition.value(repositoryConnection.getUsername()));
    modal.password().input().shouldNotBe(empty);
    modal.password().input().shouldNotBe(Condition.value("password"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSave_Add_WithAnonymous() {
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(nxrm3MockSever.baseUrl());

    modal.save().shouldBe(Condition.enabled).click();

    visitPage();

    assertOwnerRepositoryConnection(RepositoryFormat.GENERIC, nxrm3MockSever.baseUrl(), null, null);
  }

  @Test
  public void testSave_Add_WithCredentials() {
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    modal.enterUsernameAndPassword().click();
    modal.username().input().setValue("username");
    modal.password().input().setValue("password");
    modal.save().shouldBe(Condition.enabled).click();

    visitPage();

    assertOwnerRepositoryConnection(RepositoryFormat.GENERIC, nxrm3MockSever.baseUrl(), "username", "password");
  }

  private void assertOwnerRepositoryConnection(
      RepositoryFormat expectedFormat,
      String expectedBaseUrl,
      String expectedUsername,
      String expectedPassword)
  {
    List<RepositoryConnection> repositoryConnections = repositoryConnectionDAO.getByOwnerId(org.getId());
    assertThat(repositoryConnections).hasSize(1);
    assertRepositoryConnection(repositoryConnections.get(0), expectedFormat, expectedBaseUrl, expectedUsername,
        expectedPassword);
  }

  private void assertRepositoryConnection(
      RepositoryConnection repositoryConnection,
      RepositoryFormat expectedFormat,
      String expectedBaseUrl,
      String expectedUsername,
      String expectedPassword)
  {
    assertThat(repositoryConnection).isNotNull();
    assertThat(repositoryConnection.getId()).isNotBlank();
    assertThat(repositoryConnection.getFormat()).isEqualTo(expectedFormat);
    assertThat(repositoryConnection.getBaseUrl()).isEqualTo(expectedBaseUrl);
    assertThat(repositoryConnection.getUsername()).isEqualTo(expectedUsername);
    assertThat(repositoryConnection.getPassword() == null
        ? null
        : String.valueOf(
            passwordHandler.decryptPassword(repositoryConnection.getPassword()))).isEqualTo(expectedPassword);
  }

  @Test
  public void testSave_AllFormats() {
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    modal.save().shouldBe(Condition.enabled).click();

    page = visitPage();
    page.add().click();

    modal.format().chooseOption(modal.maven());
    modal.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    modal.save().shouldBe(Condition.enabled).click();

    page = visitPage();
    page.add().click();

    modal.format().chooseOption(modal.npm());
    modal.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    modal.save().shouldBe(Condition.enabled).click();

    visitPage();

    List<RepositoryConnection> repositoryConnections =
        repositoryConnectionDAO.getByOwnerId(org.getId());
    assertThat(repositoryConnections).hasSize(3);
    RepositoryConnection generic = repositoryConnections.stream()
        .filter(r -> r.getFormat().equals(RepositoryFormat.GENERIC))
        .findFirst()
        .orElse(null);
    RepositoryConnection maven = repositoryConnections.stream()
        .filter(r -> r.getFormat().equals(RepositoryFormat.MAVEN))
        .findFirst()
        .orElse(null);
    RepositoryConnection npm = repositoryConnections.stream()
        .filter(r -> r.getFormat().equals(RepositoryFormat.NPM))
        .findFirst()
        .orElse(null);
    assertRepositoryConnection(generic, RepositoryFormat.GENERIC, nxrm3MockSever.baseUrl(), null, null);
    assertRepositoryConnection(maven, RepositoryFormat.MAVEN, nxrm3MockSever.baseUrl(), null, null);
    assertRepositoryConnection(npm, RepositoryFormat.NPM, nxrm3MockSever.baseUrl(), null, null);
  }

  @Test
  public void testSave_Failure() {
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(nxrm3MockSever.baseUrl());

    tempEntity.newRepositoryConnection(
        org.getId(),
        nxrm3MockSever.baseUrl(),
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
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(nxrm3MockSever.baseUrl());

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(
        aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(200)));

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--success")
        .shouldBe(visible)
        .shouldHave(Condition.text("Repository configuration test successful."));
  }

  @Test
  public void testTest_Add_Failure() {
    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    page.add().click();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(
        aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(404)));

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--error")
        .shouldBe(visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));
  }

  @Test
  public void testTest_Edit_Unchanged_Success() {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(
        org.getId(),
        nxrm3MockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    RepositoryConnectionRow repositoryConnectionRow = page.row(repositoryConnection.getId());

    repositoryConnectionRow.edit().click();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(
        aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(200)));
    modal.test().click();

    modal.getElement()
        .find(".nx-alert--success")
        .shouldBe(visible)
        .shouldHave(Condition.text("Repository configuration test successful."));
  }

  @Test
  public void testTest_Edit_Unchanged_Failure() {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(
        org.getId(),
        nxrm3MockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    RepositoryConnectionRow repositoryConnectionRow = page.row(repositoryConnection.getId());

    repositoryConnectionRow.edit().click();

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(
        aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(404)));

    modal.test().click();

    modal.getElement()
        .find(".nx-alert--error")
        .shouldBe(visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));
  }

  @Test
  public void testTest_Edit_Changed_Success() {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(
        org.getId(),
        nxrm3MockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    RepositoryConnectionRow repositoryConnectionRow = page.row(repositoryConnection.getId());

    repositoryConnectionRow.edit().click();

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(
        aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(200)));

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.format().chooseOption(modal.maven());
    modal.test().click();

    modal.getElement()
        .find(".nx-alert--success")
        .shouldBe(visible)
        .shouldHave(Condition.text("Repository configuration test successful."));
  }

  @Test
  public void testTest_Edit_Changed_Failure() {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(
        org.getId(),
        nxrm3MockSever.baseUrl(),
        "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    InnerSourceRepositoryBaseConfigurationsPage page = visitPage();

    RepositoryConnectionRow repositoryConnectionRow = page.row(repositoryConnection.getId());

    repositoryConnectionRow.edit().click();

    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(
        aResponse().withHeader(NexusRepository3Client.NXRM_VERSION_HEADER_NAME,
            NXRM_VERSION_HEADER_MOCK_VALUE).withStatus(404)));

    InnerSourceRepositoryConfigurationModal modal = new InnerSourceRepositoryConfigurationModal();

    modal.format().chooseOption(modal.maven());
    modal.test().click();

    modal.getElement()
        .find(".nx-alert--error")
        .shouldBe(visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));
  }
}
