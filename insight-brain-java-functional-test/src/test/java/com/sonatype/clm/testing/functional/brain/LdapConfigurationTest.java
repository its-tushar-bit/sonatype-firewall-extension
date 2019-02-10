/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ILdapForm;
import com.sonatype.clm.testing.functional.elements.LdapConnectionForm;
import com.sonatype.clm.testing.functional.elements.LdapNameEditor;
import com.sonatype.clm.testing.functional.elements.LdapNameEditor.NameEditor;
import com.sonatype.clm.testing.functional.elements.LdapUserAndGroupSettingsForm;
import com.sonatype.clm.testing.functional.elements.LdapUserAndGroupSettingsForm.CheckUserMappingModal;
import com.sonatype.clm.testing.functional.elements.LdapUserAndGroupSettingsForm.TestLoginModal;
import com.sonatype.clm.testing.functional.elements.PopoverViolations;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.LdapConfigurationPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class LdapConfigurationTest
    extends AbstractFunctionalTest
{
  @Rule
  public TestLdapServer testLdapServer = new TestLdapServer();

  private LdapServer server;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    server = tempEntity.newLdapServer("Test Ldap Server");
    refresh();
  }

  @After
  public void end() {
    LdapServerDAO ldapServerDAO = new LdapServerDAO();
    for (LdapServer server : ldapServerDAO.getAll()) {
      ldapServerDAO.delete(server);
    }
  }

  @Test
  public void testCreateLdapServer() throws Exception {
    LdapServerDAO ldapServerDAO = new LdapServerDAO();

    refreshOrOpen(LdapServerListPage.URL);

    LdapServerListPage serverListPage = new LdapServerListPage();
    serverListPage.ldapServerList().elements().shouldHaveSize(1);
    serverListPage.newServerButton().click();

    waitUntilUrl(LdapConfigurationPage.createLdapUrl());
    LdapConfigurationPage.backButton().shouldHave(text("Back to LDAP Servers")).click();
    serverListPage.ldapServerList().elements().shouldHaveSize(1);
    serverListPage.newServerButton().click();

    waitUntilUrl(LdapConfigurationPage.createLdapUrl());
    LdapNameEditor ldapNameEditor = LdapConfigurationPage.ldapNameEditor();
    NameEditor nameEditor = ldapNameEditor.nameEditor();

    ldapNameEditor.saveButton().shouldBe(visible, disabled);
    ldapNameEditor.cancelButton().shouldBe(visible, enabled);

    nameEditor.shouldBe(visible).setValue("Another Ldap Server");
    ldapNameEditor.saveButton().shouldBe(visible, enabled).click();
    ldapNameEditor.saveButton().shouldBe(hidden);
    ldapNameEditor.cancelButton().shouldBe(hidden);
    LdapConfigurationPage.ldapConnectionForm().shouldBe(visible);

    eyesWatcher.eyesCheck();
    LdapConfigurationPage.backButton().shouldHave(text("Back to LDAP Servers"));

    server = ldapServerDAO.getByName("Another Ldap Server");
    assertThat(server).isNotNull();

    testFormValidation();

    startTestLdapServer();

    testConnection();
    testUserMapping();
    testLdapFormDataMatchesPersistedData();
  }

  @Test
  public void testCancelCreate() {
    refreshOrOpen(LdapServerListPage.URL);

    LdapServerListPage serverListPage = new LdapServerListPage();
    serverListPage.newServerButton().click();

    LdapNameEditor ldapNameEditor = LdapConfigurationPage.ldapNameEditor();
    ldapNameEditor.cancelButton().shouldBe(visible, enabled).click();

    serverListPage.shouldBe(visible);
    serverListPage.newServerButton().click();

    NameEditor nameEditor = ldapNameEditor.nameEditor();
    nameEditor.shouldBe(visible).setValue("Another Ldap Server");

    ldapNameEditor.cancelButton().shouldBe(visible, enabled).click();
    UnsavedModal unsavedModal = new UnsavedModal();
    unsavedModal.shouldBe(visible);
    unsavedModal.continueButton().click();

    serverListPage.shouldBe(visible);
  }

  @Test
  public void testResetForm() {
    refreshOrOpen(LdapConfigurationPage.editLdapUrl(server.getId()));
    LdapConfigurationPage.root().should(appear);
    LdapConnectionForm ldapConnectionForm = LdapConfigurationPage.ldapConnectionForm();

    ldapConnectionForm.hostname().shouldBe(visible, empty).setValue("ldap.clm");
    ldapConnectionForm.searchBase().shouldBe(visible, empty).setValue("dc=win,dc=blackforest,dc=local");

    discardChangesAndReset(ldapConnectionForm);

    // User And Group Form
    LdapConfigurationPage.userAndGroupSettingsTab().scrollIntoView(false).click();
    LdapUserAndGroupSettingsForm userAndGroupSettingsForm = LdapConfigurationPage.ldapUserAndGroupSettingsForm();

    // Fill out form
    userAndGroupSettingsForm.userObjectClass().shouldBe(empty).setValue("user");
    userAndGroupSettingsForm.userIDAttribute().shouldBe(empty).setValue("sAMAccountName");
    userAndGroupSettingsForm.userRealNameAttribute().shouldBe(empty).setValue("displayName");
    userAndGroupSettingsForm.userEmailAttribute().shouldBe(empty).setValue("mail");

    discardChangesAndReset(userAndGroupSettingsForm);
  }

  @Test
  public void testDeleteServer() {
    refreshOrOpen(LdapServerListPage.URL);

    LdapServerListPage serverListPage = new LdapServerListPage();
    serverListPage.ldapServerList().elements().shouldHaveSize(1).get(0).shouldBe(visible).click();

    waitUntilUrl(LdapConfigurationPage.editLdapUrl(server.getId()));
    LdapConfigurationPage.root().should(appear);
    LdapConfigurationPage.deleteButton().shouldBe(visible).click();
    LdapConfigurationPage.deleteConfirmationButton().shouldBe(visible).click();

    waitUntilUrl(LdapServerListPage.URL);
    serverListPage.ldapServerList().elements().shouldHaveSize(0);
    serverListPage.ldapServerList().emptyDescriptor().shouldBe(visible);
    assertThat(new LdapServerDAO().getById(server.getId())).isNull();
  }

  private void testFormValidation() {
    LdapConnectionForm ldapConnectionForm = LdapConfigurationPage.ldapConnectionForm();
    ldapConnectionForm.shouldBe(visible);

    // Test port validation
    ldapConnectionForm.port().setValue("0");
    PopoverViolations.on(ldapConnectionForm.port()).shouldShowError("Minimum allowed value is 1");

    ldapConnectionForm.port().setValue("999999");
    PopoverViolations.on(ldapConnectionForm.port()).shouldShowError("Maximum allowed value is 65535");

    testRequiredFormFields(ldapConnectionForm);

    LdapConfigurationPage.userAndGroupSettingsTab().scrollIntoView(false).click();
    LdapUserAndGroupSettingsForm userAndGroupSettingsForm = LdapConfigurationPage.ldapUserAndGroupSettingsForm();
    userAndGroupSettingsForm.shouldBe(visible);

    testRequiredFormFields(userAndGroupSettingsForm);

    LdapConfigurationPage.connectionTab().scrollIntoView(false).click();
    ldapConnectionForm.shouldBe(visible);
  }

  private void testRequiredFormFields(ILdapForm ldapForm) {
    for (SelenideElement element : ldapForm.requiredFields()) {
      element.sendKeys("a");
      element.sendKeys(Keys.BACK_SPACE);
      popoverViolations(element).shouldHave(text("Please enter a value"));
    }

    resetForm(ldapForm);
  }

  private void discardChangesAndReset(ILdapForm ldapForm) {
    for (SelenideElement field : ldapForm.requiredFields()) {
      field.shouldNotHave(cssClass("ng-invalid-required"));
    }

    ldapForm.saveButton().scrollIntoView(false).shouldBe(visible, enabled);

    resetForm(ldapForm);

    ldapForm.saveButton().shouldBe(disabled);
    ldapForm.cancelButton().shouldBe(disabled);
  }

  private void resetForm(ILdapForm ldapForm) {
    ldapForm.cancelButton().scrollIntoView(false).shouldBe(visible, enabled).click();

    // Continue and discard changes (reset)
    LdapConfigurationPage.discardChangesModalButton().shouldBe(visible, enabled).click();
    LdapConfigurationPage.discardChangesModalButton().shouldBe(hidden);
  }

  private void startTestLdapServer() throws Exception {
    testLdapServer.start();
    testLdapServer.loadData("/ldapData/ldap_users.ldif");
  }

  private void testConnection() {
    // On connection configuration page
    LdapConnectionForm connectionForm = LdapConfigurationPage.ldapConnectionForm();
    connectionForm.should(visible);

    connectionForm.protocol().shouldHave(value("LDAP"));
    connectionForm.hostname().shouldBe(visible).setValue(testLdapServer.getHostname());
    connectionForm.port().shouldBe(visible).shouldHave(value("389")).setValue("" + testLdapServer.getPort());
    connectionForm.searchBase().shouldBe(visible).setValue("ou=users,dc=company,dc=com");

    connectionForm.cancelButton().shouldBe(enabled);
    connectionForm.saveButton().shouldBe(enabled);
    connectionForm.testConnectionButton().shouldBe(enabled).scrollIntoView(false).click();

    connectionForm.successAlertBox().shouldBe(visible).shouldHave(text("Success!"));

    // fill all inputs to ensure persisted on save
    connectionForm.authenticationMethod().shouldHave(value("NONE")).selectOption("SIMPLE");
    connectionForm.saslRealm().shouldBe(visible, empty).setValue("just checking if persisted");
    connectionForm.systemUsername().shouldBe(visible, empty).setValue("just checking if persisted");
    connectionForm.systemPassword().shouldBe(visible, empty).setValue("just checking if persisted");
    connectionForm.saveButton().scrollIntoView(false);
    connectionForm.connectionTimeout().shouldBe(value("30")).setValue("31");
    connectionForm.retryDelay().shouldBe(value("30")).setValue("31");

    connectionForm.saveButton().shouldBe(enabled).click();

    // Connection saved
    connectionForm.successAlertBox().shouldBe(visible).shouldHave(text("Configuration saved."));
    connectionForm.cancelButton().shouldBe(disabled);
    connectionForm.saveButton().shouldBe(disabled);

    // Ensure persisted Connection matches
    LdapConnection persistedConnection = new LdapConnectionDAO().getByServerId(server.getId());

    assertThat(persistedConnection).isNotNull();
    assertThat(persistedConnection.getProtocol()).isEqualTo(LdapProtocol.LDAP);
    assertThat(persistedConnection.getHostname()).isEqualTo(testLdapServer.getHostname());
    assertThat(persistedConnection.getPort()).isEqualTo(testLdapServer.getPort());
    assertThat(persistedConnection.getSearchBase()).isEqualTo("ou=users,dc=company,dc=com");
    assertThat(persistedConnection.getAuthenticationMethod()).isEqualTo(LdapAuthenticationMethod.SIMPLE);
    assertThat(persistedConnection.getSaslRealm()).isEqualTo("just checking if persisted");
    assertThat(persistedConnection.getSystemUsername()).isEqualTo("just checking if persisted");
    assertThat(persistedConnection.getSystemPassword()).isNotEqualTo("just checking if persisted");
    assertThat(persistedConnection.getConnectionTimeout()).isEqualTo(31);
    assertThat(persistedConnection.getRetryDelay()).isEqualTo(31);

    // Revert back to no authentication
    connectionForm.authenticationMethod().shouldHave(value("SIMPLE")).selectOption("NONE");
    connectionForm.saveButton().shouldBe(enabled).click();
    connectionForm.successAlertBox().shouldBe(visible).shouldHave(text("Configuration saved."));
    connectionForm.saveButton().shouldBe(disabled);
  }

  private void testUserMapping() {
    LdapConfigurationPage.userAndGroupSettingsTab().scrollIntoView(false).click();
    LdapConfigurationPage.ldapConnectionForm().shouldBe(hidden);

    LdapUserAndGroupSettingsForm userAndGroupSettingsForm = LdapConfigurationPage.ldapUserAndGroupSettingsForm();
    userAndGroupSettingsForm.shouldBe(visible);

    userAndGroupSettingsForm.checkUserLoginButton().scrollIntoView(false).shouldBe(visible, disabled);
    userAndGroupSettingsForm.checkUserMappingButton().shouldBe(visible, disabled);
    userAndGroupSettingsForm.saveButton().shouldBe(visible, disabled);
    userAndGroupSettingsForm.cancelButton().shouldBe(visible, disabled);

    // Fill out form
    userAndGroupSettingsForm.userObjectClass().scrollIntoView(false).shouldBe(empty).setValue("person");
    userAndGroupSettingsForm.userIDAttribute().shouldBe(empty).setValue("uid");
    userAndGroupSettingsForm.userRealNameAttribute().shouldBe(empty).setValue("cn");
    userAndGroupSettingsForm.userEmailAttribute().shouldBe(empty).setValue("mail");
    userAndGroupSettingsForm.userSubtree().shouldNotBe(selected).click();

    userAndGroupSettingsForm.groupSearchWarning().shouldBe(hidden);
    userAndGroupSettingsForm.groupMappingType().shouldBe(text("NONE")).selectOption("DYNAMIC");
    userAndGroupSettingsForm.groupSearchWarning().scrollIntoView(true).shouldBe(visible)
        .shouldHave(text(LdapUserAndGroupSettingsForm.GROUP_SEARCH_WARNING));
    userAndGroupSettingsForm.userMemberOfGroupAttribute().shouldBe(empty).setValue("departmentNumber");

    // buttons now enabled
    userAndGroupSettingsForm.checkUserLoginButton().shouldBe(visible, enabled);
    userAndGroupSettingsForm.checkUserMappingButton().shouldBe(visible, enabled);
    userAndGroupSettingsForm.saveButton().shouldBe(visible, enabled);
    userAndGroupSettingsForm.cancelButton().shouldBe(visible, enabled);

    // Test Login
    userAndGroupSettingsForm.checkUserLoginButton().shouldBe(enabled).click();
    TestLoginModal testLoginModal = userAndGroupSettingsForm.testLoginModal();
    testLoginModal.shouldBe(visible);
    testLoginModal.username().shouldBe(empty).setValue("test_user2");
    testLoginModal.password().shouldBe(empty).setValue("test");
    testLoginModal.testLoginButton().shouldBe(enabled).click();
    testLoginModal.successAlertBox().shouldBe(visible).shouldHave(text("Success!"));
    eyesWatcher.eyesCheck("Test login modal");
    testLoginModal.cancelButton().shouldBe(enabled).click();
    testLoginModal.shouldBe(hidden);

    // Test Check User Mapping
    userAndGroupSettingsForm.checkUserMappingButton().shouldBe(enabled).click();
    CheckUserMappingModal userMappingModal = userAndGroupSettingsForm.checkUserMappingModal();
    userMappingModal.shouldBe(visible);
    userMappingModal
        .shouldHaveUserEntry(1, "test_user", "Test User", "test.user@company.com", "ab, abc, xb");
    userMappingModal
        .shouldHaveUserEntry(2, "test_user2", "Test User 2", "test.user2@company.com", "ab, bc, bx");

    // since this user has no email data, it has less fields filled and should be ordered last
    userMappingModal
        .shouldHaveUserEntry(3, "test*user", "Test*User", "", "ab, bc, bx");
    eyesWatcher.eyesCheck("Check user mapping modal");
    userMappingModal.cancelButton().shouldBe(enabled).click();

    // Fill all remaining fields only to ensure persisted on save
    userAndGroupSettingsForm.userBaseDN().scrollIntoView(true).shouldBe(empty).setValue("just checking if persisted");
    userAndGroupSettingsForm.userFilter().shouldBe(empty).setValue("just checking if persisted");

    // Save and ensure persistence of the user mapping
    userAndGroupSettingsForm.saveButton().scrollIntoView(false).shouldBe(enabled).click();
    userAndGroupSettingsForm.successAlertBox().shouldBe(visible).shouldHave(text("Configuration saved."));
    userAndGroupSettingsForm.saveButton().shouldBe(disabled);

    LdapUserMapping persistedUserMapping = new LdapUserMappingDAO().getByServerId(server.getId());

    assertThat(persistedUserMapping).isNotNull();
    assertThat(persistedUserMapping.getUserBaseDN()).isEqualTo("just checking if persisted");
    assertThat(persistedUserMapping.getUserObjectClass()).isEqualTo("person");
    assertThat(persistedUserMapping.getUserFilter()).isEqualTo("just checking if persisted");
    assertThat(persistedUserMapping.getUserIDAttribute()).isEqualTo("uid");
    assertThat(persistedUserMapping.getUserRealNameAttribute()).isEqualTo("cn");
    assertThat(persistedUserMapping.getUserEmailAttribute()).isEqualTo("mail");
    assertThat(persistedUserMapping.isUserSubtree()).isTrue();
    assertThat(persistedUserMapping.getGroupMappingType()).isEqualTo(LdapGroupMappingType.DYNAMIC);
    assertThat(persistedUserMapping.getUserMemberOfGroupAttribute()).isEqualTo("departmentNumber");
  }

  private void testLdapFormDataMatchesPersistedData() {
    refresh();

    LdapConnection persistedConnection = new LdapConnectionDAO().getByServerId(server.getId());
    LdapUserMapping persistedUserMapping = new LdapUserMappingDAO().getByServerId(server.getId());

    // Test Connection
    LdapConnectionForm connectionForm = LdapConfigurationPage.ldapConnectionForm();
    connectionForm.shouldBe(visible);
    connectionForm.protocol().shouldHave(value(persistedConnection.getProtocol().getProtocol()));
    connectionForm.hostname().shouldHave(value(persistedConnection.getHostname()));
    connectionForm.port().shouldHave(value("" + persistedConnection.getPort()));
    connectionForm.searchBase().shouldHave(value(persistedConnection.getSearchBase()));
    connectionForm.authenticationMethod().shouldHave(value(persistedConnection.getAuthenticationMethod().getMethod()));
    connectionForm.saslRealm().shouldHave(value(persistedConnection.getSaslRealm()));
    connectionForm.systemUsername().shouldHave(value(persistedConnection.getSystemUsername()));
    connectionForm.connectionTimeout().shouldHave(value("" + persistedConnection.getConnectionTimeout()));
    connectionForm.retryDelay().shouldHave(value("" + persistedConnection.getRetryDelay()));

    // Test User Mapping
    LdapConfigurationPage.userAndGroupSettingsTab().click();
    LdapUserAndGroupSettingsForm userAndGroupSettingsForm = LdapConfigurationPage.ldapUserAndGroupSettingsForm();
    userAndGroupSettingsForm.shouldBe(visible);
    userAndGroupSettingsForm.userBaseDN().shouldHave(value(persistedUserMapping.getUserBaseDN()));
    userAndGroupSettingsForm.userObjectClass().shouldHave(value(persistedUserMapping.getUserObjectClass()));
    userAndGroupSettingsForm.userFilter().shouldHave(value(persistedUserMapping.getUserFilter()));
    userAndGroupSettingsForm.userIDAttribute().shouldHave(value(persistedUserMapping.getUserIDAttribute()));
    userAndGroupSettingsForm.userRealNameAttribute().shouldHave(value(persistedUserMapping.getUserRealNameAttribute()));
    userAndGroupSettingsForm.userEmailAttribute().shouldHave(value(persistedUserMapping.getUserEmailAttribute()));
    assertThat(userAndGroupSettingsForm.userSubtree().isSelected()).isEqualTo(persistedUserMapping.isUserSubtree());

    userAndGroupSettingsForm.groupMappingType()
        .shouldHave(value(persistedUserMapping.getGroupMappingType().toString()));
    userAndGroupSettingsForm.userMemberOfGroupAttribute()
        .shouldHave(value(persistedUserMapping.getUserMemberOfGroupAttribute()));
  }
}
