/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.user;

import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.pages.MtiqUserManagementPage;
import com.sonatype.clm.testing.functional.pages.KeycloakLoginPage;
import com.sonatype.insight.brain.api.v2.service.ApiSamlConfigurationService;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.keycloak.KeycloakServerRule;
import com.sonatype.insight.keycloak.KeycloakServerUtil;

import com.codeborne.selenide.Selenide;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.mockito.Mockito;

import static com.sonatype.clm.testing.functional.elements.CLM.RSC_DISABLED;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.CollectionCondition.size;

public class MtiqUserManagementTest
    extends AbstractMtiqFunctionalTest
{
  // Rather than actually connecting to Auth0, use a keycloak server as a stand-in for the SAML IdP. The other Auth0
  // APIs are mocked in tests as-needed
  @ClassRule
  public static KeycloakServerRule keycloakServerRule = new KeycloakServerRule();

  private KeycloakServerUtil keycloak = keycloakServerRule.getKeycloakServerUtil();

  private final ApiSamlConfigurationService apiSamlConfigurationService =
      testCLMServer.getCLMServer().getInstance(ApiSamlConfigurationService.class);

  private static final String KEYCLOAK_USER_EMAIL_1 = "user1@example.com";

  private static final String KEYCLOAK_USER_EMAIL_2 = "user2@example.com";

  private static final String password = "my-password";

  private MtiqUserManagementPage page = new MtiqUserManagementPage();

  private TenantMetadataDAO tenantMetadataDAO = new TenantMetadataDAO();

  private String userId1;

  private String userId2;

  @BeforeClass
  public static void waitForKeycloak() {
    // Wait for keycloak to finish starting
    Selenide.open(keycloakServerRule.getKeycloakServerUtil().getBaseUrl() + "/admin/");
    Selenide.Wait().until(webDriver -> webDriver.getCurrentUrl().contains("redirect_uri"));
  }

  @Before
  public void setupKeycloakUsers() {
    // This is, for some reason, necessary to get the BaseUrl set properly in the backend
    // prior to the apiSamlConfigurationService call below
    refreshOrOpen(MtiqUserManagementPage.url());

    apiSamlConfigurationService.insertOrUpdateSamlConfiguration(keycloak.getSamlMetadataXml(), null);
    String metadata = apiSamlConfigurationService.getMetadata();
    ClientRepresentation clientRepresentation = keycloak.createClientRepresentation(metadata);
    clientRepresentation.setProtocolMappers(KeycloakServerUtil.protocolMappers());
    keycloak.createClient(clientRepresentation);

    // Setup two users in keycloak: one admin and one not
    userId1 = keycloak.createUser("User", "One", KEYCLOAK_USER_EMAIL_1, KEYCLOAK_USER_EMAIL_1, password, null);
    userId2 = keycloak.createUser("User", "Two", KEYCLOAK_USER_EMAIL_2, KEYCLOAK_USER_EMAIL_2, password, null);
    String adminGroupId = keycloak.createGroup("admin");
    keycloak.assignUserToGroup(userId1, adminGroupId);
    tempEntity.newMembershipMapping("global", Role.SYSTEM_ADMIN_ROLE_ID, "admin", MemberType.GROUP);

    refreshOrOpen(MtiqUserManagementPage.url());
  }

  @Before
  public void setupConfiguration() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));

    tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connectionId", "connectionName"));
  }

  @After
  public void cleanup() {
    keycloak.clean();
    tenantMetadataDAO.getAll().forEach(tenantMetadataDAO::delete);
  }

  @Test
  public void testPageLoad() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_1, password);
    page.shouldBe(visible);
  }

  @Test
  public void testPageNotAccessibleToNonAdmin() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_2, password);
    waitUntilUrl(MtiqUserManagementPage.url());
    page.loadError().shouldBe(visible);
  }

  @Test
  public void testList() {
    loginBothSamlUsers();

    page.userItems().shouldHave(size(2));
    page.userItems().get(0).shouldHave(text("User One"));
    page.userItems().get(0).shouldHave(text("user1@example.com"));
    page.userItems().get(0).shouldHave(text("Current User"));

    page.userItems().get(1).shouldHave(text("User Two"));
    page.userItems().get(1).shouldHave(text("user2@example.com"));
    page.userItems().get(1).shouldNotHave(text("Current User"));

    // No edit on click like in on-prem
    page.userItems().get(0).click();
    page.editUserForm().shouldNot(exist);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testDelete() {
    loginBothSamlUsers();

    // cannot delete self
    page.userItem(0).deleteBtn().shouldBe(RSC_DISABLED);

    page.userItem(1).deleteBtn().shouldNotBe(RSC_DISABLED);
    page.userItem(1).deleteBtn().click();

    NxDeleteModal deleteModal = new NxDeleteModal("#delete-user-modal");

    deleteModal.header().shouldHave(text("Delete User"));
    deleteModal.alertContent().shouldHave(text("You are about to permanently remove " +
         KEYCLOAK_USER_EMAIL_2 + ". This action cannot be undone."));
    deleteModal.submitButton().click();
    deleteModal.should(disappear);

    page.userItems().shouldHaveSize(1);

    // verify that the API call to delete the use from Auth0 was made as well
    Mockito.verify(auth0ManagementAPI).deleteUserByEmailFromConnection(KEYCLOAK_USER_EMAIL_2, "connectionId");
  }

  /**
   * Login as user2, logout, then login as user1. End result is that the IQ server is aware of both users and
   * you are currently logged in as user1
   */
  private void loginBothSamlUsers() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_2, password);
    keycloak.logoutUser(userId2);

    logout();
    refreshOrOpen(MtiqUserManagementPage.url());
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_1, password);
  }
}
