/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LoginModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.UserDetailsModal;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.KeycloakLoginPage;
import com.sonatype.insight.brain.api.v2.service.ApiSamlConfigurationService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.keycloak.KeycloakServerRule;
import com.sonatype.insight.keycloak.KeycloakServerUtil;

import com.codeborne.selenide.Selenide;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.focused;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SamlTest
    extends AbstractFunctionalTest
{
  @ClassRule
  public static KeycloakServerRule keycloakServerRule = new KeycloakServerRule();

  private final KeycloakServerUtil keycloak = keycloakServerRule.getKeycloakServerUtil();

  private final ApiSamlConfigurationService apiSamlConfigurationService =
      testCLMServer.getCLMServer().getInstance(ApiSamlConfigurationService.class);

  @BeforeClass
  public static void beforeClass() {
    // Load the keycloak spa in browser once
    Selenide.open(keycloakServerRule.getKeycloakServerUtil().getBaseUrl() + "/admin/");
    Selenide.Wait().until(webDriver -> webDriver.getCurrentUrl().contains("redirect_uri"));
  }

  @Before
  public void before() {
    hardreset();
  }

  @After
  public void after() {
    keycloakServerRule.clean();
  }

  @Test
  public void testLoginLogout() {
    // Upload Identity Provider metadata to IQ Server
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(keycloak.getSamlMetadataXml(), null);

    // Register IQ in Keycloak
    String metadata = apiSamlConfigurationService.getMetadata();
    ClientRepresentation clientRepresentation = keycloak.createClientRepresentation(metadata);
    clientRepresentation.setProtocolMappers(KeycloakServerUtil.protocolMappers());
    keycloak.createClient(clientRepresentation);

    // Create a group and a user
    String groupId = keycloak.createGroup("group-developers");
    String username = "john.doe";
    String password = "password";
    String userId = keycloak.createUser("John", "Doe", username, "john@doe.com", password, null);
    keycloak.assignUserToGroup(userId, groupId);

    // SAML Successful login
    refreshOrOpen(IndexPage.url());
    LoginModal loginModal = new LoginModal();
    loginModal.loginButton().shouldBe(visible);
    loginModal.ssoButton().shouldBe(visible).click();
    KeycloakLoginPage.login(username, password);

    // Validations upon successful login
    DashboardPage.dashboardContainer().shouldBe(visible);

    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().userName().shouldBe(visible).shouldHave(text("John Doe"));
    MainHeader.userMenu().userDetails().click();

    UserDetailsModal modal = new UserDetailsModal();
    modal.should(appear);
    modal.username().shouldBe(text("john.doe"));
    modal.displayName().shouldBe(text("John Doe"));
    modal.groups().shouldBe(text(Group.AUTHENTICATED_USERS_GROUP_ID + ", group-developers"));
    modal.closeButton().click();

    // Successful logout
    keycloak.logoutUser(userId);
    logout();
    loginModal.loginButton().shouldBe(visible);
    loginModal.ssoButton().shouldBe(visible, focused);

    // Unsuccessful login due to wrong password
    loginModal.ssoButton().click();
    KeycloakLoginPage.login(username, "wrong-password");
    refreshOrOpen(IndexPage.url());
    loginModal.loginButton().shouldBe(visible);
    loginModal.ssoButton().shouldBe(visible, focused);
  }

  @Test
  public void testLoginSsoOnly() {
    SystemConfigurationPropertyFeature.ENABLE_SSO_ONLY.setEnabled(true);

    // Upload Identity Provider metadata to IQ Server
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(keycloak.getSamlMetadataXml(), null);

    // Register IQ in Keycloak
    String metadata = apiSamlConfigurationService.getMetadata();
    ClientRepresentation clientRepresentation = keycloak.createClientRepresentation(metadata);
    clientRepresentation.setProtocolMappers(KeycloakServerUtil.protocolMappers());
    keycloak.createClient(clientRepresentation);

    // Create a group and a user
    String groupId = keycloak.createGroup("group-developers");
    String username = "john.doe";
    String password = "password";
    String userId = keycloak.createUser("John", "Doe", username, "john@doe.com", password, null);
    keycloak.assignUserToGroup(userId, groupId);

    // Load the page. Redirection to SSO login should happen automatically
    refreshOrOpen(IndexPage.url());
    KeycloakLoginPage.login(username, password);

    DashboardPage.dashboardContainer().shouldBe(visible);
  }

  @Test
  public void testIntegrationWithMinimalConfig() {
    // Upload Identity Provider metadata to IQ Server
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(keycloak.getSamlMetadataXml(), null);

    // Register IQ in Keycloak
    String metadata = apiSamlConfigurationService.getMetadata();
    ClientRepresentation clientRepresentation = keycloak.createClientRepresentation(metadata);
    keycloak.createClient(clientRepresentation);

    // Create a user
    String username = "johanne.doanne";
    String password = "her-secret";
    keycloak.createUser("Johanne", "Doanne", username, "johnanne@doanne.com", password, null);

    // SAML Successful login
    refreshOrOpen(IndexPage.url());
    LoginModal loginModal = new LoginModal();
    loginModal.loginButton().shouldBe(visible);
    loginModal.ssoButton().shouldBe(visible).click();

    KeycloakLoginPage.login(username, password);

    DashboardPage.dashboardContainer().shouldBe(visible);

    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().userName().shouldBe(visible).shouldHave(text("johanne.doanne"));
    MainHeader.userMenu().userDetails().click();

    UserDetailsModal modal = new UserDetailsModal();
    modal.should(appear);
    modal.username().shouldBe(text("johanne.doanne"));
    modal.displayName().shouldBe(text("johanne.doanne"));
    modal.groups().shouldBe(text(Group.AUTHENTICATED_USERS_GROUP_ID));
    modal.closeButton().click();
  }

  @Test
  public void testRedirect() {
    Application application = tempEntity.newApplicationWithParent("_.-a1b2c3-._éÝÏ");
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "scanId");
    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(keycloak.getSamlMetadataXml(), null);
    String metadata = apiSamlConfigurationService.getMetadata();
    ClientRepresentation clientRepresentation = keycloak.createClientRepresentation(metadata);
    clientRepresentation.setProtocolMappers(KeycloakServerUtil.protocolMappers());
    keycloak.createClient(clientRepresentation);
    String username = "johanne.doanne";
    String password = "her-secret";
    keycloak.createUser("Johanne", "Doanne", username, "johnanne@doanne.com", password, null);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, Role.POLICY_ADMIN_ROLE_ID, username,
        MemberType.USER);

    String urlEncoded = ApplicationReportPage.url(application, policyEvaluation.getScanId());
    refreshOrOpen(urlEncoded);
    LoginModal loginModal = new LoginModal();
    loginModal.loginButton().shouldBe(visible);
    loginModal.ssoButton().shouldBe(visible, focused).click();
    KeycloakLoginPage.login(username, password);

    waitUntilUrl(urlEncoded);
  }
}
