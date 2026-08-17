/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.microsoft.playwright.Page;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.UserTokenConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.UserTokenConfigurationPageAssertions;
import com.sonatype.clm.testing.playwright.pages.UserTokenModal;
import com.sonatype.clm.testing.playwright.pages.UserTokenModalAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class UserTokenPlaywrightTest
    extends AbstractIqUiTest
{
  private String originalExpirationDays;

  @BeforeEach
  public void setUpFreshBrowserAndCaptureExpirationDays() {
    playwrightHardreset();
    originalExpirationDays = lookup(SystemConfigurationPropertyDAO.class)
        .get(SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS);
  }

  @Test
  @Tag("sanity")
  public void testGenerateUserToken() {
    User user = seedUser();
    loginAndOpenUserTokenModal(user);

    UserTokenModal modal = new UserTokenModal();
    new UserTokenModalAssertions(modal).shouldShowInitialState();

    modal.generateToken();
    new UserTokenModalAssertions(modal).shouldShowGeneratedCredentials();
    modal.close();

    new HeaderComponent().openManageUserTokenModal();
    new UserTokenModalAssertions(modal).shouldShowExistingTokenState();
    modal.close();
  }

  @Test
  @Tag("sanity")
  public void testDeleteUserToken() {
    User user = seedUser();
    loginAndOpenUserTokenModal(user);

    UserTokenModal modal = new UserTokenModal();

    modal.generateToken();
    new UserTokenModalAssertions(modal).shouldShowGeneratedCredentials();
    modal.close();

    new HeaderComponent().openManageUserTokenModal();
    new UserTokenModalAssertions(modal).shouldShowExistingTokenState();

    modal.deleteToken();
    new UserTokenModalAssertions(modal).shouldShowInitialState();
    modal.close();
  }

  private void loginAndOpenUserTokenModal(User user) {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLoginAt(DashboardPage.url(), user.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    HeaderComponent header = new HeaderComponent();
    new HeaderComponentAssertions(header).shouldBeLoggedIn();
    new DashboardPage().waitUntilSpinnersGone();
    // Wait for the post-login uiRouter transition to finish before opening the modal. The
    // userToken redux reducer treats UI_ROUTER_ON_FINISH as a full-state reset (sets
    // isUserTokenModalVisible=false), so a late onFinish event firing after the click would
    // silently unmount the modal and cause downstream "modal not visible" timeouts.
    page.waitForURL(Pattern.compile("#/dashboard/.+"),
        new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    header.openManageUserTokenModal();
    new UserTokenModalAssertions(new UserTokenModal()).shouldShowInitialState();
  }

  private User seedUser() {
    return tempEntity.newUser();
  }

  @Test
  @Tag("regression")
  public void testUserTokenConfigurationPageRenders() {
    playwrightRefreshOrOpen(UserTokenConfigurationPage.url());
    playwrightLogin();

    UserTokenConfigurationPage configPage = new UserTokenConfigurationPage();
    assertThat(configPage.container()).isVisible();
    assertThat(configPage.pageHeading()).isVisible();
    assertThat(configPage.tile()).isVisible();
  }

  @AfterEach
  public void restoreExpirationDays() {
    lookup(SystemConfigurationPropertyDAO.class)
        .set(SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS, originalExpirationDays);
  }

  @Test
  @Tag("regression")
  public void testUserToken_enablingExpirationPersistsAcrossReload() {
    lookup(SystemConfigurationPropertyDAO.class)
        .set(SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS, null);

    playwrightRefreshOrOpen(UserTokenConfigurationPage.url());
    playwrightLogin();

    UserTokenConfigurationPage configPage = new UserTokenConfigurationPage();
    UserTokenConfigurationPageAssertions configAssertions =
        new UserTokenConfigurationPageAssertions(configPage);

    configAssertions.shouldHaveExpirationToggleUnchecked();
    configPage.expirationToggle().click();
    configPage.updateButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(UserTokenConfigurationPage.url());
    configAssertions.shouldHaveExpirationToggleChecked();
  }

  @Test
  @Tag("regression")
  public void testUserToken_disablingExpirationPersistsAcrossReload() {
    lookup(SystemConfigurationPropertyDAO.class)
        .set(SystemConfigurationProperty.USER_TOKEN_DEFAULT_EXPIRATION_DAYS, "30");

    playwrightRefreshOrOpen(UserTokenConfigurationPage.url());
    playwrightLogin();

    UserTokenConfigurationPage configPage = new UserTokenConfigurationPage();
    UserTokenConfigurationPageAssertions configAssertions =
        new UserTokenConfigurationPageAssertions(configPage);

    configAssertions.shouldHaveExpirationToggleChecked();
    configPage.expirationToggle().click();
    configPage.updateButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(UserTokenConfigurationPage.url());
    configAssertions.shouldHaveExpirationToggleUnchecked();
  }
}
