/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.MtiqAdministratorsEditPage;
import com.sonatype.clm.testing.playwright.pages.MtiqAdministratorsEditPageAssertions;
import com.sonatype.clm.testing.playwright.pages.MtiqSystemPreferencesPage;
import com.sonatype.clm.testing.playwright.pages.MtiqSystemPreferencesPageAssertions;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.brain.model.security.Role;

import com.microsoft.playwright.assertions.LocatorAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqUiExclusionsPlaywrightTest
    extends AbstractMtiqUiTest
{
  private MtiqAdministratorsEditPage adminEditPage;

  private MtiqAdministratorsEditPageAssertions adminEditAssertions;

  private MtiqSystemPreferencesPage sysPrefsPage;

  private MtiqSystemPreferencesPageAssertions sysPrefsAssertions;

  // 4 consecutive @Before logins under forkCount=4 CI load can approach the base-class 30 s
  // ceiling; 15 s of headroom eliminates spurious flakes without widening the timeout globally.
  @Override
  protected void playwrightLoginAt(String path, String username, String password) {
    playwrightRefreshOrOpen(path);
    new LoginPage().loginAs(username, password);
    assertThat(new HeaderComponent().userMenu())
        .isVisible(new LocatorAssertions.IsVisibleOptions()
            .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS + PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
  }

  @Before
  public void loginAndCreatePageObjects() {
    playwrightLoginAdminAt(DashboardPage.url());
    adminEditPage = new MtiqAdministratorsEditPage();
    adminEditAssertions = new MtiqAdministratorsEditPageAssertions(adminEditPage);
    sysPrefsPage = new MtiqSystemPreferencesPage();
    sysPrefsAssertions = new MtiqSystemPreferencesPageAssertions(sysPrefsPage);
  }

  /**
   * {@code !groupSearchEnabled && !isMultiTenant} guard in {@code AdministratorsEdit.jsx}
   * requires {@code !isMultiTenant} — always {@code false} in MTIQ — so
   * {@code #ldap-servers-alert} is never rendered.
   */
  @Test
  public void testMtiqUiExclusions_administrators_ldapGroupFieldsHidden() {
    playwrightNavigateTo(MtiqAdministratorsEditPage.url(Role.POLICY_ADMIN_ROLE_ID));
    // Page load after navigation takes longer under forkCount=4 CI load.
    assertThat(adminEditPage.addMembersForm())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));

    adminEditAssertions.ldapGroupSearchAlertShouldBeHidden();
  }

  /**
   * {@code RetentionTile.jsx}: {@code isFeatureEnabledForLicense = isDataRetentionEnabled && !isMultiTenant}
   * — always {@code false} in MTIQ — so {@code #owner-pill-retention} is not rendered.
   */
  @Test
  public void testMtiqUiExclusions_ownerSummary_retentionTileHidden() {
    playwrightNavigateTo(OwnerSummaryPage.urlToRootOrg());
    new OwnerSummaryPageAssertions(new OwnerSummaryPage()).shouldBeVisible();

    assertThat(new OwnerSummaryPage().dataRetentionTile())
        .isHidden(new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  /**
   * {@code selectIsBaseUrlConfigurationEnabled = (selectTenantMode === SINGLE_TENANT)} →
   * {@code false} in MTIQ → "Base URL" NavLink not rendered.
   */
  @Test
  public void testMtiqUiExclusions_systemPreferences_baseUrlLinkHidden() {
    sysPrefsPage.menuToggle().click();
    sysPrefsAssertions.administratorsLinkShouldBeVisible();
    sysPrefsAssertions.baseUrlLinkShouldBeHidden();
  }

  /**
   * {@code MTIQFeatureService} removes {@code EMAIL_CONFIGURATION} when no mail config exists
   * (CLM-38607). Fresh test tenant has no mail config → "Email" NavLink not rendered.
   */
  @Test
  public void testMtiqUiExclusions_systemPreferences_emailLinkHidden() {
    sysPrefsPage.menuToggle().click();
    sysPrefsAssertions.administratorsLinkShouldBeVisible();
    sysPrefsAssertions.emailLinkShouldBeHidden();
  }
}
