/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions.IsVisibleOptions;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ViolationDetailsPage
    extends BasePage
{
  private static final String ROOT = "#violation-page";

  private static final String DETAILS_TILE = ROOT + " #violation-details-tile";

  private static final String CONSTRAINT_INFO = ROOT + " #policy-violation-constraint-info";

  private static final Locator.GetByRoleOptions VULNERABILITY_DETAILS_TAB_OPTS =
      new Locator.GetByRoleOptions().setName("Vulnerability Details");

  private static final Locator.GetByRoleOptions APPLICABLE_WAIVERS_TAB_OPTS =
      new Locator.GetByRoleOptions().setName("Applicable Waivers");

  private static final Locator.GetByRoleOptions SIMILAR_WAIVERS_TAB_OPTS =
      new Locator.GetByRoleOptions().setName("Similar Waivers");

  private static final Locator.GetByRoleOptions MANAGE_WAIVERS_OPTS =
      new Locator.GetByRoleOptions().setName("Manage Waivers");

  private static final Locator.GetByRoleOptions ADD_WAIVER_OPTS =
      new Locator.GetByRoleOptions().setName("Add Waiver");

  private static final Locator.GetByRoleOptions REQUEST_WAIVER_OPTS =
      new Locator.GetByRoleOptions().setName("Request Waiver");

  public ViolationDetailsPage() {
    super();
  }

  public static String url(String violationId) {
    return "/assets/index.html#/violation/" + violationId;
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator detailsTile() {
    return locator(DETAILS_TILE);
  }

  public Locator constraintInfo() {
    return locator(CONSTRAINT_INFO);
  }

  public Locator securityDetailsTile() {
    return locator(ROOT + " #security-vulnerability-details-tile");
  }

  public Locator applicableWaiversTile() {
    return locator(ROOT + " #applicable-waivers-tile");
  }

  public Locator similarWaiversTile() {
    return locator(ROOT + " #similar-waivers-tile");
  }

  public Locator securityTab() {
    return container().getByRole(AriaRole.TAB, VULNERABILITY_DETAILS_TAB_OPTS);
  }

  public Locator applicableWaiversTab() {
    return container().getByRole(AriaRole.TAB, APPLICABLE_WAIVERS_TAB_OPTS);
  }

  public Locator similarWaiversTab() {
    return container().getByRole(AriaRole.TAB, SIMILAR_WAIVERS_TAB_OPTS);
  }

  public Locator detailsTitle() {
    return locator(DETAILS_TILE + " > header.nx-tile-header .nx-tile-header__title");
  }

  public Locator detailsSubtitle() {
    return locator(DETAILS_TILE + " > header.nx-tile-header .nx-tile-header__subtitle");
  }

  public Locator threatLevel() {
    return locator(DETAILS_TILE + " .iq-violation-details__threat-level dd");
  }

  public Locator firstReported() {
    return locator(DETAILS_TILE + " .iq-violation-details__first-reported dd");
  }

  public Locator lastReported() {
    return locator(DETAILS_TILE + " .iq-violation-details__last-reported dd");
  }

  public Locator policyType() {
    return locator(DETAILS_TILE + " .iq-violation-details__policy-type dd");
  }

  public Locator componentName() {
    return locator(
        DETAILS_TILE
            + " > header.nx-tile-header .nx-tile-header__subtitle .iq-violation-details__subtitle-part:nth-child(3)");
  }

  public Locator policyName() {
    return detailsTitle();
  }

  public Locator constraintSection() {
    return constraintInfo();
  }

  public Locator constraintInfoTitle() {
    return locator(CONSTRAINT_INFO + " .nx-tile-header__title");
  }

  public Locator conditionsSection() {
    return locator(CONSTRAINT_INFO + " #policy-violation-reasons");
  }

  public Locator constraintReasons() {
    return locator(CONSTRAINT_INFO + " #policy-violation-reasons li");
  }

  public Locator manageWaiversButton() {
    return container().getByRole(AriaRole.BUTTON, MANAGE_WAIVERS_OPTS);
  }

  public Locator addWaiverButton() {
    return container().getByRole(AriaRole.BUTTON, ADD_WAIVER_OPTS);
  }

  public Locator requestWaiverButton() {
    return container().getByRole(AriaRole.BUTTON, REQUEST_WAIVER_OPTS);
  }

  public Locator warningAlert() {
    return container().getByRole(AriaRole.IMG, new Locator.GetByRoleOptions().setName("Warning"))
        .locator("..");
  }

  public Locator applicableWaiversBadge() {
    return container().getByRole(AriaRole.TAB, APPLICABLE_WAIVERS_TAB_OPTS)
        .locator(".iq-waiver-indicator__counter")
        .first();
  }

  public Locator similarWaiversSubtitle() {
    return container().getByText("Across all component versions");
  }

  public Locator similarWaiversFilterDropdown() {
    return similarWaiversTile().getByRole(AriaRole.BUTTON, CommonButtonOptions.FILTER_BUTTON_OPTS).locator("..");
  }

  public Locator similarWaiversFilterToggle() {
    return similarWaiversTile().getByRole(AriaRole.BUTTON, CommonButtonOptions.FILTER_BUTTON_OPTS);
  }

  public Locator similarWaiversFilterOptions() {
    return similarWaiversTile().getByRole(AriaRole.CHECKBOX).locator("..");
  }

  public Locator backButton() {
    return container().getByRole(AriaRole.LINK, CommonButtonOptions.BACK_BUTTON_OPTS);
  }

  public Locator popoverSection() {
    return container().locator(".iq-violation-details-popover-section");
  }

  public Locator sidebarNavItems() {
    return locator(ROOT + " #sidebar-nav-list li");
  }

  public Locator sidebarNavItem(int index) {
    return locator(ROOT + " #sidebar-nav-list li:nth-child(" + (index + 1) + ")");
  }

  public void openSecurityTab() {
    securityTab().click();
    assertThat(securityDetailsTile()).isVisible();
  }

  public void openSimilarWaiversTab() {
    similarWaiversTab().click();
    assertThat(similarWaiversTile()).isVisible();
  }

  public void openApplicableWaiversTab() {
    applicableWaiversTab().click();
    assertThat(applicableWaiversTile()).isVisible();
  }

  public ListWaiversTablePage waitAndOpenApplicableWaiversTab(double timeoutMs) {
    assertThat(container()).isVisible(new IsVisibleOptions().setTimeout(timeoutMs));
    assertThat(applicableWaiversTab()).isVisible(new IsVisibleOptions().setTimeout(timeoutMs));
    applicableWaiversTab().click();
    ListWaiversTablePage listPage = new ListWaiversTablePage();
    assertThat(listPage.container()).isVisible();
    return listPage;
  }
}
