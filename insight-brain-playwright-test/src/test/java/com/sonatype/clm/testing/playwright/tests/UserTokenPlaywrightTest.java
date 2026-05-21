/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.microsoft.playwright.Page;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.HeaderComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.UserTokenModal;
import com.sonatype.clm.testing.playwright.pages.UserTokenModalAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Coverage for the Manage User Token modal opened from the user menu
 * ({@link UserTokenModal}, JSX: {@code UserTokenModal.jsx}).
 *
 * <p>
 * Authoring rules: see {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md}.
 *
 * <ul>
 * <li>Each test logs in as a different non-admin user, so {@code @Before} clears browser storage to
 * keep tests independent (§3b).</li>
 * <li>Per-test backend wiring (user creation) lives in the private {@link #seedUser()} helper.</li>
 * <li>Modal state assertions are encoded as semantic methods on {@link UserTokenModal}
 * ({@code shouldShowInitialState()}, {@code shouldShowGeneratedCredentials()},
 * {@code shouldShowExistingTokenState()}) instead of being repeated in each test (§4).</li>
 * </ul>
 */
public class UserTokenPlaywrightTest
    extends AbstractIqUiTest
{
  @Before
  public void clearBrowserState() {
    playwrightHardreset();
  }

  @Test
  @Category(SanityTest.class)
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
  @Category(SanityTest.class)
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

  // --------------- Test helpers ---------------

  /**
   * Log in as the given user and open the Manage User Token modal.
   *
   * <p>
   * <strong>Why we wait for the dashboard to settle before opening the modal:</strong> the
   * userToken redux reducer treats {@code UI_ROUTER_ON_FINISH} as a full-state reset
   * ({@code userTokenReducer.js:153} — {@code [UI_ROUTER_ON_FINISH]: always(initialState)}),
   * which sets {@code isUserTokenModalVisible: false} and unmounts the modal. Under parallel
   * runs the post-login uiRouter transition (e.g. resolving
   * {@code dashboard.overview.violations}) frequently completed <em>after</em> the test had
   * already clicked "Manage User Token" — firing {@code UI_ROUTER_ON_FINISH} mid-modal-open
   * and silently unmounting the modal. The downstream
   * {@code waitFor("#user-token-modal #generate-user-token")} then timed out because the
   * modal element itself never re-appeared.
   *
   * <p>
   * Waiting for the dashboard's chrome (header menu bar visible + spinners gone) before
   * opening the user menu lets the post-login router transition finish first, so the modal
   * mounts <em>after</em> all router transitions have settled.
   */
  private void loginAndOpenUserTokenModal(User user) {
    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLoginAt(DashboardPage.url(), user.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    HeaderComponent header = new HeaderComponent();
    new HeaderComponentAssertions(header).shouldBeLoggedIn();
    new DashboardPage().waitUntilSpinnersGone();
    // Belt-and-suspenders for the UI_ROUTER_ON_FINISH race documented above: even after the
    // header chrome has rendered and the dashboard spinner has cleared, uiRouter can still be
    // mid-transition on a cold backend. Waiting for the URL hash to actually land on the
    // dashboard route guarantees the transition has completed before we click "Manage User
    // Token", so the modal won't be unmounted by a late onFinish event.
    page.waitForURL(Pattern.compile("#/dashboard/.+"),
        new Page.WaitForURLOptions().setTimeout(PlaywrightTiming.URL_EXACT_TIMEOUT_MS));

    header.openManageUserTokenModal();
    new UserTokenModalAssertions(new UserTokenModal()).shouldShowInitialState();
  }

  /**
   * Per-test user creation. Uses {@code tempEntity.newUser()}'s no-arg overload to get a unique
   * username per test (authoring guide §7b: "Name every seeded entity uniquely").
   */
  private User seedUser() {
    return tempEntity.newUser();
  }
}
