/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright page object for the Firewall Component Details page.
 */
public class FirewallComponentDetailsPage
    extends BasePage
{
  private static final String ROOT = "#firewall-component-details-page";

  private static final String SECURITY_TAB_ID = "security";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public FirewallComponentDetailsPage() {
    super();
  }

  /**
   * Build the deep-link URL for the Security tab of a {@code ProxyRepositoryComponent}, mirroring
   * the Selenide {@code FirewallComponentDetailsPage#urlSecurityTab}. Used by tests that
   * prefer direct navigation over clicking through the dashboard quarantine row (more
   * deterministic when the click target depends on data-loading order).
   */
  public static String urlSecurityTab(ProxyRepositoryComponent component) {
    return buildComponentDetailsUrl(component, SECURITY_TAB_ID);
  }

  /**
   * Build the deep-link URL for the Policy Violations tab of a {@code ProxyRepositoryComponent}.
   * Used by tests that prefer direct navigation over clicking through the dashboard quarantine
   * row (more deterministic when the click target depends on data-loading order).
   */
  public static String urlViolationsTab(ProxyRepositoryComponent component) {
    return buildComponentDetailsUrl(component, "violations");
  }

  private static String buildComponentDetailsUrl(ProxyRepositoryComponent component, String tabId) {
    try {
      String componentIdentifierJson =
          URLEncoder.encode(OBJECT_MAPPER.writeValueAsString(component.getComponentIdentifier()),
              StandardCharsets.UTF_8.name());
      String pathname = URLEncoder.encode(component.getPathname(), StandardCharsets.UTF_8.name());
      return "/assets/index.html#/firewall/repository/" + component.getRepositoryId()
          + "/component/" + componentIdentifierJson
          + "/" + component.getHash()
          + "/" + component.getMatchStateId()
          + "/" + tabId
          + "?pathname=" + pathname;
    }
    catch (JsonProcessingException | UnsupportedEncodingException e) {
      throw new IllegalStateException("Failed to build component details URL", e);
    }
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator title() {
    return locator("#component-details-title");
  }

  public Locator reevaluateButton() {
    return locator(ROOT + " #firewall-component-details-page__reevaluate-button");
  }

  public Locator formatTag() {
    return locator(ROOT + " .iq-component-format-tag");
  }

  public Locator tabs() {
    return locator(ROOT + " .nx-tab");
  }

  public Locator loadingSpinner() {
    return locator(ROOT + " .nx-loading-spinner");
  }

  public Locator componentInfoTile() {
    return locator(ROOT + " .iq-component-information-tile");
  }

  public Locator componentInfoItems() {
    return locator(ROOT + " .nx-read-only__item .nx-read-only__data");
  }

  /**
   * Read the value cell of the {@link FirewallOverviewComponentInformation} tile by its
   * label, e.g. {@code componentInfoValueByLabel("Match State")}. Survives DOM-order changes
   * to the tile's identification info list (skill §4a).
   */
  public Locator componentInfoValueByLabel(String label) {
    return locator(ROOT + " #firewall-overview-component-information-tile"
        + " .nx-read-only__item:has(.nx-read-only__label:text-is(\"" + label + "\"))"
        + " .nx-read-only__data");
  }

  /** "Visit Project Website" link inside the Component Information tile (rendered when website is set). */
  public Locator componentInfoWebsiteLink() {
    return locator(ROOT + " #firewall-overview-component-information-tile"
        + " .iq-identification-info-definition-list__website-link");
  }

  public Locator coordinatesButton() {
    return locator(ROOT + " .component-coordinates-button");
  }

  public Locator coordinatesPopover() {
    return locator("#iq-component-coordinates-popover");
  }

  public Locator coordinatesPopoverClose() {
    return locator("#iq-component-coordinates-popover-close-btn");
  }

  /**
   * Read the value cell of the Component Coordinates popover by its label, e.g.
   * {@code coordinatesPopoverValueByLabel("groupId")}. Strict-mode-safe — selects the
   * single {@code .nx-read-only__data} sibling of a label whose text exactly matches.
   */
  public Locator coordinatesPopoverValueByLabel(String label) {
    return locator("#iq-component-coordinates-popover"
        + " .nx-read-only__item:has(.nx-read-only__label:text-is(\"" + label + "\"))"
        + " .nx-read-only__data");
  }

  /** Open the Component Coordinates popover and wait for it to render. */
  public void openCoordinatesPopover() {
    coordinatesButton().click();
    assertThat(coordinatesPopover()).isVisible();
  }

  /** Close the Component Coordinates popover and wait for it to detach. */
  public void closeCoordinatesPopover() {
    coordinatesPopoverClose().click();
    assertThat(coordinatesPopover()).isHidden();
  }

  public Locator componentOverviewTile() {
    return locator(ROOT + " .iq-quarantine-report-component-overview-tile");
  }

  // Violations tab
  public Locator violationsTabContent() {
    return locator("#component-details-violations-tab-content");
  }

  public Locator violationsTabTitle() {
    return locator("#component-details-violations-tab-content .nx-tile-header__title");
  }

  public Locator policyViolationsTable() {
    return locator(".firewall-policy-violation-table");
  }

  public Locator policyViolationRows() {
    return locator(".firewall-policy-violation-table tbody > tr");
  }

  // Security tab
  public Locator securityTabContent() {
    return locator("#component-details-security-tab-content");
  }

  public Locator vulnerabilitiesTable() {
    return locator(ROOT + " .iq-policy-vulnerability-table");
  }

  public Locator vulnerabilityRows() {
    return locator(ROOT + " .iq-policy-vulnerability-table .iq-vulnerabilities-row");
  }

  /** Cell <em>n</em> (1-indexed) of vulnerability row {@code rowIndex} (1-indexed). */
  public Locator vulnerabilityRowCell(int rowIndex, int cellIndex) {
    return locator(ROOT + " .iq-policy-vulnerability-table tbody > tr:nth-child(" + rowIndex
        + ") .nx-cell:nth-child(" + cellIndex + ")");
  }

  /**
   * The Security-tab policy-violations table. Disambiguated by the
   * {@code .iq-policy-violations-table} class because the Security tab also renders a
   * vulnerabilities table with class {@code .iq-policy-vulnerability-table} — selecting on
   * the generic {@code .nx-table} would match both and trip Playwright strict mode.
   */
  public Locator securityTabPolicyViolationsTable() {
    return locator("#component-details-security-tab-content .iq-policy-violations-table");
  }

  public Locator securityTabPolicyViolationRows() {
    return locator("#component-details-security-tab-content .iq-policy-violations-table tbody > tr");
  }

  // Vulnerability details popover (opened by clicking a row in the vulnerabilities table)
  public Locator vulnerabilityDetailsPopover() {
    return locator("#component-details-vulnerability-details-popover");
  }

  public Locator vulnerabilityDetailsPopoverTitle() {
    return locator("#component-details-vulnerability-details-popover .iq-vulnerability-details"
        + " .iq-vulnerability-details__vulnerability-id-and-conditional-labels"
        + " .iq-vulnerability-details__vulnerability-id");
  }

  public Locator vulnerabilityDetailsPopoverCloseButton() {
    return locator("#vulnerability-close-btn");
  }

  /**
   * The "Security" tab in the RSC {@code NxTabs} list. Selected by accessible role + text per the
   * test-authoring skill §4a (strict-mode-safe and survives DOM-order changes).
   */
  public Locator securityTab() {
    return page.getByRole(AriaRole.TAB)
        .filter(new Locator.FilterOptions().setHasText("Security"));
  }

  /** Click the Security tab and wait for its panel to render. */
  public void selectSecurityTab() {
    securityTab().click();
    assertThat(securityTabContent()).isVisible();
  }

  /** Click vulnerability row {@code rowIndex} (1-indexed) and wait for the details popover. */
  public void openVulnerabilityDetailsPopover(int rowIndex) {
    if (rowIndex < 1) {
      throw new IllegalArgumentException("rowIndex must be >= 1, got " + rowIndex);
    }
    // Web-first assertions auto-retry (no test-side polling loops).
    // Re-evaluate sits inside the header NxLoadWrapper and only appears after isLoadingComponentDetails
    // clears — avoids acting while the page still shows "Loading…".
    assertThat(reevaluateButton())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
    // Data rows only (.iq-vulnerabilities-row); tbody can contain loading/empty rows without this class.
    Locator row = vulnerabilityRows().nth(rowIndex - 1);
    assertThat(row)
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
    // Do not call scrollIntoViewIfNeeded here: the table can re-render while scrolling and
    // Playwright throws "Element is not attached to the DOM". Force-click skips the scroll step.
    row.click(new Locator.ClickOptions()
        .setForce(true)
        .setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
    assertThat(vulnerabilityDetailsPopover())
        .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS));
  }

  // Labels tab
  public Locator labelsTabContent() {
    return locator("#component-details-labels-tab-content");
  }

  // Waivers
  public Locator waiversView() {
    return locator("#firewall-details-view-waivers");
  }

  public Locator deleteWaiverButton() {
    return locator(".list-waivers-row__delete-btn");
  }

  public Locator deleteWaiverModal() {
    return locator("#delete-waiver-modal");
  }

  public Locator deleteWaiverModalContinue() {
    return locator("#delete-waiver-modal-continue-button");
  }
}
