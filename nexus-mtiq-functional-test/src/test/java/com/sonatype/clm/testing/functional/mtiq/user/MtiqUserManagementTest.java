/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.user;

import java.util.Arrays;
import java.util.List;

import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxTextInput;
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
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.mockito.Mockito;
import org.openqa.selenium.Keys;

import static com.sonatype.clm.testing.functional.elements.CLM.RSC_DISABLED;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.sonatype.clm.testing.functional.mtiq.pages.MtiqUserManagementPage.NewUserForm;

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

    tenantMetadataDAO.insert(new TenantMetadata("appId", "appName", "connectionId", "connectionName", "encKeyName"));
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

  @Test
  public void openInviteUserForm() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_1, password);

    NewUserForm newUserForm = goToInviteUserForm();
    newUserForm.shouldBe(visible);
    newUserForm.saveButton().shouldBe(visible);

    List<SelenideElement> formInputs = Arrays.asList(newUserForm.firstNameInput(), newUserForm.lastNameInput(),
        newUserForm.emailInput());

    formInputs.forEach(input -> input.shouldBe(empty));
    formInputs.forEach(input -> input.shouldNotBe(disabled));
  }

  @Test
  public void testInviteUserForm_spaceValidations() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_1, password);

    NewUserForm newUserForm = goToInviteUserForm();
    String invalidSpacingError = "No leading, trailing or double spaces or tabs";

    keyInElementValue("a  a", Arrays.asList(newUserForm.firstNameInput(), newUserForm.lastNameInput()));

    new NxTextInput(newUserForm.firstNameInput()).errorMessage().shouldHave(exactText(invalidSpacingError));
    new NxTextInput(newUserForm.lastNameInput()).errorMessage().shouldHave(exactText(invalidSpacingError));
  }

  @Test
  public void testInviteUserForm_invalidCharacters() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_1, password);

    NewUserForm newUserForm = goToInviteUserForm();
    String nameValidationText = "Use valid characters: alphanumeric, \"_\", \".\", \"-\", or spaces";

    List<SelenideElement> nameInputElements = Arrays.asList(newUserForm.firstNameInput(), newUserForm.lastNameInput());
    keyInElementValue("#", nameInputElements);

    assertInputValidation(nameValidationText, nameInputElements);
  }

  @Test
  public void testInviteUserForm_emailValidations() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_1, password);

    NewUserForm newUserForm = goToInviteUserForm();
    String emailValidationText = "Use valid format: abc@xyz.com";

    List<SelenideElement> emailInput = Arrays.asList(newUserForm.emailInput());
    keyInElementValue("a", emailInput);

    assertInputValidation(emailValidationText, emailInput);
  }

  @Test
  public void testInviteUserForm_emptyValues() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_1, password);

    NewUserForm newUserForm = goToInviteUserForm();
    String validationText = "Must be non-empty";

    List<SelenideElement> inputElements = Arrays.asList(newUserForm.firstNameInput(), newUserForm.lastNameInput(),
        newUserForm.emailInput());
    keyInElementValue("a", inputElements);
    inputElements.forEach(element -> element.sendKeys(Keys.BACK_SPACE));

    assertInputValidation(validationText, inputElements);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testInviteUser_success() {
    loginBothSamlUsers();
    page.userItems().shouldHave(size(2));

    NewUserForm newUserForm = goToInviteUserForm();

    newUserForm.firstNameInput().val("User");
    newUserForm.lastNameInput().val("Three");
    newUserForm.emailInput().val("user3@example.com");

    newUserForm.saveButton().click();
    newUserForm.shouldBe(hidden);

    page.userItems().shouldHaveSize(3);

    page.userItems().get(2).shouldHave(text("User Three"));
    page.userItems().get(2).shouldHave(text("user3@example.com"));

    // verify that the API call to create the user from Auth0 was made
    Mockito.verify(auth0ManagementAPI).createOrGetUser("user3@example.com", "User", "Three", "connectionName");
  }

  /**
   * Login as user2, logout, then login as user1. End result is that the IQ server is aware of both users and you are
   * currently logged in as user1
   */
  private void loginBothSamlUsers() {
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_2, password);
    keycloak.logoutUser(userId2);

    logout();
    refreshOrOpen(MtiqUserManagementPage.url());
    KeycloakLoginPage.login(KEYCLOAK_USER_EMAIL_1, password);
  }

  private NewUserForm goToInviteUserForm() {
    Button newUserButton = page.newUserButton();
    newUserButton.click();

    newUserButton.shouldBe(hidden);
    return page.newUserForm();
  }

  private void assertInputValidation(final String validationText, final List<SelenideElement> elements) {
    elements.forEach(element -> new NxTextInput(element).errorMessage().shouldHave(exactText(validationText)));
  }

  private void keyInElementValue(final String inputText, final List<SelenideElement> elements) {
    elements.forEach(element -> element.val(inputText));
  }
}
