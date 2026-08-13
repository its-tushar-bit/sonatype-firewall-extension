/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * <p>
 * Authoring rules: see {@code TestAuthourskill.md}. Backend access is encapsulated in the
 * nested {@link FirewallConfigurationModalSeeder} (§3c).
 * <p>
 * <b>Note on isolation:</b> {@link #testToggleIntegrityRating()} flips an
 * {@code AutoUnquarantinePolicyConditionType} row that lives outside the {@code tempEntity}
 * lifecycle, so it persists across tests within the same fork. {@link #hardresetAfterTest()}
 * clears the browser state but does not roll the row back. Tests in this class therefore must
 * not assume the policy-condition table is empty at start.
 */
public class FirewallConfigurationModalPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String INFO_ALERT_TEXT =
      "Components will only auto-release from quarantine if its status changes within the 14 day window.";

  private static final String READ_MORE_HREF =
      "https://links.sonatype.com/products/firewall/doc/automatic-quarantine-release";

  private static final String READ_MORE_TARGET = "_blank";

  private static final String STATUS_INACTIVE = "Inactive";

  private static final String STATUS_ACTIVE = "Active";

  @Before
  public void openAutoUnquarantineAsAdmin() {
    setFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY);

    playwrightRefreshOrOpen(FirewallPage.url());
    playwrightLogin();
    playwrightRefreshOrOpen(FirewallAutoUnquarantinePage.url());
  }

  /**
   * Browser-state reset only — see class-level doc on why we don't roll back the
   * {@code auto_unquarantine_policy_condition_type} table here. The hardreset matches the
   * legacy Selenide test's behaviour (skill §3b: {@code @After} is for global side-effects).
   */
  @After
  public void hardresetAfterTest() {
    playwrightHardreset();
  }

  /**
   * Open the modal and verify its info alert + "Read More" external link, then cancel and
   * verify the modal hides. Mirrors the legacy Selenide
   * {@code testFirewallConfigurationModal_InfoAlertAndReadMoreLink}.
   */
  @Test
  @Category(SanityTest.class)
  public void testInfoAlertAndReadMoreLink() {
    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();

    autoPage.openConfigurationModal();
    assertThat(autoPage.configurationModal()).isVisible();
    assertThat(autoPage.modalInfoAlert()).isVisible();
    assertThat(autoPage.modalInfoAlert()).containsText(INFO_ALERT_TEXT);

    assertThat(autoPage.modalReadMoreLink()).isVisible();
    assertThat(autoPage.modalReadMoreLink()).hasAttribute("href", READ_MORE_HREF);
    assertThat(autoPage.modalReadMoreLink()).hasAttribute("target", READ_MORE_TARGET);

    autoPage.cancelConfigurationModal();
    assertThat(autoPage.configurationModal()).isHidden();
  }

  /**
   * Verify the status indicator reads "Inactive" before any toggle is enabled, then open the
   * modal and verify the Save + Cancel buttons render. Mirrors the legacy Selenide
   * {@code testFirewallConfigurationModal_DefaultValues}.
   */
  @Test
  @Category(SanityTest.class)
  public void testDefaultValues() {
    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();

    assertThat(autoPage.statusText()).containsText(STATUS_INACTIVE);

    autoPage.openConfigurationModal();
    assertThat(autoPage.configurationModal()).isVisible();
    assertThat(autoPage.modalCancelButton()).isVisible();
    assertThat(autoPage.modalSaveButton()).isVisible();
  }

  /**
   * Toggle the Integrity Rating auto-unquarantine condition to "on", save, and verify the UI
   * status indicator flips to "Active". Mirrors the legacy Selenide
   * {@code testFirewallConfigurationModal_ToggleIntegrityRating}.
   */
  @Test
  @Category(SanityTest.class)
  public void testToggleIntegrityRating() {
    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();

    autoPage.openConfigurationModal();
    assertThat(autoPage.configurationModal()).isVisible();

    autoPage.modalIntegrityRatingToggle().click();
    autoPage.modalSaveButton().click();
    waitForSubmitMask();

    assertThat(autoPage.configurationModal()).isHidden();
    assertThat(autoPage.statusText()).containsText(STATUS_ACTIVE);
  }
}
