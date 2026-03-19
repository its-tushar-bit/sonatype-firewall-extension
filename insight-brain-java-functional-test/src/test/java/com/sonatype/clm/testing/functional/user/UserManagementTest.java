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
import com.sonatype.clm.testing.functional.elements.NxTextInput;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.UserManagementPage;
import com.sonatype.clm.testing.functional.pages.UserManagementPage.EditUserForm;
import com.sonatype.clm.testing.functional.pages.UserManagementPage.NewUserForm;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static java.util.Arrays.asList;

public class UserManagementTest
    extends AbstractFunctionalTest
{
  private static final String TEST_USERNAME = "addusertest";

  private UserDAO userDAO;

  @Before
  public void initialLogin() {
    userDAO = lookup(UserDAO.class);
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
  public void createUserTest() {
    testCreateUser_formOpen();
    testCreateUserInputs_spaceValidations();
    testCreateUserInputs_invalidCharacters();
    testCreateUserInputs_emptyValues();
    testCreateUserInputs_nonMatchingPassword();
    testCreateUser_success();
  }

  private void cleanup() {
    refreshOrOpen(UserManagementPage.url());
    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.continueButton().click();
  }

  private void cleanupWithoutUnsavedModal() {
    refreshOrOpen(UserManagementPage.url());
  }

  public void testCreateUser_formOpen() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());
    newUserForm.shouldBe(visible);
    newUserForm.saveButton().shouldBe(visible);

    List<SelenideElement> formInputs = Arrays.asList(newUserForm.firstNameInput(), newUserForm.lastNameInput(),
        newUserForm.emailInput(), newUserForm.usernameInput(), newUserForm.passwordInput(),
        newUserForm.passwordValidateInput());

    assertElementsNotDisabled(formInputs);
    assertElementsEmpty(formInputs);
    refreshOrOpen(UserManagementPage.url());
  }

  public void testCreateUserInputs_spaceValidations() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());
    String invalidSpacingError = "No leading, trailing or double spaces or tabs";

    keyInElementValue("a  a", asList(newUserForm.firstNameInput(), newUserForm.lastNameInput()));

    new NxTextInput(newUserForm.firstNameInput()).errorMessage().shouldHave(exactText(invalidSpacingError));
    new NxTextInput(newUserForm.lastNameInput()).errorMessage().shouldHave(exactText(invalidSpacingError));
    cleanup();
  }

  public void testCreateUserInputs_invalidCharacters() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());
    String nameValidationText = "Use valid characters: alphanumeric, \"_\", \".\", \"-\", or spaces";
    String usernameValidationText = "Use valid characters: alphanumeric, \"_\", \".\" or \"-\"";

    List<SelenideElement> nameInputElements = asList(newUserForm.firstNameInput(), newUserForm.lastNameInput());
    List<SelenideElement> usernameInputElements = Collections.singletonList(newUserForm.usernameInput());
    keyInElementValue("#", nameInputElements);
    keyInElementValue("#", usernameInputElements);

    assertInputValidation(nameValidationText, nameInputElements);
    assertInputValidation(usernameValidationText, usernameInputElements);
    cleanup();
  }

  public void testCreateUserInputs_emptyValues() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());
    String validationText = "Must be non-empty";

    List<SelenideElement> inputElements = asList(newUserForm.firstNameInput(), newUserForm.lastNameInput(),
        newUserForm.emailInput(), newUserForm.usernameInput(),
        newUserForm.passwordInput(), newUserForm.passwordValidateInput());
    keyInElementValue("a", inputElements);
    clearElementsValue(inputElements);

    assertInputValidation(validationText, inputElements);
    cleanupWithoutUnsavedModal();
  }

  public void testCreateUserInputs_nonMatchingPassword() {
    NewUserForm newUserForm = goToCreateUserForm(new UserManagementPage());
    String validationText = "Passwords must match!";

    List<SelenideElement> passwordValidateInput = Collections.singletonList(newUserForm.passwordValidateInput());
    keyInElementValue("23abc", passwordValidateInput);

    assertInputValidation(validationText, passwordValidateInput);
    cleanup();
  }

  public void testCreateUser_success() {
    UserManagementPage userManagementPage = new UserManagementPage();
    NewUserForm newUserForm = goToCreateUserForm(userManagementPage);

    newUserForm.firstNameInput().val("add");
    newUserForm.lastNameInput().val("user");
    newUserForm.usernameInput().val(TEST_USERNAME);
    newUserForm.emailInput().val("addusertest@email.com");
    newUserForm.passwordInput().val("123abc");
    newUserForm.passwordValidateInput().val("123abc");

    popoverViolationsList(newUserForm.getElement()).shouldHave(size(0));

    newUserForm.saveButton().click();

    newUserForm.shouldBe(hidden);
    userManagementPage.userItems().shouldHave(size(2));// created user and the admin

    userManagementPage.userItems().get(0).shouldHave(text(TEST_USERNAME + " (add user)"));
  }

  @Test
  public void testUserEditProfile() {
    User user = createUser();
    int userRow = 0;
    refreshOrOpen(UserManagementPage.url());
    UserManagementPage userManagementPage = new UserManagementPage();

    EditUserForm editUserForm = goToEditUserForm(userManagementPage, userRow);
    editUserForm.shouldBe(visible);
    editUserForm.saveButton().shouldBe(visible);

    editUserForm.firstNameInput().shouldHave(value(user.getFirstName()));
    editUserForm.lastNameInput().shouldHave(value(user.getLastName()));
    editUserForm.emailInput().shouldHave(value(user.getEmail()));

    editUserForm.firstNameInput().val("testupdateFirstName");
    editUserForm.lastNameInput().val("testupdateLastName");
    editUserForm.emailInput().val("emailLastName@email.com");
    eyesWatcher.eyesCheck();

    editUserForm.saveButton().click();
    editUserForm.should(disappear);

    userManagementPage.userItems()
        .get(userRow)
        .shouldHave(text(user.getUsername() +
            " (testupdateFirstName testupdateLastName)"));
  }

  private void keyInElementValue(final String inputText, final List<SelenideElement> elements) {
    elements.forEach(element -> element.val(inputText));
  }

  private void clearElementsValue(final List<SelenideElement> elements) {
    elements.forEach(this::clearField);
  }

  private void assertInputValidation(final String validationText, final List<SelenideElement> elements) {
    elements.forEach(element -> new NxTextInput(element).errorMessage().shouldHave(exactText(validationText)));
  }

  private NewUserForm goToCreateUserForm(final UserManagementPage userManagementPage) {
    Button newUserButton = userManagementPage.newUserButton();
    newUserButton.click();

    newUserButton.shouldBe(hidden);
    return userManagementPage.newUserForm();
  }

  private EditUserForm goToEditUserForm(final UserManagementPage userManagementPage, int row) {
    userManagementPage.userItems().get(row).shouldBe(visible).click();

    return userManagementPage.editUserForm();
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

  private void clearField(SelenideElement element) {
    while (!element.getAttribute("value").equals("")) {
      element.sendKeys(Keys.BACK_SPACE);
    }
  }
}
