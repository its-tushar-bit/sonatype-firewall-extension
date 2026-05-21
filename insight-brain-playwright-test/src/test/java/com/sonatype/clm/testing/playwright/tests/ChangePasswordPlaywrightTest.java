/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ChangePasswordModal;
import com.sonatype.clm.testing.playwright.pages.ChangePasswordModalAssertions;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright migration of the Selenide {@code ChangePasswordTest}.
 */
public class ChangePasswordPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String USERNAME = "testchangepass";

  private static final String FIRST_NAME = "John";

  private static final String LAST_NAME = "Doe";

  private static final String EMAIL = "john@doe.com";

  private static final String WRONG_OLD_PASSWORD = "unsecret";

  private static final String NEW_PASSWORD = "newsecret";

  private static final String MISMATCHED_CONFIRMATION = "newsecretdoesntmatch";

  private static final String PASSWORD_MISMATCH_MESSAGE = "New Password and Confirmation must match";

  /** Display name shown in the user menu — IQ renders {@code "First Last"}. */
  private static final String FULL_NAME = FIRST_NAME + " " + LAST_NAME;

  @Test
  @Category(SanityTest.class)
  public void testChangePassword() {
    User user = tempEntity.newUser(USERNAME, FIRST_NAME, LAST_NAME, EMAIL);

    playwrightRefreshOrOpen(ReportListPage.url());
    playwrightLoginAt(ReportListPage.url(), user.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    HeaderComponent header = new HeaderComponent();
    header.openChangePasswordModal();

    ChangePasswordModal modal = new ChangePasswordModal();
    new ChangePasswordModalAssertions(modal).shouldBeVisible();

    modal.fillPasswords(WRONG_OLD_PASSWORD, NEW_PASSWORD, MISMATCHED_CONFIRMATION);
    new ChangePasswordModalAssertions(modal).shouldShowPasswordMismatchValidation(PASSWORD_MISMATCH_MESSAGE);

    modal.newPasswordValidate().fill(NEW_PASSWORD);
    modal.submit();
    new ChangePasswordModalAssertions(modal).shouldShowInvalidCredentialsError();

    modal.oldPassword().fill(TemporaryEntity.USER_PASSWORD_CLEAR);
    modal.submit();
    new ChangePasswordModalAssertions(modal).shouldBeHidden();

    playwrightLogout();
    playwrightLoginAt(ReportListPage.url(), user.getUsername(), NEW_PASSWORD);

    header.openUserMenu();
    assertThat(header.userName()).containsText(FULL_NAME);
  }
}
