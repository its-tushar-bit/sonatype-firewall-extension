/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.user;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.ResetPasswordModal;
import com.sonatype.clm.testing.functional.elements.UserMenu;
import com.sonatype.clm.testing.functional.pages.UserManagementPage;
import com.sonatype.clm.testing.functional.pages.UserManagementPage.EditPanelForm;
import com.sonatype.clm.testing.functional.pages.UserManagementPage.NewUserForm;
import com.sonatype.clm.testing.functional.pages.UserManagementPage.SummarySection;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DeleteModal.headerText;
import static com.sonatype.clm.testing.functional.elements.PopoverViolations.on;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class UserManagementTest
    extends AbstractFunctionalTest
{
  private static final String TEST_USERNAME = "addusertest";

  private final UserDAO userDAO = new UserDAO();

  @Before
  public void initialLogin() {
    refreshOrOpen(UserManagementPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    deleteUserIfExists(TEST_USERNAME);
    logout();
  }

  @Test
  public void testPageLoad() {
    UserManagementPage userManagementPage = new UserManagementPage();
    userManagementPage.newUserButton().shouldBe(visible);
    userManagementPage.newUserForm().shouldBe(hidden);
  }

  @Test
  public void testCreateUser_formOpen() {
    UserManagementPage userManagementPage = new UserManagementPage();
    NewUserForm newUserForm = goToCreateUserForm(userManagementPage);
    newUserForm.shouldBe(visible);
    newUserForm.saveButton().shouldBe(disabled);

    List<SelenideElement> formInputs = Arrays.asList(
        newUserForm.firstNameInput(), newUserForm.lastNameInput(),
        newUserForm.emailInput(), newUserForm.usernameInput(),
        newUserForm.passwordInput(), newUserForm.passwordValidateInput());

    assertElementsNotDisabled(formInputs);
    assertElementsEmpty(formInputs);
  }

  @Test
  public void testCreateUserInputs_spaceValidations() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());

    keyInElementValue("a  a", asList(newUserForm.firstNameInput(), newUserForm.lastNameInput()));
    on(newUserForm.firstNameInput()).shouldShowInvalidSpacingError();
    on(newUserForm.lastNameInput()).shouldShowInvalidSpacingError();
  }

  @Test
  public void testCreateUserInputs__invalidCharacters() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());
    String nameValidationText = "Use valid characters: alphanumeric, \"_\", \".\", \"-\", or spaces";
    String usernameValidationText = "Use valid characters: alphanumeric, \"_\", \".\" or \"-\"";

    List<SelenideElement> nameInputElements = asList(newUserForm.firstNameInput(), newUserForm.lastNameInput());
    List<SelenideElement> usernameInputElements = Collections.singletonList(newUserForm.usernameInput());
    keyInElementValue("#", nameInputElements);
    keyInElementValue("#", usernameInputElements);

    assertPopoverValidation(nameValidationText, nameInputElements);
    assertPopoverValidation(usernameValidationText, usernameInputElements);
  }

  @Test
  public void testCreateUserInputs_emptyValues() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());
    String validationText = "Please enter a value";

    List<SelenideElement> inputElements = asList(newUserForm.firstNameInput(), newUserForm.lastNameInput(),
        newUserForm.usernameInput(), newUserForm.passwordInput(), newUserForm.passwordValidateInput());
    keyInElementValue("a", inputElements);
    keyInElementValue(Keys.BACK_SPACE.toString(), inputElements);

    assertPopoverValidation(validationText, inputElements);
  }

  @Test
  public void testCreateUserInputs_nonMatchingPassword() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());
    String validationText = "Passwords must match!";

    List<SelenideElement> passwordValidateInput = asList(newUserForm.passwordValidateInput());
    keyInElementValue("23abc", passwordValidateInput);

    assertPopoverValidation(validationText, passwordValidateInput);
  }

  @Test
  public void testCreateUser_success() {
    UserManagementPage userManagementPage = new UserManagementPage();
    NewUserForm newUserForm = goToCreateUserForm(userManagementPage);

    newUserForm.firstNameInput().val("add");
    newUserForm.lastNameInput().val("user");
    newUserForm.usernameInput().val(TEST_USERNAME);
    newUserForm.emailInput().val("addusertest@email.com");
    newUserForm.passwordInput().val("123abc");
    newUserForm.passwordValidateInput().val("123abc");

    newUserForm.saveButton().shouldBe(enabled);
    popoverViolationsList(newUserForm.getElement()).shouldHaveSize(0);

    newUserForm.saveButton().click();

    newUserForm.shouldBe(hidden);
    userManagementPage.headers().shouldHaveSize(2);// created user and the admin

    userManagementPage.headers().get(0).click();
    SummarySection summarySection = userManagementPage.summarySection(1);
    summarySection.shouldBe(visible);

    summarySection.firstName().shouldHave(text("add"));
    summarySection.lastName().shouldHave(text("user"));
    summarySection.email().shouldHave(text("addusertest@email.com"));

    userManagementPage.currentUser().shouldHave(text("ADMIN (ADMIN BUILTIN)"));
  }

  @Test
  public void testUserResetPassword() {
    User user = createUser();
    refreshOrOpen(UserManagementPage.url());
    UserManagementPage userManagementPage = new UserManagementPage();

    int userRow = 0;

    userManagementPage.resetPasswordButtons().get(userRow).shouldBe(visible).click();
    ResetPasswordModal resetPasswordModal = userManagementPage.resetPasswordModal();
    resetPasswordModal.shouldBe(visible);

    resetPasswordModal.reset().click();

    String newPassword = resetPasswordModal.newPassword().shouldBe(visible).val();
    resetPasswordModal.ok().click();

    logout();
    login(user.getUsername(), newPassword);

    UserMenu userMenu = MainHeader.userMenu();
    userMenu.dropdownToggle().shouldBe(visible).click();
    userMenu.userName().shouldBe(text(user.getFirstName() + " " + user.getLastName()));

    // close the menu again so the logout logic works
    userMenu.dropdownToggle().click();
  }

  @Test
  public void testUserEditProfile() {
    User user = createUser();
    refreshOrOpen(UserManagementPage.url());
    UserManagementPage userManagementPage = new UserManagementPage();

    int userRow = 0;
    userManagementPage.headers().get(userRow).shouldHave(text(user.getUsername()));

    userManagementPage.editUserButtons().get(userRow).shouldBe(visible).click();

    EditPanelForm editPanelForm = userManagementPage.editPanelForm();
    editPanelForm.shouldBe(visible);

    editPanelForm.firstName().shouldHave(value(user.getFirstName()));
    editPanelForm.lastName().shouldHave(value(user.getLastName()));
    editPanelForm.email().shouldHave(value(user.getEmail()));

    editPanelForm.saveButton().shouldBe(disabled);

    editPanelForm.firstName().val("testupdateFirstName");
    editPanelForm.lastName().val("testupdateLastName");
    editPanelForm.email().val("emailLastName@email.com");

    editPanelForm.saveButton().shouldBe(enabled).click();
    editPanelForm.should(disappear);

    SummarySection summarySection = userManagementPage.summarySection(1);
    summarySection.shouldBe(visible);
    summarySection.firstName().shouldHave(text("testupdateFirstName"));
    summarySection.lastName().shouldHave(text("testupdateLastName"));
    summarySection.email().shouldHave(text("emailLastName@email.com"));

    userManagementPage.currentUser().shouldHave(text("ADMIN (ADMIN BUILTIN)"));
  }

  @Test
  public void testDeleteUser() {
    User user = createUser();
    refreshOrOpen(UserManagementPage.url());
    UserManagementPage userManagementPage = new UserManagementPage();

    int userRow = 0;
    userManagementPage.headers().get(userRow).shouldHave(text(user.getUsername()));

    userManagementPage.deleteUserButtons().get(userRow).shouldBe(visible).click();

    DeleteModal.header().shouldBe(headerText("User"));
    DeleteModal.continueButton().click();
    DeleteModal.body().should(disappear);

    userManagementPage.headers().shouldHaveSize(1);
  }

  @Test
  public void testAccordionClosesEditForm() {
    User user = createUser();
    refreshOrOpen(UserManagementPage.url());

    UserManagementPage userManagementPage = new UserManagementPage();

    int userRow = 0;
    SelenideElement accordionHeader = userManagementPage.headers().get(userRow);

    accordionHeader.shouldHave(text(user.getUsername()));
    userManagementPage.editUserButtons().get(userRow).shouldBe(visible).click();

    userManagementPage.editPanelForm().shouldBe(visible);
    userManagementPage.editUserButtons().get(userRow).shouldBe(disabled);

    accordionHeader.click();
    userManagementPage.editPanelForm().shouldBe(disappear);
    userManagementPage.editUserButtons().get(userRow).shouldBe(enabled);

  }

  @Test
  public void testDeleteModal() throws Exception {
    User user = createUser();
    refreshOrOpen(UserManagementPage.url());

    UserManagementPage userManagementPage = new UserManagementPage();

    int userRow = -1;
    String username = user.getUsername().toLowerCase();
    ElementsCollection headers = userManagementPage.headers();
    headers.shouldHave(sizeGreaterThan(0));

    for (int i = 0; i < headers.size(); i++) {
      SelenideElement element = userManagementPage.headers().get(i);
      if (element.getText().toLowerCase().contains(username)) {
        userRow = i;
        break;
      }
    }
    assertThat(userRow).isNotEqualTo(-1);

    SelenideElement accordionHeader = userManagementPage.headers().get(userRow);
    accordionHeader.shouldHave(text(user.getUsername()));
    userManagementPage.deleteUserButtons().get(userRow).shouldBe(visible).click();

    DeleteModal.root().shouldBe(visible);
    // Stop server to test error messages.
    testCLMServer.stop();
    DeleteModal.continueButton().click();
    DeleteModal.error().shouldBe(visible);
    DeleteModal.body().shouldBe(visible);
    staticTempEntity.cleanupAllPersistedUserSessions();
    // Start the server again, and log back in
    testCLMServer.start();

    initialLogin();
    refreshOrOpen(UserManagementPage.url());
    // Test proper delete.
    userManagementPage.deleteUserButtons().get(userRow).click();
    DeleteModal.root().shouldBe(visible);
    DeleteModal.continueButton().click();
    DeleteModal.root().shouldNotBe(visible);
    // Confirm delete
    accordionHeader.shouldNotHave(text(user.getUsername()));
  }

  private void keyInElementValue(final String inputText, final List<SelenideElement> elements) {
    elements.forEach(element -> element.val(inputText));
  }

  private void assertPopoverValidation(final String validationText, final List<SelenideElement> elements) {
    elements.forEach(element -> on(element).shouldShowError(validationText));
  }

  private NewUserForm goToCreateUserForm(final UserManagementPage userManagementPage) {
    Button newUserButton = userManagementPage.newUserButton();
    newUserButton.click();

    newUserButton.shouldBe(hidden);
    return userManagementPage.newUserForm();
  }

  private void assertElementsEmpty(final List<SelenideElement> elements) {
    elements.forEach(element -> element.shouldBe(empty));
  }

  private void assertElementsNotDisabled(final List<SelenideElement> elements) {
    elements.forEach(element -> element.shouldNotBe(disabled));
  }

  private void deleteUserIfExists(final String username) {
    User user = userDAO.getByUsername(username);
    if (user != null) {
      userDAO.delete(user);
    }
  }
}
