/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.UserTokenModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.Test;

public class UserTokenTest
    extends AbstractFunctionalTest
{
  @Before
  public void before() {
    // Ensure we start with a clean session
    hardreset();
  }

  @Test
  public void testGenerateUserToken() {
    User user = tempEntity.newUser("user1", "user1", "user1", "user1@mail.com");
    refreshOrOpen(DashboardPage.url());
    login(user.getUsername(), user.getPassword());

    MainHeader.userMenu().dropdownToggle().shouldBe(Condition.visible).click();
    MainHeader.userMenu().manageUserToken().shouldBe(Condition.visible).click();

    UserTokenModal userTokenModal = new UserTokenModal();
    userTokenModal.should(Condition.appear);
    // A new user shouldn't have a token.
    userTokenModal.tokenExistenceAlert().shouldNotBe(Condition.visible);
    userTokenModal.deleteUserTokenButton().shouldNotBe(Condition.visible);
    userTokenModal.userCodeInput().shouldNotBe(Condition.visible);
    userTokenModal.passCodeInput().shouldNotBe(Condition.visible);

    // Generate the token
    userTokenModal.generateUserTokenButton().shouldBe(Condition.visible).click();
    userTokenModal.userCodeInput().shouldBe(Condition.visible);
    userTokenModal.passCodeInput().shouldBe(Condition.visible);
    userTokenModal.deleteUserTokenButton().shouldNotBe(Condition.visible);
    userTokenModal.generateUserTokenButton().shouldNotBe(Condition.visible);
    eyesWatcher.eyesCheck("Recently Generated User Token");
    // Close the modal
    userTokenModal.cancelButton().shouldBe(Condition.enabled).click();
    userTokenModal.should(Condition.disappear);

    // Re-open modal
    MainHeader.userMenu().dropdownToggle().shouldBe(Condition.visible).click();
    MainHeader.userMenu().manageUserToken().shouldBe(Condition.visible).click();
    userTokenModal.should(Condition.appear);
    userTokenModal.tokenExistenceAlert().shouldBe(Condition.visible);
    userTokenModal.generateUserTokenButton().shouldNotBe(Condition.visible);
    userTokenModal.passCodeInput().shouldNotBe(Condition.visible);
    userTokenModal.userCodeInput().shouldNotBe(Condition.visible);
    userTokenModal.cancelButton().shouldBe(Condition.visible).click();

    logout();
  }

  @Test
  public void testDeleteUserToken() {
    User user = tempEntity.newUser("user2", "user2", "user2", "user2@mail.com");
    refreshOrOpen(DashboardPage.url());
    login(user.getUsername(), user.getPassword());

    MainHeader.userMenu().dropdownToggle().shouldBe(Condition.visible).click();
    MainHeader.userMenu().manageUserToken().shouldBe(Condition.visible).click();

    // Generate token and close modal
    UserTokenModal userTokenModal = new UserTokenModal();
    userTokenModal.generateUserTokenButton().shouldBe(Condition.visible).click();
    userTokenModal.userCodeInput().shouldBe(Condition.visible);
    userTokenModal.passCodeInput().shouldBe(Condition.visible);
    userTokenModal.cancelButton().shouldBe(Condition.enabled).click();
    userTokenModal.should(Condition.disappear);
    // reopen and delete
    MainHeader.userMenu().dropdownToggle().shouldBe(Condition.visible).click();
    MainHeader.userMenu().manageUserToken().shouldBe(Condition.visible).click();
    userTokenModal.should(Condition.appear);
    userTokenModal.tokenExistenceAlert().shouldBe(Condition.visible);
    eyesWatcher.eyesCheck("User Token Already Exists");
    userTokenModal.deleteUserTokenButton().shouldBe(Condition.visible).click();
    userTokenModal.tokenExistenceAlert().shouldNotBe(Condition.visible);
    userTokenModal.passCodeInput().shouldNotBe(Condition.visible);
    userTokenModal.userCodeInput().shouldNotBe(Condition.visible);
    userTokenModal.generateUserTokenButton().shouldBe(Condition.visible);

    // Close the modal before logout
    userTokenModal.cancelButton().shouldBe(Condition.visible).click();
    userTokenModal.should(Condition.disappear);

    logout();
  }

  @Test
  public void testUserTokenExpirationDisplayed() {
    // Set token expiration days configuration property to enable expiration feature
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS, "30");

    User user = tempEntity.newUser("user3", "user3", "user3", "user3@mail.com");
    refreshOrOpen(DashboardPage.url());
    login(user.getUsername(), user.getPassword());

    // Generate a token first
    MainHeader.userMenu().dropdownToggle().shouldBe(Condition.visible).click();
    MainHeader.userMenu().manageUserToken().shouldBe(Condition.visible).click();

    UserTokenModal userTokenModal = new UserTokenModal();
    userTokenModal.should(Condition.appear);
    userTokenModal.generateUserTokenButton().shouldBe(Condition.visible).click();
    userTokenModal.userCodeInput().shouldBe(Condition.visible);
    userTokenModal.passCodeInput().shouldBe(Condition.visible);
    userTokenModal.cancelButton().shouldBe(Condition.enabled).click();
    userTokenModal.should(Condition.disappear);

    // Reopen modal to verify expiration info is displayed
    MainHeader.userMenu().dropdownToggle().shouldBe(Condition.visible).click();
    MainHeader.userMenu().manageUserToken().shouldBe(Condition.visible).click();
    userTokenModal.should(Condition.appear);
    userTokenModal.tokenExistenceAlert().shouldBe(Condition.visible);
    
    userTokenModal.deleteUserTokenButton().shouldBe(Condition.visible);

    // Verify expiration section is displayed
    userTokenModal.expirationSection().shouldBe(Condition.visible);
    userTokenModal.expirationHeading().shouldBe(Condition.visible).shouldHave(Condition.text("User Token Status"));
    userTokenModal.expirationSubtitle().shouldBe(Condition.visible)
        .shouldHave(Condition.text("Time remaining until user token expires"));
    userTokenModal.expirationDate().shouldBe(Condition.visible).shouldHave(Condition.text("Expires:"));

    eyesWatcher.eyesCheck("User Token with Expiration Info");

    userTokenModal.cancelButton().shouldBe(Condition.visible).click();
    logout();
  }
}
