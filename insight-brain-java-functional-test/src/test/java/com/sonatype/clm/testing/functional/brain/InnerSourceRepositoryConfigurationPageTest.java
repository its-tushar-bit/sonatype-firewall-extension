/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.InnerSourceRepositoryConfigurationPage;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryConnectionDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import com.codeborne.selenide.Condition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonatype.insight.brain.repository.client.NexusRepository3Client.NXRM_STATUS_RESOURCE;
import static org.assertj.core.api.Assertions.assertThat;

public class InnerSourceRepositoryConfigurationPageTest
    extends AbstractFunctionalTest
{
  @Rule
  public WireMockRule nxrm3MockSever = new WireMockRule(wireMockConfig().dynamicPort());

  private final RepositoryConnectionDAO repositoryConnectionDAO = new RepositoryConnectionDAO();

  private PasswordHandler passwordHandler;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    testCLMServer.getCLMServer().getConfiguration().setExperimentalFeatures(
        ImmutableMap.of(ExperimentalFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.getFlag(), true));
    passwordHandler = testCLMServer.getCLMServer().getInstance(PasswordHandler.class);
  }

  @After
  public void after() {
    repositoryConnectionDAO.deleteAll();
  }

  @Test
  public void testInitialState_Add() {
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);

    page.format().getElement().getSelectedOption().shouldHave(Condition.value("generic"));
    page.baseUrl().input().shouldBe(Condition.empty);
    page.allowAnonymousAccess().shouldBe(Condition.selected);
    page.test().shouldHave(Condition.cssClass("disabled"));
    page.cancel().shouldBe(Condition.disabled);
    page.save().shouldHave(Condition.cssClass("disabled"));
    page.delete().shouldNotBe(Condition.visible);
    page.authentication().shouldNotBe(Condition.visible);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testInitialState_Edit_WithAnonymous() {
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, nxrm3MockSever.baseUrl(), null, null);
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());

    page.format().getElement().getSelectedOption().shouldHave(Condition.value("generic"));
    page.baseUrl().input().shouldHave(Condition.value(repositoryConnection.getBaseUrl()));
    page.allowAnonymousAccess().shouldBe(Condition.selected);
    page.test().shouldNotHave(Condition.cssClass("disabled"));
    page.cancel().shouldBe(Condition.disabled);
    page.save().shouldHave(Condition.cssClass("disabled"));
    page.delete().shouldBe(Condition.enabled);
    page.authentication().shouldNotBe(Condition.visible);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testInitialState_Edit_WithCredentials() {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID,
        nxrm3MockSever.baseUrl(), "username", passwordHandler.encryptPassword("password".toCharArray()));
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());

    page.format().getElement().getSelectedOption().shouldHave(Condition.value("generic"));
    page.baseUrl().input().shouldHave(Condition.value(repositoryConnection.getBaseUrl()));
    page.enterUsernameAndPassword().shouldBe(Condition.selected);
    page.test().shouldNotHave(Condition.cssClass("disabled"));
    page.cancel().shouldBe(Condition.disabled);
    page.save().shouldHave(Condition.cssClass("disabled"));
    page.delete().shouldBe(Condition.enabled);
    page.authentication().shouldBe(Condition.visible);
    page.authentication().username().input().shouldHave(Condition.value(repositoryConnection.getUsername()));
    page.authentication().password().input().shouldHave(Condition.value("     "));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSave_Add_WithAnonymous() {
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.baseUrl().input().setValue(nxrm3MockSever.baseUrl());

    page.save().shouldBe(Condition.enabled).click();

    waitUntilNotUrl(InnerSourceRepositoryConfigurationPage.url(OwnerType.ORGANIZATION.toString(),
        Organization.ROOT_ORGANIZATION_ID));
    assertOwnerRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, RepositoryFormat.GENERIC,
        nxrm3MockSever.baseUrl(), null, null);
  }

  @Test
  public void testSave_Add_WithCredentials() {
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    page.enterUsernameAndPassword().click();
    page.authentication().username().input().setValue("username");
    page.authentication().password().input().setValue("password");

    page.save().shouldBe(Condition.enabled).click();

    waitUntilNotUrl(InnerSourceRepositoryConfigurationPage.url(OwnerType.ORGANIZATION.toString(),
        Organization.ROOT_ORGANIZATION_ID));
    assertOwnerRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, RepositoryFormat.GENERIC,
        nxrm3MockSever.baseUrl(), "username", "password");
  }

  @Test
  public void testSave_AllFormats() {
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    page.save().shouldBe(Condition.enabled).click();

    page = visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.format().chooseOption(page.maven());
    page.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    page.save().shouldBe(Condition.enabled).click();

    page = visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.format().chooseOption(page.npm());
    page.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    page.save().shouldBe(Condition.enabled).click();

    waitUntilNotUrl(InnerSourceRepositoryConfigurationPage.url(OwnerType.ORGANIZATION.toString(),
        Organization.ROOT_ORGANIZATION_ID));

    List<RepositoryConnection> repositoryConnections =
        repositoryConnectionDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(repositoryConnections).hasSize(3);
    RepositoryConnection generic = repositoryConnections.stream()
        .filter(r -> r.getFormat().equals(RepositoryFormat.GENERIC)).findFirst().orElse(null);
    RepositoryConnection maven = repositoryConnections.stream()
        .filter(r -> r.getFormat().equals(RepositoryFormat.MAVEN)).findFirst().orElse(null);
    RepositoryConnection npm = repositoryConnections.stream()
        .filter(r -> r.getFormat().equals(RepositoryFormat.NPM)).findFirst().orElse(null);
    assertRepositoryConnection(generic, RepositoryFormat.GENERIC, nxrm3MockSever.baseUrl(), null, null);
    assertRepositoryConnection(maven, RepositoryFormat.MAVEN, nxrm3MockSever.baseUrl(), null, null);
    assertRepositoryConnection(npm, RepositoryFormat.NPM, nxrm3MockSever.baseUrl(), null, null);
  }

  @Test
  public void testSave_Failure() {
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, nxrm3MockSever.baseUrl(), "username",
        passwordHandler.encryptPassword("password".toCharArray()));

    page.save().shouldBe(Condition.enabled).click();

    page.getElement().find(".nx-alert--error").shouldBe(Condition.visible)
        .shouldHave(Condition.text("An error occurred saving data."));
  }

  @Test
  public void testDelete_Success() {
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, nxrm3MockSever.baseUrl(), "username",
            passwordHandler.encryptPassword("password".toCharArray()));
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());

    page.delete().shouldBe(Condition.enabled).click();
    page.deleteModal().ok().click();

    waitUntilNotUrl(InnerSourceRepositoryConfigurationPage.url(OwnerType.ORGANIZATION.toString(),
        Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId()));
    assertThat(repositoryConnectionDAO.getById(repositoryConnection.getId())).isNull();
  }

  @Test
  public void testDelete_Failure() {
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, nxrm3MockSever.baseUrl(), "username",
            passwordHandler.encryptPassword("password".toCharArray()));
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());
    repositoryConnectionDAO.delete(repositoryConnection);

    page.delete().shouldBe(Condition.enabled).click();
    page.deleteModal().ok().click();

    page.getElement().find(".nx-alert--error").shouldBe(Condition.visible)
        .shouldHave(Condition.text("Unable to delete the configured repository."));
  }

  @Test
  public void testTest_Add_Success() {
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(aResponse().withStatus(200)));

    page.test().click();

    page.getElement().find(".nx-alert--info").shouldBe(Condition.visible)
        .shouldHave(Condition.text("Repository configuration test successful."));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testTest_Add_Failure() {
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.baseUrl().input().setValue(nxrm3MockSever.baseUrl());
    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(aResponse().withStatus(404)));

    page.test().click();

    page.getElement().find(".nx-alert--error").shouldBe(Condition.visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testTest_Edit_Unchanged_Success() {
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, nxrm3MockSever.baseUrl(), "username",
            passwordHandler.encryptPassword("password".toCharArray()));
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());
    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(aResponse().withStatus(200)));

    page.test().click();

    page.getElement().find(".nx-alert--info").shouldBe(Condition.visible)
        .shouldHave(Condition.text("Repository configuration test successful."));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testTest_Edit_Unchanged_Failure() {
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, nxrm3MockSever.baseUrl(), "username",
            passwordHandler.encryptPassword("password".toCharArray()));
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());
    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(aResponse().withStatus(404)));

    page.test().click();

    page.getElement().find(".nx-alert--error").shouldBe(Condition.visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testTest_Edit_Changed_Success() {
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, nxrm3MockSever.baseUrl(), "username",
            passwordHandler.encryptPassword("password".toCharArray()));
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());
    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(aResponse().withStatus(200)));

    page.format().chooseOption(page.maven());
    page.test().click();

    page.getElement().find(".nx-alert--info").shouldBe(Condition.visible)
        .shouldHave(Condition.text("Repository configuration test successful."));
  }

  @Test
  public void testTest_Edit_Changed_Failure() {
    RepositoryConnection repositoryConnection =
        tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID, nxrm3MockSever.baseUrl(), "username",
            passwordHandler.encryptPassword("password".toCharArray()));
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());
    nxrm3MockSever.stubFor(get(urlPathMatching(NXRM_STATUS_RESOURCE)).willReturn(aResponse().withStatus(404)));

    page.format().chooseOption(page.maven());
    page.test().click();

    page.getElement().find(".nx-alert--error").shouldBe(Condition.visible)
        .shouldHave(Condition.text("Unable to connect to the configured repository. 404 Not Found"));
  }

  @Test
  public void testCancel_Add() {
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID);
    page.cancel().shouldBe(Condition.disabled);

    page.format().chooseOption(page.maven());
    page.cancel().shouldBe(Condition.enabled).click();
    page.format().getElement().getSelectedOption().shouldHave(Condition.value(RepositoryFormat.GENERIC.toString()));
    page.cancel().shouldBe(Condition.disabled);

    page.baseUrl().input().sendKeys("a");
    page.cancel().shouldBe(Condition.enabled).click();
    page.baseUrl().input().shouldBe(Condition.empty);
    page.cancel().shouldBe(Condition.disabled);

    page.enterUsernameAndPassword().click();
    page.cancel().shouldBe(Condition.enabled).click();
    page.allowAnonymousAccess().shouldBe(Condition.selected);
    page.cancel().shouldBe(Condition.disabled);

    page.enterUsernameAndPassword().click();
    page.authentication().username().input().sendKeys("a");
    page.cancel().shouldBe(Condition.enabled).click();
    page.authentication().shouldNotBe(Condition.visible);
    page.cancel().shouldBe(Condition.disabled);
    page.enterUsernameAndPassword().click();
    page.authentication().username().input().shouldBe(Condition.empty);
    page.cancel().shouldBe(Condition.enabled).click();
    page.cancel().shouldBe(Condition.disabled);

    page.enterUsernameAndPassword().click();
    page.authentication().password().input().sendKeys("a");
    page.cancel().shouldBe(Condition.enabled).click();
    page.authentication().shouldNotBe(Condition.visible);
    page.cancel().shouldBe(Condition.disabled);
    page.enterUsernameAndPassword().click();
    page.authentication().password().input().shouldBe(Condition.empty);
    page.cancel().shouldBe(Condition.enabled).click();
    page.cancel().shouldBe(Condition.disabled);
  }

  @Test
  public void testCancel_Edit() {
    RepositoryConnection repositoryConnection = tempEntity.newRepositoryConnection(Organization.ROOT_ORGANIZATION_ID,
        nxrm3MockSever.baseUrl(), RepositoryFormat.MAVEN, "username",
        passwordHandler.encryptPassword("password".toCharArray()));
    InnerSourceRepositoryConfigurationPage page =
        visitPage(OwnerType.ORGANIZATION.toString(), Organization.ROOT_ORGANIZATION_ID, repositoryConnection.getId());
    page.cancel().shouldBe(Condition.disabled);

    page.format().chooseOption(page.npm());
    page.cancel().shouldBe(Condition.enabled).click();
    page.format().getElement().getSelectedOption().shouldHave(Condition.value(RepositoryFormat.MAVEN.toString()));
    page.cancel().shouldBe(Condition.disabled);

    page.baseUrl().input().sendKeys("a");
    page.cancel().shouldBe(Condition.enabled).click();
    page.baseUrl().input().shouldHave(Condition.value(repositoryConnection.getBaseUrl()));
    page.cancel().shouldBe(Condition.disabled);

    page.allowAnonymousAccess().click();
    page.cancel().shouldBe(Condition.enabled).click();
    page.enterUsernameAndPassword().shouldBe(Condition.selected);
    page.cancel().shouldBe(Condition.disabled);

    page.authentication().username().input().sendKeys("a");
    page.cancel().shouldBe(Condition.enabled).click();
    page.authentication().username().input().shouldHave(Condition.value(repositoryConnection.getUsername()));
    page.cancel().shouldBe(Condition.disabled);

    page.authentication().password().input().sendKeys("a");
    page.cancel().shouldBe(Condition.enabled).click();
    page.authentication().password().input().shouldHave(Condition.value("     "));
    page.cancel().shouldBe(Condition.disabled);
  }

  private void assertOwnerRepositoryConnection(
      String ownerId,
      RepositoryFormat expectedFormat,
      String expectedBaseUrl,
      String expectedUsername,
      String expectedPassword)
  {
    List<RepositoryConnection> repositoryConnections = repositoryConnectionDAO.getByOwnerId(ownerId);
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
    assertThat(repositoryConnection.getPassword() == null ? null : String.valueOf(
        passwordHandler.decryptPassword(repositoryConnection.getPassword()))).isEqualTo(expectedPassword);
  }

  private InnerSourceRepositoryConfigurationPage visitPage(String ownerType, String ownerId) {
    return visitPage(ownerType, ownerId, null);
  }

  private InnerSourceRepositoryConfigurationPage visitPage(
      String ownerType, String ownerId, String repositoryConnectionId)
  {
    if (repositoryConnectionId == null) {
      refreshOrOpen(InnerSourceRepositoryConfigurationPage.url(ownerType, ownerId));
    }
    else {
      refreshOrOpen(InnerSourceRepositoryConfigurationPage.url(ownerType, ownerId, repositoryConnectionId));
    }
    InnerSourceRepositoryConfigurationPage page = new InnerSourceRepositoryConfigurationPage();
    page.shouldBe(Condition.visible);
    return page;
  }
}
