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
 * Playwright page object for the Violation Details page (rendered by
 * {@code violation/ViolationPage.jsx}).
 *
 * <p>
 * The page tree is anchored at {@link #ROOT} ({@code #violation-page}). Every selector below is
 * scoped under {@code ROOT} so a stale chrome element on the page (drawer, modal, banner) never
 * triggers a strict-mode violation. See authoring guide §4a.
 */
public class ViolationDetailsPage
    extends BasePage
{
  private static final String ROOT = "#violation-page";

  /** Container ids used inside the violation page. Kept private so callers go through methods. */
  private static final String DETAILS_TILE = ROOT + " #violation-details-tile";

  private static final String CONSTRAINT_INFO = ROOT + " #policy-violation-constraint-info";

  public ViolationDetailsPage() {
    super();
  }

  /**
   * Hash route for the violation details page.
   *
   * <p>
   * The route is registered in {@code violation/route.js} as the abstract parent {@code sidebarView}
   * (url {@code /violation}) plus child {@code sidebarView.violation} (url {@code /{id}}), so the
   * full hash path is {@code #/violation/{id}}. Do <strong>not</strong> use {@code /violationDetails/}
   * — that path matches no registered router state, leaves {@code #iq-content} empty, and the test
   * will time out waiting for {@code #violation-page}.
   */
  public static String url(String violationId) {
    return "/assets/index.html#/violation/" + violationId;
  }

  // --------------- Top-level container ---------------

  public Locator container() {
    return locator(ROOT);
  }

  // --------------- Tiles ---------------

  public Locator detailsTile() {
    return locator(DETAILS_TILE);
  }

  public Locator constraintInfo() {
    return locator(CONSTRAINT_INFO);
  }

  /**
   * "Security Vulnerability Details" tile shown after switching to the Vulnerability Details
   * tab on a security-policy violation.
   */
  public Locator securityDetailsTile() {
    return locator(ROOT + " #security-vulnerability-details-tile");
  }

  public Locator applicableWaiversTile() {
    return locator(ROOT + " #applicable-waivers-tile");
  }

  public Locator similarWaiversTile() {
    return locator(ROOT + " #similar-waivers-tile");
  }

  // --------------- Tabs ---------------

  /**
   * "Vulnerability Details" tab ({@code NxTab} with accessible name "Vulnerability Details").
   */
  public Locator securityTab() {
    return container().getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Vulnerability Details"));
  }

  public Locator applicableWaiversTab() {
    return container().getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Applicable Waivers"));
  }

  public Locator similarWaiversTab() {
    return container().getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Similar Waivers"));
  }

  // --------------- Details tile fields ---------------

  /**
   * Details tile title — also used as a synonym for the policy name. Anchored under the immediate
   * {@code > header.nx-tile-header} child of {@code #violation-details-tile} because
   * {@code PolicyViolationConstraintInfo} (which is also a tile-with-header) is rendered <em>inside</em>
   * the violation details tile section (see {@code ViolationDetailsTile.jsx} line ~221). Without the
   * direct-child anchor this selector matches two titles and triggers a strict-mode violation.
   */
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

  /**
   * Component display in the subtitle, formatted as {@code <group> : <artifact> : <version>}.
   * Lives in the third subtitle "part" rendered by the violation page header. Anchored under
   * {@code > header.nx-tile-header} for the same reason as {@link #detailsTitle()}.
   */
  public Locator componentName() {
    return locator(
        DETAILS_TILE
            + " > header.nx-tile-header .nx-tile-header__subtitle .iq-violation-details__subtitle-part:nth-child(3)");
  }

  /** Policy name (alias for {@link #detailsTitle()}). */
  public Locator policyName() {
    return detailsTitle();
  }

  // --------------- Constraint section ---------------

  /** Constraint info tile (id-anchored). Kept as a stable alias for {@link #constraintInfo()}. */
  public Locator constraintSection() {
    return constraintInfo();
  }

  public Locator constraintInfoTitle() {
    return locator(CONSTRAINT_INFO + " .nx-tile-header__title");
  }

  /** Conditions / reasons list inside the constraint info tile. */
  public Locator conditionsSection() {
    return locator(CONSTRAINT_INFO + " #policy-violation-reasons");
  }

  public Locator constraintReasons() {
    return locator(CONSTRAINT_INFO + " #policy-violation-reasons li");
  }

  // --------------- Action buttons ---------------

  public Locator manageWaiversButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Manage Waivers"));
  }

  /**
   * "Add Waiver" button. The frontend renders a plain button or an NxSegmentedButton (when the
   * waiver-request workflow is enabled). Both expose "Add Waiver" as the accessible name.
   */
  public Locator addWaiverButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add Waiver"));
  }

  public Locator requestWaiverButton() {
    return container().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Request Waiver"));
  }

  // --------------- Sidebar nav ---------------

  public Locator sidebarNavItems() {
    return locator(ROOT + " #sidebar-nav-list li");
  }

  public Locator sidebarNavItem(int index) {
    return locator(ROOT + " #sidebar-nav-list li:nth-child(" + (index + 1) + ")");
  }

  // --------------- Semantic actions and queries ---------------

  public void openSecurityTab() {
    securityTab().click();
    assertThat(securityDetailsTile()).isVisible();
  }
}
