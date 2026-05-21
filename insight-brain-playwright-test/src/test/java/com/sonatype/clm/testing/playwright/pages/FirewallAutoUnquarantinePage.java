/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Firewall Auto-Unquarantine page.
 */
public class FirewallAutoUnquarantinePage
    extends BasePage
{
  private static final String ROOT = "#firewall-auto-unquarantine-page";

  public FirewallAutoUnquarantinePage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/firewall/autoReleaseQuarantine";
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    // byRole(HEADING,"Auto Release from Quarantine") matches BOTH the h1 AND the h3
    // "Auto Release from Quarantine Status" via substring. Scope to the container and use
    // exact matching so only the h1 is selected.
    return container().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Auto Release from Quarantine").setExact(true));
  }

  public Locator configurationModal() {
    // NxModal does not set aria-labelledby in this RSC version — use the stable id.
    return locator("#firewall-configuration-modal");
  }

  public Locator configureButton() {
    // Same NxTextLink reasoning as configureLink() — use CSS class for reliability.
    return container().locator(".nx-text-link");
  }

  // --------------- Auto-unquarantine status card ---------------

  /** The "Auto Release from Quarantine Status" card on the auto-unquarantine page. */
  public Locator statusCard() {
    return locator("#firewall-auto-unquarantine-status");
  }

  /**
   * "Active"/"Inactive" status text rendered inside the status indicator.
   * See {@code FirewallAutoUnquarantineStatus.jsx:30} — text lives in
   * {@code .iq-status-indicator > span}, not a {@code __label} class.
   */
  public Locator statusText() {
    return locator("#firewall-auto-unquarantine-status .iq-status-indicator > span");
  }

  /** "Configure" element in the status-card footer that opens the configuration modal. */
  public Locator configureLink() {
    // NxTextLink always renders with class nx-text-link — safer than relying on role
    // (the role depends on whether href is present, which can vary across RSC versions).
    return statusCard().locator(".nx-text-link");
  }

  // --------------- Auto-release configuration modal ---------------

  // Auto-unquarantine table
  public Locator unquarantineTableBody() {
    return locator("#iq-firewall-auto-unquarantine-table-body");
  }

  public Locator unquarantineTableRows() {
    return locator("#iq-firewall-auto-unquarantine-table-body tr");
  }

  // Firewall Configuration Modal
  public Locator modalIntegrityRatingToggle() {
    return locator("#auto-unquarantine-toggle-integrity-rating");
  }

  public Locator modalConditionToggles() {
    return locator("#auto-release-condition-toggles .nx-toggle");
  }

  public Locator modalConditionToggle(int index) {
    return locator("#auto-release-condition-toggles .nx-toggle:nth-of-type(" + index + ")");
  }

  public Locator modalSaveButton() {
    return locator("#firewall-configuration-modal .nx-btn--primary");
  }

  public Locator modalCancelButton() {
    return locator("#firewall-configuration-modal .nx-btn:not(.nx-btn--primary)[type='button']");
  }

  public Locator modalLoadError() {
    return locator("#firewall-configuration-modal .nx-alert--load-error");
  }

  public Locator modalInfoAlert() {
    return locator("#firewall-configuration-modal .nx-alert--info");
  }

  public Locator modalInfoLink() {
    return locator("#firewall-configuration-modal .nx-alert--info a.nx-text-link--external");
  }

  /**
   * "Read More" link inside the modal's info alert. Anchored under the modal so we don't risk
   * matching any other "Read More" link on the underlying page (legacy regression: the modal
   * is rendered as a sibling of the page's main content, not a child of it, so a non-anchored
   * locator can match unintended targets when other modals are open).
   */
  public Locator modalReadMoreLink() {
    return locator("#firewall-configuration-modal .nx-alert--info a:has-text('Read More')");
  }

  // --------------- Modal actions ---------------

  /** Click the status-card "Configure" link and wait for the modal to render. */
  public void openConfigurationModal() {
    configureLink().click();
    assertThat(configurationModal()).isVisible();
  }

  /** Click the modal's Cancel button and wait for the modal to detach. */
  public void cancelConfigurationModal() {
    modalCancelButton().click();
    assertThat(configurationModal()).isHidden();
  }

  public Locator loadError() {
    return locator(ROOT + " .nx-alert--load-error");
  }

  public Locator loadErrorRetry() {
    return locator(ROOT + " .nx-load-error__retry");
  }
}
